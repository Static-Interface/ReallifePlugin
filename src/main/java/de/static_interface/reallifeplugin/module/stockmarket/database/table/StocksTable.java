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
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpsTable;
import de.static_interface.reallifeplugin.module.stockmarket.database.row.StockRow;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StocksTable extends AbstractTable<StockRow> {

    public static final String TABLE_NAME = "stocks";
    public StocksTable(Database db) {
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
                        + "amount INT NOT NULL,"
                        + "base_price DOUBLE NOT NULL,"
                        + "corp_id INT NOT NULL UNIQUE KEY,"
                        + "dividend DOUBLE NOT NULL,"
                        + "price DOUBLE NOT NULL,"
                        + "share_holding DOUBLE NOT NULL,"
                        + "allow_buy_stocks BOOLEAN NOT NULL,"
                        + "time BIGINT NOT NULL,"
                        + "FOREIGN KEY (corp_id) REFERENCES " + db.getConfig().getTablePrefix() + CorpsTable.TABLE_NAME
                        + "(id) ON UPDATE CASCADE ON DELETE CASCADE"
                        + ");";
                break;

            case MYSQL:
            default:
                sql =
                        "CREATE TABLE IF NOT EXISTS `" + getName() + "` ("
                        + "`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                        + "`amount` INT NOT NULL,"
                        + "`base_price` DOUBLE NOT NULL,"
                        + "`corp_id` INT NOT NULL UNIQUE KEY,"
                        + "`dividend` DOUBLE NOT NULL,"
                        + "`price` DOUBLE NOT NULL,"
                        + "`share_holding` DOUBLE NOT NULL,"
                        + "`allow_buy_stocks` BOOLEAN NOT NULL,"
                        + "`time` BIGINT NOT NULL,"
                        + "FOREIGN KEY (`corp_id`) REFERENCES `" + db.getConfig().getTablePrefix() + CorpsTable.TABLE_NAME
                        + "`(`id`) ON UPDATE CASCADE ON DELETE CASCADE"
                        + ");";
                break;
        }

        PreparedStatement statement = db.getConnection().prepareStatement(sql);
        statement.executeUpdate();
        statement.close();
    }

    @Override
    public ResultSet serialize(StockRow row) throws SQLException {
        if (row.id != null) {
            throw new IllegalArgumentException("Id should be null!");
        }

        String sql = "INSERT INTO `{TABLE}` VALUES(NULL, ?, ?, ?, ?, ?, ?, ?, ?);";
        executeUpdate(sql, row.amount, row.base_price, row.corp_id, row.dividend, row.price, row.share_holding, row.allow_buy_stocks, row.time);
        return executeQuery("SELECT * FROM `{TABLE}` ORDER BY id DESC LIMIT 1");
    }
}
