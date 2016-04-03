/*
 * Copyright (c) 2013 - 2015 <http://static-interface.de> and contributors
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

package de.static_interface.reallifeplugin.module.politics.command;

import static de.static_interface.sinklibrary.configuration.GeneralLanguage.GENERAL_INVALID_VALUE;
import static de.static_interface.sinklibrary.configuration.GeneralLanguage.GENERAL_NOT_ENOUGH_MONEY;
import static de.static_interface.sinklibrary.configuration.GeneralLanguage.GENERAL_SUCCESS;
import static de.static_interface.sinklibrary.configuration.GeneralLanguage.GENERAL_SUCCESS_SET;

import de.static_interface.reallifeplugin.config.RpLanguage;
import de.static_interface.reallifeplugin.module.ModuleCommand;
import de.static_interface.reallifeplugin.module.politics.Party;
import de.static_interface.reallifeplugin.module.politics.PartyInviteQueue;
import de.static_interface.reallifeplugin.module.politics.PartyManager;
import de.static_interface.reallifeplugin.module.politics.PartyPermission;
import de.static_interface.reallifeplugin.module.politics.PoliticsModule;
import de.static_interface.reallifeplugin.module.politics.database.row.PartyRank;
import de.static_interface.reallifeplugin.module.politics.database.row.PartyUser;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyRankPermissionsTable;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyRanksTable;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyUsersTable;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.command.annotation.Usage;
import de.static_interface.sinklibrary.api.exception.NotEnoughArgumentsException;
import de.static_interface.sinklibrary.api.exception.NotEnoughPermissionsException;
import de.static_interface.sinklibrary.api.exception.UserNotOnlineException;
import de.static_interface.sinklibrary.api.user.SinkUser;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.BukkitUtil;
import de.static_interface.sinklibrary.util.CommandUtil;
import de.static_interface.sinklibrary.util.MathUtil;
import de.static_interface.sinklibrary.util.StringUtil;
import de.static_interface.sinklibrary.util.VaultBridge;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Usage("<options> [-p <party>] [-f]")
public class PartyCommand extends ModuleCommand<PoliticsModule> {

    public PartyCommand(PoliticsModule module) {
        super(module);
        getCommandOptions().setPlayerOnly(true);
        getCommandOptions().setCliOptions(new Options());
        Option partyOption = Option.builder("p")
                .hasArg()
                .longOpt("party")
                .desc("Force as given party member")
                .type(String.class)
                .argName("party name")
                .build();
        Option forceOption = Option.builder("f")
                .longOpt("force")
                .desc("Force command, even if you don't have permission for it")
                .build();
        getCommandOptions().getCliOptions().addOption(partyOption);
        getCommandOptions().getCliOptions().addOption(forceOption);
    }

    @Override
    protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
        Party party = PartyManager.getInstance().getParty(SinkLibrary.getInstance().getIngameUser((Player) sender));

        boolean isExplicitForceMode = sender.hasPermission("ReallifePlugin.Party.ForceCommand") && getCommandLine().hasOption('f');

        boolean isForceMode = isExplicitForceMode;
        if (sender.hasPermission("ReallifePlugin.Party.Admin") && getCommandLine().hasOption('p')) {
            isForceMode = true;
            party = PartyManager.getInstance().getParty(getCommandLine().getOptionValue('p'));
            if (party == null) {
                sender.sendMessage(RpLanguage.m("Party.PartyNotFound", getCommandLine().getOptionValue('p')));
                return true;
            }
        }

        if (party == null && args.length < 1) {
            sender.sendMessage(RpLanguage.m("Party.NotInParty"));
            return true;
        }

        if (party != null && args.length < 1) {
            sendInfo(sender, party);
            return true;
        }

        SinkUser user = SinkLibrary.getInstance().getUser((Object) sender);

        UUID uuid = ((Player) sender).getUniqueId();

        switch (args[0].toLowerCase().trim()) {
            case "list":
                sendPartyList(sender);
                break;
            case "deposit":
                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.DEPOSIT)) {
                    throw new NotEnoughPermissionsException();
                }
                if (args.length < 2) {
                    throw new NotEnoughArgumentsException();
                }
                try {
                    if (party == null) {
                        sender.sendMessage(RpLanguage.m("Party.NotInParty"));
                        break;
                    }
                    deposit((Player) sender, party, Double.valueOf(args[1]));
                } catch (NumberFormatException ignored) {
                    sender.sendMessage(GENERAL_INVALID_VALUE.format(args[1]));
                }
                break;

            case "withdraw":
                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.WITHDRAW)) {
                    throw new NotEnoughPermissionsException();
                }
                if (args.length < 2) {
                    throw new NotEnoughArgumentsException();
                }
                try {
                    withdraw((Player) sender, party, Double.valueOf(args[1]));
                } catch (NumberFormatException ignored) {
                    sender.sendMessage(GENERAL_INVALID_VALUE.format(args[1]));
                }
                break;

            case "join":
                if (args.length < 2) {
                    throw new NotEnoughArgumentsException();
                }
                if (party != null) {
                    sender.sendMessage(RpLanguage.m("Party.AlreadyInParty", party.getFormattedName()));
                    break;
                }
                party = PartyManager.getInstance().getParty(args[1]);
                if (party == null) {
                    sender.sendMessage(RpLanguage.m("Party.PartyNotFound", args[1]));
                    break;
                }

                if (!party.isPublic() && !PartyInviteQueue.hasInvite(uuid, party) && !isExplicitForceMode) {
                    sender.sendMessage(RpLanguage.m("Party.NotInvited", party.getFormattedName()));
                    break;
                }

                party.addMember(uuid, party.getDefaultRank());
                party.announce(RpLanguage.m("Party.Joined", ((Player) sender).getDisplayName()));
                PartyInviteQueue.remove(uuid, party);
                break;

            case "invite": {
                if (party == null) {
                    sender.sendMessage(RpLanguage.m("Party.NotInParty"));
                    break;
                }

                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.INVITE)) {
                    throw new NotEnoughPermissionsException();
                }
                if (args.length < 2) {
                    throw new NotEnoughArgumentsException();
                }

                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[1], true);
                if (!isExplicitForceMode && !target.isOnline()) {
                    throw new UserNotOnlineException(target.getDisplayName());
                }

                if (PartyInviteQueue.hasInvite(target.getUniqueId(), party)) {
                    sender.sendMessage(RpLanguage.m("Party.AlreadyInvited", target.getDisplayName()));
                    break;
                }

                if (!isExplicitForceMode) {
                    PartyInviteQueue.add(target.getUniqueId(), party);
                    sender.sendMessage(RpLanguage.m("Party.UserHasBeenInvited", target.getDisplayName()));
                    target.sendMessage(
                            RpLanguage.m("Party.GotInvited", user.getDisplayName(), party.getFormattedName(), party.getTag()));
                    break;
                }

                Party p = PartyManager.getInstance().getParty(target);
                if (p != null) {
                    sender.sendMessage(RpLanguage.m("Party.UserAlreadyInParty", target.getDisplayName(), p.getFormattedName()));
                    break;
                }

                party.addMember(target.getUniqueId(), party.getDefaultRank());
                sender.sendMessage(GENERAL_SUCCESS.format());
                break;
            }

            case "kick": {
                if (party == null) {
                    sender.sendMessage(RpLanguage.m("Party.NotInParty"));
                    break;
                }
                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.KICK)) {
                    throw new NotEnoughPermissionsException();
                }
                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[1], true);
                if (!target.isOnline()) {
                    throw new UserNotOnlineException(target.getDisplayName());
                }

                if (!party.isMember(target.getUniqueId())) {
                    sender.sendMessage(RpLanguage.m("Party.UserNotMember", target.getDisplayName(), party.getFormattedName()));
                    break;
                }

                if (!isForceMode && user instanceof IngameUser && party.getRank(target).priority <= party.getRank((IngameUser) user).priority) {
                    user.sendMessage(RpLanguage.m("Party.NotEnoughPriority"));
                    return true;
                }

                party.announce(RpLanguage.m("Party.Kicked", target.getDisplayName(), ((Player) sender).getDisplayName()));
                party.removeMember(target.getUniqueId());
                break;
            }

            case "new":
                if (!sender.hasPermission("ReallifePlugin.Party.New")) {
                    throw new NotEnoughPermissionsException();
                }
                if (args.length < 4) {
                    throw new NotEnoughArgumentsException();
                }
                String name = args[1];
                String tag = args[2];
                if (tag.length() < 2 || tag.length() > 4) {
                    sender.sendMessage(ChatColor.RED + "Parteiabkürzung zu lang oder zu kurz (min 2 und max 4 Zeichen!)");
                    break;
                }
                String founder = args[3];
                UUID founderUuid = BukkitUtil.getUniqueIdByName(founder);
                PartyManager.getInstance().createNewPary(name, tag, founderUuid);
                sender.sendMessage(ChatColor.GREEN + "Partei erfolgreich erstellt!");
                break;

            case "leave":
                if (party == null) {
                    sender.sendMessage(RpLanguage.m("Party.NotInParty"));
                    break;
                }
                party.announce(RpLanguage.m("Party.Left", ((Player) sender).getDisplayName()));
                party.removeMember(((Player) sender).getUniqueId());
                break;

            case "rank":
                if (party == null) {
                    sender.sendMessage(RpLanguage.m("Party.NotInParty"));
                    break;
                }
                handleRankCommand(sender, party, args, isForceMode);
                break;

            case "delete":
                if (party == null) {
                    sender.sendMessage(RpLanguage.m("Party.NotInParty"));
                    break;
                }
                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.DELETE)) {
                    throw new NotEnoughPermissionsException();
                }
                //Todo
                break;

            default:
                party = PartyManager.getInstance().getParty(args[0]);
                if (party == null) {
                    sender.sendMessage(ChatColor.RED + "Partei nicht gefunden");
                    break;
                }
                sendInfo(sender, party);
                break;
        }

        return true;
    }

    private void deposit(Player player, Party party, double amount) {
        if (amount <= 1) {
            player.sendMessage(GENERAL_INVALID_VALUE.format(amount));
            return;
        }

        if (VaultBridge.getBalance(player) < amount) {
            player.sendMessage(GENERAL_NOT_ENOUGH_MONEY.format());
            return;
        }

        VaultBridge.addBalance(player, -amount);
        party.addBalance(amount);

        party.announce(RpLanguage.m("Party.Deposit", player.getDisplayName(), amount));
    }

    private void withdraw(Player player, Party party, double amount) {
        if (amount < 1) {
            player.sendMessage(GENERAL_INVALID_VALUE.format(amount));
            return;
        }

        if (party.getBalance() < amount) {
            player.sendMessage(RpLanguage.m("Party.NotEnoughMoney"));
            return;
        }

        VaultBridge.addBalance(player, amount);
        party.addBalance(-amount);

        party.announce(RpLanguage.m("Party.Withdraw", player.getDisplayName(), amount));
    }

    private void handleRankCommand(CommandSender sender, Party party, String[] args, boolean isForceMode) {
        if (args.length < 2) {
            throw new NotEnoughArgumentsException();
        }

        UUID uuid = ((Player) sender).getUniqueId();

        switch (args[1].toLowerCase().trim()) {
            case "permissionslist": {
                PartyRank rank;
                if (args.length >= 3) {
                    rank = handleRank(sender, party, args[2], false);
                    if (rank == null) {
                        break;
                    }
                } else {
                    if (isForceMode) {
                        throw new NotEnoughArgumentsException();
                    }
                    rank = party.getRank(SinkLibrary.getInstance().getIngameUser((Player) sender));
                }
                sender.sendMessage(
                        ChatColor.GRAY + "Permissions: " + ChatColor.GOLD + rank.name + ChatColor.GRAY + " (" + ChatColor.GOLD + rank.priority
                        + ChatColor.GRAY + ")");
                for (PartyPermission perm : PartyPermission.values()) {
                    Object
                            value =
                            getModule().getTable(PartyRankPermissionsTable.class)
                                    .getOption(perm.getPermissionString(), rank.id, perm.getValueType(), perm.getDefaultValue());
                    if (PartyManager.getInstance().hasPartyPermission(rank, PartyPermission.ALL) && value == Boolean.TRUE) {
                        value = true;
                    }
                    sender.sendMessage("• " + ChatColor.GOLD + perm.getPermissionString() + ": " + ChatColor.GRAY + perm.getDescription() + " ("
                                       + (PartyManager.getInstance().hasPartyPermission(rank, perm) ? ChatColor.DARK_GREEN : ChatColor.DARK_RED)
                                       + "value: " + Objects.toString(value) + ChatColor.GRAY + ")");
                }
                break;
            }

            case "list": {
                sender.sendMessage(ChatColor.GOLD + party.getFormattedName() + ChatColor.GRAY + "'s Ranks:");
                for (PartyRank rank : party.getRanks()) {
                    String defaultText = ChatColor.GRAY + "";
                    int defaultRankId = party.getDefaultRank().id;
                    if (defaultRankId == rank.id) {
                        defaultText += ChatColor.BOLD + " (default)" + ChatColor.RESET + ChatColor.GRAY;
                    }
                    sender.sendMessage(
                            "• " + ChatColor.GOLD + rank.name + defaultText + " (" + ChatColor.GOLD + rank.priority + ChatColor.GRAY + ")" + (
                                    StringUtil.isEmptyOrNull(rank.description) ? "" : ": " + rank.description));
                }
                break;
            }

            case "new": {
                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.MANAGE_RANKS)) {
                    throw new NotEnoughPermissionsException();
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
                    PartyRank userRank = party.getRank(SinkLibrary.getInstance().getIngameUser(((Player) sender)));

                    if (priortiy < userRank.priority) {
                        sender.sendMessage(RpLanguage.m("Party.NotEnoughPriority"));
                        break;
                    }
                }
                PartyRank rank = new PartyRank();
                rank.name = name;
                rank.prefix = ChatColor.GOLD.toString();
                rank.description = null;
                rank.priority = priortiy;
                rank.partyId = party.getId();

                getModule().getTable(PartyRanksTable.class).insert(rank);

                sender.sendMessage(GENERAL_SUCCESS.format());
                break;
            }

            case "delete": {
                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.MANAGE_RANKS)) {
                    throw new NotEnoughPermissionsException();
                }
                if (args.length < 3) {
                    throw new NotEnoughArgumentsException();
                }

                PartyRank rank = handleRank(sender, party, args[2], !isForceMode);
                if (rank == null) {
                    break;
                }

                int defaultRankId = party.getDefaultRank().id;
                if (defaultRankId == rank.id) {
                    sender.sendMessage(RpLanguage.m("Party.DeletingDefaultRank"));
                    break;
                }

                //Set all with this rank to default rank before deleting
                getModule().getTable(PartyUsersTable.class)
                        .executeUpdate("UPDATE `{TABLE}` SET `party_rank` = ? WHERE `party_id` = ? AND `party_rank` = ?", defaultRankId,
                                       party.getId(), rank.id);
                getModule().getTable(PartyRanksTable.class).executeUpdate("DELETE FROM {TABLE} WHERE id = ?", rank.id);

                sender.sendMessage(GENERAL_SUCCESS.format());
                break;
            }

            case "priority": {
                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.MANAGE_RANKS)) {
                    throw new NotEnoughPermissionsException();
                }
                if (args.length < 4) {
                    throw new NotEnoughArgumentsException();
                }

                PartyRank rank = handleRank(sender, party, args[2], !isForceMode);
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
                    getModule().getTable(PartyRanksTable.class).executeUpdate("UPDATE `{TABLE}` SET `priority`=? WHERE `id` = ?", priortiy, rank.id);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                sender.sendMessage(GENERAL_SUCCESS.format());
                break;
            }

            case "prefix": {
                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.MANAGE_RANKS)) {
                    throw new NotEnoughPermissionsException();
                }
                if (args.length < 4) {
                    throw new NotEnoughArgumentsException();
                }
                PartyRank rank = handleRank(sender, party, args[2], !isForceMode);
                if (rank == null) {
                    break;
                }

                String prefix = ChatColor.translateAlternateColorCodes('&', StringUtil.formatArrayToString(args, " ", 3, args.length));
                try {
                    getModule().getTable(PartyRanksTable.class).executeUpdate("UPDATE `{TABLE}` SET `prefix`=? WHERE `id` = ?", prefix, rank.id);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                sender.sendMessage(GENERAL_SUCCESS_SET.format("Prefix", prefix));
                break;
            }

            case "description": {
                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.MANAGE_RANKS)) {
                    throw new NotEnoughPermissionsException();
                }
                if (args.length < 4) {
                    throw new NotEnoughArgumentsException();
                }

                PartyRank rank = handleRank(sender, party, args[2], !isForceMode);
                if (rank == null) {
                    break;
                }

                String description = ChatColor.translateAlternateColorCodes('&', StringUtil.formatArrayToString(args, " ", 3, args.length));
                if (description.trim().equalsIgnoreCase("null") || description.trim().equalsIgnoreCase("off") || description.trim()
                        .equalsIgnoreCase("remove")) {
                    description = null;
                }

                try {
                    getModule().getTable(PartyRanksTable.class)
                            .executeUpdate("UPDATE `{TABLE}` SET `description`=? WHERE `id` = ?", description, rank.id);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                sender.sendMessage(GENERAL_SUCCESS_SET.format("Description", description));
                break;
            }

            case "rename": {
                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.MANAGE_RANKS)) {
                    throw new NotEnoughPermissionsException();
                }
                if (args.length < 4) {
                    throw new NotEnoughArgumentsException();
                }

                PartyRank rank = handleRank(sender, party, args[2], !isForceMode);
                if (rank == null) {
                    break;
                }

                try {
                    getModule().getTable(PartyRanksTable.class)
                            .executeUpdate("UPDATE `{TABLE}` SET `name`=? WHERE `id` = ?", args[3], rank.id);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                sender.sendMessage(GENERAL_SUCCESS_SET.format("Name", args[3]));
                break;
            }

            case "set": {
                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.SET_RANK)) {
                    throw new NotEnoughPermissionsException();
                }
                if (args.length < 4) {
                    throw new NotEnoughArgumentsException();
                }

                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[2]);
                if (!party.isMember(target.getUniqueId())) {
                    sender.sendMessage(RpLanguage.m("Party.UserNotMember", target.getDisplayName(), party.getFormattedName()));
                    break;
                }

                PartyRank rank = handleRank(sender, party, args[3], !isForceMode);
                if (rank == null) {
                    break;
                }

                party.setRank(target, rank);

                sender.sendMessage(GENERAL_SUCCESS_SET.format("Rank of " + target.getName(), rank.name));
                break;
            }
            case "setpermission":
                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.MANAGE_RANKS)) {
                    throw new NotEnoughPermissionsException();
                }
                if (args.length < 5) {
                    throw new NotEnoughArgumentsException();
                }

                PartyRank rank = handleRank(sender, party, args[2], !isForceMode);
                if (rank == null) {
                    break;
                }
                PartyPermission permission = PartyPermission.getPermission(args[3].toUpperCase());
                if (permission == null) {
                    sender.sendMessage(RpLanguage.m("Party.UnknownPermission", args[3].toUpperCase()));
                    break;
                }

                if(!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, permission)) {
                    sender.sendMessage(RpLanguage.m("Party.NotEnoughPriority"));
                    break;
                }

                String[] valueArgs = new String[args.length - 4];
                int valuePosition = 0;
                for (int i = 4; i < args.length; i++) {
                    valueArgs[valuePosition] = args[i];
                    valuePosition++;
                }
                Object value = CommandUtil.parseValue(valueArgs);
                getModule().getTable(PartyRankPermissionsTable.class).setOption(permission.getPermissionString(), value, rank.id);
                sender.sendMessage(GENERAL_SUCCESS_SET.format(permission, Objects.toString(value)));
                break;

            default:
                sender.sendMessage("Unknown subcommand: " + args[1]);
                break;
        }
    }

    private PartyRank handleRank(CommandSender sender, Party party, String rankName, boolean checkPriority) {
        PartyRank targetRank = party.getRank(rankName);
        if (targetRank == null) {
            sender.sendMessage(RpLanguage.m("Party.RankNotFound", rankName));
            return null;
        }

        if (checkPriority) {
            PartyRank userRank = party.getRank(SinkLibrary.getInstance().getIngameUser(((Player) sender)));

            if (targetRank.priority < userRank.priority) {
                sender.sendMessage(RpLanguage.m("Party.NotEnoughPriority"));
                return null;
            }
        }
        return targetRank;
    }

    private void sendInfo(CommandSender sender, Party party) {
        sender.sendMessage("");
        sender.sendMessage(
                ChatColor.GOLD + "------ Partei: " + party.getFormattedName() + ChatColor.GRAY + " (" + party.getTag() + ")" + ChatColor.GOLD
                + " ------");
        if (!StringUtil.isEmptyOrNull(party.getDescription())) {
            sender.sendMessage(ChatColor.GOLD + "Beschreibung:");
            for (String s : party.getDescription().split("&nl")) {
                sender.sendMessage(ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', s));
            }

            sender.sendMessage("");
        }
        sender.sendMessage(ChatColor.GOLD + "Betrag: " + ChatColor.GRAY + MathUtil.round(party.getBalance()));

        int limit = 10;

        boolean membersFound = false;
        List<PartyRank> ranks = party.getRanks();
        for (PartyRank rank : ranks) {
            List<PartyUser> rankUsers = party.getRankUsers(rank);
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

            int index = 0;
            for (PartyUser user : rankUsers) {
                if (index == limit) {
                    users += ChatColor.RED + " +" + (rankUsers.size() - index) + " weitere...";
                    break;
                }

                String formattedName = PartyManager.getInstance().getFormattedName(user);
                if (users.equals("")) {
                    users = ChatColor.GRAY + "    " + formattedName;
                    continue;
                }

                users += ChatColor.GRAY + ", " + formattedName;
                index++;
            }

            sender.sendMessage(users);
        }

        if (!membersFound) {
            sender.sendMessage(ChatColor.RED + "Keine Mitglieder gefunden");
        }
        sender.sendMessage("");
    }

    private void sendPartyList(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "------------" + ChatColor.GRAY + " Parteien " + ChatColor.GOLD + "------------");

        boolean partyFound = PartyManager.getInstance().getParties().size() > 0;
        if (partyFound) {
            for (Party p : PartyManager.getInstance().getParties()) {
                String
                        formattedParty =
                        ChatColor.AQUA + p.getFormattedName() + ChatColor.GRAY + " (" + p.getTag() + ")" + ChatColor.AQUA + " [" + ChatColor.WHITE
                        + +p.getMembers().size() + ChatColor.AQUA + "]";
                sender.sendMessage("• " + formattedParty);
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Keine Parteien gefunden!");
        }
    }
}
