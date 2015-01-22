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

package de.static_interface.reallifeplugin.commands;

import static de.static_interface.reallifeplugin.ReallifeLanguageConfiguration.m;

import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.corporation.Corporation;
import de.static_interface.reallifeplugin.corporation.CorporationUtil;
import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.database.table.impl.stockmarket.StockUsersTable;
import de.static_interface.reallifeplugin.database.table.impl.stockmarket.StocksTable;
import de.static_interface.reallifeplugin.database.table.row.corp.CorpUserRow;
import de.static_interface.reallifeplugin.database.table.row.stockmarket.StockRow;
import de.static_interface.reallifeplugin.database.table.row.stockmarket.StockUserRow;
import de.static_interface.reallifeplugin.stockmarket.Stock;
import de.static_interface.reallifeplugin.stockmarket.StockMarketUtil;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.command.SinkCommand;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.VaultBridge;
import org.apache.commons.cli.ParseException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;

public class StockMarketCommand extends SinkCommand {

    public StockMarketCommand(Plugin plugin) {
        super(plugin);
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

                Corporation corp = CorporationUtil.getUserCorporation(user);

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
                Database db = ReallifeMain.getInstance().getDB();

                StockRow row = new StockRow();
                row.amount = amount;
                row.basePrice = price;
                row.corpId = corp.getId();
                row.dividend = divided;
                row.price = price;
                row.shareHolding = share;
                row.time = System.currentTimeMillis();

                try {
                    row = db.getStocksTable().insert(row);
                } catch (SQLException e) {
                    user.sendMessage(ChatColor.RED + "An internal error occured");
                    e.printStackTrace();
                    return true;
                }

                int ceoShare = (int) ((double) (100 * amount) / share) - amount;

                try {
                    addStocks(user, StockMarketUtil.getStock(row.id), ceoShare);
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
                Stock stock = StockMarketUtil.getStock(tag);

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
                    addStocks(user, stock, amount);
                } catch (SQLException e) {
                    e.printStackTrace();
                    user.sendMessage(ChatColor.DARK_RED + "Error: " + ChatColor.RED + e.getMessage());
                    return true;
                }

                user.sendMessage(m("General.Success"));
                break;
            }

            default:
                user.sendMessage("/sm <gopublic/buy>");
                break;
        }

        return true;
    }

    private void addStocks(IngameUser user, Stock stock, int amount) throws SQLException {
        int newAmount = stock.getAmount() - amount;
        Database db = ReallifeMain.getInstance().getDB();
        StocksTable table = db.getStocksTable();
        table.executeUpdate("UPDATE `{TABLE}` SET `amount` = ? WHERE `id` = ?", newAmount, stock.getId());

        CorpUserRow tmp = CorporationUtil.getCorpUser(user);
        if (tmp == null) {
            tmp = CorporationUtil.insertUser(user, null);
        }

        StockUsersTable usersTable = db.getStockUsersTable();

        StockUserRow[] rows = usersTable.get("SELECT * FROM `{TABLE}` WHERE `user_id` = ? AND `stock_id` = ?", tmp.uuid, stock.getId());
        if (rows.length > 0) {
            StockUserRow row = rows[0];
            int totalAmount = row.amount + amount;
            usersTable.executeUpdate("UPDATE `{TABLE}` SET `amount` = ? WHERE `id` = ?", totalAmount, row.id);
            return;
        }

        StockUserRow row = new StockUserRow();
        row.amount = amount;
        row.stockId = stock.getId();
        row.userId = tmp.id;
        usersTable.insert(row);
    }
}
