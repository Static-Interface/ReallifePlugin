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

package de.static_interface.reallifeplugin.module.bank.database.wrapper;

import static de.static_interface.sinklibrary.database.query.Query.eq;
import static de.static_interface.sinklibrary.database.query.Query.from;

import de.static_interface.reallifeplugin.module.bank.BankManager;
import de.static_interface.reallifeplugin.module.bank.database.row.BankUserRow;
import de.static_interface.reallifeplugin.module.bank.database.table.BankUsersTable;

import javax.annotation.Nullable;

public class BankUser {

    private int id;
    private BankUsersTable bankUsersTable;

    public BankUser(BankUsersTable tbl, BankUserRow row) {
        this.id = row.id;
        bankUsersTable = tbl;
    }

    public int getId() {
        return id;
    }

    public BankUserRow getBase() {
        return from(bankUsersTable).select().where("id", eq("?")).get(id);
    }

    @Nullable
    public Bank getBank() {
        if (getBase().bankId == null) {
            return null;
        }

        return BankManager.getInstance().getBank(getBase().bankId);
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof BankUser && (((BankUser) o).getId() == id);
    }
}
