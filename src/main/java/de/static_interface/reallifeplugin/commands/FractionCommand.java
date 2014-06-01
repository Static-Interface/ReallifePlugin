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

import de.static_interface.reallifeplugin.fractions.Fraction;
import de.static_interface.reallifeplugin.fractions.FractionUtil;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.User;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.static_interface.reallifeplugin.LanguageConfiguration._;

public class FractionCommand implements CommandExecutor
{

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        User user = SinkLibrary.getUser(sender);
        Fraction userFraction = FractionUtil.getUserFraction(user.getUniqueId());

        if(args.length < 1 && !user.isConsole())
        {
            if (userFraction != null)
            {
                sendFractionInfo(user, userFraction);
                return true;
            }
            user.sendMessage(_("Fractions.NotInFraction"));
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
            case "leader":
            {
                if (!user.isConsole())
                {
                    if (moreArgs.length < 1)
                    {
                        user.sendMessage(de.static_interface.sinklibrary.configuration.LanguageConfiguration._("General.CommandMisused.Arguments.TooFew"));
                        return true;
                    }
                    handleLeaderCommand(user, moreArgs, userFraction);
                    break;
                }
            }
            case "admin":
            {
                if (!user.hasPermission("reallifeplugin.fractions.admin"))
                {
                    user.sendMessage(de.static_interface.sinklibrary.configuration.LanguageConfiguration._("Permissions.General"));
                    return true;
                }
                if (moreArgs.length < 1)
                {
                    user.sendMessage(de.static_interface.sinklibrary.configuration.LanguageConfiguration._("General.CommandMisused.Arguments.TooFew"));
                    return true;
                }
                handleAdminCommand(user, moreArgs);
                break;
            }

            default:
            {
                Fraction fraction = FractionUtil.getFraction(args[0]);
                sendFractionInfo(user, fraction);
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

                boolean successful = FractionUtil.createFraction(args);
                String msg = successful ? _("Fractions.Created") : _("Fractions.CreationFailed");
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

                boolean successful = FractionUtil.deleteFraction(args[1]);

                String msg = successful ? _("Fractions.Deleted") : _("Fractions.DeletionFailed");
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
                Fraction fraction = FractionUtil.getFraction(args[1]);
                if (fraction == null)
                {
                    user.sendMessage(String.format(_("Fractions.DoesntExists"),args[1]));
                    return;
                }
                fraction.setBase(args[2]);
                user.sendMessage(_("Fractions.BaseSet"));
                break;
            }
        }
    }

    private void handleLeaderCommand(User user, String[] args, Fraction fraction)
    {
        if (!FractionUtil.isLeader(user, fraction))
        {
            user.sendMessage(_("Fractions.NotLeader"));
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
                fraction.removeMember(target.getUniqueId());
                if (user.isOnline())
                {
                    user.sendMessage(String.format(_("Fractions.Kicked"), fraction.getName()));
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
                fraction.addMember(target.getUniqueId());
                if (user.isOnline())
                {
                    user.sendMessage(String.format(_("Fractions.Added"), fraction.getName()));
                }
                break;
            }
        }
    }

    private void sendFractionInfo(User user, Fraction fraction)
    {
        throw new NotImplementedException();
    }
}