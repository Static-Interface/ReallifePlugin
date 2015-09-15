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

import de.static_interface.reallifeplugin.module.ModuleListener;
import de.static_interface.reallifeplugin.module.contract.conversation.ContractEventType;
import de.static_interface.reallifeplugin.module.contract.conversation.ContractType;
import de.static_interface.reallifeplugin.module.contract.database.row.Contract;
import de.static_interface.reallifeplugin.module.contract.database.row.ContractUserOptions;
import de.static_interface.reallifeplugin.module.contract.database.table.ContractUserOptionsTable;
import de.static_interface.reallifeplugin.module.payday.event.PaydayEvent;
import de.static_interface.reallifeplugin.module.payday.model.Entry;
import de.static_interface.reallifeplugin.module.payday.model.PaydayPlayer;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.user.IngameUser;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class ContractListener extends ModuleListener<ContractModule> {

    private Map<UUID, List<ContractEntry>> ownerEntries = new HashMap<>();

    public ContractListener(ContractModule module) {
        super(module);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPaydayLowest(PaydayEvent event) {
        for (PaydayPlayer p : event.getPlayers()) {
            final IngameUser user = SinkLibrary.getInstance().getIngameUser(p.getPlayer());

            for (final ContractUserOptions options : ContractManager.getInstance().getOptions(user)) {
                final Contract c = ContractManager.getInstance().getContract(options.contractId, true);
                if (c == null || c.ownerId == options.userId) {
                    continue;
                }

                IngameUser owner = ContractManager.getInstance().getIngameUser(c.ownerId);
                Long lastcheck = options.lastCheck;
                boolean isExpired = c.expireTime <= System.currentTimeMillis();

                if (ContractEventType.valueOf(c.events) != ContractEventType.MONEY) {
                    continue;
                }
                Double amount = options.money;
                if (amount == null) {
                    amount = 0D;
                }

                switch (ContractType.valueOf(c.type)) {
                    case NORMAL:
                        if (lastcheck != null && lastcheck < c.expireTime && isExpired) {
                            ContractEntry entry = new ContractEntry(user.getName(), options.money, c);
                            p.addEntry(entry);
                            break;
                        }
                        continue;
                    case PERIODIC:
                        long time = System.currentTimeMillis();
                        if (isExpired) {
                            time = c.expireTime;
                        }
                        if (time - options.lastCheck < c.period) {
                            continue;
                        }
                        amount *= (int) ((time - options.lastCheck) / c.period);
                }

                if ((amount < 0 && user.getBalance() - Math.abs(amount) < 0) || (amount > 0 && owner.getBalance() - Math.abs(amount) < 0)) {
                    //Try next time
                    continue;
                }

                getModule().getTable(ContractUserOptionsTable.class)
                        .executeUpdate("UPDATE `{TABLE}` SET `lastCheck` = ? WHERE `id` = ?", System.currentTimeMillis(), options.id);

                if (amount == 0) {
                    continue;
                }

                ContractEntry entry = new ContractEntry(user.getName(), amount, c);
                p.addEntry(entry);
                ContractEntry ownerEntry = new ContractEntry(owner.getName(), user.getName(), -amount, c);
                List<ContractEntry> entries = ownerEntries.get(owner.getUniqueId());
                if (entries == null) {
                    entries = new ArrayList<>();
                }
                entries.add(ownerEntry);
                ownerEntries.put(owner.getUniqueId(), entries);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPayDayHighest(PaydayEvent event) {
        for (PaydayPlayer p : event.getPlayers()) {
            List<ContractEntry> entries = ownerEntries.get(p.getPlayer().getUniqueId());
            if (entries == null || entries.size() == 0) {
                continue;
            }
            for (ContractEntry entry : entries) {
                p.addEntry(entry);
                entries.remove(entry);
            }
            if (entries.size() == 0) {
                ownerEntries.remove(p.getPlayer().getUniqueId());
            } else {
                ownerEntries.put(p.getPlayer().getUniqueId(), entries);
            }
        }

        //Offline players or no payday
        for (UUID offlinePlayer : ownerEntries.keySet()) {
            IngameUser user = SinkLibrary.getInstance().getIngameUser(offlinePlayer);
            for (ContractEntry e : ownerEntries.get(offlinePlayer)) {
                user.addBalance(e.getAmount());
                //we don't have to check for success because the check is already done on the LOWEST listener
            }
        }
        ownerEntries.clear();
    }

    private static class ContractEntry extends Entry {

        private String username;
        private double amount;
        private Contract contract;

        private String from;

        public ContractEntry(String username, double amount, Contract contract) {
            this.username = username;
            this.amount = amount;
            this.contract = contract;
        }

        public ContractEntry(String name, String from, double amount, Contract contract) {
            this(name, amount, contract);
            this.from = from;
        }

        @Override
        public String getSourceAccount() {
            return username;
        }

        @Override
        public String getReason() {
            if (from != null) {
                return "Vertrag: " + ChatColor.translateAlternateColorCodes('&', contract.name) + ChatColor.RESET + ChatColor.GOLD + "(" + from + ")";
            }
            return "Vertrag: " + ChatColor.translateAlternateColorCodes('&', contract.name);
        }

        @Override
        public double getAmount() {
            return amount;
        }

        @Override
        public boolean sendToTarget() {
            return false;
        }

        @Override
        public String getTargetAccount() {
            return null;
        }
    }
}
