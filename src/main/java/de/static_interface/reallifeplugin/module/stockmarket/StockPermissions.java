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


import de.static_interface.reallifeplugin.permission.Permission;
import de.static_interface.reallifeplugin.permission.Permissions;

public class StockPermissions extends Permissions {

    public static final Permission GOPUBLIC = new Permission("stock_gopublic", "Go public", false);
    public static final Permission TOGGLE_BUY = new Permission("stock_togglebuy", "Enable/disable buying", false);
    public static final Permission FORCE_SELL = new Permission("stock_force_sell", "Can still sell stocks even if disabled", false);
    private static StockPermissions instance;

    private StockPermissions() {
        add(GOPUBLIC, TOGGLE_BUY, FORCE_SELL);
    }

    public static void init() {
        instance = new StockPermissions();
    }

    public static StockPermissions getInstance() {
        return instance;
    }

    public static void unload() {
        instance = null;
    }
}
