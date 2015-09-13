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

import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.config.ReallifeLanguageConfiguration;
import de.static_interface.reallifeplugin.module.ModuleCommand;
import de.static_interface.reallifeplugin.module.contract.ContractManager;
import de.static_interface.reallifeplugin.module.contract.ContractModule;
import de.static_interface.reallifeplugin.module.contract.conversation.ContractConversation;
import de.static_interface.reallifeplugin.module.contract.conversation.ContractEventType;
import de.static_interface.reallifeplugin.module.contract.conversation.ContractType;
import de.static_interface.reallifeplugin.module.contract.database.row.Contract;
import de.static_interface.reallifeplugin.module.contract.database.row.ContractUserOptions;
import de.static_interface.reallifeplugin.module.contract.database.table.ContractsTable;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.exception.NotEnoughPermissionsException;
import de.static_interface.sinklibrary.configuration.LanguageConfiguration;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.DateUtil;
import org.apache.commons.cli.ParseException;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ContractCommand extends ModuleCommand<ContractModule> {

    public ContractCommand(ContractModule module) {
        super(module);
        getCommandOptions().setPlayerOnly(true);
    }

    @Override
    protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
        if (args.length < 1) {
            sender.sendMessage("Usage: /contract <new/list/get/cancel>");
        }
        IngameUser user = SinkLibrary.getInstance().getIngameUser((Player) sender);
        switch (getArg(args, 0, String.class).toLowerCase()) {
            case "new": {
                ContractConversation.createNewView((Player) sender, ReallifeMain.getInstance());
                break;
            }

            case "list": {
                List<Contract> contracts = ContractManager.getInstance().getContracts(user);
                if (contracts.size() == 0) {
                    user.sendMessage(ReallifeLanguageConfiguration.CONTRACTS_NOT_FOUND.format());
                    break;
                }
                for (Contract c : contracts) {
                    String s = "";
                    if (c.isCancelled || c.expireTime <= System.currentTimeMillis()) {
                        s = ChatColor.RESET.toString() + ChatColor.GRAY + " [Expired]";
                    }
                    user.sendMessage(ChatColor.GOLD + "ID:"
                                     + " #" + c.id + ChatColor.GRAY + ": " + c.name + s);
                }
                break;
            }

            case "cancel": {
                if (!user.hasPermission("reallifeplugin.contract.cancel")) {
                    throw new NotEnoughPermissionsException();
                }

                Contract c = ContractManager.getInstance().getContract(getArg(args, 1, Integer.class));
                if (c == null) {
                    user.sendMessage(ReallifeLanguageConfiguration.CONTRACT_NOT_FOUND.format());
                    break;
                }
                getModule().getTable(ContractsTable.class).executeUpdate("UPDATE `{TABLE}` SET `is_cancelled` = 1 WHERE `id` = ?", c.id);
                user.sendMessage(ReallifeLanguageConfiguration.CONTRACT_CANCELLED.format(ChatColor.translateAlternateColorCodes('&', c.name)));
                break;
            }

            case "get": {
                double balance = (double) getModule().getValue("ContractBookCost", 500D);
                if (user.getBalance() - balance < 0) {
                    user.sendMessage(LanguageConfiguration.GENERAL_NOT_ENOUGH_MONEY.format());
                    break;
                }

                user.addBalance(-balance);

                Contract c = ContractManager.getInstance().getContract(getArg(args, 1, Integer.class), true);
                ContractUserOptions cuo = null;
                List<ContractUserOptions> options = ContractManager.getInstance().getOptions(user);

                if (options != null && options.size() > 0 && c != null) {
                    for (ContractUserOptions option : options) {
                        if (option.contractId == c.id) {
                            cuo = option;
                            break;
                        }
                    }
                }

                if (cuo == null || c == null) {
                    user.sendMessage(ReallifeLanguageConfiguration.CONTRACT_NOT_FOUND.format());
                    break;
                }

                ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
                BookMeta meta = (BookMeta) book.getItemMeta();
                List<String> pages = new ArrayList<>();
                meta.setTitle(ChatColor.translateAlternateColorCodes('&', c.name));
                meta.setAuthor(ContractManager.getInstance().getIngameUser(c.ownerId).getDisplayName());

                Collections.addAll(pages, ChatColor.translateAlternateColorCodes('&', c.content).split("\n"));

                String s = ChatColor.DARK_RED + "Contract ID: " + ChatColor.RED + "#" + c.id + "\n";

                ContractType type = ContractType.valueOf(c.type);
                s += ChatColor.DARK_RED + "Contract type: " + ChatColor.RED + type.name() + "\n";

                if (type == ContractType.PERIODIC) {
                    s += ChatColor.DARK_RED + "Period: " + ChatColor.RED + DateUtil.formatTimeLeft(new Date(c.period)) + "\n";
                }

                ContractEventType eType = ContractEventType.valueOf(c.events);
                s += ChatColor.DARK_RED + "Event type: " + ChatColor.RED + eType.name() + "\n";

                if (cuo != null && cuo.userId != c.ownerId && eType == ContractEventType.MONEY) {
                    s += ChatColor.DARK_RED + "Money: " + ChatColor.RED + cuo.money + "\n";
                }

                s += ChatColor.DARK_RED + "Expire time: " + ChatColor.RED + ContractConversation.FORMATTER.format(new Date(c.expireTime));

                pages.add(s);

                meta.setPages(pages);
                book.setItemMeta(meta);
                user.getPlayer().getInventory().addItem(book);
                break;
            }

            default:
                return false;
        }
        return true;
    }
}
