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

import de.static_interface.reallifeplugin.config.RpLanguage;
import de.static_interface.reallifeplugin.module.contract.conversation.ContractConversation;
import de.static_interface.reallifeplugin.module.contract.conversation.ContractEventType;
import de.static_interface.reallifeplugin.module.contract.conversation.ContractType;
import de.static_interface.reallifeplugin.module.contract.database.row.Contract;
import de.static_interface.reallifeplugin.module.contract.database.row.ContractUserOptions;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.DateUtil;
import org.bukkit.ChatColor;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;

public class ContractQueue {

    private static Map<UUID, String> queue = new HashMap<>();
    private static Map<String, List<ContractUserOptions>> options = new HashMap<>();
    private static Map<String, Contract> contracts = new HashMap<>();
    private static Map<String, UUID> contractOwners = new HashMap<>();

    public static void createQueue(Contract contract, List<ContractUserOptions> optionsRows) {
        int ownerId = contract.ownerId;
        IngameUser owner = ContractManager.getInstance().getIngameUser(ownerId);
        contractOwners.put(contract.name, owner.getUniqueId());
        options.put(contract.name, optionsRows);
        for (ContractUserOptions userOption : optionsRows) {
            if (userOption.userId == ownerId) {
                continue;
            }
            IngameUser user = ContractManager.getInstance().getIngameUser(userOption.userId);
            String c = queue.get(user.getUniqueId());
            if (c != null) {
                throw new IllegalStateException("User " + user.getDisplayName() + " already has a queue entry");
            }
            contracts.put(contract.name, contract);
            queue.put(user.getUniqueId(), contract.name);
            user.sendMessage(RpLanguage.CONTRACT_ADDED.format(owner.getDisplayName(), contract.name)); // added to contract

            user.sendMessage(ChatColor.GRAY + "Inhalt:");
            for (String s : Objects.toString(contract.content).split("\\Q&n\\E")) {
                user.sendMessage("    " + ChatColor.translateAlternateColorCodes('&', s));
            }

            user.sendMessage(ChatColor.GRAY + "Endet am: " + ChatColor.GOLD + ContractConversation.FORMATTER.format(new Date(contract.expireTime)));

            if (userOption.money == null || userOption.money == 0) {
                continue;
            }

            if (ContractEventType.valueOf(contract.events) == ContractEventType.DEFAULT) {
                user.sendMessage(RpLanguage.CONTRACT_ACCEPT_MESSAGE.format());
                continue;
            }

            String t = "zahlst";
            if (userOption.money > 0) {
                t = "erhälst";
            }
            switch (ContractType.valueOf(contract.type)) {
                case PERIODIC:
                    user.sendMessage(
                            ChatColor.GOLD + "Du " + t + " " + ChatColor.RED + Math.abs(userOption.money) + ChatColor.GOLD + " alle " + ChatColor.RED
                            + DateUtil.formatTimeLeft(new Date(contract.period)));
                    break;

                case NORMAL:
                    user.sendMessage(
                            ChatColor.GOLD + "Du " + t + " " + ChatColor.RED + Math.abs(userOption.money) + ChatColor.GOLD + " sobald der Vertrag "
                            + ChatColor.RED + "ausläuft");
                    break;
            }
            user.sendMessage(RpLanguage.CONTRACT_ACCEPT_MESSAGE.format());
        }
    }

    public static Contract getContract(IngameUser user) {
        String c = queue.get(user.getUniqueId());
        if (c == null) {
            return null;
        }
        return contracts.get(c);
    }

    public static void accept(IngameUser user, Contract contract) {
        contract = contracts.get(contract.name);
        removeFromQueue(user);
        boolean left = false;
        for (ContractUserOptions option : getOptions(contract)) {
            IngameUser optionsUser = ContractManager.getInstance().getIngameUser(option.userId);
            if (Objects.equals(queue.get(optionsUser.getUniqueId()), (contract.name))) {
                left = true;
                break;
            }
        }
        if (!left) {
            ContractManager.getInstance().onAccepted(contract, ContractQueue.getOptions(contract));
        }
    }

    private static void removeFromQueue(IngameUser user) {
        queue.remove(user.getUniqueId());
    }

    public static void deny(IngameUser user, Contract contract) {
        removeFromQueue(user);
        Integer userId = ContractManager.getInstance().getUserId(user);
        List<ContractUserOptions> contractUsers = options.get(contract.name);
        for (ContractUserOptions option : contractUsers) {
            if (option.userId == userId) {
                contractUsers.remove(option);
            }
        }

        options.put(contract.name, contractUsers);
    }

    public static void cancel(Contract contract) {
        for (UUID user : queue.keySet()) {
            SinkLibrary.getInstance().getIngameUser(user).sendMessage(RpLanguage.CONTRACT_CANCELLED.format(contract.name));
        }

        onQueueFinish(contract);
    }

    public static void onQueueFinish(Contract contract) {
        for (UUID uuid : queue.keySet()) {
            removeFromQueue(SinkLibrary.getInstance().getIngameUser(uuid));
        }

        options.remove(contract.name);
        contracts.remove(contract.name);
        contractOwners.remove(contract.name);
    }

    public static List<ContractUserOptions> getOptions(Contract contract) {
        return options.get(contract.name);
    }

    @Nullable
    public static Contract getCreatorContract(IngameUser user) {
        for (String contract : contractOwners.keySet()) {
            UUID owner = contractOwners.get(contract);
            if (owner.equals(user.getUniqueId())) {
                return contracts.get(contract);
            }
        }
        return null;
    }

    public static boolean contains(IngameUser user, Contract c) {
        return queue.get(user.getUniqueId()).contains(c.name);
    }
}
