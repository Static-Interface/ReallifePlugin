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
import de.static_interface.reallifeplugin.module.stockmarket.database.row.StockPriceRow;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StockPricesTable extends AbstractTable<StockPriceRow> {

    public static final String TABLE_NAME = "stock_price";
    public StockPricesTable(Database db) {
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
                        + "cause TEXT,"
                        + "new_price DOUBLE NOT NULL,"
                        + "old_price DOUBLE NOT NULL,"
                        + "stock_id INT NOT NULL,"
                        + "time BIGINT NOT NULL,"
                        + "FOREIGN KEY (stock_id) REFERENCES " + db.getConfig().getTablePrefix() + StocksTable.TABLE_NAME
                        + "(id) ON UPDATE CASCADE ON DELETE CASCADE,"
                        + "INDEX stock_id_I (stock_id)"
                        + ");";
                break;

            case MYSQL:
            default:
                sql =
                        "CREATE TABLE IF NOT EXISTS `" + getName() + "` ("
                        + "`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                        + "`cause` TEXT,"
                        + "`new_price` DOUBLE NOT NULL,"
                        + "`old_price` DOUBLE NOT NULL,"
                        + "`stock_id` INT NOT NULL,"
                        + "`time` BIGINT NOT NULL,"
                        + "FOREIGN KEY (`stock_id`) REFERENCES `" + db.getConfig().getTablePrefix() + StocksTable.TABLE_NAME
                        + "`(`id`) ON UPDATE CASCADE ON DELETE CASCADE,"
                        + "INDEX `stock_id_I` (`stock_id`)"
                        + ");";
                break;
        }

        PreparedStatement statement = db.getConnection().prepareStatement(sql);
        statement.executeUpdate();
        statement.close();
    }

    //Todo

    @Override
    public ResultSet serialize(StockPriceRow row) throws SQLException {
        if (row.id != null) {
            throw new IllegalArgumentException("Id should be null!");
        }

        String sql = "INSERT INTO `{TABLE}` VALUES(NULL, ?, ?, ?, ?, ?);";
        executeUpdate(sql, row.cause, row.new_price, row.old_price, row.stock_id, row.time);
        return executeQuery("SELECT * FROM `{TABLE}` ORDER BY id DESC LIMIT 1");
    }
}
