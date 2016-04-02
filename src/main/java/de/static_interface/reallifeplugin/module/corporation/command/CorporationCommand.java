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

import static de.static_interface.reallifeplugin.config.RpLanguage.m;
import static de.static_interface.sinklibrary.configuration.GeneralLanguage.GENERAL_INVALID_VALUE;
import static de.static_interface.sinklibrary.configuration.GeneralLanguage.GENERAL_NOT_ENOUGH_MONEY;
import static de.static_interface.sinklibrary.configuration.GeneralLanguage.GENERAL_SUCCESS;
import static de.static_interface.sinklibrary.configuration.GeneralLanguage.GENERAL_SUCCESS_SET;
import static de.static_interface.sinklibrary.configuration.GeneralLanguage.TIMEUNIT_DAYS;
import static de.static_interface.sinksql.query.Query.eq;
import static de.static_interface.sinksql.query.Query.from;
import static de.static_interface.sinksql.query.Query.gt;

import de.static_interface.reallifeplugin.config.RpLanguage;
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
import de.static_interface.reallifeplugin.permission.Permission;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.command.SinkCommandBase;
import de.static_interface.sinklibrary.api.command.SinkSubCommand;
import de.static_interface.sinklibrary.api.command.annotation.Aliases;
import de.static_interface.sinklibrary.api.command.annotation.DefaultPermission;
import de.static_interface.sinklibrary.api.command.annotation.Description;
import de.static_interface.sinklibrary.api.command.annotation.Usage;
import de.static_interface.sinklibrary.api.exception.UserNotFoundException;
import de.static_interface.sinklibrary.api.exception.UserNotOnlineException;
import de.static_interface.sinklibrary.api.user.SinkUser;
import de.static_interface.sinklibrary.configuration.GeneralLanguage;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.CommandUtil;
import de.static_interface.sinklibrary.util.Debug;
import de.static_interface.sinklibrary.util.MathUtil;
import de.static_interface.sinklibrary.util.StringUtil;
import de.static_interface.sinklibrary.util.VaultBridge;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;

@Description("Interact with corporations!")
@DefaultPermission
@Usage("<corp>")
@Aliases("corp")
public class CorporationCommand extends ModuleCommand<CorporationModule> {

    public CorporationCommand(CorporationModule module) {
        super(module);
    }

    @Override
    public void onRegistered() {
        registerSubCommand(new CorporationSubCommand(this, "user") {
            @Description("Get information about an user")
            @Usage("[player]")
            @Override
            @DefaultPermission
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                IngameUser target = null;
                if (sender instanceof Player) {
                    target = SinkLibrary.getInstance().getIngameUser((Player) sender);
                }

                if (args.length > 0) {
                    target = SinkLibrary.getInstance().getIngameUser(args[0]);
                    if (!target.hasPlayedBefore()) {
                        throw new UserNotFoundException(args[0]);
                    }
                }

                if (target == null) {
                    return false;
                }

                sendUserInfo(sender, target);
                return true;
            }
        });

