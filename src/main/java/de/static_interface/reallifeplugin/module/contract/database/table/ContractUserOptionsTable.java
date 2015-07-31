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

package de.static_interface.reallifeplugin.module.contract.database.table;

import de.static_interface.reallifeplugin.database.AbstractTable;
import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.module.contract.database.row.ContractUserOptionsRow;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ContractUserOptionsTable extends AbstractTable<ContractUserOptionsRow> {

    public static final String TABLE_NAME = "contract_user_options";

    public ContractUserOptionsTable(Database db) {
        super(TABLE_NAME, db);
    }

    @Override
    public void create() throws SQLException {
        String sql;

        switch (db.getDialect()) {
            case H2:
                sql =
                        "CREATE TABLE IF NOT EXISTS " + getName() + " ("
                        + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                        + "user_id INT NOT NULL,"
                        + "contract_id INT NOT NULL,"
                        + "money DOUBLE,"
                        + "isCreator BOOLEAN NOT NULL"
                        + "FOREIGN KEY (user_id) REFERENCES " + db.getConfig().getTablePrefix() + ContractUsersTable.TABLE_NAME
                        + "(id) ON UPDATE CASCADE ON DELETE CASCADE,"
                        + "FOREIGN KEY (contract_id) REFERENCES " + db.getConfig().getTablePrefix() + ContractsTable.TABLE_NAME
                        + "(id) ON UPDATE CASCADE ON DELETE CASCADE,"
                        + "INDEX isCreator_I (isCreator)"
                        + ");";
                break;

            case MYSQL:
            default:
                sql =
                        "CREATE TABLE IF NOT EXISTS `" + getName() + "` ("
                        + "`id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                        + "`user_id` INT NOT NULL,"
                        + "`contract_id` INT NOT NULL,"
                        + "`money` DOUBLE,"
                        + "`isCreator` BOOLEAN NOT NULL"
                        + "FOREIGN KEY (`user_id`) REFERENCES " + db.getConfig().getTablePrefix() + ContractUsersTable.TABLE_NAME
                        + "(`id`) ON UPDATE CASCADE ON DELETE CASCADE,"
                        + "FOREIGN KEY (`contract_id`) REFERENCES " + db.getConfig().getTablePrefix() + ContractsTable.TABLE_NAME
                        + "(`id`) ON UPDATE CASCADE ON DELETE CASCADE,"
                        + "INDEX `isCreator_I` (`isCreator`)"
                        + ");";
                break;
        }

        PreparedStatement statement = db.getConnection().prepareStatement(sql);
        statement.executeUpdate();
        statement.close();
    }
}
