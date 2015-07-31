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

package de.static_interface.reallifeplugin.database.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.database.DatabaseConfiguration;
import org.bukkit.plugin.Plugin;
import org.jooq.SQLDialect;

import java.sql.SQLException;

public class MySqlDatabase extends Database {

    public MySqlDatabase(DatabaseConfiguration config, Plugin plugin) {
        super(config, plugin, SQLDialect.MYSQL, '`');
    }

    @Override
    public void setupConfig() {
        HikariConfig hConfig = new HikariConfig();
        hConfig.setMaximumPoolSize(10);
        hConfig.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
        hConfig.addDataSourceProperty("serverName", getConfig().getAddress());
        hConfig.addDataSourceProperty("port", getConfig().getPort());
        hConfig.addDataSourceProperty("databaseName", getConfig().getDatabaseName());
        hConfig.addDataSourceProperty("user", getConfig().getUsername());
        hConfig.addDataSourceProperty("password", getConfig().getPassword());
        hConfig.addDataSourceProperty("autoDeserialize", true);
        hConfig.setConnectionTimeout(5000);
        dataSource = new HikariDataSource(hConfig);
    }

    @Override
    public void connect() throws SQLException {
        try {
            connection = dataSource.getConnection();
        } catch (SQLException e) {
            dataSource.close();
            throw e;
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }

        if (dataSource != null) {
            dataSource.close();
        }
    }
}
