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

import de.static_interface.reallifeplugin.corporation.Corporation;
import de.static_interface.reallifeplugin.corporation.CorporationUtil;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.User;
import de.static_interface.sinklibrary.configuration.LanguageConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static de.static_interface.reallifeplugin.LanguageConfiguration.m;

public class CorporationCommand implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        User user = SinkLibrary.getUser(sender);
        Corporation userCorp = CorporationUtil.getUserCorporation(user.getUniqueId());

        if(args.length < 1 && !user.isConsole())
        {
            if (userCorp != null)
            {
                sendCorporationInfo(user, userCorp);
                return true;
            }
            user.sendMessage(m("Corporations.NotInCorporation"));
            return true;
        }
        else if (args.length < 1 && user.isConsole())
        {
            return false;
        }

        List<String> tmp = new ArrayList<>(Arrays.asList(args));
        tmp.remove(args[0]);
        String[] moreArgs= tmp.toArray(new String[tmp.size()]);

        switch(args[0].toLowerCase())
        {
            case "help":
            {
                sendHelp(user);
                break;
            }

            case "?":
            {
                sendHelp(user);
                break;
            }

            case "ceo":
            {
                if (!user.isConsole())
                {
                    if (moreArgs.length < 1)
                    {
                        user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                        return true;
                    }
                    handleCEOCommand(user, moreArgs, userCorp);
                    break;
                }
            }

            case "admin":
            {
                if (!user.hasPermission("reallifeplugin.corporations.admin"))
                {
                    user.sendMessage(LanguageConfiguration.m("Permissions.General"));
                    return true;
                }
                if (moreArgs.length < 1)
                {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return true;
                }
                handleAdminCommand(user, moreArgs);
                break;
            }

            default:
            {
                Corporation corporation = CorporationUtil.getCorporation(args[0]);
                sendCorporationInfo(user, corporation);
                break;
            }
        }

        return true;
    }

    private void handleAdminCommand(User user, String[] args)
    {
        switch(args[0].toLowerCase())
        {
            case "help":
            {
                sendAdminHelp(user);
                break;
            }

            case "?":
            {
                sendAdminHelp(user);
                break;
            }

            case "new":
            {
                if (args.length < 4)
                {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }

                String name = args[1];
                UUID ceo = SinkLibrary.getUser(args[2]).getUniqueId();
                String base = args[3];

                boolean successful = CorporationUtil.createCorporation(user, name, ceo , base, user.getPlayer().getWorld());
                Corporation corp = CorporationUtil.getCorporation(name);
                String msg = successful ? m("Corporation.Created") : m("Corporation.CreationFailed");
                msg = String.format(msg, corp.getFormattedName());
                user.sendMessage(msg);
                break;
            }

            case "delete":
            {
                if (args.length < 2)
                {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }

                Corporation corp = CorporationUtil.getCorporation(args[1]);
                boolean successful = CorporationUtil.deleteCorporation(user, corp);

                if (successful)
                {
                    user.sendMessage(String.format(m("Corporation.Deleted"), corp.getFormattedName()));
                }
                break;
            }

            case "setbase":
            {
                if (args.length < 3)
                {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }
                Corporation corporation= CorporationUtil.getCorporation(args[1]);
                if (corporation == null)
                {
                    user.sendMessage(String.format(m("Corporation.DoesntExists"),args[1]));
                    return;
                }
                corporation.setBase(user.getPlayer().getWorld(), args[2]);
                user.sendMessage(m("Corporation.BaseSet"));
                break;
            }

            case "setceo":
            {
                if (args.length < 3)
                {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }
                Corporation corporation= CorporationUtil.getCorporation(args[1]);
                if (corporation == null)
                {
                    user.sendMessage(String.format(m("Corporation.DoesntExists"),args[1]));
                    return;
                }
                User newCEO = SinkLibrary.getUser(args[2]);
                corporation.setCEO(newCEO.getUniqueId());
                user.sendMessage(m("Corporation.CEOSet"));
                break;
            }

            default:
            {
                user.sendDebugMessage(ChatColor.RED + "Unknown subcommand: " + args[0]);
                break;
            }
        }
    }

    private void handleCEOCommand(User user, String[] args, Corporation corporation)
    {
        if (!CorporationUtil.isCEO(user, corporation))
        {
            user.sendMessage(m("Corporation.NotCEO"));
            return;
        }
        switch(args[0].toLowerCase())
        {
            case "help":
            {
                sendCEOHelp(user);
                break;
            }

            case "?":
            {
                sendCEOHelp(user);
                break;
            }

            case "add":
            {
                if (args.length < 2)
                {
                    return;
                }

                User target = SinkLibrary.getUser(args[1]);
                corporation.addMember(target.getUniqueId());
                user.sendMessage(String.format(m("Corporation.Added"), corporation.getName()));
                break;
            }

            case "kick":
            {
                if (args.length < 2)
                {
                    return;
                }
                User target = SinkLibrary.getUser(args[1]);
                corporation.removeMember(target.getUniqueId());
                user.sendMessage(String.format(m("Corporation.Kicked"), corporation.getName()));
                break;
            }
        }
    }

    private void sendHelp(User user)
    {
        user.sendMessage(ChatColor.RED + "Corp Commands: ");
        user.sendMessage(ChatColor.GOLD + "/corp");
        user.sendMessage(ChatColor.GOLD + "/corp help");
        user.sendMessage(ChatColor.GOLD + "/corp <corp>");
        user.sendMessage(ChatColor.GOLD + "/corp ceo help");
        user.sendMessage(ChatColor.GOLD + "/corp admin help");
    }

    private void sendAdminHelp(User user)
    {
        user.sendMessage(ChatColor.RED + "Admin Commands: ");
        user.sendMessage(ChatColor.GOLD + "/corp admin new <corp> <ceo> <base>");
        user.sendMessage(ChatColor.GOLD + "/corp admin delete <corp>");
        user.sendMessage(ChatColor.GOLD + "/corp admin setbase <corp> <region>");
        user.sendMessage(ChatColor.GOLD + "/corp admin setceo <corp> <player>");
    }

    private void sendCEOHelp(User user)
    {
        user.sendMessage(ChatColor.RED + "CEO Commands: ");
        user.sendMessage(ChatColor.GOLD +  "/corp ceo add <name>");
        user.sendMessage(ChatColor.GOLD +  "/corp ceo kick <name>");
    }

    private void sendCorporationInfo(User user, Corporation corporation)
    {
        //Todo
        user.sendMessage(corporation.toString());
    }
}
