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

import de.static_interface.reallifeplugin.module.corporation.database.table.CorpUsersTable;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpsTable;
import de.static_interface.sinklibrary.database.Row;
import de.static_interface.sinklibrary.database.annotation.Column;
import de.static_interface.sinklibrary.database.annotation.ForeignKey;
import de.static_interface.sinklibrary.database.annotation.Index;

public class CorpTradesRow implements Row {

    @Column(autoIncrement = true, primaryKey = true)
    public Integer id;

    @Column(name = "corp_id")
    @ForeignKey(table = CorpsTable.class, column = "id", onDelete = CASCADE, onUpdate = CASCADE)
    @Index
    public int corpId;

    @Column(name = "material_name")
    public String materialName;

    @Column(name = "new_amount")
    public int newAmount;

    @Column
    public double price;

    @Column(name = "sign_amount")
    public int signAmount;

    @Column(name = "changed_amount")
    public int changedAmount;

    @Column
    public long time;

    @Column
    public int type;

    @Column(name = "user_id")
    @ForeignKey(table = CorpUsersTable.class, column = "id", onDelete = CASCADE, onUpdate = CASCADE)
    @Index
    public int userId;

    @Column
    public String world;

    @Column
    public int x;

    @Column
    public int y;

    @Column
    public int z;
}
