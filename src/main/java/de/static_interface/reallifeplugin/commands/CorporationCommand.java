/*
 * Copyright (c) 2014 http://adventuria.eu, http://static-interface.de and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.static_interface.reallifeplugin.commands;

import static de.static_interface.reallifeplugin.LanguageConfiguration.m;

import de.static_interface.reallifeplugin.corporation.Corporation;
import de.static_interface.reallifeplugin.corporation.CorporationUtil;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.SinkUser;
import de.static_interface.sinklibrary.configuration.LanguageConfiguration;
import de.static_interface.sinklibrary.util.StringUtil;
import de.static_interface.sinklibrary.util.VaultHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CorporationCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        SinkUser user = SinkLibrary.getInstance().getUser(sender);
        Corporation userCorp = CorporationUtil.getUserCorporation(user.getUniqueId());

        if (args.length < 1 && !user.isConsole()) {
            if (userCorp != null) {
                sendCorporationInfo(user, userCorp);
                return true;
            }
            user.sendMessage(m("Corporation.NotInCorporation"));
            return true;
        } else if (args.length < 1 && user.isConsole()) {
            return false;
        }

        List<String> tmp = new ArrayList<>(Arrays.asList(args));
        tmp.remove(args[0]);
        String[] moreArgs = tmp.toArray(new String[tmp.size()]);

        switch (args[0].toLowerCase()) {
            case "help": {
                sendHelp(user);
                break;
            }

            case "?": {
                sendHelp(user);
                break;
            }

            case "ceo": {
                if (!user.isConsole()) {
                    if (moreArgs.length < 1) {
                        user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                        return true;
                    }
                    handleCEOCommand(user, moreArgs, userCorp);
                    break;
                }
            }

            case "admin": {
                if (!user.hasPermission("reallifeplugin.corporations.admin")) {
                    user.sendMessage(LanguageConfiguration.m("Permissions.General"));
                    return true;
                }
                if (moreArgs.length < 1) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return true;
                }
                handleAdminCommand(user, moreArgs);
                break;
            }

            default: {
                Corporation corporation = CorporationUtil.getCorporation(args[0]);
                sendCorporationInfo(user, corporation);
                break;
            }
        }

        return true;
    }

    private void handleAdminCommand(SinkUser user, String[] args) {
        switch (args[0].toLowerCase()) {
            case "help": {
                sendAdminHelp(user);
                break;
            }

            case "?": {
                sendAdminHelp(user);
                break;
            }

            case "new": {
                if (args.length < 4) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }
                //Todo: remove ceo from any corporation
                //Todo cancel if ceo is already a ceo of another corporation
                String name = args[1];
                UUID ceo = SinkLibrary.getInstance().getUser(args[2]).getUniqueId();
                String base = args[3];

                boolean successful = CorporationUtil.createCorporation(user, name, ceo, base, user.getPlayer().getWorld());
                Corporation corp = CorporationUtil.getCorporation(name);
                String msg = successful ? m("Corporation.Created") : m("Corporation.CreationFailed");
                msg = StringUtil.format(msg, corp.getFormattedName());
                user.sendMessage(msg);
                break;
            }

            case "delete": {
                if (args.length < 2) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }

                Corporation corp = CorporationUtil.getCorporation(args[1]);
                boolean successful = CorporationUtil.deleteCorporation(user, corp);

                if (successful) {
                    user.sendMessage(StringUtil.format(m("Corporation.Deleted"), corp.getFormattedName()));
                }
                break;
            }

            case "setbase": {
                if (args.length < 3) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }
                Corporation corporation = CorporationUtil.getCorporation(args[1]);
                if (corporation == null) {
                    user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[1]));
                    return;
                }
                corporation.setBase(user.getPlayer().getWorld(), args[2]);
                user.sendMessage(m("Corporation.BaseSet"));
                break;
            }

            case "setceo": {
                if (args.length < 3) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }
                Corporation corporation = CorporationUtil.getCorporation(args[1]);
                if (corporation == null) {
                    user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[1]));
                    return;
                }
                SinkUser newCEO = SinkLibrary.getInstance().getUser(args[2]);
                corporation.setCEO(newCEO.getUniqueId());
                user.sendMessage(m("Corporation.CEOSet"));
                break;
            }

            default: {
                user.sendDebugMessage(ChatColor.RED + "Unknown subcommand: " + args[0]);
                break;
            }
        }
    }

    private void handleCEOCommand(SinkUser user, String[] args, Corporation corporation) {
        if (!CorporationUtil.isCEO(user, corporation)) {
            user.sendMessage(m("Corporation.NotCEO"));
            return;
        }
        switch (args[0].toLowerCase()) {
            case "help": {
                sendCEOHelp(user);
                break;
            }

            case "?": {
                sendCEOHelp(user);
                break;
            }

            case "add": {
                if (args.length < 2) {
                    user.sendMessage(ChatColor.RED + "/corp ceo help");
                    break;
                }
                //Todo: Check if targt is already in a corporation
                SinkUser target = SinkLibrary.getInstance().getUser(args[1]);

                if (corporation.getMembers().contains(target.getUniqueId())) {
                    user.sendMessage(StringUtil.format(m("Corporation.AlreadyMember"), target.getName()));
                    break;
                }

                corporation.addMember(target.getUniqueId());
                if (target.isConsole()) {
                    target.sendMessage(StringUtil.format(m("Corporation.Added"), corporation.getName()));
                }
                user.sendMessage(StringUtil.format(m("Corporation.CEOAdded"), CorporationUtil.getFormattedName(target.getUniqueId())));
                break;
            }

            case "kick": {
                if (args.length < 2) {
                    user.sendMessage(ChatColor.RED + "/corp ceo help");
                    break;
                }
                SinkUser target = SinkLibrary.getInstance().getUser(args[1]);

                if (!corporation.getMembers().contains(target.getUniqueId())) {
                    user.sendMessage(StringUtil.format(m("Corporation.NotMember"), target.getName()));
                    break;
                }

                corporation.removeMember(target.getUniqueId());
                if (target.isConsole()) {
                    target.sendMessage(StringUtil.format(m("Corporation.Kicked"), corporation.getName()));
                }
                user.sendMessage(StringUtil.format(m("Corporation.CEOKicked"), CorporationUtil.getFormattedName(target.getUniqueId())));
                break;
            }
        }
    }

    private void sendHelp(SinkUser user) {
        user.sendMessage(ChatColor.RED + "Corp Commands: ");
        user.sendMessage(ChatColor.GOLD + "/corp");
        user.sendMessage(ChatColor.GOLD + "/corp help");
        user.sendMessage(ChatColor.GOLD + "/corp <corp>");
        user.sendMessage(ChatColor.GOLD + "/corp ceo help");
        user.sendMessage(ChatColor.GOLD + "/corp admin help");
    }

    private void sendAdminHelp(SinkUser user) {
        user.sendMessage(ChatColor.RED + "Admin Commands: ");
        user.sendMessage(ChatColor.GOLD + "/corp admin new <corp> <ceo> <base>");
        user.sendMessage(ChatColor.GOLD + "/corp admin delete <corp>");
        user.sendMessage(ChatColor.GOLD + "/corp admin setbase <corp> <region>");
        user.sendMessage(ChatColor.GOLD + "/corp admin setceo <corp> <player>");
    }

    private void sendCEOHelp(SinkUser user) {
        user.sendMessage(ChatColor.RED + "CEO Commands: ");
        user.sendMessage(ChatColor.GOLD + "/corp ceo add <name>");
        user.sendMessage(ChatColor.GOLD + "/corp ceo kick <name>");
    }

    private void sendCorporationInfo(SinkUser user, Corporation corporation) {
        if (corporation == null) {
            user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), ""));
            return;
        }
        user.sendMessage("");
        String text = ChatColor.GOLD + " Corporation: " + corporation.getFormattedName();
        String divider = "";
        for (int i = 0; i < 32; i++) {
            divider += "-";
        }
        user.sendMessage(ChatColor.RED + text);
        user.sendMessage(ChatColor.GOLD + divider);
        OfflinePlayer ceo = Bukkit.getOfflinePlayer(corporation.getCEO());
        user.sendMessage(ChatColor.GRAY + "CEO: " + CorporationUtil.getFormattedName(ceo.getUniqueId()));
        user.sendMessage(ChatColor.GRAY + "Base: " + ChatColor.GOLD + corporation.getBase().getId());
        user.sendMessage(ChatColor.GRAY + "Money: " + ChatColor.GOLD + corporation.getMoney() + " " + VaultHelper.getCurrenyName());
        String members = "";
        for (UUID member : corporation.getMembers()) {
            String name = CorporationUtil.getFormattedName(member);
            if (members.equals("")) {
                members = name;
                continue;
            }
            members += ChatColor.GRAY + ", " + name;
        }
        user.sendMessage(ChatColor.GRAY + "Members: " + ChatColor.GOLD + members);
        user.sendMessage(ChatColor.GOLD + divider);
        user.sendMessage("");
    }
}
