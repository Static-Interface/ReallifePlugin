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

import de.static_interface.reallifeplugin.corporation.Corporation;
import de.static_interface.reallifeplugin.corporation.CorporationUtil;
import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.database.table.impl.stockmarket.StockTradesTable;
import de.static_interface.reallifeplugin.database.table.impl.stockmarket.StocksTable;
import de.static_interface.reallifeplugin.database.table.row.stockmarket.StockRow;

import java.sql.SQLException;

public class Stock {

    private final int id;

    private StocksTable stocksTable;
    private StockTradesTable stocksTradeHistoryTable;

    public Stock(Database db, int id) {
        this.id = id;

        stocksTable = db.getStocksTable();
        stocksTradeHistoryTable = db.getStockTradeHistoryTable();
    }

    public int getAmount() {
        return getBase().amount;
    }

    private StockRow getBase() {
        try {
            return stocksTable.get("SELECT * FROM `{TABLE}` WHERE `id`=?", id)[0];
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Corporation getCorporation() {
        return CorporationUtil.getCorporation(getId());
    }

    public final int getId() {
        return id;
    }
}
