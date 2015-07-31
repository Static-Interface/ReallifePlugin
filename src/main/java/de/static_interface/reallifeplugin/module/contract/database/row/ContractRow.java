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

import de.static_interface.reallifeplugin.database.Column;
import de.static_interface.reallifeplugin.database.Row;

import javax.annotation.Nullable;

public class ContractRow implements Row {

    @Column
    public Integer id;

    @Column
    public String name;

    @Column
    public int creator;

    @Column
    public String content;

    @Column
    public int type;

    @Column
    public String events;

    @Column
    public String user_ids;

    @Column
    @Nullable
    public Long period;

    @Column
    public long creation_time;

    @Column
    public long expire_time;
}
