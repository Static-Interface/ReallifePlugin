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

package de.static_interface.reallifeplugin.database;

import com.mysema.query.sql.SQLTemplates;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class Database {

    private final DatabaseConfiguration config;
    private final DatabaseType type;
    private final SQLTemplates dialect;
    protected HikariDataSource dataSource;
    protected Plugin plugin;
    protected Connection connection;

    public Database(DatabaseConfiguration config, Plugin plugin, DatabaseType type) {
        this.plugin = plugin;
        this.type = type;
        this.config = config;
        this.dialect = generateDialect();
    }

    public abstract void setupConfig();

    public abstract void connect() throws SQLException;

    public abstract void close() throws SQLException;

    public abstract SQLTemplates generateDialect();

    public DatabaseConfiguration getConfig() {
        return config;
    }

    public DatabaseType getType() {
        return type;
    }

    public Connection getConnection() {
        return connection;
    }

    public SQLTemplates getDialect() {
        return dialect;
    }
}
