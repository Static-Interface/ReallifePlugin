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
import de.static_interface.reallifeplugin.module.contract.ContractEvent;
import de.static_interface.reallifeplugin.module.contract.ContractType;
import de.static_interface.reallifeplugin.module.contract.database.row.ContractRow;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ContractsTable extends AbstractTable<ContractRow> {

    public static final String TABLE_NAME = "contracts";

    public ContractsTable(Database db) {
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

        String events = "";
        for (ContractEvent event : row.events) {
            if (event == null) {
                continue;
            }
            if (events.equals("")) {
                events = "" + event.getId();
                continue;
            }

            events += ", " + event.getId();
        }

        String users = "";
        for (Integer user : row.userIds) {
            if (user == null) {
                continue;
            }
            if (users.equals("")) {
                users = "" + user;
                continue;
            }

            users += ", " + user;
        }

        executeUpdate(sql, row.name, row.creator, row.content, row.type, events, users, row.period, row.creationTime,
                      row.expireTime);
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
            if (hasColumn(rs, "creator_id")) {
                row.creator = rs.getInt("creator_id");
            }
            if (hasColumn(rs, "content")) {
                row.content = rs.getString("content");
            }
            if (hasColumn(rs, "type")) {
                row.type = ContractType.getById(rs.getInt("type"));
            }
            if (hasColumn(rs, "events")) {
                String[] rawStrings = rs.getString("events").split(",");
                List<ContractEvent> events = new ArrayList<>();
                for (String s : rawStrings) {
                    s = s.trim();
                    events.add(ContractEvent.getById(Integer.valueOf(s)));
                }
                row.events = events;
            }
            if (hasColumn(rs, "user_ids")) {
                String[] rawStrings = rs.getString("user_ids").split(",");
                List<Integer> userIds = new ArrayList<>();
                for (String s : rawStrings) {
                    s = s.trim();
                    userIds.add(Integer.valueOf(s));
                }
                row.userIds = userIds;
            }
            if (hasColumn(rs, "period")) {
                row.period = rs.getLong("period");
                if (rs.wasNull()) {
                    row.period = null;
                }
            }
            if (hasColumn(rs, "creation_time")) {
                row.creationTime = rs.getLong("creation_time");
            }
            if (hasColumn(rs, "expire_time")) {
                row.expireTime = rs.getLong("expire_time");
            }
            rows[i] = row;
            i++;
        }
        return rows;
    }
}
