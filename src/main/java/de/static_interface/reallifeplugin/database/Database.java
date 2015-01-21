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
import de.static_interface.reallifeplugin.database.table.impl.corp.CorpTradesTable;
import de.static_interface.reallifeplugin.database.table.impl.corp.CorpUsersTable;
import de.static_interface.reallifeplugin.database.table.impl.corp.CorpsTable;
import de.static_interface.reallifeplugin.database.table.impl.stockmarket.StockTradesTable;
import de.static_interface.reallifeplugin.database.table.impl.stockmarket.StocksTable;
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

    private CorpsTable corpsTable;
    private CorpUsersTable corpUsersTable;
    private CorpTradesTable corpTradesTable;

    private StocksTable stocksTable;
    private StockTradesTable stocksTradeHistoryTable;

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

            stocksTable = new StocksTable(this);
            stocksTable.create();

            stocksTradeHistoryTable = new StockTradesTable(this);
            stocksTradeHistoryTable.create();
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

    public StocksTable getStocksTable() {
        return stocksTable;
    }

    public StockTradesTable getStocksTradeHistoryTable() {
        return stocksTradeHistoryTable;
    }

    public SQLTemplates getDialect() {
        return dialect;
    }
}
