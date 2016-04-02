/*
 * Copyright (c) 2013 - 2016 <http://static-interface.de> and contributors
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

package de.static_interface.reallifeplugin.module.bank.database.row;

import static de.static_interface.sinklibrary.database.CascadeAction.CASCADE;

import de.static_interface.reallifeplugin.module.bank.database.table.BankTable;
import de.static_interface.reallifeplugin.module.bank.database.table.BankUsersTable;
import de.static_interface.sinklibrary.database.Row;
import de.static_interface.sinklibrary.database.annotation.Column;
import de.static_interface.sinklibrary.database.annotation.ForeignKey;

import javax.annotation.Nullable;

public class BannedUserRow implements Row {

    @Column(primaryKey = true, autoIncrement = true)
    public Integer id;

    @Column(name = "bank_id")
    @ForeignKey(table = BankTable.class, column = "id", onDelete = CASCADE, onUpdate = CASCADE)
    public int bankId;

    @Column(name = "user_id")
    @ForeignKey(table = BankUsersTable.class, column = "id")
    public int userId;

    @Column(name = "ban_timestamp")
    public long banTimestamp;

    @Column(name = "unban_timestamp")
    @Nullable
    public Long unbanTimestamp;
}
