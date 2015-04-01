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

package de.static_interface.reallifeplugin.module.contract.database.table;

import de.static_interface.reallifeplugin.database.AbstractTable;
import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.module.contract.database.row.ContractRow;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ContractQueueTable extends AbstractTable<ContractRow> {

    public static final String TABLE_NAME = "contracts";

    public ContractQueueTable(Database db) {
        super(TABLE_NAME, db);
    }

    @Override
    public void create() throws SQLException {

    }

    @Override
    public ResultSet serialize(ContractRow row) throws SQLException {
        return null;
    }

    @Override
    public ContractRow[] deserialize(ResultSet rs) throws SQLException {
        return new ContractRow[0];
    }
}
