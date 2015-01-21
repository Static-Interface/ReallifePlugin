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
import de.static_interface.reallifeplugin.database.table.row.stockmarket.StockRow;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

public class StockMarketUtil {

    @Nullable
    public static StockRow getStock(int id) {
        for (StockRow stock : getStocks()) {
            if (stock.id == id) {
                return stock;
            }
        }

        return null;
    }

    public static Collection<StockRow> getStocks() {
        Database db = ReallifeMain.getInstance().getDB();
        StockRow[] rows;
        try {
            rows = db.getStocksTable().get("SELECT * FROM `{TABLE}`", "");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        List<StockRow> parsedRows = new ArrayList<>();
        for (StockRow row : rows) {
            Corporation corp = CorporationUtil.getCorporation(row.corpId);
            if (corp == null) {
                continue;
            }
            parsedRows.add(row);
        }
        return parsedRows;
    }
}
