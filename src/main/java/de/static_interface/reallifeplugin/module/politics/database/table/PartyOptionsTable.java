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

package de.static_interface.reallifeplugin.module.politics.database.table;

import de.static_interface.sinksql.AbstractTable;
import de.static_interface.sinksql.CascadeAction;
import de.static_interface.sinksql.Database;
import de.static_interface.sinksql.impl.table.OptionsTable;

public class PartyOptionsTable extends OptionsTable {

    public static final String TABLE_NAME = "party_options";

    public PartyOptionsTable(Database db) {
        super(TABLE_NAME, db);
    }

    @Override
    public Class<? extends AbstractTable> getForeignTable() {
        return PartyTable.class;
    }

    @Override
    public String getForeignColumn() {
        return "id";
    }

    @Override
    public CascadeAction getForeignOnUpdateAction() {
        return CascadeAction.CASCADE;
    }

    @Override
    public CascadeAction getForeignOnDeleteAction() {
        return CascadeAction.CASCADE;
    }
}
