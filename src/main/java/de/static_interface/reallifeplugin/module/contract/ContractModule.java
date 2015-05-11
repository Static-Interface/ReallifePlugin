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

package de.static_interface.reallifeplugin.module.contract;

import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.database.AbstractTable;
import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.contract.command.CAcceptCommand;
import de.static_interface.reallifeplugin.module.contract.command.CCancelCommand;
import de.static_interface.reallifeplugin.module.contract.command.CDenyCommand;
import de.static_interface.reallifeplugin.module.contract.command.ContractCommand;
import de.static_interface.reallifeplugin.module.contract.database.table.ContractUserOptionsTable;
import de.static_interface.reallifeplugin.module.contract.database.table.ContractUsersTable;
import de.static_interface.reallifeplugin.module.contract.database.table.ContractsTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ContractModule extends Module<ReallifeMain> {

    public static final String NAME = "Contract";

    public ContractModule(ReallifeMain plugin, Database db) {
        super(plugin, ReallifeMain.getInstance().getSettings(), db, NAME, false);
    }

    @Override
    protected void onEnable() {
        addDefaultValue("creationcost", 500);
        registerModuleCommand("contract", new ContractCommand(this));
        registerModuleCommand("ccancel", new CCancelCommand(this));
        registerModuleCommand("caccept", new CAcceptCommand(this));
        registerModuleCommand("cdeny", new CDenyCommand(this));
    }

    @Override
    protected Collection<AbstractTable> getTables() {
        List<AbstractTable> tables = new ArrayList<>();
        tables.add(new ContractsTable(getDatabase()));
        tables.add(new ContractUsersTable(getDatabase()));
        tables.add(new ContractUserOptionsTable(getDatabase()));
        return tables;
    }

}
