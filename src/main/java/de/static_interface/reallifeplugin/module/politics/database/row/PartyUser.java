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

package de.static_interface.reallifeplugin.module.politics.database.row;

import static de.static_interface.sinksql.CascadeAction.CASCADE;
import static de.static_interface.sinksql.CascadeAction.SET_NULL;

import de.static_interface.reallifeplugin.module.politics.database.table.PartyRanksTable;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyTable;
import de.static_interface.sinksql.Row;
import de.static_interface.sinksql.annotation.Column;
import de.static_interface.sinksql.annotation.ForeignKey;
import de.static_interface.sinksql.annotation.Index;

import javax.annotation.Nullable;

public class PartyUser implements Row {

    @Column(autoIncrement = true, primaryKey = true)
    public Integer id;

    @Column(uniqueKey = true)
    public String uuid;

    @Column(name = "party_id")
    @ForeignKey(table = PartyTable.class, column = "id", onUpdate = CASCADE, onDelete = SET_NULL)
    @Index
    @Nullable
    public Integer partyId;

    @Column(name = "party_rank")
    @ForeignKey(table = PartyRanksTable.class, column = "id", onUpdate = CASCADE, onDelete = SET_NULL)
    @Nullable
    public Integer partyRank;
}
