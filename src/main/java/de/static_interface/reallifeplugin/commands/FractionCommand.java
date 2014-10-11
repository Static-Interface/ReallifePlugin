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

import static de.static_interface.reallifeplugin.ReallifeLanguageConfiguration.m;

import de.static_interface.reallifeplugin.fractions.Fraction;
import de.static_interface.reallifeplugin.fractions.FractionUtil;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.command.SinkCommand;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.StringUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FractionCommand extends SinkCommand {

    public FractionCommand(Plugin plugin) {
        super(plugin);
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }

    @Override
    public boolean onExecute(CommandSender sender, String label, String[] args) {
        IngameUser user = SinkLibrary.getInstance().getIngameUser((Player) sender);
        Fraction userFraction = FractionUtil.getUserFraction(user.getUniqueId());

        if (args.length < 1) {
            if (userFraction != null) {
                sendFractionInfo(user, userFraction);
                return true;
            }
            user.sendMessage(m("Fractions.NotInFraction"));
            return true;
        }

        List<String> tmp = new ArrayList<>(Arrays.asList(args));
        tmp.remove(args[0]);
        String[] moreArgs = tmp.toArray(new String[tmp.size()]);

        switch (args[0].toLowerCase()) {
            case "leader": {
                if (moreArgs.length < 1) {
                    user.sendMessage(
                            de.static_interface.sinklibrary.configuration.LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return true;
                }
                handleLeaderCommand(user, moreArgs, userFraction);
                break;
            }
            case "admin": {
                if (!user.hasPermission("reallifeplugin.fractions.admin")) {
                    user.sendMessage(de.static_interface.sinklibrary.configuration.LanguageConfiguration.m("Permissions.General"));
                    return true;
                }
                if (moreArgs.length < 1) {
                    user.sendMessage(
                            de.static_interface.sinklibrary.configuration.LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return true;
                }
                handleAdminCommand(user, moreArgs);
                break;
            }

            default: {
                Fraction fraction = FractionUtil.getFraction(args[0]);
                sendFractionInfo(user, fraction);
                break;
            }
        }

        return true;
    }

    private void handleAdminCommand(IngameUser user, String[] args) {
        switch (args[0]) {
            case "new": {
                if (args.length < 5) {
                    user.sendMessage(
                            de.static_interface.sinklibrary.configuration.LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }

                boolean successful = FractionUtil.createFraction(args);
                String msg = successful ? m("Fractions.Created") : m("Fractions.CreationFailed");
                msg = StringUtil.format(msg, args[1]);
                user.sendMessage(msg);
                break;
            }

            case "delete": {
                if (args.length < 2) {
                    user.sendMessage(
                            de.static_interface.sinklibrary.configuration.LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }

                boolean successful = FractionUtil.deleteFraction(args[1]);

                String msg = successful ? m("Fractions.Deleted") : m("Fractions.DeletionFailed");
                msg = StringUtil.format(msg, args[1]);
                user.sendMessage(msg);
                break;
            }

            case "setbase": {
                if (args.length < 3) {
                    user.sendMessage(
                            de.static_interface.sinklibrary.configuration.LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }
                Fraction fraction = FractionUtil.getFraction(args[1]);
                if (fraction == null) {
                    user.sendMessage(StringUtil.format(m("Fractions.DoesntExists"), args[1]));
                    return;
                }
                fraction.setBase(args[2]);
                user.sendMessage(m("Fractions.BaseSet"));
                break;
            }
        }
    }

    private void handleLeaderCommand(IngameUser user, String[] args, Fraction fraction) {
        if (!FractionUtil.isLeader(user, fraction)) {
            user.sendMessage(m("Fractions.NotLeader"));
            return;
        }
        switch (args[0].toLowerCase()) {
            case "kick": {
                if (args.length < 2) {
                    return;
                }
                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[1]);
                fraction.removeMember(target.getUniqueId());
                if (user.isOnline()) {
                    user.sendMessage(StringUtil.format(m("Fractions.Kicked"), fraction.getName()));
                }
                break;
            }
            case "add": {
                if (args.length < 2) {
                    return;
                }
                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[1]);
                fraction.addMember(target.getUniqueId());
                if (user.isOnline()) {
                    user.sendMessage(StringUtil.format(m("Fractions.Added"), fraction.getName()));
                }
                break;
            }
        }
    }

    private void sendFractionInfo(IngameUser user, Fraction fraction) {
        throw new NotImplementedException();
    }
}
