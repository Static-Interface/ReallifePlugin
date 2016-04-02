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

package de.static_interface.reallifeplugin.module.politics;

import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.politics.command.PartyCommand;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyOptionsTable;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyRankPermissionsTable;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyRanksTable;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyTable;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyUsersTable;
import de.static_interface.sinksql.AbstractTable;
import de.static_interface.sinksql.Database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PoliticsModule extends Module<ReallifeMain> {

    public static final String NAME = "Politics";

    public PoliticsModule(ReallifeMain plugin, Database db) {
        super(plugin, ReallifeMain.getInstance().getSettings(), db, NAME, false);
    }

    @Override
    public void onEnable() {
        PartyManager.init(this);
        registerModuleCommand("party", new PartyCommand(this));
    }

    @Override
    protected Collection<AbstractTable> getTables() {
        List<AbstractTable> tables = new ArrayList<>();
        AbstractTable table = new PartyTable(getDatabase());
        tables.add(table);
        table = new PartyRanksTable(getDatabase());
        tables.add(table);
        table = new PartyUsersTable(getDatabase());
        tables.add(table);
        table = new PartyOptionsTable(getDatabase());
        tables.add(table);
        table = new PartyRankPermissionsTable(getDatabase());
        tables.add(table);
        return tables;
    }

}
