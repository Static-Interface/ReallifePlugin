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

import static de.static_interface.reallifeplugin.config.RpLanguage.m;

import de.static_interface.reallifeplugin.module.payday.model.Entry;
import de.static_interface.reallifeplugin.module.stockmarket.database.row.StockUserRow;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.MathUtil;

public class StockEntry extends Entry {

    private final StockUserRow userStock;
    private final Stock stock;
    private final IngameUser user;

    public StockEntry(IngameUser user, StockUserRow userStock, Stock stock) {
        this.user = user;
        this.userStock = userStock;
        this.stock = stock;
    }

    @Override
    public String getSourceAccount() {
        return user.getName();
    }

    @Override
    public String getReason() {
        return m("StockMarket.PaydayEntry", stock.getTag());
    }

    @Override
    public double getAmount() {
        return MathUtil.round(((stock.getDividend() * stock.getPrice()) / 100) * userStock.amount);
    }

    @Override
    public boolean sendToTarget() {
        return false;
    }

    @Override
    public String getTargetAccount() {
        return null;
    }
}
