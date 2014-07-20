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
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.static_interface.reallifeplugin.LanguageConfiguration._;

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
            user.sendMessage(_("Corporations.NotInCorporation"));
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
            case "ceo":
            {
                if (!user.isConsole())
                {
                    if (moreArgs.length < 1)
                    {
                        user.sendMessage(LanguageConfiguration._("General.CommandMisused.Arguments.TooFew"));
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
                    user.sendMessage(LanguageConfiguration._("Permissions.General"));
                    return true;
                }
                if (moreArgs.length < 1)
                {
                    user.sendMessage(LanguageConfiguration._("General.CommandMisused.Arguments.TooFew"));
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
        switch(args[0])
        {
            case "new":
            {
                if (args.length < 5)
                {
                    user.sendMessage(de.static_interface.sinklibrary.configuration.LanguageConfiguration._("General.CommandMisused.Arguments.TooFew"));
                    return;
                }

                boolean successful = CorporationUtil.createCorporation(args);
                String msg = successful ? _("Corporation.Created") : _("Corporation.CreationFailed");
                msg = String.format(msg, args[1]);
                user.sendMessage(msg);
                break;
            }

            case "delete":
            {
                if (args.length < 2)
                {
                    user.sendMessage(de.static_interface.sinklibrary.configuration.LanguageConfiguration._("General.CommandMisused.Arguments.TooFew"));
                    return;
                }

                boolean successful = CorporationUtil.deleteCorporation(args[1]);

                String msg = successful ? _("Corporation.Deleted") : _("Corporation.DeletionFailed");
                msg = String.format(msg, args[1]);
                user.sendMessage(msg);
                break;
            }

            case "setbase":
            {
                if (args.length < 3)
                {
                    user.sendMessage(de.static_interface.sinklibrary.configuration.LanguageConfiguration._("General.CommandMisused.Arguments.TooFew"));
                    return;
                }
                Corporation corporation= CorporationUtil.getCorporation(args[1]);
                if (corporation == null)
                {
                    user.sendMessage(String.format(_("Corporation.DoesntExists"),args[1]));
                    return;
                }
                corporation.setBase(user.getPlayer().getWorld(), args[2]);
                user.sendMessage(_("Corporation.BaseSet"));
                break;
            }
        }
    }

    private void handleCEOCommand(User user, String[] args, Corporation corporation)
    {
        if (!CorporationUtil.isCEO(user, corporation))
        {
            user.sendMessage(_("Corporation.NotCEO"));
            return;
        }
        switch(args[0].toLowerCase())
        {
            case "kick":
            {
                if (args.length < 2)
                {
                    return;
                }
                User target = SinkLibrary.getUser(args[1]);
                corporation.removeMember(target.getUniqueId());
                if (user.isOnline())
                {
                    user.sendMessage(String.format(_("Corporation.Kicked"), corporation.getName()));
                }
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
                if (user.isOnline())
                {
                    user.sendMessage(String.format(_("Corporation.Added"), corporation.getName()));
                }
                break;
            }
        }
    }

    private void sendCorporationInfo(User user, Corporation corporation)
    {
        throw new NotImplementedException();
    }
}
