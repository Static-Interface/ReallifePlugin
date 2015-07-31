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
import de.static_interface.reallifeplugin.module.corporation.database.row.CorpRow;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CorpsTable extends AbstractTable<CorpRow> {

    public static final String TABLE_NAME = "corps";
    public CorpsTable(Database db) {
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
                        + "balance DOUBLE NOT NULL,"
                        + "base_id VARCHAR(255) NOT NULL,"
                        + "base_world VARCHAR(255) NOT NULL,"
                        + "ceo_uuid VARCHAR(36) NOT NULL UNIQUE KEY,"
                        + "corp_name VARCHAR(255) NOT NULL UNIQUE KEY,"
                        + "isdeleted TINYINT(0) NOT NULL,"
                        + "tag VARCHAR(5) UNIQUE KEY,"
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
                        + "`tag` VARCHAR(5) UNIQUE KEY,"
                        + "`time` BIGINT NOT NULL"
                        + ");";
                break;
        }

        PreparedStatement statement = db.getConnection().prepareStatement(sql);
        statement.executeUpdate();
        statement.close();
    }
}
