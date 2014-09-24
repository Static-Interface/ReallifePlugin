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

import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.SinkUser;
import de.static_interface.sinklibrary.configuration.PlayerConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class InsuranceCommand implements CommandExecutor {
    // Todo: Replace hardcode with LanguageConfiguration
    public static final String ACTIVATED_PATH = "Insurance.Activated";
    public static final String TIMEOUT_TIMESTAMP = "Insurance.Timestamp";

    public static void createVars(SinkUser user) {
        PlayerConfiguration config = user.getPlayerConfiguration();
        if (config.get(ACTIVATED_PATH) == null) {
            config.set(ACTIVATED_PATH, false);
        }
        if (config.get(TIMEOUT_TIMESTAMP) == null) {
            config.set(TIMEOUT_TIMESTAMP, 0);
        }
    }

    public static boolean isActive(Player player) {
        SinkUser user = SinkLibrary.getInstance().getUser(player);
        return (boolean) user.getPlayerConfiguration().get(ACTIVATED_PATH);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        SinkUser user = SinkLibrary.getInstance().getUser(sender);

        if (user.isConsole() || args.length < 1) {
            return false;
        }

        PlayerConfiguration config = user.getPlayerConfiguration();

        createVars(user);

        boolean active = (boolean) config.get(ACTIVATED_PATH);
        long timestamp = Long.parseLong(String.valueOf(config.get(TIMEOUT_TIMESTAMP)));

        switch (args[0].toLowerCase()) {
            case "on":
                if (active) {
                    user.sendMessage(ChatColor.DARK_RED + "Fehler: " + ChatColor.RED + "Die Versicherung ist schon aktiviert!");
                    return true;
                }

                config.set(ACTIVATED_PATH, true);
                config.set(TIMEOUT_TIMESTAMP, System.currentTimeMillis() + 12 * 60 * 60 * 1000); //12 h

                user.sendMessage(ChatColor.DARK_GREEN + "Die Versicherung wurde erfolgreich aktiviert");
                break;
            case "off":
                if (!active) {
                    user.sendMessage(ChatColor.DARK_RED + "Fehler: " + ChatColor.RED + "Die Versicherung ist schon deaktiviert!");
                    return true;
                }

                if (timestamp != 0 && timestamp > System.currentTimeMillis()) {
                    user.sendMessage(
                            ChatColor.DARK_RED + "Fehler: " + ChatColor.RED + "Die Versicherung kann erst nach 12 Stunden deaktiviert werden.");

                    long millis = timestamp - System.currentTimeMillis();

                    String
                            timeleft =
                            String.format("Es sind noch %d Stunde(n) und %d Minute(n) uebrig!", TimeUnit.MILLISECONDS.toHours(millis),
                                          TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)));

                    user.sendMessage(ChatColor.RED + timeleft);
                    return true;
                }

                config.set(ACTIVATED_PATH, false);
                config.set(TIMEOUT_TIMESTAMP, 0);

                user.sendMessage(ChatColor.DARK_GREEN + "Die Versicherung wurde erfolgreich deaktiviert");
                break;
        }
        return true;
    }
}
