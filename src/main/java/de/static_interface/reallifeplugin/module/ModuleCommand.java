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

package de.static_interface.reallifeplugin.module;

import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.sinklibrary.api.command.SinkCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public abstract class ModuleCommand<T extends Module> extends SinkCommand {

    private final T module;

    public ModuleCommand(T module) {
        super(module.getPlugin());
        this.module = module;
    }

    @Override
    public boolean onPreExecute(CommandSender sender, Command command, String label, String[] args) {
        return !(module == null || !module.isEnabled()) && super.onPreExecute(sender, command, label, args);
    }

    public final T getModule() {
        return module;
    }

    public final Database getDatabase() {
        return module.getDatabase();
    }
}
