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

package de.static_interface.reallifeplugin.module.bank;

import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.bank.command.BankCommand;
import de.static_interface.reallifeplugin.module.bank.database.table.AccountAccessTable;
import de.static_interface.reallifeplugin.module.bank.database.table.AccountsTable;
import de.static_interface.reallifeplugin.module.bank.database.table.BankTable;
import de.static_interface.reallifeplugin.module.bank.database.table.BankUsersTable;
import de.static_interface.reallifeplugin.module.bank.database.table.BannedUsersTable;
import de.static_interface.reallifeplugin.module.bank.database.table.LoansTable;
import de.static_interface.sinklibrary.api.configuration.Configuration;
import de.static_interface.sinklibrary.database.AbstractTable;
import de.static_interface.sinklibrary.database.Database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

public class BankingModule extends Module<ReallifeMain> {

    public static final String NAME = "Banking";

    public BankingModule(ReallifeMain plugin, Configuration config,
                         @Nullable Database db) {
        super(plugin, config, db, NAME, true);
    }

    @Override
    protected void onEnable() {
        BankManager.init(this);
        registerModuleCommand("bank", new BankCommand(this));
    }

    @Override
    protected void onDisable() {
        BankManager.unload();
    }

    @Override
    protected Collection<AbstractTable> getTables() {
        List<AbstractTable> tables = new ArrayList<>();
        AbstractTable table = new BankTable(getDatabase());
        tables.add(table);
        table = new BankUsersTable(getDatabase());
        tables.add(table);
        table = new AccountsTable(getDatabase());
        tables.add(table);
        table = new AccountAccessTable(getDatabase());
        tables.add(table);
        table = new LoansTable(getDatabase());
        tables.add(table);
        table = new BannedUsersTable(getDatabase());
        tables.add(table);
        return tables;
    }
}
