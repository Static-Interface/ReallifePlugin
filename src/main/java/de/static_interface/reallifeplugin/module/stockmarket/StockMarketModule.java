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

package de.static_interface.reallifeplugin.module.stockmarket;

import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.corporation.CorporationModule;
import de.static_interface.reallifeplugin.module.corporation.CorporationPermissions;
import de.static_interface.reallifeplugin.module.stockmarket.database.table.StockPricesTable;
import de.static_interface.reallifeplugin.module.stockmarket.database.table.StockTradesTable;
import de.static_interface.reallifeplugin.module.stockmarket.database.table.StockUsersTable;
import de.static_interface.reallifeplugin.module.stockmarket.database.table.StocksTable;
import de.static_interface.sinklibrary.api.configuration.Configuration;
import de.static_interface.sinklibrary.database.AbstractTable;
import de.static_interface.sinklibrary.database.Database;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

public class StockMarketModule extends Module<ReallifeMain> {

    public static final String NAME = "StockMarket";
    public static final int STOCK_TIME = 60 * 60;
    private BukkitTask stocksTask;

    public StockMarketModule(ReallifeMain plugin, @Nullable Database db) {
        super(plugin, new Configuration(new File(ReallifeMain.getInstance().getDataFolder(), NAME + ".yml")) {
            @Override
            public void addDefaults() {
                addDefault("Transfer.Cooldown", 2, "In minutes");
                addDefault("GoPublic.MaxStocks", 10000);
                addDefault("GoPublic.MaxPrice", 500);
                addDefault("GoPublic.MinStocksDividend", 3);
                addDefault("GoPublic.MinStocksShare", 20);
            }
        }, db, NAME, true);
    }

    @Override
    protected Collection<AbstractTable> getTables() {
        List<AbstractTable> tables = new ArrayList<>();
        AbstractTable table = new StocksTable(getDatabase());
        tables.add(table);
        table = new StockPricesTable(getDatabase());
        tables.add(table);
        table = new StockTradesTable(getDatabase());
        tables.add(table);
        table = new StockUsersTable(getDatabase());
        tables.add(table);
        return tables;
    }

    @Override
    protected void onEnable() {
        if (!Module.isEnabled(CorporationModule.class)) {
            getPlugin().getLogger().warning("Corporation module not active, deactivating...");
            disable();
            return;
        }

        StockPermissions.init();
        CorporationPermissions.getInstance().merge(StockPermissions.getInstance());

        final CorporationModule corpModule = Module.getModule(CorporationModule.class);
        stocksTask = Bukkit.getScheduler().runTaskTimer(getPlugin(), new Runnable() {
            @Override
            public void run() {
                StockMarket.getInstance().onStocksUpdate(StockMarketModule.this, corpModule);
            }
        }, 20 * STOCK_TIME, 20 * STOCK_TIME);
        registerModuleCommand("stockmarket", new StockMarketCommand(this, corpModule));
        registerModuleListener(new StockMarketListener(this, corpModule));
    }

    @Override
    protected void onDisable() {
        if (stocksTask != null) {
            stocksTask.cancel();
        }

        if (CorporationPermissions.getInstance() != null) {
            CorporationPermissions.getInstance().unmerge(StockPermissions.getInstance());
        }
        StockMarket.unload();
        StockPermissions.unload();
    }
}
