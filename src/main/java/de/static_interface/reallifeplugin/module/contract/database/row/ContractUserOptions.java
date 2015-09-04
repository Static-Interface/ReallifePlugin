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

import static de.static_interface.sinklibrary.database.CascadeAction.CASCADE;

import de.static_interface.reallifeplugin.module.contract.database.table.ContractUsersTable;
import de.static_interface.reallifeplugin.module.contract.database.table.ContractsTable;
import de.static_interface.sinklibrary.database.Row;
import de.static_interface.sinklibrary.database.annotation.Column;
import de.static_interface.sinklibrary.database.annotation.ForeignKey;
import de.static_interface.sinklibrary.database.annotation.Index;

import javax.annotation.Nullable;

public class ContractUserOptions implements Row {

    @Column(autoIncrement = true, primaryKey = true)
    public Integer id;

    @Column(name = "user_id")
    @ForeignKey(table = ContractUsersTable.class, column = "id", onUpdate = CASCADE, onDelete = CASCADE)
    @Index
    public int userId;

    @Column(name = "contract_id")
    @ForeignKey(table = ContractsTable.class, column = "id", onUpdate = CASCADE, onDelete = CASCADE)
    @Index
    public int contractId;

    @Column
    @Nullable
    public Double money;

    @Column
    @Nullable
    public Long lastCheck;

    @Column
    public int moneyFailCount = 0;
}
