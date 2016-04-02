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

package de.static_interface.reallifeplugin.module.bank.command;

import de.static_interface.reallifeplugin.module.ModuleCommand;
import de.static_interface.reallifeplugin.module.bank.BankingModule;
import de.static_interface.sinklibrary.api.command.SinkCommandBase;
import de.static_interface.sinklibrary.api.command.SinkSubCommand;
import de.static_interface.sinklibrary.api.command.annotation.DefaultPermission;
import de.static_interface.sinklibrary.api.command.annotation.Description;
import de.static_interface.sinklibrary.api.command.annotation.Usage;
import org.apache.commons.cli.ParseException;
import org.bukkit.command.CommandSender;

@Description("Bank commands")
@DefaultPermission
public class BankCommand extends ModuleCommand<BankingModule> {

    public BankCommand(BankingModule module) {
        super(module);
    }

    @Override
    public void onRegistered() {
        registerSubCommand(new SinkSubCommand<SinkCommandBase>(this, "create") {
            @DefaultPermission
            @Usage("<owner> <name> <tag>")
            @Override
            protected boolean onExecute(CommandSender commandSender, String s, String[] strings) throws ParseException {
                return false;
            }
        });

        registerSubCommand(new BankOwnerCommand(this));
        registerSubCommand(new BankAdminCommand(this));
    }

    @Override
    protected boolean onExecute(CommandSender commandSender, String s, String[] strings) throws ParseException {
        return false;
    }
}
