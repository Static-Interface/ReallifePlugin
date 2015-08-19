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

import static de.static_interface.reallifeplugin.database.CascadeAction.CASCADE;

import de.static_interface.reallifeplugin.database.Row;
import de.static_interface.reallifeplugin.database.annotation.Column;
import de.static_interface.reallifeplugin.database.annotation.ForeignKey;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpsTable;

import javax.annotation.Nullable;

public class CorpRank implements Row, Comparable<CorpRank> {

    @Column(autoIncrement = true, primaryKey = true)
    public Integer id;

    @Column
    public String name;

    @Column
    @Nullable
    public String description;

    @Column
    @Nullable
    public String prefix;

    @Column
    public int priority;

    @Column(name = "corp_id")
    @ForeignKey(table = CorpsTable.class, column = "id", onUpdate = CASCADE, onDelete = CASCADE)
    @Nullable
    public Integer corpId;


    @Override
    public int compareTo(CorpRank o) {
        if (o == null) {
            return 1;
        }
        return Integer.valueOf(priority).compareTo(o.priority);
    }
}
