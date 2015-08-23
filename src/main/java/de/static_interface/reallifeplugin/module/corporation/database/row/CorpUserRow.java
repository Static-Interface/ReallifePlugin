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

import static de.static_interface.sinklibrary.database.CascadeAction.CASCADE;
import static de.static_interface.sinklibrary.database.CascadeAction.SET_NULL;

import de.static_interface.reallifeplugin.module.corporation.database.table.CorpRanksTable;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpsTable;
import de.static_interface.sinklibrary.database.Row;
import de.static_interface.sinklibrary.database.annotation.Column;
import de.static_interface.sinklibrary.database.annotation.ForeignKey;
import de.static_interface.sinklibrary.database.annotation.Index;

import javax.annotation.Nullable;

public class CorpUserRow implements Row {

    @Column(autoIncrement = true, primaryKey = true)
    public Integer id;

    @Column(name = "corp_id")
    @ForeignKey(table = CorpsTable.class, column = "id", onUpdate = CASCADE, onDelete = SET_NULL)
    @Index
    @Nullable
    public Integer corpId;

    @Column(name = "corp_rank")
    @ForeignKey(table = CorpRanksTable.class, column = "id", onUpdate = CASCADE, onDelete = SET_NULL)
    @Nullable
    public Integer corpRank;

    @Column(uniqueKey = true)
    public String uuid;
}
