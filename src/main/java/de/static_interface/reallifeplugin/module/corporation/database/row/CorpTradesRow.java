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

package de.static_interface.reallifeplugin.module.corporation.database.row;

import de.static_interface.reallifeplugin.database.Row;

public class CorpTradesRow implements Row {
    public Integer id;
    public int corp_id;
    public String material_name;
    public int new_amount;
    public double price;
    public int sign_amount;
    public int changed_amount;
    public long time;
    public int type;
    public int user_id;
    public String world;
    public int x;
    public int y;
    public int z;
}
