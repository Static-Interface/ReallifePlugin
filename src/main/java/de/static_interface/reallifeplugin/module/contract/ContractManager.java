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

import de.static_interface.reallifeplugin.config.ReallifeLanguageConfiguration;
import de.static_interface.reallifeplugin.module.contract.database.row.Contract;
import de.static_interface.reallifeplugin.module.contract.database.row.ContractUser;
import de.static_interface.reallifeplugin.module.contract.database.row.ContractUserOptions;
import de.static_interface.reallifeplugin.module.contract.database.table.ContractUserOptionsTable;
import de.static_interface.reallifeplugin.module.contract.database.table.ContractUsersTable;
import de.static_interface.reallifeplugin.module.contract.database.table.ContractsTable;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.user.IngameUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

public class ContractManager {

    private static ContractManager instance;
    private ContractModule module;

    ContractManager(ContractModule module) {
        this.module = module;
    }

    public static void init(ContractModule module) {
        instance = new ContractManager(module);
    }

    public static ContractManager getInstance() {
        return instance;
    }

    static void unload() {
        instance = null;
    }

    public IngameUser getIngameUser(int id) {
        UUID uuid = UUID.fromString(module.getTable(ContractUsersTable.class).get("SELECT * FROM `{TABLE}` WHERE `id` = ?", id)[0].uuid);
        return SinkLibrary.getInstance().getIngameUser(uuid);
    }

    public int getUserId(IngameUser user) {
        ContractUser row;
        try {
            row = module.getTable(ContractUsersTable.class).get("SELECT * FROM `{TABLE}` WHERE `uuid` = ?", user.getUniqueId().toString())[0];
            if (row == null) {
                throw new NullPointerException("row equals null");
            }
        } catch (Exception e) {
            row = new ContractUser();
            row.uuid = user.getUniqueId().toString();
            row = module.getTable(ContractUsersTable.class).insert(row);
        }
        return row.id;
    }

    public void onAccepted(Contract contract, List<ContractUserOptions> users) {
        ContractQueue.onQueueFinish(contract);
        if (users.size() == 1) {
            return;
        }

        contract = module.getTable(ContractsTable.class).insert(contract);
        for (ContractUserOptions option : users) {
            option.contractId = contract.id;
            option.lastCheck = System.currentTimeMillis();
            module.getTable(ContractUserOptionsTable.class).insert(option);

            IngameUser user = getIngameUser(option.userId);
            if (user.isOnline()) {
                user.sendMessage(ReallifeLanguageConfiguration.CONTRACT_CREATED.format(contract.name));
            }
        }
    }

    public List<Contract> getContracts(IngameUser user) {
        int id = getUserId(user);
        ContractUserOptions[]
                userContractOptions =
                module.getTable(ContractUserOptionsTable.class).get("SELECT * FROM `{TABLE}` WHERE `user_id` = ?", id);
        List<Contract> contract = new ArrayList<>();

        for (ContractUserOptions useroption : userContractOptions) {
            Contract c = getContract(useroption.contractId, true);
            contract.add(c);
        }
        return contract;
    }

    @Nullable
    public Contract getContract(int id) {
        return getContract(id, false);
    }

    @Nullable
    public Contract getContract(int id, boolean includeExpired) {
        Contract[] result;
        if (includeExpired) {
            result = module.getTable(ContractsTable.class)
                    .get("SELECT * FROM `{TABLE}` WHERE `id` = ? AND `is_cancelled` = 0", id);
        } else {
            result = module.getTable(ContractsTable.class)
                    .get("SELECT * FROM `{TABLE}` WHERE  (`expire_time` < 0 OR `expire_time` > ?) AND `is_cancelled` = 0 AND `id` = ?",
                         System.currentTimeMillis(), id);
        }
        if (result == null || result.length == 0) {
            return null;
        }
        return result[0];
    }

    public List<ContractUserOptions> getOptions(IngameUser user) {
        int id = getUserId(user);
        return Arrays.asList(module.getTable(ContractUserOptionsTable.class).get("SELECT * FROM `{TABLE}` WHERE `user_id` = ?", id));
    }
}
