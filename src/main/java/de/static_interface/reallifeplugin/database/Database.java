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
import de.static_interface.reallifeplugin.database.table.Table;
import de.static_interface.reallifeplugin.database.table.impl.corp.CorpTradesTable;
import de.static_interface.reallifeplugin.database.table.impl.corp.CorpUsersTable;
import de.static_interface.reallifeplugin.database.table.impl.corp.CorpsTable;
import de.static_interface.reallifeplugin.database.table.impl.stockmarket.StockPricesTable;
import de.static_interface.reallifeplugin.database.table.impl.stockmarket.StockTradesTable;
import de.static_interface.reallifeplugin.database.table.impl.stockmarket.StockUsersTable;
import de.static_interface.reallifeplugin.database.table.impl.stockmarket.StocksTable;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

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
    private StockTradesTable stockTradeHistoryTable;
    private StockUsersTable stockUsersTable;
    private StockPricesTable stockPriceTable;

    private List<Table> tables = new ArrayList<>();

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
        corpsTable = new CorpsTable(this);
        tables.add(corpsTable);
        corpUsersTable = new CorpUsersTable(this);
        tables.add(corpUsersTable);
        corpTradesTable = new CorpTradesTable(this);
        tables.add(corpTradesTable);
        stocksTable = new StocksTable(this);
        tables.add(stocksTable);
        stockPriceTable = new StockPricesTable(this);
        tables.add(stockPriceTable);
        stockTradeHistoryTable = new StockTradesTable(this);
        tables.add(stockTradeHistoryTable);
        stockUsersTable = new StockUsersTable(this);
        tables.add(stockUsersTable);
    }

    public void addTable(Table table) {
        try {
            addTable(table, false);
        } catch (SQLException e) {
            //shouldn't happen
        }
    }

    public void addTable(Table table, boolean create) throws SQLException {
        tables.add(table);
        if (create) {
            table.create();
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

    public StockPricesTable getStockPriceTable() {
        return stockPriceTable;
    }

    public StockTradesTable getStockTradeHistoryTable() {
        return stockTradeHistoryTable;
    }

    public StockUsersTable getStockUsersTable() {
        return stockUsersTable;
    }

    public SQLTemplates getDialect() {
        return dialect;
    }

    @Nullable
    public Table getTable(String table) {
        for (Table tbl : tables) {
            if (tbl.getName().equals(table)) {
                return tbl;
            }
        }
        return null;
    }
}
