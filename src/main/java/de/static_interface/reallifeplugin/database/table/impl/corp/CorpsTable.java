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

package de.static_interface.reallifeplugin.database.table.impl.corp;

import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.database.table.Table;
import de.static_interface.reallifeplugin.database.table.row.corp.CorpRow;
import org.apache.commons.lang.Validate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class CorpsTable extends Table<CorpRow> {

    public CorpsTable(Database db) {
        super(Table.CORPS_TABLE, db);
    }

    @Override
    public void create() throws SQLException {
        String sql;

        switch (db.getType()) {
            case H2:
                sql =
                        "CREATE TABLE IF NOT EXISTS " + getName() + " ("
                        + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                        + "balance DOUBLE NOT NULL,"
                        + "base_id VARCHAR(255) NOT NULL,"
                        + "base_world VARCHAR(255) NOT NULL,"
                        + "ceo_uuid VARCHAR(36) NOT NULL UNIQUE KEY,"
                        + "corp_name VARCHAR(255) NOT NULL UNIQUE KEY,"
                        + "isdeleted TINYINT(0) NOT NULL,"
                        + "tag VARCHAR(4) UNIQUE KEY,"
                        + "time BIGINT NOT NULL"
                        + ");";
                break;

            case MYSQL:
            default:
                sql =
                        "CREATE TABLE IF NOT EXISTS `" + getName() + "` ("
                        + "`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                        + "`balance` DOUBLE NOT NULL,"
                        + "`base_id` VARCHAR(255) NOT NULL,"
                        + "`base_world` VARCHAR(255) NOT NULL,"
                        + "`ceo_uuid` VARCHAR(36) NOT NULL UNIQUE KEY,"
                        + "`corp_name` VARCHAR(255) NOT NULL UNIQUE KEY, "
                        + "`isdeleted` TINYINT(0) NOT NULL,"
                        + "`tag` VARCHAR(4) UNIQUE KEY,"
                        + "`time` BIGINT NOT NULL"
                        + ");";
                break;
        }

        PreparedStatement statement = db.getConnection().prepareStatement(sql);
        statement.executeUpdate();
        statement.close();
    }

    @Override
    public ResultSet serialize(CorpRow row) throws SQLException {
        Validate.notNull(row);
        if (row.id != null) {
            throw new IllegalArgumentException("Id should be null!");
        }

        if (row.tag != null && row.tag.length() > 4) {
            throw new IllegalArgumentException("Tags may only have max. 4 characters!");
        }

        String sql = "INSERT INTO `{TABLE}` VALUES(NULL, ?, ?, ?, ?, ?, ?, ?, ?);";
        executeUpdate(sql, row.balance, row.baseId, row.baseWorld, row.ceoUniqueId.toString(), row.corpName,
                      row.isDeleted, row.tag, row.time);

        return executeQuery("SELECT * FROM `{TABLE}` ORDER BY id DESC LIMIT 1");
    }

    @Override
    public CorpRow[] deserialize(ResultSet rs) throws SQLException {
        int rowcount = 0;
        if (rs.last()) {
            rowcount = rs.getRow();
            rs.beforeFirst();
        }

        CorpRow[] rows = new CorpRow[rowcount];
        int i = 0;

        while (rs.next()) {
            CorpRow row = new CorpRow();
            if (hasColumn(rs, "id")) {
                row.id = rs.getInt("id");
            }
            if (hasColumn(rs, "balance")) {
                row.balance = rs.getDouble("balance");
            }
            if (hasColumn(rs, "base_id")) {
                row.baseId = rs.getString("base_id");
            }
            if (hasColumn(rs, "base_world")) {
                row.baseWorld = rs.getString("base_world");
            }
            if (hasColumn(rs, "ceo_uuid")) {
                row.ceoUniqueId = UUID.fromString(rs.getString("ceo_uuid"));
            }
            if (hasColumn(rs, "corp_name")) {
                row.corpName = rs.getString("corp_name");
            }
            if (hasColumn(rs, "isdeleted")) {
                row.isDeleted = rs.getBoolean("isdeleted");
            }
            if (hasColumn(rs, "tag")) {
                row.tag = rs.getString("tag");
            }
            if (hasColumn(rs, "time")) {
                row.time = rs.getLong("time");
            }
            rows[i] = row;
            i++;
        }
        return rows;
    }
}
