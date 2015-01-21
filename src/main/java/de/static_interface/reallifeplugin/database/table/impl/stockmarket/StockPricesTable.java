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

package de.static_interface.reallifeplugin.database.table.impl.stockmarket;

import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.database.table.Table;
import de.static_interface.reallifeplugin.database.table.row.stockmarket.StocksPriceRow;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StockPricesTable extends Table<StocksPriceRow> {

    public StockPricesTable(Database db) {
        super(Table.STOCKS_PRICE_TABLE, db);
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
                        + "corp_id INT NOT NULL,"
                        + "dividend_percent DOUBLE NOT NULL,"
                        + "time BIGINT NOT NULL,"
                        + "FOREIGN KEY (corp_id) REFERENCES " + db.getConfig().getTablePrefix() + Table.CORPS_TABLE
                        + "(id) ON UPDATE CASCADE ON DELETE CASCADE,"
                        + "INDEX corp_id_I (corp_id)"
                        + ");";
                break;

            case MYSQL:
            default:
                sql =
                        "CREATE TABLE IF NOT EXISTS `" + getName() + "` ("
                        + "`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                        + "`amount` INT NOT NULL,"
                        + "`base_price` DOUBLE NOT NULL,"
                        + "`corp_id` INT NOT NULL,"
                        + "`dividend_percent` DOUBLE NOT NULL,"
                        + "`time` BIGINT NOT NULL,"
                        + "FOREIGN KEY (`corp_id`) REFERENCES `" + db.getConfig().getTablePrefix() + Table.CORPS_TABLE
                        + "`(`id`) ON UPDATE CASCADE ON DELETE CASCADE,"
                        + "INDEX `corp_id_I` (`corp_id`)"
                        + ");";
                break;
        }

        PreparedStatement statement = db.getConnection().prepareStatement(sql);
        statement.executeUpdate();
        statement.close();
    }

    //Todo

    @Override
    public ResultSet serialize(StocksPriceRow row) throws SQLException {
        return null;
    }

    @Override
    public StocksPriceRow[] deserialize(ResultSet rs) throws SQLException {
        return new StocksPriceRow[0];
    }
}
