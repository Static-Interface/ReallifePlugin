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

package de.static_interface.reallifeplugin.database.table;

import de.static_interface.reallifeplugin.database.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import javax.annotation.Nullable;

public abstract class Table<T> {

    private final String name;
    protected Database db;

    public Table(String name, Database db) {
        this.name = name;
        this.db = db;
    }

    public String getName() {
        return name;
    }

    public abstract void create() throws SQLException;

    public abstract ResultSet serialize(T row) throws SQLException;

    public abstract T[] deserialize(ResultSet resultSet) throws SQLException;

    public T[] get(String query, Object... paramObjects) throws SQLException {
        PreparedStatement statement = db.getConnection().prepareStatement(query);
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
        return deserialize(serialize(row))[0];
    }

    public ResultSet executeQuery(String sql, @Nullable Object... paramObjects) throws SQLException {
        sql = sql.replaceAll("\\Q{TABLE}\\E", getName());
        PreparedStatement statment = db.getConnection().prepareStatement(sql);
        if (paramObjects != null) {
            int i = 1;
            for (Object s : paramObjects) {
                statment.setObject(i, s);
                i++;
            }
        }
        return statment.executeQuery();
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
}
