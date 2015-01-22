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

package de.static_interface.reallifeplugin.stockmarket;

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
import de.static_interface.reallifeplugin.event.StocksUpdateEvent;
import de.static_interface.sinklibrary.util.BukkitUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

public class StockMarketUtil {

    @Nullable
    public static Stock getStock(int id) {
        for (Stock stock : getStocks()) {
            if (stock.getId() == id) {
                return stock;
            }
        }

        return null;
    }

    @Nullable
    public static Stock getStock(String tag) {
        tag = tag.toUpperCase();
        for (Stock stock : getStocks()) {
            if (stock.getTag().equalsIgnoreCase(tag)) {
                return stock;
            }
        }
        return null;
    }

    public static Collection<Stock> getStocks() {
        Database db = ReallifeMain.getInstance().getDB();
        StockRow[] rows;
        try {
            rows = db.getStocksTable().get("SELECT id FROM `{TABLE}`");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        List<Stock> parsedRows = new ArrayList<>();
        for (StockRow row : rows) {
            Corporation corp = CorporationUtil.getCorporation(row.corpId);
            if (corp == null) {
                continue;
            }
            Stock stock = new Stock(ReallifeMain.getInstance().getDB(), row.id);
            parsedRows.add(stock);
        }
        return parsedRows;
    }

    public static void onStocksUpdate() {
        StocksUpdateEvent event = new StocksUpdateEvent();
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        Database db = ReallifeMain.getInstance().getDB();

        Collection<Stock> stocks = getStocks();

        if (stocks.size() == 0) {
            return;
        }

        String prefix = ChatColor.GRAY + "[" + ChatColor.GOLD + "Börse" + ChatColor.GRAY + "] ";
        String s = "";

        for (Stock stock : stocks) {
            double percent;
            try {
                percent = calculateStockQuotation(stock);
            } catch (SQLException e) {
                ReallifeMain.getInstance().getLogger()
                        .warning("onStocksUpdate(): Skipping " + stock.getTag() + ": calculateStockQuotation() failed: ");
                e.printStackTrace();
                continue;
            }

            boolean down = percent < 0;

            percent = Math.abs(percent);

            double oldPrice = stock.getPrice();
            double newPrice = oldPrice;

            if (down) {
                newPrice = newPrice + (newPrice * percent);
            } else {
                newPrice = newPrice - (newPrice * percent);
            }

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

            if (newPrice > oldPrice) {
                s += ChatColor.DARK_GREEN + "▲ " + percent + "%";
            }
            if (newPrice == oldPrice) {
                s += "● " + percent + "%";
            }
            if (oldPrice > newPrice) {
                s += ChatColor.DARK_RED + "▼ " + percent + "%";
            }
        }

        BukkitUtil.broadcastMessage(prefix + s, false);
    }

    private static double calculateStockQuotation(Stock stock) throws SQLException {
        Database db = ReallifeMain.getInstance().getDB();
        CorpTradesTable corpTrades = db.getCorpTradesTable();
        long timeSpan = 1000 * 60 * 60 * 24 * 3; // 3 days, Todo: make this configurable
        long time = System.currentTimeMillis() - timeSpan;
        long timeBefore = time - timeSpan;
        CorpTradesRow[]
                rows =
                corpTrades.get("SELECT * FROM `{TABLE}` WHERE `corp_id` = ? AND `time` > ? AND `type` = 0", stock.getCorporation().getId(), time);

        // calculate amount
        long changedAmount = 0;
        for (CorpTradesRow row : rows) {
            changedAmount += Math.abs(row.changedAmount);
        }

        rows =
                corpTrades.get("SELECT * FROM `{TABLE}` WHERE `corp_id` = ? AND `time` > ? AND `time` < ?  AND `type` = 0",
                               stock.getCorporation().getId(), timeBefore, time);

        // calculate amount from last time
        long changedAmountBefore = 0;
        for (CorpTradesRow row : rows) {
            changedAmountBefore += Math.abs(row.changedAmount);
        }

        // compare them
        if (changedAmount > changedAmountBefore) {
            return (changedAmount / changedAmountBefore) * 100;
        }

        if (changedAmountBefore > changedAmount) {
            return -(changedAmountBefore / changedAmount) * 100;
        }

        return 0;
    }
}
