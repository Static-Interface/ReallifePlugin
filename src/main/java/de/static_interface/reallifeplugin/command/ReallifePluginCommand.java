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

package de.static_interface.reallifeplugin.command;

import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.payday.PaydayModule;
import de.static_interface.sinklibrary.api.command.SinkCommand;
import de.static_interface.sinklibrary.api.exception.NotEnoughArgumentsException;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ReallifePluginCommand extends SinkCommand {

    public ReallifePluginCommand(Plugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onExecute(CommandSender sender, String label, String[] args) {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }

        switch (args[0]) {
            case "payday":
                if (Module.isEnabled(PaydayModule.NAME)) {
                    boolean skipTimeCheck = false;

                    for (String s : args) {
                        if (s.equalsIgnoreCase("skiptimecheck")) {
                            skipTimeCheck = true;
                            break;
                        }
                    }

                    PaydayModule module = Module.getModule(PaydayModule.NAME, PaydayModule.class);
                    module.getPayDayTask().run(!skipTimeCheck);
                    break;
                }
            default:
                sender.sendMessage("Unknown subcommand: " + args[0]);
                return false;
        }
        return true;
    }
}
