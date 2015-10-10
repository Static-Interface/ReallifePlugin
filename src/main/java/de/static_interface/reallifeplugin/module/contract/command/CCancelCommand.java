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

package de.static_interface.reallifeplugin.module.contract.command;

import de.static_interface.reallifeplugin.config.RpLanguage;
import de.static_interface.reallifeplugin.module.ModuleCommand;
import de.static_interface.reallifeplugin.module.contract.ContractModule;
import de.static_interface.reallifeplugin.module.contract.ContractQueue;
import de.static_interface.reallifeplugin.module.contract.database.row.Contract;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.command.annotation.Aliases;
import de.static_interface.sinklibrary.api.command.annotation.DefaultPermission;
import de.static_interface.sinklibrary.api.command.annotation.Description;
import de.static_interface.sinklibrary.user.IngameUser;
import org.apache.commons.cli.ParseException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Description("Cancel a pending contract")
@DefaultPermission
@Aliases("ccancel")
public class CCancelCommand extends ModuleCommand<ContractModule> {

    public CCancelCommand(ContractModule module) {
        super(module);
        getCommandOptions().setPlayerOnly(true);
    }

    @Override
    protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
        IngameUser user = SinkLibrary.getInstance().getIngameUser((Player) sender);
        Contract contract = ContractQueue.getCreatorContract(user);
        if (contract == null) {
            user.sendMessage(RpLanguage.CONTRACT_NOT_FOUND.format());
            return true;
        }

        ContractQueue.cancel(contract);
        return true;
    }
}
