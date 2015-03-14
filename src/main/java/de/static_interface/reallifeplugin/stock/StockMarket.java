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

package de.static_interface.reallifeplugin.stock;

import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.corporation.Corporation;
import de.static_interface.reallifeplugin.corporation.CorporationUtil;
import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.database.table.impl.corp.CorpTradesTable;
import de.static_interface.reallifeplugin.database.table.impl.stockmarket.StockPricesTable;
import de.static_interface.reallifeplugin.database.table.impl.stockmarket.StocksTable;
import de.static_interface.reallifeplugin.database.table.row.corp.CorpTradesRow;
import de.static_interface.reallifeplugin.database.table.row.stockmarket.StockPriceRow;
import de.static_interface.reallifeplugin.database.table.row.stockmarket.StockRow;
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.stockmarket.StockMarketModule;
import de.static_interface.reallifeplugin.module.stockmarket.event.StocksUpdateEvent;
import de.static_interface.sinklibrary.util.BukkitUtil;
import de.static_interface.sinklibrary.util.MathUtil;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

public class StockMarket {

    private static StockMarket instance;
    private HashMap<Integer, Long> amountCache = new HashMap<>();

    public static StockMarket getInstance() {
        if (instance == null) {
            instance = new StockMarket();
        }
        return instance;
    }

    @Nullable
    public Stock getStock(Database db, int id) {
        for (Stock stock : getAllStocks(db)) {
            if (stock.getId() == id) {
                return stock;
            }
        }

        return null;
    }

    @Nullable
    public Stock getStock(Database db, String tag) {
        tag = tag.toUpperCase();
        for (Stock stock : getAllStocks(db)) {
            if (stock.getTag().equalsIgnoreCase(tag)) {
                return stock;
            }
        }
        return null;
    }

    public Collection<Stock> getAllStocks(Database db) {
        StockRow[] rows;
        try {
            rows = db.getStocksTable().get("SELECT * FROM `{TABLE}`");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        List<Stock> parsedRows = new ArrayList<>();
        for (StockRow row : rows) {
            Corporation corp = CorporationUtil.getCorporation(db, row.corpId);
            if (corp == null) {
                ReallifeMain.getInstance().getLogger().warning("Corp ID not found: " + row.corpId);
                continue;
            }
            Stock stock = new Stock(db, row.id);
            parsedRows.add(stock);
        }
        return parsedRows;
    }

    public boolean onStocksUpdate(StockMarketModule module) {
        Database db = module.getDatabase();
        if (!Module.isEnabled(StockMarketModule.NAME)) {
            return false;
        }

        HashMap<Stock, Double> newPrices = new HashMap<>();

        Collection<Stock> stocks = getAllStocks(db);

        String prefix = ChatColor.GRAY + "[" + ChatColor.GOLD + "Börse" + ChatColor.GRAY + "] ";
        String s = "";

        for (Stock stock : stocks) {
            double percent;
            try {
                percent = MathUtil.round(calculateStockQuotation(db, stock));
            } catch (Exception e) {
                ReallifeMain.getInstance().getLogger()
                        .warning("onStocksUpdate(): Skipping " + stock.getTag() + ": Couldn't calculate stock quotation: ");
                e.printStackTrace();
                continue;
            }
            boolean down = percent < 0;

            percent = Math.abs(percent);

            double newPrice = stock.getPrice();

            if (down) {
                newPrice = newPrice - (newPrice * (percent / 100));
            } else {
                newPrice = newPrice + (newPrice * (percent / 100));
            }

            newPrices.put(stock, MathUtil.round(newPrice));
        }

        StocksUpdateEvent event = new StocksUpdateEvent(module, newPrices);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        int i = 0;
        for (Stock stock : event.getNewPrices().keySet()) {
            Double newPrice = event.getNewPrices().get(stock);
            if (newPrice == null) {
                continue;
            }

            double oldPrice = stock.getPrice();
            try {
                StocksTable table = db.getStocksTable();
                table.executeUpdate("UPDATE `{TABLE}` SET `price`=? WHERE `id`=?", newPrice, stock.getId());
            } catch (Exception e) {
                ReallifeMain.getInstance().getLogger().warning("onStocksUpdate(): Skipping " + stock.getTag() + ": Couldn't update price: ");
                e.printStackTrace();
                continue;
            }

            try {

                StockPriceRow row = new StockPriceRow();
                row.cause = "onStocksUpdate";
                row.newPrice = newPrice;
                row.oldPrice = oldPrice;
                row.stockId = stock.getId();
                row.time = System.currentTimeMillis();

                StockPricesTable table = db.getStockPriceTable();
                table.insert(row);
            } catch (Exception e) {
                ReallifeMain.getInstance().getLogger().warning(stock.getTag() + ": Couldn't insert price");
                e.printStackTrace();
            }

            if (!s.equals("")) {
                s += ChatColor.GRAY + " - ";
            }

            s += ChatColor.GOLD + stock.getTag() + ChatColor.GRAY + " " + stock.getPrice() + " ";

            double percent = MathUtil.round(((oldPrice - newPrice) / newPrice) * 100);

            if (newPrice > oldPrice) {
                s += ChatColor.DARK_GREEN + "▲ " + percent + "%";
            }
            if (newPrice == oldPrice) {
                s += "● " + percent + "%";
            }
            if (oldPrice > newPrice) {
                s += ChatColor.DARK_RED + "▼ " + percent + "%";
            }
            i++;
        }

        if (i > 0) {
            BukkitUtil.broadcastMessage(prefix + s, false);
            return true;
        }

        return false;
    }

    public double calculateStockQuotation(Database db, Stock stock) throws SQLException, IOException {
        Validate.notNull(stock);

        long timeSpan = 1000 * 3 * 60 * 60 * 24; // 3 days, Todo: make this configurable

        long changedAmount = getChangedAmount(db, stock, timeSpan);

        Long changedAmountBefore = amountCache.get(stock.getId());
        if (amountCache.get(stock.getId()) == null) {
            changedAmountBefore = getChangedAmount(db, stock, timeSpan - (StockMarketModule.STOCK_TIME * 1000));
        }
        // calculate amount from last time
        //if (changedAmountBefore == 0) {
        //    return 0;
        //}

        amountCache.put(stock.getId(), changedAmount);

        //896
        //704

        // compare them
        if (changedAmount > changedAmountBefore) {
            return ((double) (changedAmountBefore * 100) / changedAmount) / 100; //??
        }

        if (changedAmountBefore > changedAmount) {
            return ((double) -(changedAmount * 100) / changedAmountBefore) / 100;
        }

        return 0;
    }

    private long getChangedAmount(Database db, Stock stock, long timespan) throws SQLException {
        CorpTradesTable corpTrades = db.getCorpTradesTable();

        CorpTradesRow[]
                rows =
                corpTrades.get("SELECT * FROM `{TABLE}` WHERE `corp_id` = ? AND `time` > ? AND `type` = 0", stock.getCorporation().getId(),
                               System.currentTimeMillis() - timespan);

        // calculate amount
        long changedAmount = 0;
        for (CorpTradesRow row : rows) {
            changedAmount += Math.abs(row.changedAmount);
        }
        return changedAmount;
    }

    public List<Stock> getAllStocks(Database db, Player player) {
        return null;
    }
}
