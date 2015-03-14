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
import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.database.table.Table;
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.corporation.CorporationModule;
import de.static_interface.reallifeplugin.stock.StockMarket;
import de.static_interface.sinklibrary.SinkLibrary;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;

public class StockMarketModule extends Module {

    public static final String NAME = "StockMarket";
    public static final int STOCK_TIME = 60 * 60;
    private static StockMarketModule instance;
    private BukkitTask stocksTask;

    public StockMarketModule(Plugin plugin, @Nullable Database db) {
        super(plugin, ReallifeMain.getInstance().getSettings(), db, NAME, true,
              Table.STOCKS_TABLE, Table.STOCK_USERS_TABLE, Table.STOCK_PRICE_TABLE, Table.STOCK_TRADES_TABLE);
    }

    @Nullable
    public static StockMarketModule getInstance() {
        return instance;
    }

    @Override
    protected void onEnable() {
        instance = this;
        if (!Module.isEnabled(CorporationModule.NAME)) {
            getPlugin().getLogger().warning("Corporation module not active, deactivating...");
            disable();
            return;
        }
        stocksTask = Bukkit.getScheduler().runTaskTimer(getPlugin(), new Runnable() {
            @Override
            public void run() {
                StockMarket.getInstance().onStocksUpdate(getDatabase());
            }
        }, 0, 20 * STOCK_TIME);
        SinkLibrary.getInstance().registerCommand("stockmarket", new StockMarketCommand(this));
    }

    @Override
    protected void onDisable() {
        instance = null;
        if (stocksTask != null) {
            stocksTask.cancel();
        }
    }
}
