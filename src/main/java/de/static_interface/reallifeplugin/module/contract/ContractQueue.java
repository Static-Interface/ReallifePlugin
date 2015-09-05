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
import de.static_interface.reallifeplugin.module.contract.database.row.ContractUserOptions;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.DateUtil;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

public class ContractQueue {

    private static Map<UUID, List<Contract>> queue = new HashMap<>();
    private static Map<Contract, Set<ContractUserOptions>> options = new HashMap<>();

    public static void createQueue(Contract contract, List<ContractUserOptions> optionsRows) {
        int ownerId = contract.ownerId;
        IngameUser owner = ContractManager.getInstance().getIngameUser(ownerId);
        for (ContractUserOptions userOption : optionsRows) {
            IngameUser user = ContractManager.getInstance().getIngameUser(userOption.userId);
            List<Contract> rows = queue.get(user.getUniqueId());
            if (rows == null) {
                rows = new ArrayList<>();
            }
            rows.add(contract);
            queue.put(user.getUniqueId(), rows);
            if (userOption.userId != ownerId) {
                user.sendMessage(ReallifeLanguageConfiguration.CONTRACT_ADDED.format(owner.getDisplayName(), contract.name)); // added to contract

                for (String s : Objects.toString(contract.content).split("\\Q&n\\E")) {
                    user.sendMessage(ChatColor.GOLD + s);
                }

                if (userOption.money == null || userOption.money == 0) {
                    continue;
                }
                String t = "pay";
                if (userOption.money > 0) {
                    t = "receive";
                }
                user.sendMessage("You will " + t + " " + userOption.money + " every " + DateUtil.formatDateDiff(contract.period));
                user.sendMessage(ReallifeLanguageConfiguration.CONTRACT_ACCEPT_MESSAGE.format(rows.size() - 1));
            }
        }
    }

    public static Contract getContract(IngameUser user, int id) {
        List<Contract> rows = queue.get(user.getUniqueId());
        if (rows == null || rows.size() == 0) {
            return null;
        }
        return rows.get(id);
    }

    public static void accept(IngameUser user, Contract contract) {
        removeFromQueue(user, contract);
        boolean left = false;
        for (ContractUserOptions options : getOptions(contract)) {
            IngameUser optionsUser = ContractManager.getInstance().getIngameUser(options.userId);
            if (getQueue(optionsUser).contains(contract)) {
                left = true;
                break;
            }
        }
        if (!left) {
            ContractManager.getInstance().onAccepted(contract, ContractQueue.getOptions(contract));
        }
    }

    private static void removeFromQueue(IngameUser user, Contract contract) {
        List<Contract> rows = queue.get(user.getUniqueId());
        if (rows == null) {
            rows = new ArrayList<>();
        } else {
            rows.remove(contract);
        }
        queue.put(user.getUniqueId(), rows);
    }

    public static List<Contract> getQueue(IngameUser user) {
        List<Contract> queueRows = queue.get(user.getUniqueId());
        if (queueRows == null) {
            return new ArrayList<>();
        }
        List<Contract> parsedContracts = new ArrayList<>();
        for (Contract c : queueRows) {
            if (c != null) {
                parsedContracts.add(c);
            }
        }
        return parsedContracts;
    }

    public static void deny(IngameUser user, Contract contract) {
        removeFromQueue(user, contract);
        Integer userId = ContractManager.getInstance().getUserId(user);
        Set<ContractUserOptions> contractUsers = options.get(contract);
        for (ContractUserOptions option : contractUsers) {
            if (option.userId == userId) {
                contractUsers.remove(option);
            }
        }

        options.put(contract, contractUsers);
    }

    public static void cancel(Contract contract) {
        for (UUID user : queue.keySet()) {
            List<Contract> rows = queue.get(user);
            if (rows == null || !rows.contains(contract)) {
                queue.remove(user);
                continue;
            }
            rows.remove(contract);
            SinkLibrary.getInstance().getIngameUser(user).sendMessage(ReallifeLanguageConfiguration.CONTRACT_CANCELLED.format(contract.name));
        }

        onQueueFinish(contract);
    }

    public static void onQueueFinish(Contract contract) {
        for (UUID uuid : queue.keySet()) {
            List<Contract> qu = queue.get(uuid);
            qu.remove(contract);
            queue.put(uuid, qu);
        }

        options.remove(contract);
    }

    public static Set<ContractUserOptions> getOptions(Contract contract) {
        return options.get(contract);
    }

    @Nullable
    public static Contract getCreatorContract(IngameUser user) {
        int userId = ContractManager.getInstance().getUserId(user);
        for (Contract c : queue.get(user.getUniqueId())) {
            if (c.ownerId == userId) {
                return c;
            }
        }
        return null;
    }

    public static boolean contains(IngameUser user, Contract c) {
        return queue.get(user.getUniqueId()).contains(c);
    }
}
