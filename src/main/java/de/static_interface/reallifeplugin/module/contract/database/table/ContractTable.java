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

public class ContractTable extends AbstractTable<ContractRow> {

    public static final String TABLE_NAME = "contracts_queue";

    public ContractTable(Database db) {
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
                        + "name VARCHAR(255) NOT NULL,"
                        + "creator VARCHAR(36) NOT NULL,"
                        + "content TEXT NOT NULL,"
                        + "type INT NOT NULL,"
                        + "events VARCHAR(255) NOT NULL,"
                        + "users TEXT NOT NULL,"
                        + "money_amounts TEXT,"
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
                        + "`creator` VARCHAR(255) NOT NULL,"
                        + "`content` TEXT NOT NULL,"
                        + "`type` INT NOT NULL,"
                        + "`events` VARCHAR(255) NOT NULL,"
                        + "`users` TEXT NOT NULL,"
                        + "`money_amounts` TEXT,"
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

        String sql = "INSERT INTO `{TABLE}` VALUES(NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        executeUpdate(sql, row.name, row.creator, row.content, row.type, row.events, row.users, row.money_amounts, row.period, row.creation_time,
                      row.expire_time);
        return executeQuery("SELECT * FROM `{TABLE}` ORDER BY id DESC LIMIT 1");
    }

    @Override
    public ContractRow[] deserialize(ResultSet rs) throws SQLException {
        int rowcount = 0;
        if (rs.last()) {
            rowcount = rs.getRow();
            rs.beforeFirst();
        }

        ContractRow[] rows = new ContractRow[rowcount];
        int i = 0;

        while (rs.next()) {
            ContractRow row = new ContractRow();
            if (hasColumn(rs, "id")) {
                row.id = rs.getInt("id");
            }
            if (hasColumn(rs, "name")) {
                row.name = rs.getString("name");
            }
            if (hasColumn(rs, "creator")) {
                row.creator = rs.getString("creator");
            }
            if (hasColumn(rs, "content")) {
                row.content = rs.getString("content");
            }
            if (hasColumn(rs, "type")) {
                row.type = rs.getInt("type");
            }
            if (hasColumn(rs, "events")) {
                row.events = rs.getString("events");
            }
            if (hasColumn(rs, "users")) {
                row.users = rs.getString("users");
            }
            if (hasColumn(rs, "money_amounts")) {
                row.money_amounts = rs.getString("money_amounts");
                if (rs.wasNull()) {
                    row.money_amounts = null;
                }
            }
            if (hasColumn(rs, "period")) {
                row.period = rs.getLong("period");
                if (rs.wasNull()) {
                    row.period = null;
                }
            }
            if (hasColumn(rs, "creation_time")) {
                row.creation_time = rs.getLong("creation_time");
            }
            if (hasColumn(rs, "expire_time")) {
                row.expire_time = rs.getLong("expire_time");
            }
            rows[i] = row;
            i++;
        }
        return rows;
    }
}
