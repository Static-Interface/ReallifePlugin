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

package de.static_interface.reallifeplugin.module.stockmarket.database.row;

import de.static_interface.reallifeplugin.database.Row;
import de.static_interface.reallifeplugin.database.annotation.Column;
import de.static_interface.reallifeplugin.database.annotation.ForeignKey;
import de.static_interface.reallifeplugin.module.stockmarket.database.table.StocksTable;

import javax.annotation.Nullable;

public class StockPriceRow implements Row {

    @Column(autoIncrement = true, primaryKey = true)
    public Integer id;

    @Column
    @Nullable
    public String cause;

    @Column
    public double new_price;

    @Column
    public double old_price;

    @Column
    @ForeignKey(table = StocksTable.class, column = "id")
    public int stock_id;

    @Column
    public long time;
}
