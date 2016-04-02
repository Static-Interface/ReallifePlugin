/*
 * Copyright (c) 2013 - 2016 <http://static-interface.de> and contributors
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

package de.static_interface.reallifeplugin.module.bank.database.row;

import de.static_interface.sinksql.Row;
import de.static_interface.sinksql.annotation.Column;
import de.static_interface.sinksql.annotation.UniqueKey;

public class BankRow implements Row {

    @Column(autoIncrement = true, primaryKey = true)
    public Integer id;

    @Column(name = "ceo_uuid")
    @UniqueKey
    public String ceoUuid;

    @Column(name = "establishment_date")
    public long establishmentDate;

    @Column
    @UniqueKey
    public String name;

    @Column
    @UniqueKey
    public String tag;

    @Column
    @UniqueKey
    public long balance;
}
