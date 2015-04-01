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

package de.static_interface.reallifeplugin.module.stockmarket.database.table;

import de.static_interface.reallifeplugin.database.AbstractTable;
import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpUsersTable;
import de.static_interface.reallifeplugin.module.stockmarket.database.row.StockTradeRow;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StockTradesTable extends AbstractTable<StockTradeRow> {

    public static final String NAME = "stock_trades";
    public StockTradesTable(Database db) {
        super(NAME, db);
    }

    @Override
    public void create() throws SQLException {
        String sql;

        switch (db.getType()) {
            case H2:
                sql =
                        "CREATE TABLE IF NOT EXISTS " + getName() + " ("
                        + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                        + "amount INT NOT NULL,"
                        + "new_amount INT NOT NULL,"
                        + "price DOUBLE NOT NULL,"
                        + "stock_id INT NOT NULL,"
                        + "time BIGINT NOT NULL,"
                        + "type INT NOT NULL,"
                        + "user_id INT NOT NULL,"
                        + "FOREIGN KEY (stock_id) REFERENCES " + db.getConfig().getTablePrefix() + StocksTable.TABLE_NAME
                        + "(id) ON UPDATE CASCADE ON DELETE CASCADE,"
                        + "FOREIGN KEY (user_id) REFERENCES " + db.getConfig().getTablePrefix() + CorpUsersTable.TABLE_NAME
                        + "(id) ON UPDATE CASCADE ON DELETE CASCADE,"
                        + "INDEX stock_id_I (stock_id),"
                        + "INDEX user_id_I (user_id)"
                        + ");";
                break;

            case MYSQL:
            default:
                sql =
                        "CREATE TABLE IF NOT EXISTS `" + getName() + "` ("
                        + "`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                        + "`amount` INT NOT NULL,"
                        + "`new_amount` INT NOT NULL,"
                        + "`price` DOUBLE NOT NULL,"
                        + "`stock_id` INT NOT NULL,"
                        + "`time` BIGINT NOT NULL,"
                        + "`type` INT NOT NULL,"
                        + "`user_id` INT NOT NULL,"
                        + "FOREIGN KEY (`stock_id`) REFERENCES `" + db.getConfig().getTablePrefix() + StocksTable.TABLE_NAME
                        + "`(`id`) ON UPDATE CASCADE ON DELETE CASCADE,"
                        + "FOREIGN KEY (`user_id`) REFERENCES `" + db.getConfig().getTablePrefix() + CorpUsersTable.TABLE_NAME
                        + "`(`id`) ON UPDATE CASCADE ON DELETE CASCADE,"
                        + "INDEX `stock_id_I` (`stock_id`),"
                        + "INDEX `user_id_I` (`user_id`)"
                        + ");";
                break;
        }

        PreparedStatement statement = db.getConnection().prepareStatement(sql);
        statement.executeUpdate();
        statement.close();
    }

    @Override
    public ResultSet serialize(StockTradeRow row) throws SQLException {
        if (row.id != null) {
            throw new IllegalArgumentException("Id should be null!");
        }

        String sql = "INSERT INTO `{TABLE}` VALUES(NULL, ?, ?, ?, ?, ?, ?, ?);";
        executeUpdate(sql, row.amount, row.newAmount,
                      row.price, row.stockId, row.time, row.type, row.userId);

        return executeQuery("SELECT * FROM `{TABLE}` ORDER BY id DESC LIMIT 1");
    }

    @Override
    public StockTradeRow[] deserialize(ResultSet rs) throws SQLException {
        int rowcount = 0;
        if (rs.last()) {
            rowcount = rs.getRow();
            rs.beforeFirst(); // not rs.first() because the rs.next() below will move on, missing the first element
        }

        StockTradeRow[] rows = new StockTradeRow[rowcount];
        int i = 0;
        while (rs.next()) {
            StockTradeRow row = new StockTradeRow();
            if (hasColumn(rs, "id")) {
                row.id = rs.getInt("id");
            }
            if (hasColumn(rs, "amount")) {
                row.amount = rs.getInt("amount");
            }
            if (hasColumn(rs, "new_amount")) {
                row.newAmount = rs.getInt("new_amount");
            }
            if (hasColumn(rs, "price")) {
                row.price = rs.getDouble("price");
            }
            if (hasColumn(rs, "stock_id")) {
                row.stockId = rs.getInt("stock_id");
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
