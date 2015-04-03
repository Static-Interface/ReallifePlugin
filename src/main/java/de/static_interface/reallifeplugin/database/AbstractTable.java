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

import de.static_interface.reallifeplugin.ReallifeMain;
import org.apache.commons.lang.Validate;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import javax.annotation.Nullable;

public abstract class AbstractTable<T> {
    private final String name;
    protected Database db;


    public AbstractTable(String name, Database db) {
        this.name = name;
        this.db = db;
    }

    public String getName() {
        return db.getConfig().getTablePrefix() + name;
    }

    public abstract void create() throws SQLException;

    public abstract ResultSet serialize(T row) throws SQLException;

    public ResultSet[] serialize(T[] rows) throws SQLException {
        ResultSet[] result  = new ResultSet[rows.length];
        for(int i = 0; i < rows.length; i++) {
            result[i] = serialize(rows[i]);
        }

        return result;
    }

    public abstract T[] deserialize(ResultSet rs) throws SQLException;

    public T[] get(String query, Object... paramObjects) throws SQLException {
        query = query.replaceAll("\\Q{TABLE}\\E", getName());
        PreparedStatement statement = db.getConnection().prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        if (paramObjects != null && paramObjects.length > 0) {
            int i = 1;
            for (Object s : paramObjects) {
                statement.setObject(i, s);
                i++;
            }
        }

        return deserialize(statement.executeQuery());
    }

    public T insert(T row) throws SQLException {
        Validate.notNull(row);
        return deserialize(serialize(row))[0];
    }

    public ResultSet executeQuery(String sql, @Nullable Object... paramObjects) throws SQLException {
        sql = sql.replaceAll("\\Q{TABLE}\\E", getName());
        try {
            PreparedStatement statment = db.getConnection().prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
                                                                             ResultSet.CONCUR_UPDATABLE);
            if (paramObjects != null) {
                int i = 1;
                for (Object s : paramObjects) {
                    statment.setObject(i, s);
                    i++;
                }
            }
            return statment.executeQuery();
        } catch (SQLException e) {
            ReallifeMain.getInstance().getLogger().severe("Couldn't execute SQL query: " + sqlToString(sql, paramObjects));
            throw e;
        }
    }

    public void executeUpdate(String sql, @Nullable Object... paramObjects) throws SQLException {
        sql = sql.replaceAll("\\Q{TABLE}\\E", getName());
        try {
            PreparedStatement statment = db.getConnection().prepareStatement(sql);
            if (paramObjects != null) {
                int i = 1;
                for (Object s : paramObjects) {
                    statment.setObject(i, s);
                    i++;
                }
            }
            statment.executeUpdate();
        } catch (SQLException e) {
            ReallifeMain.getInstance().getLogger().severe("Couldn't execute SQL update: " + sqlToString(sql, paramObjects));
            throw e;
        }
    }

    private String sqlToString(String sql, Object... paramObjects) {
        if (paramObjects == null || paramObjects.length < 1) {
            return sql;
        }

        for (Object paramObject : paramObjects) {
            sql = sql.replaceFirst("\\Q?\\E", paramObject.toString());
        }

        return sql;
    }

    public boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columns = rsmd.getColumnCount();
        for (int x = 1; x <= columns; x++) {
            if (columnName.equals(rsmd.getColumnName(x))) {
                return true;
            }
        }
        return false;
    }

    public boolean exists() {
        try {
            DatabaseMetaData dbm = db.getConnection().getMetaData();
            ResultSet tables = dbm.getTables(null, null, name, null);
            return tables.next();
        } catch (Exception e) {
            return false;
        }
    }
}