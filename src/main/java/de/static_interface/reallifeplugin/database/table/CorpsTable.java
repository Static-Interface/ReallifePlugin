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

package de.static_interface.reallifeplugin.database.table;

import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.database.table.row.CorpRow;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class CorpsTable extends Table<CorpRow> {

    public CorpsTable(Database db) {
        super("Corps", db);
    }

    @Override
    public void create() throws SQLException {
        String sql =
                "CREATE TABLE IF NOT EXISTS `" + db.getConfig().getTablePrefix() + getName() + "` ("
                + "`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + "`balance` DOUBLE NOT NULL,"
                + "`base_id` VARCHAR(36) NOT NULL,"
                + "`base_world` VARCHAR(36) NOT NULL,"
                + "`ceo_uuid` VARCHAR(36) NOT NULL,"
                + "`corp_name` VARCHAR(36) NOT NULL,"
                + "`creation_time` BIGINT NOT NULL,"
                + "`isdeleted` TINYINT(0) NOT NULL,"
                + "INDEX `corp_name_I` (`corp_name`),"
                + "INDEX `ceo_uuid_I` (`ceo_uuid`)"
                + ");";
        PreparedStatement statement = db.getConnection().prepareStatement(sql);
        statement.executeUpdate();
        statement.close();
    }

    @Override
    public ResultSet serialize(CorpRow row) throws SQLException {
        if (row.id != null) {
            throw new IllegalArgumentException("Id should be null!");
        }

        String sql = "INSERT INTO `{TABLE}` VALUES(NULL, ?, ?, ?, ?, ?, ?, ?);";
        return executeQuery(sql, row.balance, row.base_id, row.base_world, row.ceo.toString(), row.corpName, row.creationTime, row.isdeleted);
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
            if (hasColumn(rs, "base_id")) {
                row.base_id = rs.getString("base_id");
            }
            if (hasColumn(rs, "base_world")) {
                row.base_world = rs.getString("base_world");
            }
            if (hasColumn(rs, "ceo_uuid")) {
                row.ceo = UUID.fromString(rs.getString("ceo_uuid"));
            }
            if (hasColumn(rs, "corp_name")) {
                row.corpName = rs.getString("corp_name");
            }
            if (hasColumn(rs, "creation_time")) {
                row.creationTime = rs.getLong("creation_time");
            }
            if (hasColumn(rs, "isdeleted")) {
                row.isdeleted = rs.getBoolean("isdeleted");
            }
            rows[i] = row;
            i++;
        }
        return rows;
    }
}
