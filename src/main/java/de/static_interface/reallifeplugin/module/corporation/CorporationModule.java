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
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.corporation.command.CorporationCommand;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpOptionsTable;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpRankPermissionsTable;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpRanksTable;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpTradesTable;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpUsersTable;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpsTable;
import de.static_interface.reallifeplugin.module.stockmarket.StockMarketModule;
import de.static_interface.sinklibrary.database.AbstractTable;
import de.static_interface.sinklibrary.database.Database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

public class CorporationModule extends Module<ReallifeMain> {

    public static final String NAME = "Corporations";

    public CorporationModule(ReallifeMain plugin, @Nullable Database db) {
        super(plugin, ReallifeMain.getInstance().getSettings(), db, NAME, false);
    }

    @Override
    protected Collection<AbstractTable> getTables() {
        List<AbstractTable> tables = new ArrayList<>();
        AbstractTable table = new CorpsTable(getDatabase());
        tables.add(table);
        table = new CorpRanksTable(getDatabase());
        tables.add(table);
        table = new CorpUsersTable(getDatabase());
        tables.add(table);
        table = new CorpTradesTable(getDatabase());
        tables.add(table);
        table = new CorpRankPermissionsTable(getDatabase());
        tables.add(table);
        table = new CorpOptionsTable(getDatabase());
        tables.add(table);
        return tables;
    }

    @Override
    protected void onEnable() {
        CorporationManager.init(this);
        CorporationPermissions.init();
        registerModuleListener(new CorporationListener(this));
        registerModuleCommand("corporation", new CorporationCommand(this));
    }

    @Override
    protected void onDisable() {
        if (Module.isEnabled(StockMarketModule.class)) {
            getModule(StockMarketModule.class).disable();
        }

        CorporationPermissions.unload();
        CorporationManager.unload();
    }
}
