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

package de.static_interface.reallifeplugin.module.stockmarket.event;

import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.ModuleEvent;
import de.static_interface.reallifeplugin.stock.Stock;
import de.static_interface.sinklibrary.util.MathUtil;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import java.util.Collection;
import java.util.HashMap;

public class StocksUpdateEvent extends ModuleEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    boolean cancelled;
    private HashMap<Stock, Double> newPrices;

    public StocksUpdateEvent(Module module, HashMap<Stock, Double> newPrices) {
        super(module);
        this.newPrices = newPrices;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Collection<Stock> getStocks() {
        return newPrices.keySet();
    }

    public HashMap<Stock, Double> getNewPrices() {
        return newPrices;
    }

    public double getPercent(Stock stock) {
        double oldPrice = stock.getPrice();
        Double newPrice = newPrices.get(stock);
        if (newPrice == null) {
            return 0;
        }
        return MathUtil.round(((oldPrice - newPrice) / newPrice) * 100);
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean value) {
        cancelled = value;
    }
}
