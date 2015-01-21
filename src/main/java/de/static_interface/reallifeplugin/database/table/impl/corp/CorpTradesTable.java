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
import de.static_interface.reallifeplugin.database.table.row.corp.CorpTradesRow;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CorpTradesTable extends Table<CorpTradesRow> {

    public CorpTradesTable(Database db) {
        super(Table.CORP_TRADES_TABLE, db);
    }

    @Override
    public void create() throws SQLException {
        String sql;

        switch (db.getType()) {
            case H2:
                sql =
                        "CREATE TABLE IF NOT EXISTS " + getName() + " ("
                        + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                        + "corp_id INT NOT NULL,"
                        + "material_name VARCHAR(255) NOT NULL,"
                        + "new_amount INT NOT NULL,"
                        + "price DOUBLE NOT NULL,"
                        + "sign_amount INT NOT NULL,"
                        + "changed_amount INT NOT NULL,"
                        + "time BIGINT NOT NULL,"
                        + "type INT NOT NULL,"
                        + "user_id INT NOT NULL,"
                        + "world VARCHAR(36) NOT NULL,"
                        + "x INT NOT NULL,"
                        + "y INT NOT NULL,"
                        + "z INT NOT NULL,"
                        + "FOREIGN KEY (corp_id) REFERENCES " + db.getConfig().getTablePrefix() + Table.CORPS_TABLE
                        + "(id) ON UPDATE CASCADE ON DELETE CASCADE,"
                        + "FOREIGN KEY (user_id) REFERENCES " + db.getConfig().getTablePrefix() + Table.CORP_USERS_TABLE
                        + "(id) ON UPDATE CASCADE ON DELETE CASCADE,"
                        + "INDEX corp_id_I (corp_id),"
                        + "INDEX user_id_I (user_id)"
                        + ");";
                break;

            case MYSQL:
            default:
                sql =
                        "CREATE TABLE IF NOT EXISTS `" + getName() + "` ("
                        + "`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                        + "`corp_id` INT NOT NULL,"
                        + "`material_name` VARCHAR(255) NOT NULL,"
                        + "`new_amount` INT NOT NULL,"
                        + "`price` DOUBLE NOT NULL,"
                        + "`sign_amount` INT NOT NULL,"
                        + "`changed_amount` INT NOT NULL,"
                        + "`time` BIGINT NOT NULL,"
                        + "`type` INT NOT NULL,"
                        + "`user_id` INT NOT NULL,"
                        + "`world` VARCHAR(255) NOT NULL,"
                        + "`x` INT NOT NULL,"
                        + "`y` INT NOT NULL,"
                        + "`z` INT NOT NULL,"
                        + "FOREIGN KEY (`corp_id`) REFERENCES `" + db.getConfig().getTablePrefix() + Table.CORPS_TABLE
                        + "`(`id`) ON UPDATE CASCADE ON DELETE CASCADE,"
                        + "FOREIGN KEY (`user_id`) REFERENCES `" + db.getConfig().getTablePrefix() + Table.CORP_USERS_TABLE
                        + "`(`id`) ON UPDATE CASCADE ON DELETE CASCADE,"
                        + "INDEX `corp_id_I` (`corp_id`),"
                        + "INDEX `user_id_I` (`user_id`)"
                        + ");";
                break;
        }
        PreparedStatement statement = db.getConnection().prepareStatement(sql);
        statement.executeUpdate();
        statement.close();
    }

    @Override
    public ResultSet serialize(CorpTradesRow row) throws SQLException {
        if (row.id != null) {
            throw new IllegalArgumentException("Id should be null!");
        }

        Location loc = row.location;
        String sql = "INSERT INTO `{TABLE}` VALUES(NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        executeUpdate(sql, row.corpId, row.material.toString(),
                      row.newAmount, row.price, row.signAmount, row.changedAmount, row.time, row.type,
                      row.userId, loc.getWorld().getName(), (int) loc.getX(), (int) loc.getY(), (int) loc.getZ());

        return executeQuery("SELECT * FROM `{TABLE}` ORDER BY id DESC LIMIT 1");
    }

    @Override
    public CorpTradesRow[] deserialize(ResultSet rs) throws SQLException {
        int rowcount = 0;
        if (rs.last()) {
            rowcount = rs.getRow();
            rs.beforeFirst(); // not rs.first() because the rs.next() below will move on, missing the first element
        }

        CorpTradesRow[] rows = new CorpTradesRow[rowcount];
        int i = 0;
        while (rs.next()) {
            CorpTradesRow row = new CorpTradesRow();
            if (hasColumn(rs, "id")) {
                row.id = rs.getInt("id");
            }
            if (hasColumn(rs, "corp_id")) {
                row.corpId = rs.getInt("corp_id");
            }
            if (hasColumn(rs, "world") && hasColumn(rs, "x") && hasColumn(rs, "y") && hasColumn(rs, "z")) {
                String world = rs.getString("world");
                double x = rs.getInt("x");
                double y = rs.getInt("y");
                double z = rs.getInt("z");
                row.location = new Location(Bukkit.getWorld(world), x, y, z);
            }
            if (hasColumn(rs, "material_name")) {
                row.material = Material.valueOf(rs.getString("material_name"));
            }
            if (hasColumn(rs, "new_amount")) {
                row.newAmount = rs.getInt("new_amount");
            }
            if (hasColumn(rs, "price")) {
                row.price = rs.getDouble("price");
            }
            if (hasColumn(rs, "sign_amount")) {
                row.signAmount = rs.getInt("sign_amount");
            }
            if (hasColumn(rs, "changed_amount")) {
                row.changedAmount = rs.getInt("changed_amount");
            }
            if (hasColumn(rs, "time")) {
                row.time = rs.getLong("time");
            }
            if (hasColumn(rs, "type")) {
                row.type = rs.getInt("type");
            }
            if (hasColumn(rs, "userId")) {
                row.userId = rs.getInt("user_id");
            }
            rows[i] = row;
            i++;
        }
        return rows;
    }
}
