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

package de.static_interface.reallifeplugin.module.corporation;

import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.database.table.Table;
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.sinklibrary.SinkLibrary;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;

public class CorporationModule extends Module {

    public static final String NAME = "Corporations";

    public CorporationModule(Plugin plugin, @Nullable Database db) {
        super(plugin, ReallifeMain.getInstance().getSettings(), db, NAME, true,
              Table.CORPS_TABLE, Table.CORP_USERS_TABLE, Table.CORP_TRADES_TABLE);
    }

    @Override
    protected void onEnable() {
        addListener(new CorporationListener(this));
        SinkLibrary.getInstance().registerCommand("corporation", new CorporationCommand(this));
    }
}
