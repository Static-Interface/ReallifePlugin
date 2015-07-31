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

package de.static_interface.reallifeplugin.module.contract;

import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.contract.database.row.ContractRow;
import de.static_interface.reallifeplugin.module.contract.database.row.ContractUserRow;
import de.static_interface.reallifeplugin.module.contract.database.table.ContractUsersTable;
import de.static_interface.reallifeplugin.module.contract.database.table.ContractsTable;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.user.IngameUser;

import java.sql.SQLException;
import java.util.UUID;

public class Contract {

    private final int id;
    private final ContractModule module;

    public Contract(ContractModule module, int id) {
        this.id = id;
        this.module = module;
    }

    public static IngameUser getUserFromUserId(ContractModule module, int id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(Module.getTable(module, ContractUsersTable.class).get("SELECT FROM `{TABLE}` WHERE `id` = ?", id)[0].uuid);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return SinkLibrary.getInstance().getIngameUser(uuid);
    }

    public static int getIdFromIngameUser(ContractModule module, IngameUser user) {
        ContractUserRow row;
        try {
            row = Module.getTable(module, ContractUsersTable.class).get("SELECT FROM `{TABLE}` WHERE `uuid` = ?", user.getUniqueId().toString())[0];
            if (row == null) {
                throw new NullPointerException("row equals null");
            }
        } catch (Exception e) {
            row = new ContractUserRow();
            row.uuid = user.getUniqueId().toString();
            try {
                row = Module.getTable(module, ContractUsersTable.class).insert(row);
            } catch (SQLException e1) {
                throw new RuntimeException(e1);
            }
        }
        return row.id;
    }

    private ContractRow getBase() {
        try {
            return Module.getTable(getModule(), ContractsTable.class).get("SELECT FROM `{TABLE}` WHERE `id` = " + id)[0];
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private ContractModule getModule() {
        return module;
    }
}
