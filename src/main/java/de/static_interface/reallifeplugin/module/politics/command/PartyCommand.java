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
import de.static_interface.reallifeplugin.module.politics.PartyManager;
import de.static_interface.reallifeplugin.module.politics.PartyPermission;
import de.static_interface.reallifeplugin.module.politics.PoliticsModule;
import de.static_interface.reallifeplugin.module.politics.database.row.PartyRank;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyRanksTable;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyUsersTable;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.exception.NotEnoughArgumentsException;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.BukkitUtil;
import de.static_interface.sinklibrary.util.StringUtil;
import org.apache.commons.cli.ParseException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.UUID;

public class PartyCommand extends ModuleCommand<PoliticsModule> {

    public PartyCommand(PoliticsModule module) {
        super(module);
        getCommandOptions().setPlayerOnly(true);
    }

    @Override
    protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
        Party party = PartyManager.getInstance().getParty(SinkLibrary.getInstance().getIngameUser((Player) sender));
        if (party == null && args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Du bist in keiner Partei");
            return true;
        }
        if (party != null && args.length < 1) {
            sendInfo(sender, party);
            return true;
        }

        UUID uuid = ((Player) sender).getUniqueId();

        switch (args[0].toLowerCase().trim()) {
            case "list":
                sendPartyList(sender);
                break;
            case "join":
                break;
            case "invite":
                if (!PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.INVITE)) {
                    sender.sendMessage(m("Permission.General"));
                    break;
                }
                break;
            case "kick":
                if (!PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.KICK)) {
                    sender.sendMessage(m("Permission.General"));
                    break;
                }

                break;
            case "new":
                if (args.length < 4) {
                    throw new NotEnoughArgumentsException();
                }
                String name = args[1];
                String tag = args[2];
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
                break;
            case "rank":
                handleRankCommand(sender, party, args);
                break;
            case "delete":
                break;
            default:
                if (args.length < 2) {
                    throw new NotEnoughArgumentsException();
                }
                party = PartyManager.getInstance().getParty(args[1]);
                if (party == null) {
                    sender.sendMessage(ChatColor.RED + "Partei nicht gefunden");
                    break;
                }
                sendInfo(sender, party);
                break;
        }

        return true;
    }

    private void handleRankCommand(CommandSender sender, Party party, String[] args) {
        UUID uuid = ((Player) sender).getUniqueId();

        switch (args[1].toLowerCase().trim()) {
            case "new": {
                if (!PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.MANAGE_RANKS)) {
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

                PartyRank userRank = party.getUserRank(SinkLibrary.getInstance().getIngameUser(((Player) sender)));

                if (priortiy < userRank.priority) {
                    sender.sendMessage(ReallifeLanguageConfiguration.m("Party.NotEnoughPriority"));
                    break;
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
                if (!PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.MANAGE_RANKS)) {
                    sender.sendMessage(m("Permission.General"));
                    break;
                }
                if (args.length < 3) {
                    throw new NotEnoughArgumentsException();
                }

                PartyRank rank = handleRank(sender, party, args[2]);
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
                if (!PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.MANAGE_RANKS)) {
                    sender.sendMessage(m("Permission.General"));
                    break;
                }
                if (args.length < 4) {
                    throw new NotEnoughArgumentsException();
                }

                PartyRank rank = handleRank(sender, party, args[2]);
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
                if (!PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.MANAGE_RANKS)) {
                    sender.sendMessage(m("Permission.General"));
                    break;
                }
                if (args.length < 4) {
                    throw new NotEnoughArgumentsException();
                }
                PartyRank rank = handleRank(sender, party, args[2]);
                if (rank == null) {
                    break;
                }

                String prefix = ChatColor.translateAlternateColorCodes('&', StringUtil.formatArrayToString(args, " ", 3, args.length));
                try {
                    getModule().getTable(PartyRanksTable.class).executeUpdate("UPDATE `{TABLE}` SET `prefix`=? WHERE `id` = ?", prefix, rank.id);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                sender.sendMessage(ReallifeLanguageConfiguration.m("General.Success"));
                break;
            }

            case "description": {
                if (!PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.MANAGE_RANKS)) {
                    sender.sendMessage(m("Permission.General"));
                    break;
                }
                if (args.length < 4) {
                    throw new NotEnoughArgumentsException();
                }

                PartyRank rank = handleRank(sender, party, args[2]);
                if (rank == null) {
                    break;
                }

                String description = ChatColor.translateAlternateColorCodes('&', args[3]);
                try {
                    getModule().getTable(PartyRanksTable.class)
                            .executeUpdate("UPDATE `{TABLE}` SET `description`=? WHERE `id` = ?", description, rank.id);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                sender.sendMessage(ReallifeLanguageConfiguration.m("General.Success"));
                break;
            }

            case "set":
                if (!PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.SET_RANK)) {
                    sender.sendMessage(m("Permission.General"));
                    break;
                }
                if (args.length < 4) {
                    throw new NotEnoughArgumentsException();
                }

                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[2]);

                PartyRank rank = handleRank(sender, party, args[3]);
                if (rank == null) {
                    break;
                }

                try {
                    int userId = PartyManager.getInstance().getUserId(target.getUniqueId());
                    getModule().getTable(PartyUsersTable.class).executeUpdate("UPDATE `{TABLE}` SET `party_rank`=? WHERE `id` = ?", rank.id, userId);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                sender.sendMessage(ReallifeLanguageConfiguration.m("General.Success"));
                break;

            case "permission":
                if (!PartyManager.getInstance().hasPartyPermission(uuid, PartyPermission.MANAGE_RANKS)) {
                    sender.sendMessage(m("Permission.General"));
                    break;
                }
                break;

            default:
                sender.sendMessage("Unknown subcommand:" + args[2]);
                break;
        }
    }

    private PartyRank handleRank(CommandSender sender, Party party, String rankName) {
        PartyRank targetRank = party.getRank(rankName);
        if (targetRank == null) {
            sender.sendMessage(ReallifeLanguageConfiguration.m("Party.RankNotFound", rankName));
            return null;
        }
        PartyRank userRank = party.getUserRank(SinkLibrary.getInstance().getIngameUser(((Player) sender)));

        if (targetRank.priority < userRank.priority) {
            sender.sendMessage(ReallifeLanguageConfiguration.m("Party.NotEnoughPriority"));
            return null;
        }
        return targetRank;
    }

    private void sendInfo(CommandSender sender, Party party) {
        sender.sendMessage(ChatColor.GOLD + "------ Partei: " + party.getFormattedName() + ChatColor.GOLD + " ------");
        //Todo
    }

    private void sendPartyList(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "------ Parteien ------");

        String s = "";
        for (Party p : PartyManager.getInstance().getParties()) {
            String formattedParty = ChatColor.AQUA + p.getFormattedName() + " [" + ChatColor.WHITE + +p.getMembers().size() + ChatColor.AQUA + "]";
            if (s.equals("")) {
                s = formattedParty;
                continue;
            }
            s += ChatColor.RESET.toString() + ChatColor.GRAY + ", " + formattedParty;
        }
        if (s.equals("")) {
            s = ChatColor.RED + "Keine Parteien gefunden!";
        }
        sender.sendMessage(s);
    }
}
