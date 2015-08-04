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

import de.static_interface.reallifeplugin.module.ModuleCommand;
import de.static_interface.reallifeplugin.module.politics.Party;
import de.static_interface.reallifeplugin.module.politics.PoliticsModule;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.exception.NotEnoughArgumentsException;
import de.static_interface.sinklibrary.util.BukkitUtil;
import org.apache.commons.cli.ParseException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PartyCommand extends ModuleCommand<PoliticsModule> {

    public PartyCommand(PoliticsModule module) {
        super(module);
        getCommandOptions().setPlayerOnly(true);
    }

    @Override
    protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
        Party party = getModule().getParty(SinkLibrary.getInstance().getIngameUser((Player) sender));
        if (party == null && args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Du bist in keiner Partei");
            return true;
        }
        if (party != null && args.length < 1) {
            sendInfo(sender, party);
            return true;
        }

        switch (args[0].toLowerCase().trim()) {
            case "list":
                sendPartyList(sender);
                break;
            case "join":
                break;
            case "invite":
                break;
            case "new":
                if (args.length < 3) {
                    throw new NotEnoughArgumentsException();
                }
                String name = args[1];
                String owner = args[2];
                UUID uuid = BukkitUtil.getUniqueIdByName(owner);
                PartyUtil.createNewPary(SinkLibrary.getInstance().getUser((Object) sender), name, uuid);
                break;
            case "leave":
                break;
            case "rank":
                break;
            case "delete":
                break;
            default:
                if (args.length < 2) {
                    throw new NotEnoughArgumentsException();
                }
                party = getModule().getParty(args[1]);
                if (party == null) {
                    sender.sendMessage(ChatColor.RED + "Partei nicht gefunden");
                    break;
                }
                sendInfo(sender, party);
                break;
        }

        return true;
    }

    private void sendInfo(CommandSender sender, Party party) {
        sender.sendMessage(ChatColor.GOLD + "------ Partei: " + party.getFormattedName() + ChatColor.GOLD + " ------");
        //Todo
    }

    private void sendPartyList(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "------ Parteien ------");

        String s = "";
        for (Party p : getModule().getParties()) {
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
