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
import de.static_interface.reallifeplugin.database.table.row.CorpTradesRow;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CorpTradesTable extends Table<CorpTradesRow> {

    public CorpTradesTable(Database db) {
        super("CorpTrades", db);
    }

    @Override
    public void create() throws SQLException {
        String sql =
                "CREATE TABLE IF NOT EXISTS `" + db.getConfig().getTablePrefix() + getName() + "` ("
                + "`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + "`amount` INT NOT NULL,"
                + "`corp_id` INT NOT NULL,"
                + "`material_name` VARCHAR(36) NOT NULL,"
                + "`new_amount` INT NOT NULL,"
                + "`price` DOUBLE NOT NULL,"
                + "`sold_amount` INT NOT NULL,"
                + "`time` BIGINT NOT NULL,"
                + "`type` VARCHAR(10) NOT NULL,"
                + "`user_id` INT NOT NULL,"
                + "`world` VARCHAR(36) NOT NULL,"
                + "`x` INT NOT NULL,"
                + "`y` INT NOT NULL,"
                + "`z` INT NOT NULL,"
                + "INDEX `corp_id_I` (`corp_id`),"
                + "INDEX `user_id_I` (`user_id`)"
                + ");";
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
        return executeQuery(sql, row.amount, row.corp_id, row.material.toString(),
                            row.new_amount, row.price, row.soldAmount, row.time, row.type,
                            row.userId, loc.getWorld().getName(), (int) loc.getX(), (int) loc.getY(), (int) loc.getZ());
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
            if (hasColumn(rs, "amount")) {
                row.amount = rs.getInt("amount");
            }
            if (hasColumn(rs, "corp_id")) {
                row.corp_id = rs.getInt("corp_id");
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
                row.new_amount = rs.getInt("new_amount");
            }
            if (hasColumn(rs, "price")) {
                row.price = rs.getDouble("price");
            }
            if (hasColumn(rs, "sold_amount")) {
                row.soldAmount = rs.getInt("sold_amount");
            }
            if (hasColumn(rs, "time")) {
                row.time = rs.getLong("time");
            }
            if (hasColumn(rs, "type")) {
                row.type = rs.getString("type");
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
