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

package de.static_interface.reallifeplugin.module.stockmarket;

import static de.static_interface.reallifeplugin.ReallifeLanguageConfiguration.m;

import de.static_interface.reallifeplugin.corporation.Corporation;
import de.static_interface.reallifeplugin.corporation.CorporationUtil;
import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.database.table.impl.stockmarket.StockUsersTable;
import de.static_interface.reallifeplugin.database.table.impl.stockmarket.StocksTable;
import de.static_interface.reallifeplugin.database.table.row.corp.CorpUserRow;
import de.static_interface.reallifeplugin.database.table.row.stockmarket.StockRow;
import de.static_interface.reallifeplugin.database.table.row.stockmarket.StockUserRow;
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.ModuleCommand;
import de.static_interface.reallifeplugin.stock.Stock;
import de.static_interface.reallifeplugin.stock.StockMarket;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.configuration.LanguageConfiguration;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.VaultBridge;
import org.apache.commons.cli.ParseException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;

public class StockMarketCommand extends ModuleCommand {

    public StockMarketCommand(Module module) {
        super(module);
        getCommandOptions().setPlayerOnly(true);
    }

    @Override
    protected boolean onExecute(CommandSender commandSender, String label, String[] args) throws ParseException {
        if (args.length == 0) {
            return false;
        }

        String subcommand = args[0];

        IngameUser user = SinkLibrary.getInstance().getIngameUser((Player) sender);

        switch (subcommand.toLowerCase()) {
            case "gopublic": {
                if (args.length < 5) {
                    user.sendMessage("/sm gopublic <stock amount> <price> <dividend> <share> [Tag]");
                    break;
                }

                Corporation corp = CorporationUtil.getUserCorporation(getDatabase(), user);

                if (!corp.isCeo(user)) {
                    user.sendMessage(m("Corporation.NotCEO"));
                    return true;
                }

                if (corp.getTag() == null && args.length < 5) {
                    user.sendMessage(ChatColor.RED + "Tag not set!"); // Todo
                    return true;
                } else if (args.length >= 6 && corp.getTag() == null) {
                    String tag = args[5].toUpperCase();
                    if (tag.length() < 2 || tag.length() > 5) {
                        user.sendMessage(ChatColor.DARK_RED + "Min Tag Length: 2, Max Tag length: 5"); //Todo
                        return true;
                    }

                    corp.setTag(tag);
                }

                int amount = Integer.parseInt(args[1]);
                double price = Double.parseDouble(args[2]);
                double divided = Double.parseDouble(args[3]);
                double share = Double.parseDouble(args[4]);

                StockRow row = new StockRow();
                row.amount = amount;
                row.basePrice = price;
                row.corpId = corp.getId();
                row.dividend = divided;
                row.price = price;
                row.shareHolding = share;
                row.time = System.currentTimeMillis();

                try {
                    row = getDatabase().getStocksTable().insert(row);
                } catch (SQLException e) {
                    user.sendMessage(ChatColor.RED + "An internal error occured");
                    e.printStackTrace();
                    return true;
                }

                int ceoShare = (int) ((double) (100 * amount) / share) - amount;

                try {
                    addStocks(getDatabase(), user, row.id, row.amount, ceoShare, false);
                } catch (SQLException e) {
                    user.sendMessage(ChatColor.RED + "An internal error occured");
                    e.printStackTrace();
                    return true;
                }

                user.sendMessage(m("General.Success"));
                break;
            }

            case "buy": {
                if (args.length < 3) {
                    user.sendMessage("/sm buy <stock> <amount>");
                    break;
                }
                String tag = args[1].toUpperCase();
                int amount = Integer.valueOf(args[2]);
                Stock stock = StockMarket.getInstance().getStock(getDatabase(), tag);

                if (stock == null) {
                    user.sendMessage(m("Corporation.DoesntExists", tag)); //Todo: stock not found
                    return true;
                }

                if (stock.getAmount() < amount) {
                    user.sendMessage(m("StockMarket.NotEnoughStocksLeft"));
                    return true;
                }

                double price = amount * stock.getPrice();
                if (!VaultBridge.addBalance(user.getPlayer(), -price)) {
                    user.sendMessage(m("General.NotEnoughMoney"));
                    return true;
                }

                try {
                    addStocks(getDatabase(), user, stock.getId(), stock.getAmount(), amount, true);
                } catch (SQLException e) {
                    e.printStackTrace();
                    user.sendMessage(ChatColor.DARK_RED + "Error: " + ChatColor.RED + e.getMessage());
                    return true;
                }

                user.sendMessage(m("General.Success"));
                break;
            }

            case "forceupdate": {
                if (!user.hasPermission("ReallifePlugin.StockMarket.ForceUpdate")) {
                    user.sendMessage(LanguageConfiguration.m("Permissions.General"));
                    return false;
                }

                if (StockMarket.getInstance().onStocksUpdate(getDatabase())) {
                    user.sendMessage(m("General.Success"));
                } else {
                    user.sendMessage(m("StockMarket.ForceFailed"));
                }

                break;
            }

            case "list": {
                String prefix = ChatColor.GRAY + "[" + ChatColor.GOLD + "Börse" + ChatColor.GRAY + "] ";

                Collection<Stock> stocks = StockMarket.getInstance().getAllStocks(getDatabase());
                if (stocks.size() < 1) {
                    user.sendMessage(prefix + ChatColor.RED + "No stocks found"); //Todo
                    return true;
                }
                for (Stock stock : StockMarket.getInstance().getAllStocks(getDatabase())) {
                    double percent = 0;
                    String a = null;
                    try {
                        try {
                            percent = StockMarket.getInstance().calculateStockQuotation(getDatabase(), stock);
                        } catch (IOException e) {
                            a = ChatColor.GRAY + "[-] %";
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                        continue;
                    }

                    double oldPrice = stock.getPrice();

                    double newPrice = oldPrice;

                    String
                            s =
                            ChatColor.GOLD + "" + ChatColor.BOLD + stock.getTag() + ChatColor.RESET + "" + ChatColor.GRAY + " - " + ChatColor.GOLD
                            + stock.getCorporation().getName() + ChatColor.GRAY + " - " + ChatColor.GOLD + stock.getPrice() + ChatColor.GRAY + " ";

                    if (a == null) {
                        boolean down = percent < 0;

                        percent = Math.abs(percent);

                        if (down) {
                            newPrice = newPrice + (newPrice * percent);
                        } else {
                            newPrice = newPrice - (newPrice * percent);
                        }
                        if (newPrice > oldPrice) {
                            s += ChatColor.DARK_GREEN + "▲ " + percent + "%";
                        }
                        if (newPrice == oldPrice) {
                            s += ChatColor.YELLOW + "● " + percent + "%";
                        }
                        if (oldPrice > newPrice) {
                            s += ChatColor.DARK_RED + "▼ " + percent + "%";
                        }
                    } else {
                        s += a;
                    }

                    s +=
                            ChatColor.GRAY + " - " + "Dividenden: " + ChatColor.GOLD + stock.getDividend() + " €" + ChatColor.GRAY + " - "
                            + "Anzahl: " + ChatColor.GOLD + stock.getAmount();
                    user.sendMessage(prefix + s);
                }

                break;
            }

            default:
                user.sendMessage("/sm <buy/sell/list/gopublic>");
                break;
        }

        return true;
    }

    private void addStocks(Database db, IngameUser user, int stockId, int stockAmount, int amount, boolean removeFromStock) throws SQLException {
        int newAmount = stockAmount - amount;
        if (removeFromStock) {
            StocksTable table = db.getStocksTable();
            table.executeUpdate("UPDATE `{TABLE}` SET `amount` = ? WHERE `id` = ?", newAmount, stockId);
        }

        CorpUserRow tmp = CorporationUtil.getCorpUser(db, user);
        if (tmp == null) {
            tmp = CorporationUtil.insertUser(db, user, null);
        }

        StockUsersTable usersTable = db.getStockUsersTable();

        StockUserRow[] rows = usersTable.get("SELECT * FROM `{TABLE}` WHERE `user_id` = ? AND `stock_id` = ?", tmp.uuid, stockId);
        if (rows.length > 0) {
            StockUserRow row = rows[0];
            int totalAmount = row.amount + amount;
            usersTable.executeUpdate("UPDATE `{TABLE}` SET `amount` = ? WHERE `id` = ?", totalAmount, row.id);
            return;
        }

        StockUserRow row = new StockUserRow();
        row.amount = amount;
        row.stockId = stockId;
        row.userId = tmp.id;
        usersTable.insert(row);
    }
}
