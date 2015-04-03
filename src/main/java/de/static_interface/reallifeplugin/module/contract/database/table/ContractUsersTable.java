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
import de.static_interface.reallifeplugin.module.contract.database.row.ContractUserRow;
import org.apache.commons.lang.Validate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ContractUsersTable extends AbstractTable<ContractUserRow> {

    public static final String TABLE_NAME = "contract_users";

    public ContractUsersTable(Database db) {
        super(TABLE_NAME, db);
    }

    @Override
    public void create() throws SQLException {
        String sql;

        switch (db.getType()) {
            case H2:
                sql =
                        "CREATE TABLE IF NOT EXISTS " + getName() + " ("
                        + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                        + "uuid VARCHAR(36) NOT NULL"
                        + ");";
                break;

            case MYSQL:
            default:
                sql =
                        "CREATE TABLE IF NOT EXISTS `" + getName() + "` ("
                        + "`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                        + "`uuid` VARCHAR(36) NOT NULL"
                        + ");";
                break;
        }

        PreparedStatement statement = db.getConnection().prepareStatement(sql);
        statement.executeUpdate();
        statement.close();
    }

    @Override
    public ResultSet serialize(ContractUserRow row) throws SQLException {
        Validate.notNull(row);
        if (row.id != null) {
            throw new IllegalArgumentException("Id should be null!");
        }

        String sql = "INSERT INTO `{TABLE}` VALUES(NULL, ?);";
        executeUpdate(sql, row.uuid.toString());

        return executeQuery("SELECT * FROM `{TABLE}` ORDER BY id DESC LIMIT 1");
    }

    @Override
    public ContractUserRow[] deserialize(ResultSet rs) throws SQLException {
        int rowcount = 0;
        if (rs.last()) {
            rowcount = rs.getRow();
            rs.beforeFirst();
        }

        ContractUserRow[] rows = new ContractUserRow[rowcount];
        int i = 0;

        while (rs.next()) {
            ContractUserRow row = new ContractUserRow();
            if (hasColumn(rs, "id")) {
                row.id = rs.getInt("id");
            }
            if (hasColumn(rs, "uuid")) {
                row.uuid = UUID.fromString(rs.getString("uuid"));
            }
            rows[i] = row;
            i++;
        }
        return rows;
    }
}