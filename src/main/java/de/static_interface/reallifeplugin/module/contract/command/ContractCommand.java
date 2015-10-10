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

import static de.static_interface.sinklibrary.configuration.GeneralLanguage.GENERAL_NOT_ENOUGH_MONEY;

import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.config.RpLanguage;
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
import de.static_interface.sinklibrary.api.command.SinkSubCommand;
import de.static_interface.sinklibrary.api.command.annotation.Aliases;
import de.static_interface.sinklibrary.api.command.annotation.DefaultPermission;
import de.static_interface.sinklibrary.api.command.annotation.Description;
import de.static_interface.sinklibrary.api.command.annotation.Usage;
import de.static_interface.sinklibrary.api.exception.NotEnoughPermissionsException;
import de.static_interface.sinklibrary.api.user.SinkUser;
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

@Description("Manage contracts")
@DefaultPermission
@Aliases("c")
public class ContractCommand extends ModuleCommand<ContractModule> {
    public ContractCommand(ContractModule module) {
        super(module);
        getCommandOptions().setMinRequiredArgs(1);
    }

    @Override
    public void onRegistered() {
        registerSubCommand(new SinkSubCommand(this, "new") {
            {
                getCommandOptions().setPlayerOnly(true);
            }

            @Description("Create a new contract")
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                ContractConversation.createNewView((Player) sender, ReallifeMain.getInstance());
                return true;
            }
        });

        registerSubCommand(new SinkSubCommand(this, "list") {
            {
                getCommandOptions().setPlayerOnly(true);
            }

            @Description("List your contracts")
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                IngameUser user = SinkLibrary.getInstance().getIngameUser((Player) sender);
                List<Contract> contracts = ContractManager.getInstance().getContracts(user);
                if (contracts.size() == 0) {
                    sender.sendMessage(RpLanguage.CONTRACTS_NOT_FOUND.format());
                    return true;
                }
                for (Contract c : contracts) {
                    if (c == null) {
                        continue;
                    }
                    String s = "";
                    if (c.isCancelled || c.expireTime <= System.currentTimeMillis()) {
                        s = ChatColor.RESET.toString() + ChatColor.GRAY + " [Expired]";
                    }
                    sender.sendMessage(ChatColor.GOLD + "ID:"
                                       + " #" + c.id + ChatColor.GRAY + ": " + c.name + s);
                }
                return true;
            }
        });

        registerSubCommand(new SinkSubCommand(this, "cancel") {
            {
                getCommandOptions().setMinRequiredArgs(1);
                getCommandOptions().setIrcOpOnly(true);
            }

            @DefaultPermission
            @Usage("<contractId>")
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                SinkUser user = SinkLibrary.getInstance().getUser(sender);
                String permission = getSubPermission("cancel");
                if (!user.hasPermission(permission)) {
                    throw new NotEnoughPermissionsException(permission);
                }

                Contract c = ContractManager.getInstance().getContract(getArg(args, 1, Integer.class));
                if (c == null) {
                    user.sendMessage(RpLanguage.CONTRACT_NOT_FOUND.format());
                    return true;
                }
                getModule().getTable(ContractsTable.class).executeUpdate("UPDATE `{TABLE}` SET `is_cancelled` = 1 WHERE `id` = ?", c.id);
                user.sendMessage(RpLanguage.CONTRACT_CANCELLED.format(ChatColor.translateAlternateColorCodes('&', c.name)));
                return true;
            }
        });

        registerSubCommand(new SinkSubCommand(this, "get") {
            {
                getCommandOptions().setPlayerOnly(true);
                getCommandOptions().setMinRequiredArgs(1);
            }

            @Usage("<contractId>")
            @Description("Get a copy of a contract as a book")
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                IngameUser user = SinkLibrary.getInstance().getIngameUser((Player) sender);
                double balance = (double) getModule().getValue("ContractBookCost", 500D);
                if (user.getBalance() - balance < 0) {
                    user.sendMessage(GENERAL_NOT_ENOUGH_MONEY.format());
                    return true;
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
                    user.sendMessage(RpLanguage.CONTRACT_NOT_FOUND.format());
                    return true;
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

                user.addBalance(-balance);

                meta.setPages(pages);
                book.setItemMeta(meta);
                user.getPlayer().getInventory().addItem(book);
                return true;
            }
        });
    }

    @Override
    protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
        return false;
    }
}
