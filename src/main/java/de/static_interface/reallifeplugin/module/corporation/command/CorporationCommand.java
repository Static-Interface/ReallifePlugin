/*
 * Copyright (c) 2013 - 2014 <http://static-interface.de> and contributors
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.static_interface.reallifeplugin.module.corporation.command;

import static de.static_interface.reallifeplugin.config.ReallifeLanguageConfiguration.m;

import de.static_interface.reallifeplugin.config.ReallifeLanguageConfiguration;
import de.static_interface.reallifeplugin.database.permission.Permission;
import de.static_interface.reallifeplugin.module.ModuleCommand;
import de.static_interface.reallifeplugin.module.corporation.Corporation;
import de.static_interface.reallifeplugin.module.corporation.CorporationInviteQueue;
import de.static_interface.reallifeplugin.module.corporation.CorporationManager;
import de.static_interface.reallifeplugin.module.corporation.CorporationModule;
import de.static_interface.reallifeplugin.module.corporation.CorporationOptions;
import de.static_interface.reallifeplugin.module.corporation.CorporationPermissions;
import de.static_interface.reallifeplugin.module.corporation.database.row.CorpRank;
import de.static_interface.reallifeplugin.module.corporation.database.row.CorpTradesRow;
import de.static_interface.reallifeplugin.module.corporation.database.row.CorpUserRow;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpRankPermissionsTable;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpRanksTable;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpTradesTable;
import de.static_interface.reallifeplugin.util.CommandUtil;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.exception.NotEnoughArgumentsException;
import de.static_interface.sinklibrary.api.exception.UserNotFoundException;
import de.static_interface.sinklibrary.api.exception.UserNotOnlineException;
import de.static_interface.sinklibrary.api.user.SinkUser;
import de.static_interface.sinklibrary.configuration.LanguageConfiguration;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.user.IrcUser;
import de.static_interface.sinklibrary.util.MathUtil;
import de.static_interface.sinklibrary.util.StringUtil;
import de.static_interface.sinklibrary.util.VaultBridge;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CorporationCommand extends ModuleCommand<CorporationModule> {

    public CorporationCommand(CorporationModule module) {
        super(module);
        getCommandOptions().setPlayerOnly(true);
        getCommandOptions().setCmdLineSyntax("{PREFIX}{ALIAS} <options> [-c <corp>] [-f]");
        getCommandOptions().setCliOptions(new Options());
        Option corpOption = Option.builder("c")
                .hasArg()
                .longOpt("corp")
                .desc("Force as given corporation member")
                .type(String.class)
                .argName("corp name")
                .build();
        Option forceOption = Option.builder("f")
                .longOpt("force")
                .desc("Force command, even if you don't have permission for it")
                .build();
        getCommandOptions().getCliOptions().addOption(corpOption);
        getCommandOptions().getCliOptions().addOption(forceOption);
    }

    @Override
    public boolean onExecute(CommandSender sender, String label, String[] args) {
        SinkUser user = SinkLibrary.getInstance().getUser(sender);
        Corporation userCorp = null;
        if (user instanceof IngameUser) {
            userCorp = CorporationManager.getInstance().getUserCorporation((IngameUser) user);
        }

        boolean isExplicitForceMode = sender.hasPermission("reallifeplugin.corporations.forcecommand") && getCommandLine().hasOption('f');

        boolean isForceMode = isExplicitForceMode;
        if (sender.hasPermission("reallifeplugin.corporations.admin") && getCommandLine().hasOption('c')) {
            isForceMode = true;
            userCorp = CorporationManager.getInstance().getCorporation(getCommandLine().getOptionValue('c'));
            if (userCorp == null) {
                sender.sendMessage(ReallifeLanguageConfiguration.m("Corporation.CorporationNotFound", getCommandLine().getOptionValue('c')));
                return true;
            }
        }

        if (args.length < 1 && user instanceof IngameUser) {
            if (userCorp != null) {
                sendCorporationInfo(user, userCorp);
                return true;
            }
            user.sendMessage(m("Corporation.NotInCorporation"));
            return true;
        } else if (args.length < 1 && !(user instanceof IngameUser)) {
            return false;
        }

        List<String> tmp = new ArrayList<>(Arrays.asList(args));
        tmp.remove(args[0]);
        String[] moreArgs = tmp.toArray(new String[tmp.size()]);

        switch (args[0].toLowerCase()) {
            case "?":
            case "help":
                sendHelp(user);
                break;

            case "user": {
                IngameUser target = (IngameUser) user;
                if (args.length > 1) {
                    target = SinkLibrary.getInstance().getIngameUser(args[1]);
                    if (!target.hasPlayedBefore()) {
                        throw new UserNotFoundException(args[1]);
                    }
                }

                sendUserInfo(user, target);
                break;
            }

            case "admin": {
                if (user instanceof IngameUser || (user instanceof IrcUser && user.isOp())) {
                    if (!user.hasPermission("reallifeplugin.corporations.admin")) {
                        user.sendMessage(LanguageConfiguration.m("Permissions.General"));
                        break;
                    }
                    if (moreArgs.length < 1) {
                        throw new NotEnoughArgumentsException();
                    }
                    handleAdminCommand(user, moreArgs);
                    break;
                }
            }

            case "list": {
                boolean listByMoney = false;
                if (args.length > 1) {
                    listByMoney = args[1].equalsIgnoreCase("money");
                }
                sendCorporationsList(user, listByMoney);
                break;
            }

            case "leave": {
                if (user instanceof IngameUser) {
                    if (userCorp == null) {
                        user.sendMessage(m("Corporation.NotInCorporation"));
                        break;
                    }

                    userCorp.removeMember((IngameUser) user);

                    userCorp.announce(StringUtil.format(m("Corporation.UserLeftCorporation"), user, null, null));
                    user.sendMessage(m("Corporation.LeftCorporation"));
                    break;
                }
            }

            case "deposit": {
                if (!isForceMode && !CorporationManager.getInstance().hasCorpPermission((IngameUser) user, CorporationPermissions.DEPOSIT)) {
                    sender.sendMessage(LanguageConfiguration.m("Permissions.General"));
                    break;
                }
                if (!(user instanceof IngameUser)) {
                    break;
                }

                if (args.length < 2) {
                    throw new NotEnoughArgumentsException();
                }
                try {
                    if (userCorp == null) {
                        user.sendMessage(m("Corporation.NotInCorporation"));
                        break;
                    }
                    deposit((IngameUser) user, userCorp, Double.valueOf(args[1]));
                } catch (NumberFormatException ignored) {
                    user.sendMessage(m("General.InvalidValue", args[1]));
                }
                break;
            }

            case "join": {
                if (args.length < 2) {
                    throw new NotEnoughArgumentsException();
                }
                if (userCorp != null) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Corporation.AlreadyInCorporation", userCorp.getFormattedName()));
                    break;
                }

                Corporation corp = CorporationManager.getInstance().getCorporation(args[1]);
                if (corp == null) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Corporation.CorporationNotFound", args[1]));
                    break;
                }

                if (!corp.isPublic() && !CorporationInviteQueue.hasInvite(((IngameUser) user).getUniqueId(), corp) && !isExplicitForceMode) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Corporation.NotInvited", corp.getFormattedName()));
                    break;
                }

                if (corp.getMemberLimit() > 0 && corp.getMembers().size() >= (corp.getMemberLimit() + 1)) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Corporation.CorporationFull"));
                    break;
                }

                corp.addMember((IngameUser) user, corp.getDefaultRank());
                corp.announce(ReallifeLanguageConfiguration.m("Corporation.Joined", ((Player) sender).getDisplayName()));
                CorporationInviteQueue.remove(((IngameUser) user).getUniqueId(), corp);
                break;
            }

            case "rank":
                if (userCorp == null) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Corporation.NotInCorporation"));
                    break;
                }
                handleRankCommand(sender, userCorp, args, isForceMode);
                break;

            case "invite": {
                if (userCorp == null) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Corporation.NotInCorporation"));
                    break;
                }
                if (!isForceMode && !CorporationManager.getInstance().hasCorpPermission((IngameUser) user, CorporationPermissions.KICK)) {
                    sender.sendMessage(LanguageConfiguration.m("Permissions.General"));
                    break;
                }
                if (args.length < 2) {
                    throw new NotEnoughArgumentsException();
                }

                if (userCorp.getMemberLimit() > 0 && userCorp.getMembers().size() >= (userCorp.getMemberLimit() + 1)) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Corporation.CorporationFull"));
                    break;
                }

                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[1], true);
                if (!isExplicitForceMode && !target.isOnline()) {
                    throw new UserNotOnlineException(target.getDisplayName());
                }

                if (CorporationInviteQueue.hasInvite(target.getUniqueId(), userCorp)) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Corporation.AlreadyInvited", target.getDisplayName()));
                    break;
                }

                if (!isExplicitForceMode) {
                    CorporationInviteQueue.add(target.getUniqueId(), userCorp);
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Corporation.UserHasBeenInvited", target.getDisplayName()));
                    target.sendMessage(
                            ReallifeLanguageConfiguration
                                    .m("Corporation.GotInvited", user.getDisplayName(), userCorp.getFormattedName(), userCorp.getName()));
                    break;
                }

                Corporation corp = CorporationManager.getInstance().getUserCorporation(target);
                if (corp != null) {
                    sender.sendMessage(ReallifeLanguageConfiguration
                                               .m("Corporation.UserAlreadyInCorporation", target.getDisplayName(), corp.getFormattedName(),
                                                  corp.getName()));
                    break;
                }

                userCorp.addMember(target, userCorp.getDefaultRank());
                sender.sendMessage(ReallifeLanguageConfiguration.m("General.Success"));
                break;
            }

            case "kick": {
                if (!isForceMode && !CorporationManager.getInstance().hasCorpPermission((IngameUser) user, CorporationPermissions.INVITE)) {
                    sender.sendMessage(LanguageConfiguration.m("Permissions.General"));
                    break;
                }
                if (args.length < 2) {
                    throw new NotEnoughArgumentsException();
                }

                boolean success = false;
                for (IngameUser u : userCorp.getMembers()) {
                    if (ChatColor.stripColor(u.getDisplayName()).equalsIgnoreCase(args[1])
                        || u.getName().equalsIgnoreCase(args[1])) {

                        if (user instanceof IngameUser && userCorp.getRank(u).priority >= userCorp.getRank((IngameUser) user).priority
                            && !isForceMode) {
                            user.sendMessage(m("Corporation.NotEnoughPriority"));
                            return true;
                        }
                        userCorp.removeMember(u);
                        if (u.isOnline()) {
                            u.sendMessage(StringUtil.format(m("Corporation.Kicked"), userCorp.getName()));
                            user.sendMessage(StringUtil.format(m("Corporation.CEOKicked"), CorporationManager.getInstance().getFormattedName(u)));
                        }
                        success = true;
                        break;
                    }
                }
                if (!success) {
                    user.sendMessage(StringUtil.format(m("Corporation.NotMember"), args[1]));
                    break;
                }

                break;
            }

            case "delete": {
                if (!isForceMode && !CorporationManager.getInstance().hasCorpPermission((IngameUser) user, CorporationPermissions.DELETE)) {
                    sender.sendMessage(LanguageConfiguration.m("Permissions.General"));
                    break;
                }
                if (args.length < 2) {
                    throw new NotEnoughArgumentsException();
                }

                Corporation corp = CorporationManager.getInstance().getCorporation(args[1]);
                boolean successful = CorporationManager.getInstance().deleteCorporation(user, corp);

                if (successful) {
                    user.sendMessage(StringUtil.format(m("Corporation.Deleted"), corp.getFormattedName()));
                }
                break;
            }

            case "withdraw":
                if (!isForceMode && !CorporationManager.getInstance().hasCorpPermission((IngameUser) user, CorporationPermissions.WITHDRAW)) {
                    sender.sendMessage(LanguageConfiguration.m("Permissions.General"));
                    break;
                }
                if (args.length < 2) {
                    throw new NotEnoughArgumentsException();
                }
                if (!(user instanceof IngameUser)) {
                    break;
                }
                try {
                    withdraw((IngameUser) user, userCorp, Double.valueOf(args[1]));
                } catch (NumberFormatException ignored) {
                    user.sendMessage(m("General.InvalidValue", args[1]));
                }
                break;

            default: {
                Corporation corporation = CorporationManager.getInstance().getCorporation(args[0]);
                sendCorporationInfo(user, corporation);
                break;
            }
        }

        return true;
    }

    private void handleRankCommand(CommandSender sender, Corporation corp, String[] args, boolean isForceMode) {
        if (!(sender instanceof Player)) {
            return;
        }

        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }

        IngameUser user = SinkLibrary.getInstance().getIngameUser((Player) sender);

        switch (args[1].toLowerCase().trim()) {
            case "permissionslist": {
                CorpRank rank;
                if (args.length >= 3) {
                    rank = handleRank(sender, corp, args[2], false);
                    if (rank == null) {
                        break;
                    }
                } else {
                    if (isForceMode) {
                        throw new NotEnoughArgumentsException();
                    }
                    rank = corp.getRank(SinkLibrary.getInstance().getIngameUser((Player) sender));
                }
                sender.sendMessage(
                        ChatColor.GRAY + "Permissions: " + ChatColor.GOLD + rank.name + ChatColor.GRAY + " (" + ChatColor.GOLD + rank.priority
                        + ChatColor.GRAY + ")");
                for (Permission perm : CorporationPermissions.getInstance().getPermissions()) {
                    Object
                            value =
                            getModule().getTable(CorpRankPermissionsTable.class)
                                    .getOption(perm.getPermissionString(), rank.id, perm.getValueType(), perm.getDefaultValue());
                    if (CorporationManager.getInstance().hasCorpPermission(rank, CorporationPermissions.ALL) && value == Boolean.TRUE) {
                        value = true;
                    }
                    sender.sendMessage("• " + ChatColor.GOLD + perm.getPermissionString() + ": " + ChatColor.GRAY + perm.getDescription() + " ("
                                       + (CorporationManager.getInstance().hasCorpPermission(rank, perm) ? ChatColor.DARK_GREEN : ChatColor.DARK_RED)
                                       + "value: " + Objects.toString(value) + ChatColor.GRAY + ")");
                }
                break;
            }

            case "list": {
                sender.sendMessage(ChatColor.GOLD + corp.getFormattedName() + ChatColor.GRAY + "'s Ranks:");
                for (CorpRank rank : corp.getRanks()) {
                    String defaultText = ChatColor.GRAY + "";
                    int defaultRankId = corp.getDefaultRank().id;
                    if (defaultRankId == rank.id) {
                        defaultText += ChatColor.BOLD + " (default)" + ChatColor.RESET + ChatColor.GRAY;
                    }
                    sender.sendMessage(
                            "• " + ChatColor.GOLD + rank.name + defaultText + " (" + ChatColor.GOLD + rank.priority + ChatColor.GRAY + ")" + (
                                    StringUtil.isEmptyOrNull(rank.description) ? "" : ": " + rank.description));
                }
                break;
            }

            case "delete": {
                if (!isForceMode && !CorporationManager.getInstance().hasCorpPermission(user, CorporationPermissions.MANAGE_RANKS)) {
                    sender.sendMessage(LanguageConfiguration.m("Permissions.General"));
                    break;
                }
                if (args.length < 3) {
                    throw new NotEnoughArgumentsException();
                }

                CorpRank rank = handleRank(sender, corp, args[2], !isForceMode);
                if (rank == null) {
                    break;
                }

                int defaultRankId = corp.getDefaultRank().id;
                if (defaultRankId == rank.id) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Corporation.DeletingDefaultRank"));
                    break;
                }

                for (CorpUserRow row : corp.getRankUsers(rank)) {
                    IngameUser rankUser = SinkLibrary.getInstance().getIngameUser(UUID.fromString(row.uuid));
                    corp.setRank(rankUser, corp.getDefaultRank());
                }

                getModule().getTable(CorpRanksTable.class).executeUpdate("DELETE FROM `{TABLE}` WHERE `id` = ?", rank.id);
                sender.sendMessage(ReallifeLanguageConfiguration.m("General.Success"));
                break;
            }

            case "new": {
                if (!isForceMode && !CorporationManager.getInstance().hasCorpPermission(user, CorporationPermissions.MANAGE_RANKS)) {
                    sender.sendMessage(LanguageConfiguration.m("Permissions.General"));
                    break;
                }
                if (args.length < 4) {
                    throw new NotEnoughArgumentsException();
                }

                String name = args[2];

                int priortiy;
                try {
                    priortiy = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Invalid number: " + args[3]);
                    break;
                }

                if (!isForceMode) {
                    CorpRank userRank = corp.getRank(SinkLibrary.getInstance().getIngameUser(((Player) sender)));

                    if (priortiy < userRank.priority) {
                        sender.sendMessage(ReallifeLanguageConfiguration.m("Corporation.NotEnoughPriority"));
                        break;
                    }
                }
                CorpRank rank = new CorpRank();
                rank.name = name;
                rank.prefix = ChatColor.GOLD.toString();
                rank.description = null;
                rank.priority = priortiy;
                rank.corpId = corp.getId();

                getModule().getTable(CorpRanksTable.class).insert(rank);

                sender.sendMessage(ReallifeLanguageConfiguration.m("General.Success"));
                break;
            }

            case "priority": {
                if (!isForceMode && !CorporationManager.getInstance().hasCorpPermission(user, CorporationPermissions.MANAGE_RANKS)) {
                    sender.sendMessage(LanguageConfiguration.m("Permissions.General"));
                    break;
                }
                if (args.length < 4) {
                    throw new NotEnoughArgumentsException();
                }

                CorpRank rank = handleRank(sender, corp, args[2], !isForceMode);
                if (rank == null) {
                    break;
                }

                int priortiy;
                try {
                    priortiy = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Invalid number: " + args[3]);
                    break;
                }

                try {
                    getModule().getTable(CorpRanksTable.class).executeUpdate("UPDATE `{TABLE}` SET `priority`=? WHERE `id` = ?", priortiy, rank.id);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                sender.sendMessage(ReallifeLanguageConfiguration.m("General.Success"));
                break;
            }

            case "prefix": {
                if (!isForceMode && !CorporationManager.getInstance().hasCorpPermission(user, CorporationPermissions.MANAGE_RANKS)) {
                    sender.sendMessage(LanguageConfiguration.m("Permissions.General"));
                    break;
                }
                if (args.length < 4) {
                    throw new NotEnoughArgumentsException();
                }
                CorpRank rank = handleRank(sender, corp, args[2], !isForceMode);
                if (rank == null) {
                    break;
                }

                String prefix = ChatColor.translateAlternateColorCodes('&', StringUtil.formatArrayToString(args, " ", 3, args.length));
                try {
                    getModule().getTable(CorpRanksTable.class).executeUpdate("UPDATE `{TABLE}` SET `prefix`=? WHERE `id` = ?", prefix, rank.id);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                sender.sendMessage(ReallifeLanguageConfiguration.m("General.SuccessSet", prefix));
                break;
            }

            case "description": {
                if (!isForceMode && !CorporationManager.getInstance().hasCorpPermission(user, CorporationPermissions.MANAGE_RANKS)) {
                    sender.sendMessage(LanguageConfiguration.m("Permissions.General"));
                    break;
                }
                if (args.length < 4) {
                    throw new NotEnoughArgumentsException();
                }

                CorpRank rank = handleRank(sender, corp, args[2], !isForceMode);
                if (rank == null) {
                    break;
                }

                String description = ChatColor.translateAlternateColorCodes('&', StringUtil.formatArrayToString(args, " ", 3, args.length));
                if (description.trim().equalsIgnoreCase("null") || description.trim().equalsIgnoreCase("off") || description.trim()
                        .equalsIgnoreCase("remove")) {
                    description = null;
                }

                try {
                    getModule().getTable(CorpRanksTable.class)
                            .executeUpdate("UPDATE `{TABLE}` SET `description`=? WHERE `id` = ?", description, rank.id);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                sender.sendMessage(ReallifeLanguageConfiguration.m("General.SuccessSet", description));
                break;
            }

            case "rename": {
                if (!isForceMode && !CorporationManager.getInstance().hasCorpPermission(user, CorporationPermissions.MANAGE_RANKS)) {
                    sender.sendMessage(LanguageConfiguration.m("Permissions.General"));
                    break;
                }
                if (args.length < 4) {
                    throw new NotEnoughArgumentsException();
                }

                CorpRank rank = handleRank(sender, corp, args[2], !isForceMode);
                if (rank == null) {
                    break;
                }

                try {
                    getModule().getTable(CorpRanksTable.class)
                            .executeUpdate("UPDATE `{TABLE}` SET `name`=? WHERE `id` = ?", args[3], rank.id);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                sender.sendMessage(ReallifeLanguageConfiguration.m("General.SuccessSet", args[3]));
                break;
            }

            case "set": {
                if (!isForceMode && !CorporationManager.getInstance().hasCorpPermission(user, CorporationPermissions.SET_RANK)) {
                    sender.sendMessage(LanguageConfiguration.m("Permissions.General"));
                    break;
                }
                if (args.length < 4) {
                    throw new NotEnoughArgumentsException();
                }

                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[2]);
                if (!corp.isMember(target)) {
                    sender.sendMessage(
                            ReallifeLanguageConfiguration.m("Corporation.UserNotMember", target.getDisplayName(), corp.getFormattedName()));
                    break;
                }

                CorpRank rank = handleRank(sender, corp, args[3], !isForceMode);
                if (rank == null) {
                    break;
                }

                corp.setRank(target, rank);
                sender.sendMessage(ReallifeLanguageConfiguration.m("General.SuccessSet", rank.name));
                break;
            }

            case "setpermission":
                if (!isForceMode && !CorporationManager.getInstance().hasCorpPermission(user, CorporationPermissions.MANAGE_RANKS)) {
                    sender.sendMessage(LanguageConfiguration.m("Permissions.General"));
                    break;
                }
                if (args.length < 5) {
                    throw new NotEnoughArgumentsException();
                }

                CorpRank rank = handleRank(sender, corp, args[2], !isForceMode);
                if (rank == null) {
                    break;
                }
                Permission permission = CorporationPermissions.getInstance().getPermission(args[3].toUpperCase());
                if (permission == null) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Corporation.UnknownPermission", args[3].toUpperCase()));
                    break;
                }

                if (!isForceMode && !CorporationManager.getInstance().hasCorpPermission(user, permission)) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Corporation.NotEnoughPriority"));
                    break;
                }

                String[] valueArgs = new String[args.length - 4];
                int valuePosition = 0;
                for (int i = 4; i < args.length; i++) {
                    valueArgs[valuePosition] = args[i];
                    valuePosition++;
                }
                Object value = CommandUtil.parseValue(valueArgs);
                getModule().getTable(CorpRankPermissionsTable.class).setOption(permission.getPermissionString(), value, rank.id);

                for (CorpUserRow row : corp.getRankUsers(rank)) {
                    IngameUser rankUser = SinkLibrary.getInstance().getIngameUser(UUID.fromString(row.uuid));
                    corp.onUpdateRank(rankUser, rank);
                }

                sender.sendMessage(ReallifeLanguageConfiguration.m("General.SuccessSet", Objects.toString(value)));
                break;

            default:
                sender.sendMessage("Unknown subcommand: " + args[1]);
                break;
        }
    }

    private void deposit(IngameUser user, Corporation corp, double amount) {
        if (amount < 1) {
            user.sendMessage(m("General.InvalidValue", amount));
            return;
        }

        if (VaultBridge.getBalance(user.getPlayer()) < amount) {
            user.sendMessage(m("General.NotEnoughMoney"));
            return;
        }

        VaultBridge.addBalance(user.getPlayer(), -amount);
        corp.addBalance(amount);

        corp.announce(m("Corporation.Deposit", user.getDisplayName(), amount));
    }

    private void withdraw(IngameUser user, Corporation corp, double amount) {
        if (amount < 1) {
            user.sendMessage(m("General.InvalidValue", amount));
            return;
        }

        if (corp.getBalance() < amount) {
            user.sendMessage(m("Corporation.NotEnoughMoney"));
            return;
        }

        VaultBridge.addBalance(user.getPlayer(), amount);
        corp.addBalance(-amount);

        corp.announce(m("Corporation.Withdraw", user.getDisplayName(), amount));
    }

    private void handleAdminCommand(SinkUser user, String[] args) {
        switch (args[0].toLowerCase()) {
            case "?":
            case "help":
                sendAdminHelp(user);
                break;

            case "new": {
                if ((user instanceof IngameUser && args.length < 4) || (!(user instanceof IngameUser) && args.length < 5)) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }
                //Todo: remove ceo_user_id from any corporation
                //Todo cancel if ceo_user_id is already a ceo_user_id of another corporation

                String name = args[1];
                String base = args[3];
                World world;
                if (user instanceof IngameUser) {
                    world = ((IngameUser) user).getPlayer().getWorld();
                } else {
                    world = Bukkit.getWorld(args[4]);
                }

                boolean successful = CorporationManager.getInstance().createCorporation(getModule(), user, name, args[2], base, world);
                Corporation corp = CorporationManager.getInstance().getCorporation(name);
                String msg = successful ? m("Corporation.Created") : m("Corporation.CreationFailed");
                msg = StringUtil.format(msg, corp.getFormattedName());
                user.sendMessage(msg);
                break;
            }

            case "setmemberlimit": {
                if (args.length < 3) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }

                Corporation corporation = CorporationManager.getInstance().getCorporation(args[1]);
                if (corporation == null) {
                    user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[1]));
                    return;
                }

                int limit = Integer.valueOf(args[2]);
                corporation.setMemberLimit(limit);
                sender.sendMessage(ReallifeLanguageConfiguration.m("General.SuccessSet"));
                break;
            }

            case "enablefishing": {
                if (args.length < 2) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }

                Corporation corporation = CorporationManager.getInstance().getCorporation(args[1]);
                if (corporation == null) {
                    user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[1]));
                    return;
                }

                corporation.setOption(CorporationOptions.FISHING, true);
                sender.sendMessage(m("General.Success"));
                break;
            }

            case "disablefishing": {
                if (args.length < 2) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }

                Corporation corporation = CorporationManager.getInstance().getCorporation(args[1]);
                if (corporation == null) {
                    user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[1]));
                    return;
                }

                corporation.setOption(CorporationOptions.FISHING, false);
                sender.sendMessage(m("General.Success"));
                break;
            }

            case "getmemberlimit": {
                if (args.length < 2) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }

                Corporation corporation = CorporationManager.getInstance().getCorporation(args[1]);
                if (corporation == null) {
                    user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[1]));
                    return;
                }

                sender.sendMessage("Limit: " + corporation.getMemberLimit());
                break;
            }

            case "restrictblocks": {
                if (args.length < 4) {
                    throw new NotEnoughArgumentsException();
                }

                Corporation corp = CorporationManager.getInstance().getCorporation(args[2]);
                if (corp == null) {
                    user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[2]));
                    return;
                }

                List<String>
                        restrictedBlocks =
                        corp.getOption(CorporationOptions.RESTRICTED_BLOCKS, ArrayList.class, new ArrayList<String>());

                switch (args[1].toLowerCase()) {
                    case "add": {
                        String name = args[3];
                        if (!name.equalsIgnoreCase("all")) {
                            name = Material.getMaterial(args[3]).name();
                        }
                        restrictedBlocks.add(name.toUpperCase());
                        corp.setOption(CorporationOptions.RESTRICTED_BLOCKS, restrictedBlocks);
                        user.sendMessage(ReallifeLanguageConfiguration.m("General.Success"));
                        break;
                    }

                    case "remove": {
                        String name = args[3];
                        if (!name.equalsIgnoreCase("all")) {
                            name = Material.getMaterial(args[3]).name();
                        }
                        restrictedBlocks.remove(name.toUpperCase());
                        corp.setOption(CorporationOptions.RESTRICTED_BLOCKS, restrictedBlocks);
                        user.sendMessage(ReallifeLanguageConfiguration.m("General.Success"));
                        break;
                    }

                    case "list":
                        user.sendMessage("Restricted Blocks: " + StringUtil
                                .formatArrayToString(restrictedBlocks.toArray(new String[restrictedBlocks.size()]), ", "));
                        break;

                    default:
                        user.sendMessage(m("General.UnknownSubCommand", args[1]));
                        break;
                }
            }

            case "allowedblocks": {
                if (args.length < 4) {
                    throw new NotEnoughArgumentsException();
                }

                Corporation corp = CorporationManager.getInstance().getCorporation(args[2]);
                if (corp == null) {
                    user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[2]));
                    return;
                }

                List<String>
                        allowedBlocks =
                        corp.getOption(CorporationOptions.ALLOWED_BLOCKS, ArrayList.class, new ArrayList<String>());

                switch (args[1].toLowerCase()) {
                    case "add": {
                        String name = args[3];
                        if (!name.equalsIgnoreCase("all")) {
                            name = Material.getMaterial(args[3]).name();
                        }
                        allowedBlocks.add(name.toUpperCase());
                        corp.setOption(CorporationOptions.ALLOWED_BLOCKS, allowedBlocks);
                        user.sendMessage(ReallifeLanguageConfiguration.m("General.Success"));
                        break;
                    }

                    case "remove": {
                        String name = args[3];
                        if (!name.equalsIgnoreCase("all")) {
                            name = Material.getMaterial(args[3]).name();
                        }
                        allowedBlocks.remove(name.toUpperCase());
                        corp.setOption(CorporationOptions.ALLOWED_BLOCKS, allowedBlocks);
                        user.sendMessage(ReallifeLanguageConfiguration.m("General.Success"));
                        break;
                    }

                    case "list":
                        user.sendMessage(
                                "Allowed Blocks: " + StringUtil.formatArrayToString(allowedBlocks.toArray(new String[allowedBlocks.size()]), ", "));
                        break;

                    default:
                        user.sendMessage(m("General.UnknownSubCommand", args[1]));
                        break;
                }
            }

            case "setbase": {
                if (args.length < 3 || (!(user instanceof IngameUser) && args.length < 4)) {
                    throw new NotEnoughArgumentsException();
                }

                Corporation corporation = CorporationManager.getInstance().getCorporation(args[1]);

                World world;
                if (user instanceof IngameUser) {
                    world = ((IngameUser) user).getPlayer().getWorld();
                } else {
                    world = Bukkit.getWorld(args[3]);
                }

                if (corporation == null) {
                    user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[1]));
                    return;
                }
                corporation.setBase(world, args[2]);
                user.sendMessage(m("Corporation.BaseSet"));
                break;
            }

            case "give": {
                if (args.length < 3) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }
                Corporation corporation = CorporationManager.getInstance().getCorporation(args[1]);
                if (corporation == null) {
                    user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[1]));
                    return;
                }
                double amount;
                try {
                    amount = Double.valueOf(args[2]);
                    if (amount < 1) {
                        user.sendMessage(m("General.InvalidValue", amount));
                        return;
                    }
                } catch (NumberFormatException e) {
                    user.sendMessage(m("General.InvalidValue", args[1]));
                    return;
                }

                if (corporation.addBalance(amount)) {
                    user.sendMessage(m("General.Success"));
                } else {
                    user.sendMessage(ChatColor.DARK_RED + "Failure"); //Todo
                }

                break;
            }

            case "rename": {
                if (args.length < 3) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }

                Corporation corporation = CorporationManager.getInstance().getCorporation(args[1]);
                if (corporation == null) {
                    user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[1]));
                    return;
                }

                if (CorporationManager.getInstance().renameCorporation(user, corporation, args[2])) {
                    user.sendMessage(m("General.Success"));
                }
                break;
            }

            case "take": {
                if (args.length < 3) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }
                Corporation corporation = CorporationManager.getInstance().getCorporation(args[1]);
                if (corporation == null) {
                    user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[1]));
                    return;
                }
                double amount;
                try {
                    amount = Double.valueOf(args[2]);
                    if (amount < 1) {
                        user.sendMessage(m("General.InvalidValue", amount));
                        return;
                    }
                    amount = -amount;
                } catch (NumberFormatException e) {
                    user.sendMessage(m("General.InvalidValue", args[1]));
                    return;
                }
                corporation.addBalance(amount);
                user.sendMessage(m("General.Success"));
                break;
            }

            default: {
                user.sendMessage(m("General.UnknownSubCommand", args[0]));
                break;
            }
        }
    }



    private void sendHelp(SinkUser user) {
        user.sendMessage(ChatColor.RED + "Corp Commands: ");
        user.sendMessage(ChatColor.GOLD + "/corp");
        user.sendMessage(ChatColor.GOLD + "/corp <corp>");
        user.sendMessage(ChatColor.GOLD + "/corp help");
        user.sendMessage(ChatColor.GOLD + "/corp leave");
        user.sendMessage(ChatColor.GOLD + "/corp user <user>");
        user.sendMessage(ChatColor.GOLD + "/corp invite <user>");
        user.sendMessage(ChatColor.GOLD + "/corp list");
        user.sendMessage(ChatColor.GOLD + "/corp deposit <amount>");
        user.sendMessage(ChatColor.GOLD + "/corp kick <user>");
        user.sendMessage(ChatColor.GOLD + "/corp withdraw <amount>");
        user.sendMessage(ChatColor.GOLD + "/corp delete <user>");
        user.sendMessage(ChatColor.GOLD + "/corp rank <subcommand> <options>");

        if (user.hasPermission("reallifeplugin.corporations.admin")) {
            user.sendMessage(ChatColor.GOLD + "/corp admin help");
        }
    }

    private void sendAdminHelp(SinkUser user) {
        user.sendMessage(ChatColor.RED + "Admin Commands: ");
        user.sendMessage(ChatColor.GOLD + "/corp admin new <corp> <ceo> <base>");
        user.sendMessage(ChatColor.GOLD + "/corp admin delete <corp>");
        user.sendMessage(ChatColor.GOLD + "/corp admin setbase <corp> <region>");
        user.sendMessage(ChatColor.GOLD + "/corp admin give <corp> <amount>");
        user.sendMessage(ChatColor.GOLD + "/corp admin take <corp> <amount>");
        user.sendMessage(ChatColor.GOLD + "/corp admin rename <oldname> <newname>");
    }

    private void sendCorporationsList(SinkUser user, boolean listByMoney) {
        user.sendMessage(ChatColor.GOLD + "Corporations: ");
        String msg = "";
        for (Corporation corporation : CorporationManager.getInstance().getCorporations()) {
            int data;
            if (listByMoney) {
                data = (int) corporation.getBalance();
            } else {
                data = corporation.getMembers().size();
            }
            String formattedName = corporation.getFormattedName() + ChatColor.WHITE + "[" +
                                   data + "]" + ChatColor.RESET;
            if (msg.equals("")) {
                msg = formattedName;
                continue;
            }
            msg += " " + formattedName;
        }
        user.sendMessage(msg);
    }


    private void sendUserInfo(SinkUser user, IngameUser target) {
        user.sendMessage("");
        String divider = ChatColor.GOLD + "";
        for (int i = 0; i < 32; i++) {
            divider += "-";
        }
        user.sendMessage("");
        user.sendMessage(ChatColor.GOLD + " User: " + CorporationManager.getInstance().getFormattedName(target));
        user.sendMessage(divider);
        Corporation corp = CorporationManager.getInstance().getUserCorporation(target);
        user.sendMessage(ChatColor.GRAY + "Corporation: " + ChatColor.GOLD + (corp == null ? "-" : corp.getFormattedName()));
        String rank;
        if (corp == null) {
            rank = ChatColor.GOLD + "-";
        } else {
            rank = corp.getRank(target).name;
        }
        user.sendMessage(ChatColor.GRAY + "Rank: " + rank);

        String soldItems = "-";
        Integer userId = CorporationManager.getInstance().getUserId(target);
        if (corp != null) {
            //Todo make time configurable
            int days = 3;
            long maxTime = System.currentTimeMillis() - 1000 * 60 * 60 * 24 * days;
            CorpTradesRow[] rows =
                    getModule().getTable(CorpTradesTable.class).get(
                            "SELECT * FROM `{TABLE}` WHERE `user_id` = ? AND `corp_id` = ? AND `time` > ?", userId,
                            corp.getId(), maxTime);
            if (rows.length > 0) {
                int i = 0;
                for (CorpTradesRow row : rows) {
                    if (row.type != 0) {
                        continue; //wasn't selling items
                    }
                    i += row.changedAmount;
                }
                soldItems = m("Corporation.ItemsSold", target.getDisplayName(), i, days + " " + LanguageConfiguration.m("TimeUnit.Days"));
            }
        }
        user.sendMessage(ChatColor.GRAY + "Items sold: " + ChatColor.GOLD + soldItems);

        user.sendMessage(divider);
        user.sendMessage("");
    }


    private void sendCorporationInfo(SinkUser user, Corporation corporation) {
        if (corporation == null) {
            user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), ""));
            return;
        }

        user.sendMessage("");
        String text = ChatColor.GOLD + " Corporation: " + corporation.getFormattedName();
        String divider = ChatColor.GOLD + "";
        for (int i = 0; i < 32; i++) {
            divider += "-";
        }
        user.sendMessage(ChatColor.RED + text);
        user.sendMessage(divider);

        boolean membersFound = false;
        List<CorpRank> ranks = corporation.getRanks();
        for (CorpRank rank : ranks) {
            List<CorpUserRow> rankUsers = corporation.getRankUsers(rank);
            if (rankUsers.size() < 1) {
                continue;
            }
            membersFound = true;

            String users = "";

            sender.sendMessage("");
            if (!StringUtil.isEmptyOrNull(rank.description)) {
                sender.sendMessage(ChatColor.GOLD + rank.name + ChatColor.GRAY + ": " + rank.description);
            } else {
                sender.sendMessage(ChatColor.GOLD + rank.name);
            }

            for (CorpUserRow rankUser : rankUsers) {
                String
                        formattedName =
                        CorporationManager.getInstance().getFormattedName(SinkLibrary.getInstance().getIngameUser(UUID.fromString(rankUser.uuid)));
                if (users.equals("")) {
                    users = ChatColor.GRAY + "    " + formattedName;
                    continue;
                }

                users += ChatColor.GRAY + ", " + formattedName;
            }

            sender.sendMessage(users);
        }

        sender.sendMessage("");

        if (!membersFound) {
            sender.sendMessage(ChatColor.RED + "Keine Mitglieder gefunden");
        }

        if (corporation.getBaseRegion() != null) {
            user.sendMessage(ChatColor.GRAY + "Base: " + ChatColor.GOLD + corporation.getBaseRegion().getId());
        }

        user.sendMessage(ChatColor.GRAY + "Money: " + ChatColor.GOLD + MathUtil.round(corporation.getBalance()) + " " + VaultBridge.getCurrenyName());
        user.sendMessage(divider);
        user.sendMessage("");
    }

    private CorpRank handleRank(CommandSender sender, Corporation corp, String rankName, boolean checkPriority) {
        CorpRank targetRank = corp.getRank(rankName);
        if (targetRank == null) {
            sender.sendMessage(ReallifeLanguageConfiguration.m("Corporation.RankNotFound", rankName));
            return null;
        }

        if (checkPriority) {
            CorpRank userRank = corp.getRank(SinkLibrary.getInstance().getIngameUser(((Player) sender)));

            if (targetRank.priority < userRank.priority) {
                sender.sendMessage(ReallifeLanguageConfiguration.m("Corporation.NotEnoughPriority"));
                return null;
            }
        }
        return targetRank;
    }
}
