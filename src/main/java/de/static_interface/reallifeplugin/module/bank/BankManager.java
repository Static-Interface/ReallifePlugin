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

package de.static_interface.reallifeplugin.module.bank;

import static de.static_interface.sinksql.query.Query.eq;
import static de.static_interface.sinksql.query.Query.from;

import de.static_interface.reallifeplugin.module.bank.database.row.BankRow;
import de.static_interface.reallifeplugin.module.bank.database.row.BankUserRow;
import de.static_interface.reallifeplugin.module.bank.database.table.BankTable;
import de.static_interface.reallifeplugin.module.bank.database.table.BankUsersTable;
import de.static_interface.reallifeplugin.module.bank.database.wrapper.Bank;
import de.static_interface.reallifeplugin.module.bank.database.wrapper.BankUser;

import java.util.UUID;

public class BankManager {

    private static BankManager instance;
    private BankingModule module;

    public BankManager(BankingModule module) {
        this.module = module;
    }

    static void init(BankingModule module) {
        instance = new BankManager(module);
    }

    static void unload() {
        instance = null;
    }

    public static BankManager getInstance() {
        return instance;
    }

    public Bank getBank(int id) {
        BankTable table = module.getTable(BankTable.class);
        BankRow row = from(table)
                .select()
                .where("id", eq("?"))
                .get(id);
        if (row == null) {
            throw new IllegalArgumentException("Bank with id " + id + " not found!");
        }
        return new Bank(table, row);
    }

    public BankUser getUser(UUID uuid) {
        BankUsersTable table = module.getTable(BankUsersTable.class);
        BankUserRow row = from(table)
                .select()
                .where("uuid", eq("?"))
                .get(uuid.toString());

        if (row == null) {
            row = new BankUserRow();
            row.uuid = uuid.toString();
            row = table.insert(row);
        }
        return new BankUser(table, row);
    }
}
