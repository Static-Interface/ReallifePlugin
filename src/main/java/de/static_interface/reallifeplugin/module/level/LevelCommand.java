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

package de.static_interface.reallifeplugin.module.level;

import de.static_interface.reallifeplugin.module.ModuleCommand;
import de.static_interface.sinklibrary.api.exception.NotEnoughArgumentsException;
import org.apache.commons.cli.ParseException;
import org.bukkit.command.CommandSender;

public class LevelCommand extends ModuleCommand<LevelModule> {

    public LevelCommand(LevelModule module) {
        super(module);
    }

    @Override
    protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }

        switch (args[0]) {
            case "info":
                break;

            case "next":
                break;

            case "accept":
                break;

            default:
                return false;
        }

        return true;
    }
}
