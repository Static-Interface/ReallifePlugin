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

import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.corporation.Corporation;
import de.static_interface.reallifeplugin.module.corporation.CorporationManager;
import de.static_interface.reallifeplugin.module.corporation.CorporationModule;
import de.static_interface.reallifeplugin.module.stockmarket.database.row.StockRow;
import de.static_interface.reallifeplugin.module.stockmarket.database.table.StocksTable;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.MathUtil;

public class Stock {

    private final int id;
    private StocksTable stocksTable;

    public Stock(StockMarketModule module, CorporationModule corpModule, int id) {
        this.id = id;
        stocksTable = Module.getTable(module, StocksTable.class);
    }

    public int getAmount() {
        return getBase().amount;
    }

    private StockRow getBase() {
        return stocksTable.get("SELECT * FROM `{TABLE}` WHERE `id`=?", id)[0];
    }

    public Corporation getCorporation() {
        return CorporationManager.getInstance().getCorporation(getBase().corp_id);
    }

    public final int getId() {
        return id;
    }

    public double getPrice() {
        return MathUtil.round(getBase().price);
    }

    public String getTag() {
        return getCorporation().getTag();
    }

    public double getDividend() {
        return getBase().dividend;
    }

    public boolean canSellStocks(IngameUser user) {
        return CorporationManager.getInstance().hasCorpPermission(user, StockPermissions.FORCE_SELL) || getBase().allow_buy_stocks;
    }

    public double getShare() {
        return MathUtil.round(getBase().share_holding);
    }
}
