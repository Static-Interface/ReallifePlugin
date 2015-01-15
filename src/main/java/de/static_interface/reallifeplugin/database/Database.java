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
import de.static_interface.reallifeplugin.database.table.CorpTradesTable;
import de.static_interface.reallifeplugin.database.table.CorpUsersTable;
import de.static_interface.reallifeplugin.database.table.CorpsTable;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class Database {

    private final DatabaseConfiguration config;
    private final DatabaseType type;
    protected HikariDataSource db;
    protected Plugin plugin;
    protected Connection connection;
    private CorpsTable corpsTable;
    private CorpUsersTable corpUsersTable;
    private CorpTradesTable corpTradesTable;

    public Database(DatabaseConfiguration config, Plugin plugin, DatabaseType type) {
        this.plugin = plugin;
        this.type = type;
        this.config = config;
    }

    public abstract void setupConfig();

    public abstract void connect() throws SQLException;

    public abstract void close() throws SQLException;

    public DatabaseConfiguration getConfig() {
        return config;
    }

    public DatabaseType getType() {
        return type;
    }

    public void initTables() throws SQLException {
        if (connection == null) {
            throw new IllegalStateException("Use connect() before calling this method");
        }

        createTables();
    }

    public Connection getConnection() {
        return connection;
    }

    protected void createTables() throws SQLException {
        try {
            corpsTable = new CorpsTable(this);
            corpsTable.create();

            corpUsersTable = new CorpUsersTable(this);
            corpUsersTable.create();

            corpTradesTable = new CorpTradesTable(this);
            corpTradesTable.create();

        } catch (SQLException e) {
            connection.close();
            throw e;
        }
    }

    public CorpsTable getCorpsTable() {
        return corpsTable;
    }

    public CorpUsersTable getCorpUsersTable() {
        return corpUsersTable;
    }

    public CorpTradesTable getCorpTradesTable() {
        return corpTradesTable;
    }
}
