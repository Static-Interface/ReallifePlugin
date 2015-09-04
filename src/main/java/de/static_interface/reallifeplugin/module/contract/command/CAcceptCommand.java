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

import static de.static_interface.reallifeplugin.config.ReallifeLanguageConfiguration.m;

import de.static_interface.reallifeplugin.config.ReallifeLanguageConfiguration;
import de.static_interface.reallifeplugin.module.ModuleCommand;
import de.static_interface.reallifeplugin.module.contract.ContractManager;
import de.static_interface.reallifeplugin.module.contract.ContractModule;
import de.static_interface.reallifeplugin.module.contract.ContractQueue;
import de.static_interface.reallifeplugin.module.contract.database.row.Contract;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.user.IngameUser;
import org.apache.commons.cli.ParseException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CAcceptCommand extends ModuleCommand<ContractModule> {

    public CAcceptCommand(ContractModule module) {
        super(module);
        getCommandOptions().setPlayerOnly(true);
    }

    @Override
    protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
        IngameUser user = SinkLibrary.getInstance().getIngameUser((Player) sender);
        List<Contract> queue = ContractQueue.getQueue(user);
        if (queue.size() == 0) {
            user.sendMessage(ReallifeLanguageConfiguration.CONTRACT_NO_PENDINGS.format());
            return true;
        }

        int id = getArg(args, 0, Integer.class);
        Contract c = ContractManager.getInstance().getContract(id);
        if (c == null || !ContractQueue.contains(user, c)) {
            user.sendMessage(ReallifeLanguageConfiguration.CONTRACT_NOT_FOUND.format());
        }

        ContractQueue.accept(user, c);

        user.sendMessage(m(ReallifeLanguageConfiguration.CONTRACT_ACCEPTED.format()));

        IngameUser creator = ContractManager.getInstance().getIngameUser(c.ownerId);
        if (creator.isOnline()) {
            creator.sendMessage(
                    ReallifeLanguageConfiguration.CONTRACT_ACCEPTED_OWNER.format(sender, ChatColor.translateAlternateColorCodes('&', c.name)));
        }

        return true;
    }
}
