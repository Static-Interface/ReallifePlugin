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

import static de.static_interface.sinklibrary.configuration.LanguageConfiguration.m;

import de.static_interface.reallifeplugin.config.ReallifeLanguageConfiguration;
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
import de.static_interface.sinklibrary.api.exception.NotEnoughArgumentsException;
import de.static_interface.sinklibrary.api.exception.UserNotOnlineException;
import de.static_interface.sinklibrary.api.user.SinkUser;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.BukkitUtil;
import de.static_interface.sinklibrary.util.MathUtil;
import de.static_interface.sinklibrary.util.StringUtil;
import de.static_interface.sinklibrary.util.VaultBridge;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PartyCommand extends ModuleCommand<PoliticsModule> {

    public PartyCommand(PoliticsModule module) {
        super(module);
        getCommandOptions().setPlayerOnly(true);
        getCommandOptions().setCmdLineSyntax("{PREFIX}{ALIAS} <options> [-p <party>]");
        getCommandOptions().setCliOptions(new Options());
        Option partyOption = Option.builder("p")
                .hasArg()
                .longOpt("party")
                .desc("Force as party")
                .type(String.class)
                .argName("party name")
                .build();
        Option forceOption = Option.builder("f")
                .longOpt("force")
                .desc("Force command")
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
                sender.sendMessage(ReallifeLanguageConfiguration.m("Party.PartyNotFound", getCommandLine().getOptionValue('p')));
                return true;
            }
        }

        if (party == null && args.length < 1) {
            sender.sendMessage(ReallifeLanguageConfiguration.m("Party.NotInParty"));
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
                if (args.length < 2) {
                    throw new NotEnoughArgumentsException();
                }
                try {
                    if (party == null) {
                        sender.sendMessage(ReallifeLanguageConfiguration.m("Party.NotInParty"));
                        break;
                    }
                    deposit((Player) sender, party, Double.valueOf(args[1]));
                } catch (NumberFormatException ignored) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("General.InvalidValue", args[1]));
                }
                break;

            case "withdraw":
                if (args.length < 2) {
                    throw new NotEnoughArgumentsException();
                }
                try {
                    withdraw((Player) sender, party, Double.valueOf(args[1]));
                } catch (NumberFormatException ignored) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("General.InvalidValue", args[1]));
                }
                break;

            case "join":
                if (args.length < 2) {
                    throw new NotEnoughArgumentsException();
                }
                if (party != null) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Party.AlreadyInParty", party.getFormattedName()));
                    break;
                }
                party = PartyManager.getInstance().getParty(args[1]);
                if (party == null) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Party.PartyNotFound", args[1]));
                    break;
                }

                if (!party.isPublic() && !PartyInviteQueue.hasInvite(uuid, party)) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Party.NotInvited", party.getFormattedName()));
                    break;
                }

                party.addMember(uuid, party.getDefaultRank().id);
                party.announce(ReallifeLanguageConfiguration.m("Party.Joined", ((Player) sender).getDisplayName()));
                PartyInviteQueue.remove(uuid, party);
                break;

            case "invite": {
                if (party == null) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Party.NotInParty"));
                    break;
                }

                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.INVITE)) {
                    sender.sendMessage(m("Permission.General"));
                    break;
                }
                if (args.length < 2) {
                    throw new NotEnoughArgumentsException();
                }

                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[1], true);
                if (!target.isOnline()) {
                    throw new UserNotOnlineException(target.getDisplayName());
                }

                if (PartyInviteQueue.hasInvite(uuid, party)) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Party.AlreadyInvited", target.getDisplayName()));
                    break;
                }

                if (!isExplicitForceMode) {
                    PartyInviteQueue.add(target.getUniqueId(), party);
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Party.UserHasBeenInvited", target.getDisplayName()));
                    target.sendMessage(
                            ReallifeLanguageConfiguration.m("Party.GotInvited", user.getDisplayName(), party.getFormattedName(), party.getTag()));
                    break;
                }

                Party p = PartyManager.getInstance().getParty(target);
                if (p != null) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Party.UserAlreadyInParty", target.getDisplayName(), p.getFormattedName()));
                    break;
                }

                party.addMember(target.getUniqueId(), party.getDefaultRank().id);
                sender.sendMessage(ReallifeLanguageConfiguration.m("General.Success"));
                break;
            }

            case "kick": {
                if (party == null) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Party.NotInParty"));
                    break;
                }
                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.KICK)) {
                    sender.sendMessage(m("Permission.General"));
                    break;
                }
                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[1], true);
                if (!target.isOnline()) {
                    throw new UserNotOnlineException(target.getDisplayName());
                }

                if (!party.isMember(target.getUniqueId())) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Party.UserNotMember", target.getDisplayName(), party.getFormattedName()));
                    break;
                }

                party.announce(ReallifeLanguageConfiguration.m("Party.Kicked", target.getDisplayName(), ((Player) sender).getDisplayName()));
                party.removeMember(target.getUniqueId());
                break;
            }

            case "new":
                if (!sender.hasPermission("ReallifePlugin.Party.New")) {
                    sender.sendMessage(m("Permission.General"));
                    break;
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
                try {
                    PartyManager.getInstance().createNewPary(name, tag, founderUuid);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                sender.sendMessage(ChatColor.GREEN + "Partei erfolgreich erstellt!");
                break;

            case "leave":
                if (party == null) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Party.NotInParty"));
                    break;
                }
                party.announce(ReallifeLanguageConfiguration.m("Party.Left", ((Player) sender).getDisplayName()));
                party.removeMember(((Player) sender).getUniqueId());
                break;

            case "rank":
                if (party == null) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Party.NotInParty"));
                    break;
                }
                handleRankCommand(sender, party, args, isForceMode);
                break;

            case "delete":
                if (party == null) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Party.NotInParty"));
                    break;
                }
                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.DELETE)) {
                    sender.sendMessage(m("Permission.General"));
                    break;
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
            player.sendMessage(ReallifeLanguageConfiguration.m("General.InvalidValue", amount));
            return;
        }

        if (VaultBridge.getBalance(player) < amount) {
            player.sendMessage(ReallifeLanguageConfiguration.m("General.NotEnoughMoney"));
            return;
        }

        VaultBridge.addBalance(player, -amount);
        party.addBalance(amount);

        party.announce(ReallifeLanguageConfiguration.m("Party.Deposit", player.getDisplayName(), amount));
    }

    private void withdraw(Player player, Party party, double amount) {
        if (amount < 1) {
            player.sendMessage(ReallifeLanguageConfiguration.m("General.InvalidValue", amount));
            return;
        }

        if (party.getBalance() < amount) {
            player.sendMessage(ReallifeLanguageConfiguration.m("Party.NotEnoughMoney"));
            return;
        }

        VaultBridge.addBalance(player, amount);
        party.addBalance(-amount);

        party.announce(ReallifeLanguageConfiguration.m("Party.Withdraw", player.getDisplayName(), amount));
    }

    private void handleRankCommand(CommandSender sender, Party party, String[] args, boolean isForceMode) {
        UUID uuid = ((Player) sender).getUniqueId();

        switch (args[1].toLowerCase().trim()) {
            case "permissionslist": {
                PartyRank rank = party.getRank(SinkLibrary.getInstance().getIngameUser((Player) sender));
                if (args.length >= 3) {
                    rank = handleRank(sender, party, args[2], false);
                    if (rank == null) {
                        break;
                    }
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
                    sender.sendMessage(m("Permission.General"));
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
                    PartyRank userRank = party.getRank(SinkLibrary.getInstance().getIngameUser(((Player) sender)));

                    if (priortiy < userRank.priority) {
                        sender.sendMessage(ReallifeLanguageConfiguration.m("Party.NotEnoughPriority"));
                        break;
                    }
                }
                PartyRank rank = new PartyRank();
                rank.name = name;
                rank.prefix = ChatColor.GOLD.toString();
                rank.description = null;
                rank.priority = priortiy;
                rank.partyId = party.getId();

                try {
                    getModule().getTable(PartyRanksTable.class).insert(rank);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                sender.sendMessage(ReallifeLanguageConfiguration.m("General.Success"));
                break;
            }

            case "delete": {
                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.MANAGE_RANKS)) {
                    sender.sendMessage(m("Permission.General"));
                    break;
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
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Party.DeletingDefaultRank"));
                    break;
                }

                try {
                    //Set all with this rank to default rank before deleting
                    getModule().getTable(PartyUsersTable.class)
                            .executeUpdate("UPDATE `{TABLE}` SET `party_rank` = ? WHERE `party_id` = ? AND `party_rank` = ?", defaultRankId,
                                           party.getId(), rank.id);
                    getModule().getTable(PartyRanksTable.class).executeUpdate("DELETE FROM {TABLE} WHERE id = ?", rank.id);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                sender.sendMessage(ReallifeLanguageConfiguration.m("General.Success"));
                break;
            }

            case "priority": {
                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.MANAGE_RANKS)) {
                    sender.sendMessage(m("Permission.General"));
                    break;
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

                sender.sendMessage(ReallifeLanguageConfiguration.m("General.Success"));
                break;
            }

            case "prefix": {
                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.MANAGE_RANKS)) {
                    sender.sendMessage(m("Permission.General"));
                    break;
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

                sender.sendMessage(ReallifeLanguageConfiguration.m("General.SuccessSet", prefix));
                break;
            }

            case "description": {
                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.MANAGE_RANKS)) {
                    sender.sendMessage(m("Permission.General"));
                    break;
                }
                if (args.length < 4) {
                    throw new NotEnoughArgumentsException();
                }

                PartyRank rank = handleRank(sender, party, args[2], !isForceMode);
                if (rank == null) {
                    break;
                }

                String description = StringUtil.formatArrayToString(args, " ", 3, args.length);
                try {
                    getModule().getTable(PartyRanksTable.class)
                            .executeUpdate("UPDATE `{TABLE}` SET `description`=? WHERE `id` = ?", description, rank.id);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                sender.sendMessage(ReallifeLanguageConfiguration.m("General.SuccessSet", description));
                break;
            }

            case "rename": {
                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.MANAGE_RANKS)) {
                    sender.sendMessage(m("Permission.General"));
                    break;
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

                sender.sendMessage(ReallifeLanguageConfiguration.m("General.SuccessSet", args[3]));
                break;
            }

            case "set": {
                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.SET_RANK)) {
                    sender.sendMessage(m("Permission.General"));
                    break;
                }
                if (args.length < 4) {
                    throw new NotEnoughArgumentsException();
                }

                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[2]);

                PartyRank rank = handleRank(sender, party, args[3], !isForceMode);
                if (rank == null) {
                    break;
                }

                try {
                    int userId = PartyManager.getInstance().getUserId(target.getUniqueId());
                    getModule().getTable(PartyUsersTable.class).executeUpdate("UPDATE `{TABLE}` SET `party_rank`=? WHERE `id` = ?", rank.id, userId);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                sender.sendMessage(ReallifeLanguageConfiguration.m("General.SuccessSet", rank.name));
                break;
            }
            case "setpermission":
                if (!isForceMode && !PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.MANAGE_RANKS)) {
                    sender.sendMessage(m("Permission.General"));
                    break;
                }
                if (args.length < 5) {
                    throw new NotEnoughArgumentsException();
                }

                PartyRank rank = handleRank(sender, party, args[2], !isForceMode);
                if (rank == null) {
                    break;
                }
                PartyPermission permission = PartyPermission.valueOf(args[3].toUpperCase());
                if (permission == null) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Party.UnknownPermission", args[3].toUpperCase()));
                    break;
                }

                String[] valueArgs = new String[args.length - 4];
                int valuePosition = 0;
                for (int i = 4; i < args.length; i++) {
                    valueArgs[valuePosition] = args[i];
                    valuePosition++;
                }
                Object value = parseValue(valueArgs);
                getModule().getTable(PartyRankPermissionsTable.class).setOption(permission.getPermissionString(), value, rank.id);
                break;

            default:
                sender.sendMessage("Unknown subcommand: " + args[1]);
                break;
        }
    }

    private Object parseValue(String[] args) {
        String arg = args[0].trim();

        if (arg.equalsIgnoreCase("null")) {
            return null;
        }

        try {
            Long l = Long.parseLong(arg);
            if (l <= Byte.MAX_VALUE) {
                return Byte.parseByte(arg);
            } else if (l <= Short.MAX_VALUE) {
                return Short.parseShort(arg); // Value is a Short
            } else if (l <= Integer.MAX_VALUE) {
                return Integer.parseInt(arg); // Value is an Integer
            }
            return l; // Value is a Long
        } catch (NumberFormatException ignored) {
        }

        try {
            return Float.parseFloat(arg); // Value is Float
        } catch (NumberFormatException ignored) {
        }

        try {
            return Double.parseDouble(arg); // Value is Double
        } catch (NumberFormatException ignored) {
        }

        //Parse Booleans
        if (arg.equalsIgnoreCase("true")) {
            return true;
        } else if (arg.equalsIgnoreCase("false")) {
            return false;
        }

        if (arg.startsWith("'") && arg.endsWith("'") && arg.length() == 3) {
            return arg.toCharArray()[1]; // Value is char
        }

        String tmp = "";
        for (String s : args) {
            if (tmp.equals("")) {
                tmp = s;
            } else {
                tmp += " " + s;
            }
        }

        if (tmp.startsWith("[") && tmp.endsWith("]")) {
            List<String> list = Arrays.asList(tmp.substring(1, tmp.length() - 1).split(", "));
            List<Object> arrayList = new ArrayList<>();
            for (String s : list) {
                arrayList.add(parseValue(s.split(" ")));
            }
            return arrayList.toArray(new Object[arrayList.size()]);
        }

        return tmp; //is string
    }

    private PartyRank handleRank(CommandSender sender, Party party, String rankName, boolean checkPriority) {
        PartyRank targetRank = party.getRank(rankName);
        if (targetRank == null) {
            sender.sendMessage(ReallifeLanguageConfiguration.m("Party.RankNotFound", rankName));
            return null;
        }

        if (checkPriority) {
            PartyRank userRank = party.getRank(SinkLibrary.getInstance().getIngameUser(((Player) sender)));

            if (targetRank.priority < userRank.priority) {
                sender.sendMessage(ReallifeLanguageConfiguration.m("Party.NotEnoughPriority"));
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
            sender.sendMessage("");
            List<PartyUser> rankUsers = party.getRankUsers(rank);
            if (rankUsers.size() < 1) {
                continue;
            }
            membersFound = true;
            sender.sendMessage(ChatColor.GOLD + rank.name + ":");
            String users = "";

            if (!StringUtil.isEmptyOrNull(rank.description)) {
                sender.sendMessage(ChatColor.GRAY + "    Beschreibung: " + ChatColor.RESET + rank.description);
            }

            int index = 0;
            for (PartyUser user : rankUsers) {
                if (index == limit) {
                    users += ChatColor.RED + " +" + (rankUsers.size() - index) + " weitere...";
                    break;
                }

                String formattedName = PartyManager.getInstance().getFormattedName(user);
                if (users.equals("")) {
                    users = ChatColor.GRAY + "    -> " + formattedName;
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
