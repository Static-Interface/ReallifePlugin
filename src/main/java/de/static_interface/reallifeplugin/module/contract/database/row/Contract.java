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

package de.static_interface.reallifeplugin.module.contract.database.row;

import de.static_interface.sinksql.Row;
import de.static_interface.sinksql.annotation.Column;

import javax.annotation.Nullable;

public class Contract implements Row {
    @Column(autoIncrement = true, primaryKey = true)
    public Integer id;

    @Column
    public String name;

    @Column
    public String content;

    @Column
    public String type;

    @Column
    public String events;

    @Column
    public int ownerId;

    @Column
    @Nullable
    public Long period;

    @Column(name = "creation_time")
    public long creationTime;

    @Column(name = "expire_time")
    public long expireTime;
    @Column(name = "is_cancelled")
    public boolean isCancelled = false;

    @Override
    public int hashCode() {
        return (name.hashCode()) * 2;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Contract)) {
            return false;
        }

        if (((Contract) o).name == null) {
            return false;
        }

        return (((Contract) o).name).equals(name);
    }
}
