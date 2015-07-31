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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ContractsTable extends AbstractTable<ContractRow> {

    public static final String TABLE_NAME = "contracts";

    public ContractsTable(Database db) {
        super(TABLE_NAME, db);
    }

    @Override
    public void create() throws SQLException {
        String sql;

        switch (db.getDialect()) {
            case H2:
                sql =
                        "CREATE TABLE IF NOT EXISTS " + getName() + " ("
                        + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                        + "name VARCHAR(255) NOT NULL,"
                        + "creator_id VARCHAR(36) NOT NULL,"
                        + "content TEXT NOT NULL,"
                        + "type INT NOT NULL,"
                        + "events VARCHAR(255) NOT NULL,"
                        + "user_ids TEXT NOT NULL,"
                        + "period BIGINT,"
                        + "creation_time BIGINT NOT NULL,"
                        + "expire_time BIGINT NOT NULL"
                        + ");";
                break;

            case MYSQL:
            default:
                sql =
                        "CREATE TABLE IF NOT EXISTS `" + getName() + "` ("
                        + "`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                        + "`name` VARCHAR(255) NOT NULL,"
                        + "`creator_id` VARCHAR(255) NOT NULL,"
                        + "`content` TEXT NOT NULL,"
                        + "`type` INT NOT NULL,"
                        + "`events` VARCHAR(255) NOT NULL,"
                        + "`user_ids` TEXT NOT NULL,"
                        + "`period` BIGINT,"
                        + "`creation_time` BIGINT NOT NULL,"
                        + "`expire_time` BIGINT NOT NULL"
                        + ");";
                break;
        }

        PreparedStatement statement = db.getConnection().prepareStatement(sql);
        statement.executeUpdate();
        statement.close();
    }

    @Override
    public ResultSet serialize(ContractRow row) throws SQLException {
        if (row.id != null) {
            throw new IllegalArgumentException("Id should be null!");
        }

        String sql = "INSERT INTO `{TABLE}` VALUES(NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        executeUpdate(sql, row.name, row.creator, row.content, row.type, row.events, row.user_ids, row.period, row.creation_time,
                      row.expire_time);
        return executeQuery("SELECT * FROM `{TABLE}` ORDER BY id DESC LIMIT 1");
    }
}
