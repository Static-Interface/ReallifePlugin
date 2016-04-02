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

import de.static_interface.reallifeplugin.module.bank.database.table.AccountsTable;
import de.static_interface.reallifeplugin.module.bank.database.table.BankUsersTable;
import de.static_interface.sinksql.Row;
import de.static_interface.sinksql.annotation.Column;
import de.static_interface.sinksql.annotation.ForeignKey;

public class AccountUserAccessRow implements Row {

    @Column(primaryKey = true, autoIncrement = true)
    public Integer id;

    @Column(name = "date_added")
    public long dateAdded;

    @Column
    @ForeignKey(table = BankUsersTable.class, column = "id")
    public int user;

    @Column
    @ForeignKey(table = AccountsTable.class, column = "id")
    public int account;

    @Column
    public String permission;
}