        registerSubCommand(new CorporationSubCommand(this, "list") {
            {
                Options options = getCommandOptions().getCliOptions();
                if (options == null) {
                    options = new Options();
                }
                Option moneyOption = Option.builder("m")
                        .desc("Sort by money")
                        .longOpt("money")
                        .build();
                options.addOption(moneyOption);
                getCommandOptions().setCliOptions(options);
            }

            @Description("List all corporations")
            @Usage("[-m]")
            @DefaultPermission
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                sendCorporationsList(sender, getCommandLine().hasOption('m'));
                return true;
            }
        });

        registerSubCommand(new CorporationSubCommand(this, "leave") {
            {
                getCommandOptions().setPlayerOnly(true);
            }

            @Description("Leave a corporation")
            @Override
            @DefaultPermission
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                Corporation userCorp = getUserCorporation(sender);
                if (getUserCorporation(sender) == null) {
                    sender.sendMessage(m("Corporation.NotInCorporation"));
                    return true;
                }

                IngameUser user = SinkLibrary.getInstance().getIngameUser((Player) sender);
                userCorp.removeMember(user);
                userCorp.announce(StringUtil.format(m("Corporation.UserLeftCorporation"), user, null));
                sender.sendMessage(m("Corporation.LeftCorporation"));
                return true;
            }
        });

        registerSubCommand(new CorporationSubCommand(this, "deposit", true, CorporationPermissions.DEPOSIT) {
            {
                getCommandOptions().setPlayerOnly(true);
                getCommandOptions().setMinRequiredArgs(1);
            }

            @Description("Deposit money to corporation account")
            @Usage("<amount>")
            @Override
            @DefaultPermission
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                if (getUserCorporation(sender) == null) {
                    sender.sendMessage(m("Corporation.NotInCorporation"));
                    return true;
                }

                deposit(SinkLibrary.getInstance().getIngameUser((Player) sender), getUserCorporation(sender), getArg(args, 0, Double.class));
                return true;
            }
        });

        registerSubCommand(new CorporationSubCommand(this, "withdraw", true, CorporationPermissions.WITHDRAW) {
            {
                getCommandOptions().setPlayerOnly(true);
                getCommandOptions().setMinRequiredArgs(1);
            }

            @Description("Withdraw money from corporation account")
            @Usage("<amount>")
            @Override
            @DefaultPermission
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                if (getUserCorporation(sender) == null) {
                    sender.sendMessage(m("Corporation.NotInCorporation"));
                    return true;
                }

                withdraw(SinkLibrary.getInstance().getIngameUser((Player) sender), getUserCorporation(sender), getArg(args, 0, Double.class));
                return true;
            }
        });

        registerSubCommand(new CorporationSubCommand(this, "join") {
            {
                getCommandOptions().setPlayerOnly(true);
                getCommandOptions().setMinRequiredArgs(1);
            }

            @Description("Join a corporation")
            @Usage("<corporation>")
            @Override
            @DefaultPermission
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                Corporation userCorp = getUserCorporation(sender);
                if (userCorp != null) {
                    sender.sendMessage(RpLanguage.m("Corporation.AlreadyInCorporation", userCorp.getFormattedName()));
                    return true;
                }

                Corporation corp = CorporationManager.getInstance().getCorporation(args[0]);
                if (corp == null) {
                    sender.sendMessage(RpLanguage.m("Corporation.CorporationNotFound", args[0]));
                    return true;
                }

                if (!corp.isPublic() && !CorporationInviteQueue.hasInvite(((Player) sender).getUniqueId(), corp) && !isForceMode(sender)) {
                    sender.sendMessage(RpLanguage.m("Corporation.NotInvited", corp.getFormattedName()));
                    return true;
                }

                if (corp.getMemberLimit() > 0 && corp.getMemberCount() >= (corp.getMemberLimit() + 1)) {
                    sender.sendMessage(RpLanguage.m("Corporation.CorporationFull"));
                    return true;
                }

                IngameUser user = SinkLibrary.getInstance().getIngameUser((Player) sender);

                corp.addMember(user, corp.getDefaultRank());
                corp.announce(RpLanguage.m("Corporation.Joined", ((Player) sender).getDisplayName()));
                CorporationInviteQueue.remove(user.getUniqueId(), corp);
                return true;
            }
        });

        registerSubCommand(new CorporationSubCommand(this, "invite", true, CorporationPermissions.INVITE) {
            {
                getCommandOptions().setMinRequiredArgs(1);
            }

            @Description("Invite a player to the corporation")
            @Usage("<player>")
            @Override
            @DefaultPermission
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                SinkUser user = SinkLibrary.getInstance().getUser(sender);

                Corporation userCorp = getUserCorporation(sender);
                if (userCorp == null) {
                    sender.sendMessage(RpLanguage.m("Corporation.NotInCorporation"));
                    return true;
                }

                if (userCorp.getMemberLimit() > 0 && userCorp.getMemberCount() >= (userCorp.getMemberLimit() + 1)) {
                    sender.sendMessage(RpLanguage.m("Corporation.CorporationFull"));
                    return true;
                }

                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[0], true);
                if (!isForceMode(sender) && !target.isOnline()) {
                    throw new UserNotOnlineException(target.getDisplayName());
                }

                if (CorporationInviteQueue.hasInvite(target.getUniqueId(), userCorp)) {
                    sender.sendMessage(RpLanguage.m("Corporation.AlreadyInvited", target.getDisplayName()));
                    return true;
                }

                if (!isExplicitForceMode(sender)) {
                    CorporationInviteQueue.add(target.getUniqueId(), userCorp);
                    sender.sendMessage(RpLanguage.m("Corporation.UserHasBeenInvited", target.getDisplayName()));
                    target.sendMessage(
                            RpLanguage
                                    .m("Corporation.GotInvited", user.getDisplayName(), userCorp.getFormattedName(), userCorp.getName()));
                    return true;
                }

                Corporation corp = CorporationManager.getInstance().getUserCorporation(target);
                if (corp != null) {
                    sender.sendMessage(RpLanguage
                                               .m("Corporation.UserAlreadyInCorporation", target.getDisplayName(), corp.getFormattedName(),
                                                  corp.getName()));
                    return true;
                }

                userCorp.addMember(target, userCorp.getDefaultRank());
                sender.sendMessage(GENERAL_SUCCESS.format());
                return true;
            }
        });

        registerSubCommand(new CorporationSubCommand(this, "kick", true, CorporationPermissions.KICK) {
            {
                getCommandOptions().setMinRequiredArgs(1);
            }

            @Description("Kick a player from a corporation")
            @Usage("<player>")
            @Override
            @DefaultPermission
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                Corporation userCorp = getUserCorporation(sender);
                if (userCorp == null) {
                    sender.sendMessage(RpLanguage.m("Corporation.NotInCorporation"));
                    return true;
                }

                IngameUser u = SinkLibrary.getInstance().getIngameUser(args[0], false);
                if (!userCorp.isMember(u)) {
                    sender.sendMessage(StringUtil.format(m("Corporation.NotMember"), args[0]));
                    return true;
                }

                IngameUser user = null;
                if (sender instanceof Player) {
                    user = SinkLibrary.getInstance().getIngameUser((Player) sender);
                }
                if (!isForceMode(sender) && (!(user instanceof IngameUser) || userCorp.getRank(u).priority <= userCorp.getRank(user).priority)) {
                    user.sendMessage(m("Corporation.NotEnoughPriority"));
                    return true;
                }
                userCorp.removeMember(u);

                if (u.isOnline()) {
                    u.sendMessage(StringUtil.format(m("Corporation.Kicked"), userCorp.getName()));
                    String name = CorporationManager.getInstance().getFormattedName(u);
                    if (name == null) {
                        name = args[0];
                    }
                    user.sendMessage(StringUtil.format(m("Corporation.CEOKicked"), name));
                }
                return true;
            }
        });

        registerSubCommand(new CorporationSubCommand(this, "delete", true, CorporationPermissions.DELETE) {
            @Description("Delete the corporation. THIS CAN NOT BE UNDONE.")
            @Override
            @DefaultPermission
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                if (args.length > 0) {
                    sender.sendMessage("Too many arguments");
                    return true;
                }
                Corporation corp = getUserCorporation(sender);
                if (corp == null) {
                    sender.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), ""));
                    return false;
                }

                CorporationManager.getInstance().deleteCorporation(corp);
                sender.sendMessage(StringUtil.format(m("Corporation.Deleted"), corp.getFormattedName()));

                return true;
            }
        });

        registerAdminSubCommands();
        registerRankCommands();
    }

    private void registerAdminSubCommands() {
        SinkSubCommand adminCommand = new CorporationSubCommand(this, "admin") {
            {
                getCommandOptions().setIrcOpOnly(true);
            }

            @DefaultPermission
            @Description("Administrate corporations")
            @Override
            protected boolean onExecute(CommandSender commandSender, String s, String[] strings) throws ParseException {
                return false;
            }
        };

        registerSubCommand(adminCommand);

        adminCommand.registerSubCommand(new CorporationSubCommand(adminCommand, "new") {
            {
                getCommandOptions().setMinRequiredArgs(3);
            }

            @Usage("<name> <base> <CEO> [world]")
            @Description("Create a new corporation")
            @DefaultPermission
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                SinkUser user = SinkLibrary.getInstance().getUser(sender);
                if ((!(user instanceof IngameUser) && args.length < 4)) {
                    return false;
                }

                String name = args[0];
                String ceo = args[1];
                String base = args[2];
                World world;
                if (user instanceof IngameUser) {
                    world = ((IngameUser) user).getPlayer().getWorld();
                } else {
                    world = Bukkit.getWorld(args[3]);
                }

                boolean successful = CorporationManager.getInstance().createCorporation(getModule(), user, name, ceo, base, world);
                Corporation corp = CorporationManager.getInstance().getCorporation(name);
                String msg = successful ? m("Corporation.Created") : m("Corporation.CreationFailed");
                msg = StringUtil.format(msg, corp.getFormattedName());
                user.sendMessage(msg);
                return true;
            }
        });

        adminCommand.registerSubCommand(new CorporationSubCommand(adminCommand, "setmemberlimit") {
            {
                getCommandOptions().setMinRequiredArgs(1);
            }

            @Usage("<corporation> <memberlimit>")
            @Description("Set the maximum member limit for a corporation")
            @DefaultPermission
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                Corporation corporation = CorporationManager.getInstance().getCorporation(args[0]);
                if (corporation == null) {
                    sender.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[0]));
                    return true;
                }

                int limit = getArg(args, 1, Integer.class);
                corporation.setMemberLimit(limit);
                sender.sendMessage(GENERAL_SUCCESS_SET.format("MemberLimit", limit));
                return true;
            }
        });

        adminCommand.registerSubCommand(new CorporationSubCommand(adminCommand, "getmemberlimit") {
            {
                getCommandOptions().setMinRequiredArgs(1);
            }

            @Usage("<corporation>")
            @Description("Get the maximum member limit for a corporation")
            @DefaultPermission
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                Corporation corporation = CorporationManager.getInstance().getCorporation(args[0]);
                if (corporation == null) {
                    sender.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[0]));
                    return true;
                }

                sender.sendMessage("Limit: " + corporation.getMemberLimit());
                return true;
            }
        });

        adminCommand.registerSubCommand(new CorporationSubCommand(adminCommand, "enablefishing") {
            {
                getCommandOptions().setMinRequiredArgs(1);
            }

            @Usage("<corporation>")
            @Description("Enable fishing for a corporation")
            @DefaultPermission
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                Corporation corporation = CorporationManager.getInstance().getCorporation(args[0]);
                if (corporation == null) {
                    sender.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[0]));
                    return true;
                }

                corporation.setOption(CorporationOptions.FISHING, true);
                sender.sendMessage(GENERAL_SUCCESS.format());
                return true;
            }
        });

        adminCommand.registerSubCommand(new CorporationSubCommand(adminCommand, "disablefishing") {
            {
                getCommandOptions().setMinRequiredArgs(1);
            }

            @Usage("<corporation>")
            @Description("Disable fishing for a corporation")
            @DefaultPermission
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                Corporation corporation = CorporationManager.getInstance().getCorporation(args[0]);
                if (corporation == null) {
                    sender.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[0]));
                    return true;
                }

                corporation.setOption(CorporationOptions.FISHING, false);
                sender.sendMessage(GENERAL_SUCCESS.format());
                return true;
            }
        });

        adminCommand.registerSubCommand(new CorporationSubCommand(adminCommand, "setbase") {
            {
                getCommandOptions().setMinRequiredArgs(2);
            }

            @Override
            @Usage("<corporation> <base> [world]")
            @DefaultPermission
            @Description("Create a new corporation")
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                SinkUser user = SinkLibrary.getInstance().getUser(sender);
                if (!(user instanceof IngameUser) && args.length < 3) {
                    return false;
                }

                Corporation corporation = CorporationManager.getInstance().getCorporation(args[0]);

                World world;
                if (user instanceof IngameUser && args.length < 3) {
                    world = ((IngameUser) user).getPlayer().getWorld();
                } else {
                    world = Bukkit.getWorld(args[2]);
                }

                if (corporation == null) {
                    user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[0]));
                    return true;
                }
                corporation.setBase(world, args[1]);
                user.sendMessage(m("Corporation.BaseSet"));
                return true;
            }
        });

        CorporationSubCommand moneyCommand = new CorporationSubCommand(adminCommand, "money") {
            @DefaultPermission
            @Description("Money transactions")
            @Override
            protected boolean onExecute(CommandSender commandSender, String s, String[] strings) throws ParseException {
                return false;
            }
        };

        adminCommand.registerSubCommand(moneyCommand);

        final Options forceTransactionOptions = new Options();
        forceTransactionOptions.addOption(Option.builder("s")
                                                  .longOpt("skipchecks")
                                                  .desc("force transactions")
                                                  .build());
        moneyCommand.registerSubCommand(new CorporationSubCommand(moneyCommand, "give") {
            {
                getCommandOptions().setMinRequiredArgs(2);
                getCommandOptions().setCliOptions(forceTransactionOptions);
            }

            @Override
            @Usage("<corporation> <amount>")
            @DefaultPermission
            @Description("Give money to a corporation")
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                Corporation corporation = CorporationManager.getInstance().getCorporation(args[0]);
                if (corporation == null) {
                    sender.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[0]));
                    return true;
                }
                double amount = getArg(args, 1, Double.class);
                if (amount <= 0) {
                    sender.sendMessage(GENERAL_INVALID_VALUE.format(amount));
                    return true;
                }

                if (corporation.addBalance(amount, !getCommandLine().hasOption('s'))) {
                    sender.sendMessage(GENERAL_SUCCESS.format());
                } else {
                    sender.sendMessage(ChatColor.DARK_RED + "Transaction Failure (try -s option?)");
                }
                return true;
            }
        });

        moneyCommand.registerSubCommand(new CorporationSubCommand(moneyCommand, "take") {
            {
                getCommandOptions().setMinRequiredArgs(2);
                getCommandOptions().setCliOptions(forceTransactionOptions);
            }

            @Override
            @Usage("<corporation> <amount>")
            @DefaultPermission
            @Description("Take money from a corporation")
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                Corporation corporation = CorporationManager.getInstance().getCorporation(args[0]);
                if (corporation == null) {
                    sender.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[0]));
                    return true;
                }
                double amount = getArg(args, 1, Double.class);
                if (amount <= 0) {
                    sender.sendMessage(GENERAL_INVALID_VALUE.format(amount));
                    return true;
                }

                amount = -amount;
                if (corporation.addBalance(amount, !getCommandLine().hasOption('s'))) {
                    sender.sendMessage(GENERAL_SUCCESS.format());
                } else {
                    sender.sendMessage(ChatColor.DARK_RED + "Transaction Failure (try -s option?)");
                }
                return true;
            }
        });

        adminCommand.registerSubCommand(new CorporationSubCommand(adminCommand, "rename") {
            {
                getCommandOptions().setMinRequiredArgs(2);
            }

            @Usage("<corporation> <new_name>")
            @DefaultPermission
            @Description("Rename a corporation")
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                Corporation corporation = CorporationManager.getInstance().getCorporation(args[0]);
                if (corporation == null) {
                    sender.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[0]));
                    return true;
                }

                if (CorporationManager.getInstance().renameCorporation(SinkLibrary.getInstance().getUser(sender), corporation, args[1])) {
                    sender.sendMessage(GENERAL_SUCCESS.format());
                }

                return true;
            }
        });
    }

    private void registerRankCommands() {
        CorporationSubCommand rankCommand = new CorporationSubCommand(this, "rank", true) {
            @DefaultPermission
            @Description("Manage ranks")
            @Override
            protected boolean onExecute(CommandSender commandSender, String s, String[] strings) throws ParseException {
                return false;
            }
        };

        registerSubCommand(rankCommand);

        rankCommand.registerSubCommand(new CorporationSubCommand(rankCommand, "permissionslist", true) {
            @DefaultPermission
            @Aliases("plist")
            @Usage("[group]")
            @Description("List all permissions")
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                Corporation corp = getUserCorporation(sender);
                CorpRank rank;
                if (args.length > 0) {
                    rank = handleRank(sender, corp, args[0], false);
                    if (rank == null) {
                        return false;
                    }
                } else {
                    if (isForceMode(sender)) {
                        return false;
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
                return true;
            }
        });

        rankCommand.registerSubCommand(new CorporationSubCommand(rankCommand, "list", true) {
            @DefaultPermission
            @Description("List all ranks")
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                Corporation corp = getUserCorporation(sender);
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
                return true;
            }
        });

        rankCommand.registerSubCommand(new CorporationSubCommand(rankCommand, "delete", true, CorporationPermissions.MANAGE_RANKS) {
            {
                getCommandOptions().setMinRequiredArgs(1);
            }

            @DefaultPermission
            @Usage("<rank>")
            @Description("Delete a rank")
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                Corporation corp = getUserCorporation(sender);
                CorpRank rank = handleRank(sender, corp, args[0], !isForceMode(sender));
                if (rank == null) {
                    return false;
                }

                int defaultRankId = corp.getDefaultRank().id;
                if (defaultRankId == rank.id) {
                    sender.sendMessage(RpLanguage.m("Corporation.DeletingDefaultRank"));
                    return true;
                }

                for (CorpUserRow row : corp.getRankUsers(rank)) {
                    IngameUser rankUser = SinkLibrary.getInstance().getIngameUser(UUID.fromString(row.uuid));
                    corp.setRank(rankUser, corp.getDefaultRank());
                }

                from(getModule().getTable(CorpRanksTable.class))
                        .delete()
                        .where("id", eq("?"))
                        .execute(rank.id);

                sender.sendMessage(GENERAL_SUCCESS.format());
                return true;
            }
        });

        rankCommand.registerSubCommand(new CorporationSubCommand(rankCommand, "new", true, CorporationPermissions.MANAGE_RANKS) {
            {
                getCommandOptions().setMinRequiredArgs(2);
            }

            @DefaultPermission
            @Usage("<rank> <priority>")
            @Description("Creata a new rank")
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                String rankName = args[0];
                Corporation corp = getUserCorporation(sender);
                int priortiy = getArg(args, 1, Integer.class);

                if (!isForceMode(sender)) {
                    CorpRank userRank = corp.getRank(SinkLibrary.getInstance().getIngameUser(((Player) sender)));

                    if (priortiy < userRank.priority) {
                        sender.sendMessage(RpLanguage.m("Corporation.NotEnoughPriority"));
                        return true;
                    }
                }
                CorpRank rank = new CorpRank();
                rank.name = rankName;
                rank.prefix = ChatColor.GOLD.toString();
                rank.description = null;
                rank.priority = priortiy;
                rank.corpId = corp.getId();

                getModule().getTable(CorpRanksTable.class).insert(rank);

                sender.sendMessage(GENERAL_SUCCESS.format());
                return true;
            }
        });

        rankCommand.registerSubCommand(new CorporationSubCommand(rankCommand, "priority", true, CorporationPermissions.MANAGE_RANKS) {
            {
                getCommandOptions().setMinRequiredArgs(2);
            }

            @DefaultPermission
            @Usage("<rank> <new-priority>")
            @Description("Set the priority for a rank")
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                String rankName = args[0];
                Corporation corp = getUserCorporation(sender);
                int priortiy = getArg(args, 1, Integer.class);

                CorpRank rank = handleRank(sender, corp, rankName, !isForceMode(sender));
                if (rank == null) {
                    return false;
                }

                from(getModule().getTable(CorpRanksTable.class))
                        .update()
                        .set("priority", "?")
                        .where("id", eq("?"))
                        .execute(priortiy, rank.id);

                sender.sendMessage(GENERAL_SUCCESS.format());
                return true;
            }
        });

        rankCommand.registerSubCommand(new CorporationSubCommand(rankCommand, "prefix", true, CorporationPermissions.MANAGE_RANKS) {
            {
                getCommandOptions().setMinRequiredArgs(2);
            }

            @DefaultPermission
            @Usage("<rank> <prefix>")
            @Description("Set the prefix of a rank")
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                String rankName = args[0];
                Corporation corp = getUserCorporation(sender);
                CorpRank rank = handleRank(sender, corp, rankName, !isForceMode(sender));
                if (rank == null) {
                    return false;
                }
                String prefix = ChatColor.translateAlternateColorCodes('&', StringUtil.formatArrayToString(args, " ", 0, args.length));

                from(getModule().getTable(CorpRanksTable.class))
                        .update()
                        .set("prefix", "?")
                        .where("id", eq("?"))
                        .execute(prefix, rank.id);

                sender.sendMessage(GENERAL_SUCCESS_SET.format("Prefix", prefix));
                return true;
            }
        });

        rankCommand.registerSubCommand(new CorporationSubCommand(rankCommand, "description", true, CorporationPermissions.MANAGE_RANKS) {
            {
                getCommandOptions().setMinRequiredArgs(2);
            }

            @DefaultPermission
            @Usage("<rank> <description>")
            @Description("Set the description of a rank")
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                String rankName = args[0];
                Corporation corp = getUserCorporation(sender);
                CorpRank rank = handleRank(sender, corp, rankName, !isForceMode(sender));
                if (rank == null) {
                    return false;
                }

                String description = ChatColor.translateAlternateColorCodes('&', StringUtil.formatArrayToString(args, " ", 1, args.length));
                if (description.trim().equalsIgnoreCase("null") || description.trim().equalsIgnoreCase("off") || description.trim()
                        .equalsIgnoreCase("remove")) {
                    description = null;
                }

                from(getModule().getTable(CorpRanksTable.class))
                        .update()
                        .set("description", "?")
                        .where("id", eq("?"))
                        .execute(description, rank.id);

                sender.sendMessage(GENERAL_SUCCESS_SET.format("Description", description));
                return true;
            }
        });

        rankCommand.registerSubCommand(new CorporationSubCommand(rankCommand, "rename", true, CorporationPermissions.MANAGE_RANKS) {
            {
                getCommandOptions().setMinRequiredArgs(2);
            }

            @DefaultPermission
            @Usage("<rank> <new_name>")
            @Description("Rename a rank")
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                String rankName = args[0];
                String newName = args[1];
                Corporation corp = getUserCorporation(sender);
                CorpRank rank = handleRank(sender, corp, rankName, !isForceMode(sender));
                if (rank == null) {
                    return false;
                }

                from(getModule().getTable(CorpRanksTable.class))
                        .update()
                        .set("name", "?")
                        .where("id", eq("?"))
                        .execute(newName, rank.id);

                sender.sendMessage(GENERAL_SUCCESS_SET.format("Name", newName));
                return true;
            }
        });

        rankCommand.registerSubCommand(new CorporationSubCommand(rankCommand, "set", true, CorporationPermissions.SET_RANK) {
            {
                getCommandOptions().setMinRequiredArgs(2);
            }

            @DefaultPermission
            @Usage("<player> <rank>")
            @Description("Set the rank of a player")
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                IngameUser target = getArg(args, 0, IngameUser.class);
                String rankName = args[1];
                Corporation corp = getUserCorporation(sender);
                CorpRank rank = handleRank(sender, corp, rankName, !isForceMode(sender));
                if (rank == null) {
                    return false;
                }

                if (!corp.isMember(target)) {
                    sender.sendMessage(
                            RpLanguage.m("Corporation.UserNotMember", target.getDisplayName(), corp.getFormattedName()));
                    return true;
                }

                corp.setRank(target, rank);
                sender.sendMessage(GENERAL_SUCCESS_SET.format("Rank of " + target.getName(), rank.name));
                return true;
            }
        });

        rankCommand.registerSubCommand(new CorporationSubCommand(rankCommand, "setpermission", true, CorporationPermissions.MANAGE_RANKS) {
            {
                getCommandOptions().setMinRequiredArgs(3);
            }

            @DefaultPermission
            @Usage("<rank> <permission> <value>")
            @Description("Set a permission for a rank")
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                String rankName = args[0];
                Corporation corp = getUserCorporation(sender);
                CorpRank rank = handleRank(sender, corp, rankName, !isForceMode(sender));
                if (rank == null) {
                    return false;
                }

                Permission permission = CorporationPermissions.getInstance().getPermission(args[1].toUpperCase());
                if (permission == null) {
                    sender.sendMessage(RpLanguage.m("Corporation.UnknownPermission", args[1].toUpperCase()));
                    return true;
                }

                SinkUser user = SinkLibrary.getInstance().getUser(sender);
                if (!isForceMode(sender) && (getUserCorporation(sender) == null ||
                                             (user instanceof IngameUser && !CorporationManager.getInstance()
                                                     .hasCorpPermission((IngameUser) user, permission)))) {
                    sender.sendMessage(RpLanguage.m("Corporation.NotEnoughPriority"));
                    return true;
                }

                String[] valueArgs = Arrays.copyOfRange(args, 2, args.length);
                Object value = CommandUtil.parseValue(valueArgs);
                getModule().getTable(CorpRankPermissionsTable.class).setOption(permission.getPermissionString(), value, rank.id);

                for (CorpUserRow row : corp.getRankUsers(rank)) {
                    IngameUser rankUser = SinkLibrary.getInstance().getIngameUser(UUID.fromString(row.uuid));
                    corp.onUpdateRank(rankUser, rank);
                }

                sender.sendMessage(GENERAL_SUCCESS_SET.format(permission, Objects.toString(value)));
                return true;
            }
        });
    }

    @Override
    public boolean onExecute(CommandSender sender, String label, String[] args) {
        SinkUser user = SinkLibrary.getInstance().getUser(sender);
        Corporation userCorp = null;
        if (user instanceof IngameUser) {
            userCorp = CorporationManager.getInstance().getUserCorporation((IngameUser) user);
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

        Corporation corporation = CorporationManager.getInstance().getCorporation(args[0]);
        sendCorporationInfo(user, corporation);
        return true;
    }

    private void deposit(IngameUser user, Corporation corp, double amount) {
        if (amount < 1) {
            user.sendMessage(GENERAL_INVALID_VALUE.format(amount));
            return;
        }

        if (VaultBridge.getBalance(user.getPlayer()) < amount) {
            user.sendMessage(GENERAL_NOT_ENOUGH_MONEY.format());
            return;
        }

        VaultBridge.addBalance(user.getPlayer(), -amount);
        corp.addBalance(amount);

        corp.announce(m("Corporation.Deposit", user.getDisplayName(), amount));
    }

    private void withdraw(IngameUser user, Corporation corp, double amount) {
        if (amount < 1) {
            user.sendMessage(GENERAL_INVALID_VALUE.format(amount));
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

    private void sendCorporationsList(CommandSender sender, boolean listByMoney) {
        sender.sendMessage(ChatColor.GOLD + "Corporations: ");
        String msg = "";
        for (Corporation corporation : CorporationManager.getInstance().getCorporations()) {
            int data;
            if (listByMoney) {
                data = (int) corporation.getBalance();
            } else {
                data = corporation.getMemberCount();
            }
            String formattedName = corporation.getFormattedName() + ChatColor.WHITE + "[" +
                                   data + "]" + ChatColor.RESET;
            if (msg.equals("")) {
                msg = formattedName;
                continue;
            }
            msg += " " + formattedName;
        }
        sender.sendMessage(msg);
    }


    private void sendUserInfo(CommandSender sender, IngameUser target) {
        sender.sendMessage("");
        String divider = ChatColor.GOLD + "";
        for (int i = 0; i < 32; i++) {
            divider += "-";
        }
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + " User: " + CorporationManager.getInstance().getFormattedName(target));
        sender.sendMessage(divider);
        Corporation corp = CorporationManager.getInstance().getUserCorporation(target);
        sender.sendMessage(ChatColor.GRAY + "Corporation: " + ChatColor.GOLD + (corp == null ? "-" : corp.getFormattedName()));
        String rank;
        if (corp == null) {
            rank = ChatColor.GOLD + "-";
        } else {
            rank = corp.getRank(target).name;
        }
        sender.sendMessage(ChatColor.GRAY + "Rank: " + rank);

        String soldItems = "-";
        Integer userId = CorporationManager.getInstance().getUserId(target);
        if (corp != null) {
            //Todo make time configurable
            int days = 3;
            long maxTime = System.currentTimeMillis() - 1000 * 60 * 60 * 24 * days;
            CorpTradesRow[] rows =
                    from(
                            getModule().getTable(CorpTradesTable.class))
                            .select()
                            .where("user_id", eq("?"))
                            .and("corp_id", eq("?"))
                            .and("time", gt("?"))
                            .getResults(userId, corp.getId(), maxTime);

            if (rows.length > 0) {
                int i = 0;
                for (CorpTradesRow row : rows) {
                    if (row.type != 0) {
                        continue; //wasn't selling items
                    }
                    i += row.changedAmount;
                }
                soldItems = m("Corporation.ItemsSold", target.getDisplayName(), i, days + " " + TIMEUNIT_DAYS.format());
            }
        }
        sender.sendMessage(ChatColor.GRAY + "Items sold: " + ChatColor.GOLD + soldItems);

        sender.sendMessage(divider);
        sender.sendMessage("");
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

            user.sendMessage("");
            if (!StringUtil.isEmptyOrNull(rank.description)) {
                user.sendMessage(ChatColor.GOLD + rank.name + ChatColor.GRAY + ": " + rank.description);
            } else {
                user.sendMessage(ChatColor.GOLD + rank.name);
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

            user.sendMessage(users);
        }

        user.sendMessage("");

        if (!membersFound) {
            user.sendMessage(ChatColor.RED + "Keine Mitglieder gefunden");
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
            sender.sendMessage(RpLanguage.m("Corporation.RankNotFound", rankName));
            return null;
        }

        if (checkPriority) {
            CorpRank userRank = corp.getRank(SinkLibrary.getInstance().getIngameUser(((Player) sender)));

            if (targetRank.priority < userRank.priority) {
                sender.sendMessage(RpLanguage.m("Corporation.NotEnoughPriority"));
                return null;
            }
        }
        return targetRank;
    }

    private abstract class CorporationSubCommand extends SinkSubCommand<SinkCommandBase> {

        private boolean needsCorp;
        @Nullable
        private Permission permission;

        public CorporationSubCommand(SinkCommandBase parentCommand, String name) {
            this(parentCommand, name, false, null);
        }

        public CorporationSubCommand(SinkCommandBase parentCommand, String name, boolean needsCorp) {
            this(parentCommand, name, needsCorp, null);
        }

        public CorporationSubCommand(SinkCommandBase parentCommand, String name, @Nullable Permission permission) {
            this(parentCommand, name, false, permission);
        }

        public CorporationSubCommand(SinkCommandBase parentCommand, String name, boolean needsCorp, @Nullable Permission permission) {
            super(parentCommand, name);
            this.needsCorp = needsCorp;
            this.permission = permission;
        }

        @Override
        public void onRegistered() {
            Debug.logMethodCall(getDebuggableName());
            Options options = getCommandOptions().getCliOptions();
            debug("needsCorp == " + needsCorp);
            debug("permission == " + (permission != null ? permission.getPermissionString() : "null"));
            if (needsCorp) {
                Option corpOption = Option.builder("c")
                        .hasArg()
                        .longOpt("corp")
                        .desc("Force as given corporation member")
                        .argName("<corp name>")
                        .build();
                options.addOption(corpOption);
            }

            if (permission != null) {
                Option forceOption = Option.builder("f")
                        .longOpt("force")
                        .desc("Force command, even if you don't have permission for it")
                        .build();

                options.addOption(forceOption);
            }
        }

        @Nullable
        public Corporation getUserCorporation(CommandSender sender) throws ParseException {
            if (needsCorp
                && getCommandLine().hasOption('c')
                && sender.hasPermission("reallifeplugin.corporations.admin")) {
                return CorporationManager.getInstance().getCorporation(getCommandLine().getOptionValue('c'));
            }

            Corporation userCorp = null;
            if (sender instanceof Player) {
                userCorp = CorporationManager.getInstance().getUserCorporation(SinkLibrary.getInstance().getIngameUser((Player) sender));
            }
            return userCorp;
        }

        @Override
        public boolean onPreExecute(CommandSender sender, Command cmd, String label, String[] args) {
            try {
                if (needsCorp && getUserCorporation(sender) == null) {
                    debug("needsCorp check failed");
                    sender.sendMessage(m("Corporation.NotInCorporation"));
                    return false;
                }
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }

            try {
                if (hasPermission(sender)) {
                    return true;
                }
            } catch (ParseException e) {
                return false;
            }

            sender.sendMessage(GeneralLanguage.PERMISSIONS_GENERAL.format());
            return false;
        }

        public boolean hasPermission(CommandSender sender) throws ParseException {
            if (permission == null) {
                return true;
            }

            if (!(sender instanceof Player)) {
                return sender.isOp();
            }
            IngameUser user = SinkLibrary.getInstance().getIngameUser((Player) sender);
            Corporation corp = getUserCorporation(sender);
            boolean hasCorpPermission = false;
            if (corp != null) {
                hasCorpPermission = CorporationManager.getInstance().hasCorpPermission(user, permission);
            }

            return isForceMode(sender) || hasCorpPermission;
        }

        public boolean isForceMode(CommandSender sender) {
            return permission != null && testPermission(sender, "reallifeplugin.corporations.admin") && (getCommandLine().hasOption('f')
                                                                                                         || getCommandLine().hasOption('c'));
        }

        public boolean isExplicitForceMode(CommandSender sender) {
            return permission != null && testPermission(sender, "reallifeplugin.corporations.admin") && getCommandLine().hasOption('f');
        }
    }
}
