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

import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;
import org.jooq.SQLDialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

public abstract class Database {

    private final DatabaseConfiguration config;
    private final SQLDialect dialect;
    private final char backtick;
    protected HikariDataSource dataSource;
    protected Plugin plugin;
    protected Connection connection;

    public Database(DatabaseConfiguration config, Plugin plugin, SQLDialect dialect, char backtick) {
        this.plugin = plugin;
        this.config = config;
        this.dialect = dialect;
        this.backtick = backtick;
    }

    public char getBacktick() {
        return backtick;
    }

    public String toDatabaseType(Class<?> clazz) {
        if (clazz == Date.class) {
            throw new RuntimeException("Date is for now not supported!");
        }
        if (clazz == java.sql.Date.class) {
            throw new RuntimeException("Date is for now not supported!");
        }
        if (clazz == Integer.class) {
            return "INT";
        }
        if (clazz == Boolean.class) {
            return "TINYINT(1)";
        }
        if (clazz == Double.class) {
            return "DOUBLE";
        }
        if (clazz == Float.class) {
            return "FLOAT";
        }
        if (clazz == Long.class) {
            return "BIGINT";
        }
        if (clazz == Short.class) {
            return "SMALLINT";
        }
        if (clazz == Byte.class) {
            return "TINYINT";
        }
        if (clazz == String.class) {
            return "TEXT";
        }
        throw new RuntimeException("No database type available for: " + clazz.getName());
    }

    public abstract void setupConfig();

    public abstract void connect() throws SQLException;

    public abstract void close() throws SQLException;

    public DatabaseConfiguration getConfig() {
        return config;
    }

    public Connection getConnection() {
        return connection;
    }

    public SQLDialect getDialect() {
        return dialect;
    }
}
