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

package de.static_interface.reallifeplugin.module.corporation.database.table;

import de.static_interface.reallifeplugin.database.AbstractTable;
import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.module.corporation.database.row.CorpTradesRow;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CorpTradesTable extends AbstractTable<CorpTradesRow> {

    public static final String TABLE_NAME = "corp_trades";
    public CorpTradesTable(Database db) {
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
                        + "FOREIGN KEY (corp_id) REFERENCES " + db.getConfig().getTablePrefix() + CorpsTable.TABLE_NAME
                        + "(id) ON UPDATE CASCADE ON DELETE CASCADE,"
                        + "FOREIGN KEY (user_id) REFERENCES " + db.getConfig().getTablePrefix() + CorpUsersTable.TABLE_NAME
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
                        + "FOREIGN KEY (`corp_id`) REFERENCES `" + db.getConfig().getTablePrefix() + CorpsTable.TABLE_NAME
                        + "`(`id`) ON UPDATE CASCADE ON DELETE CASCADE,"
                        + "FOREIGN KEY (`user_id`) REFERENCES `" + db.getConfig().getTablePrefix() + CorpUsersTable.TABLE_NAME
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

        String sql = "INSERT INTO `{TABLE}` VALUES(NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        executeUpdate(sql, row.corp_id, row.material_name,
                      row.new_amount, row.price, row.sign_amount, row.changed_amount, row.time, row.type,
                      row.user_id, row.world, row.x, row.y, row.z);

        return executeQuery("SELECT * FROM `{TABLE}` ORDER BY id DESC LIMIT 1");
    }
}
