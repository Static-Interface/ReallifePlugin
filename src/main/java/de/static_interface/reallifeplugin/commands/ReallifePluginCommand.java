/*
 * Copyright (c) 2013 - 2014 <http://static-interface.de> and contributors
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

package de.static_interface.reallifeplugin.commands;

import de.static_interface.reallifeplugin.*;
import org.bukkit.command.*;

public class ReallifePluginCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }
        switch (args[0]) {
            case "payday":
                boolean skipTime = false;

                for (String s : args) {
                    if (s.equalsIgnoreCase("--skiptime")) {
                        skipTime = true;
                        break;
                    }
                }

                ReallifeMain.getInstance().getPayDayRunnable().run(!skipTime);
                break;
            case "reload":
                ReallifeMain.getInstance().getSettings().reload();
                break;
            default:
                return false;
        }
        return true;
    }
}
