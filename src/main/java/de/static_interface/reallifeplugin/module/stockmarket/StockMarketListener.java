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

import de.static_interface.reallifeplugin.module.ModuleListener;
import de.static_interface.reallifeplugin.module.corporation.CorporationModule;
import de.static_interface.reallifeplugin.module.payday.event.PayDayEvent;
import de.static_interface.reallifeplugin.module.stockmarket.database.row.StockUserRow;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.user.IngameUser;
import org.bukkit.event.EventHandler;

import java.util.Collection;

public class StockMarketListener extends ModuleListener<StockMarketModule> {

    private CorporationModule corpModule;

    public StockMarketListener(StockMarketModule module, CorporationModule corpModule) {
        super(module);
        this.corpModule = corpModule;
    }

    @EventHandler
    public void onPayday(PayDayEvent event) {
        IngameUser user = SinkLibrary.getInstance().getIngameUser(event.getPlayer());
        Collection<StockUserRow> stocks = StockMarket.getInstance().getAllStocks(getModule(), corpModule, user, null);
        if (stocks.size() < 1) {
            return;
        }

        for (StockUserRow row : stocks) {
            Stock stock = StockMarket.getInstance().getStock(getModule(), corpModule, row.stockId);
            //if (stock.getCorporation().getCEO().getUniqueId().equals(user.getUniqueId())) {
            //    continue;
            //}
            StockEntry entry = new StockEntry(user, row, stock);
            event.addEntry(entry);
        }
    }
}
