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

import de.static_interface.reallifeplugin.module.bank.database.row.BankRow;
import de.static_interface.reallifeplugin.module.bank.database.table.BankTable;

public class Bank {

    protected BankTable bankTable;
    private int id;

    public Bank(BankTable tbl, BankRow row) {
        bankTable = tbl;
        this.id = row.id;
    }

    public BankRow getBase() {
        return from(bankTable).select().where("id", eq("?")).get(id);
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof Bank && (((Bank) o).getId() == id);
    }
}
