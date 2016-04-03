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

import static de.static_interface.reallifeplugin.config.RpLanguage.m;
import static de.static_interface.sinklibrary.configuration.GeneralLanguage.GENERAL_NOT_ENOUGH_MONEY;
import static de.static_interface.sinklibrary.configuration.GeneralLanguage.GENERAL_SUCCESS;

import de.static_interface.reallifeplugin.config.RpLanguage;
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.ModuleCommand;
import de.static_interface.reallifeplugin.module.corporation.Corporation;
import de.static_interface.reallifeplugin.module.corporation.CorporationManager;
import de.static_interface.reallifeplugin.module.corporation.CorporationModule;
import de.static_interface.reallifeplugin.module.corporation.database.row.CorpUserRow;
import de.static_interface.reallifeplugin.module.stockmarket.database.row.StockRow;
import de.static_interface.reallifeplugin.module.stockmarket.database.row.StockUserRow;
import de.static_interface.reallifeplugin.module.stockmarket.database.table.StockUsersTable;
import de.static_interface.reallifeplugin.module.stockmarket.database.table.StocksTable;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.command.annotation.Usage;
import de.static_interface.sinklibrary.api.exception.NotEnoughPermissionsException;
import de.static_interface.sinklibrary.api.exception.UserNotOnlineException;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.MathUtil;
import de.static_interface.sinklibrary.util.VaultBridge;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Usage("<options> [-c <corp>] [-f]")
public class StockMarketCommand extends ModuleCommand<StockMarketModule> {

    private CorporationModule corpModule;
    private List<Transfer> pendingTransfers;
    private HashMap<UUID, Long> transferCooldown;
    private int pendingCooldown;

    public StockMarketCommand(StockMarketModule module, CorporationModule corpModule) {
        super(module);
        getCommandOptions().setPlayerOnly(true);
        this.corpModule = corpModule;
        pendingTransfers = new ArrayList<>();
        transferCooldown = new HashMap<>();
        pendingCooldown = (int) getModule().getConfig().get("Transfer.Cooldown");
        getCommandOptions().setCliOptions(new Options());
        Option corpOption = Option.builder("c")
                .hasArg()
                .longOpt("corp")
                .desc("Force as given corporation member")
                .type(String.class)
                .argName("corp name")
                .build();
        Option forceOption = Option.builder("f")
                .longOpt("force")
                .desc("Force command, even if you don't have permission for it")
                .build();
        getCommandOptions().getCliOptions().addOption(corpOption);
        getCommandOptions().getCliOptions().addOption(forceOption);
    }


    @Override
    protected boolean onExecute(CommandSender commandSender, String label, String[] args) throws ParseException {
        IngameUser user = SinkLibrary.getInstance().getIngameUser((Player) commandSender);

        if (args.length == 0) {
            sendHelp(user);
            return false;
        }

        String subcommand = args[0];
        String prefix = ChatColor.GRAY + "[" + ChatColor.GOLD + "Börse" + ChatColor.GRAY + "] ";

        Corporation corp = CorporationManager.getInstance().getUserCorporation(user);
        boolean isForceMode = user.hasPermission("reallifeplugin.corporations.forcecommand") && getCommandLine().hasOption('f');

        if (user.hasPermission("reallifeplugin.corporations.admin") && getCommandLine().hasOption('p')) {
            isForceMode = true;
            corp = CorporationManager.getInstance().getCorporation(getCommandLine().getOptionValue('p'));
            if (corp == null) {
                user.sendMessage(RpLanguage.m("Corporation.CorporationNotFound", getCommandLine().getOptionValue('p')));
                return true;
            }
        }

        switch (subcommand.toLowerCase()) {
            case "gopublic": {
                if (args.length < 5) {
                    user.sendMessage("/sm gopublic <stock amount> <price> <dividend> <share> [Tag]");
                    break;
                }

                if (corp == null || (!isForceMode && CorporationManager.getInstance().hasCorpPermission(user, StockPermissions.GOPUBLIC))) {
                    user.sendMessage(m("Permissions.General"));
                    return true;
                }

                Stock stock = StockMarket.getInstance().getStock(getModule(), corpModule, corp.getTag());
                if (stock != null) {
                    //Todo
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
                    if (StockMarket.getInstance().getStock(getModule(), corpModule, tag) != null) {
                        //Todo
                        return true;
                    }
                    corp.setTag(tag);
                }

                int amount = Integer.parseInt(args[1]);

                int maxAmount = (int) getModule().getConfig().get("GoPublic.MaxStocks");
                if (maxAmount > 0 && amount > maxAmount) {
                    user.sendMessage(m("StockMarket.MaxStocksAmount", maxAmount));
                    return true;
                }

                double price = Double.parseDouble(args[2]);
                int maxPrice = (int) getModule().getConfig().get("GoPublic.MaxPrice");
                if (maxPrice > 0 && price > maxPrice) {
                    user.sendMessage(m("StockMarket.MaxStocksPrice", maxPrice));
                    return true;
                }

                double divided = Double.parseDouble(args[3]);
                double share = Double.parseDouble(args[4]);
                double minShare = (double) getModule().getConfig().get("GoPublic.MinStocksShare");
                if (minShare > 0 && share < minShare) {
                    user.sendMessage(m("StockMarket.MinStocksShare", minShare));
                    return true;
                }

                double minDividend = (double) getModule().getConfig().get("GoPublic.MinStocksDividend");
                if (minDividend > 0 && divided < minDividend) {
                    user.sendMessage(m("StockMarket.MinStocksDividend", minDividend));
                    return true;
                }

                StockRow row = new StockRow();
                row.amount = amount;
                row.base_price = price;
                row.corp_id = corp.getId();
                row.dividend = divided;
                row.price = price;
                row.share_holding = share;
                row.time = System.currentTimeMillis();
                row.allow_buy_stocks = true;

                row = Module.getTable(getModule(), StocksTable.class).insert(row);

                int ceoAmount = (int) ((amount * (100 - share)) / share);

                addStocks(user, row.id, row.amount, ceoAmount, false);

                user.sendMessage(GENERAL_SUCCESS.format());
                break;
            }

            case "enablebuying": {
                if (CorporationManager.getInstance().hasCorpPermission(user, StockPermissions.TOGGLE_BUY)) {
                    user.sendMessage(m("Permissions.General"));
                    return true;
                }

                Stock stock = StockMarket.getInstance().getStock(getModule(), corpModule, corp.getTag());
                if (stock == null) {
                    user.sendMessage(m("Corporation.DoesntExists", corp.getName())); //Todo: stock not found
                    return true;
                }

                StocksTable StocksTable = Module.getTable(getModule(), StocksTable.class);
                StocksTable.executeUpdate("UPDATE `{TABLE}` SET `allow_buy_stocks` = ? WHERE `corp_id` = ?", true, corp.getId());
                user.sendMessage(GENERAL_SUCCESS.format());
                break;
            }

            case "disablebuying": {
                if (CorporationManager.getInstance().hasCorpPermission(user, StockPermissions.TOGGLE_BUY)) {
                    user.sendMessage(m("Permissions.General"));
                    return true;
                }

                Stock stock = StockMarket.getInstance().getStock(getModule(), corpModule, corp.getTag());
                if (stock == null) {
                    user.sendMessage(m("Corporation.DoesntExists", corp.getName())); //Todo: stock not found
                    return true;
                }

                StocksTable StocksTable = Module.getTable(getModule(), StocksTable.class);
                StocksTable.executeUpdate("UPDATE `{TABLE}` SET `allow_buy_stocks` = ? WHERE `corp_id` = ?", false, corp.getId());

                user.sendMessage(GENERAL_SUCCESS.format());
                break;
            }

            case "buy": {
                if (args.length < 3) {
                    user.sendMessage("/sm buy <stock> <amount>");
                    break;
                }
                String tag = args[1].toUpperCase();
                int amount = Integer.valueOf(args[2]);
                Stock stock = StockMarket.getInstance().getStock(getModule(), corpModule, tag);

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
                    user.sendMessage(GENERAL_NOT_ENOUGH_MONEY.format());
                    return true;
                }

                stock.getCorporation().addBalance(price);

                addStocks(user, stock.getId(), stock.getAmount(), amount, true);

                user.sendMessage(GENERAL_SUCCESS.format());
                break;
            }

            case "sell": {
                if (args.length < 3) {
                    user.sendMessage("/sm sell <stock> <amount>");
                    break;
                }
                String tag = args[1].toUpperCase();
                int amount = Integer.valueOf(args[2]);
                Stock stock = StockMarket.getInstance().getStock(getModule(), corpModule, tag);

                if (stock == null) {
                    user.sendMessage(m("Corporation.DoesntExists", tag)); //Todo: stock not found
                    return true;
                }

                if (!stock.canSellStocks(user)) {
                    user.sendMessage(m("StockMarket.SellingDisabled", tag));
                    return true;
                }

                if (StockMarket.getInstance().getStocksAmount(getModule(), corpModule, stock, user) < amount) {
                    user.sendMessage(m("StockMarket.NotEnoughStocksLeft"));
                    return true;
                }

                double price = amount * stock.getPrice();

                if (!stock.getCorporation().addBalance(-price)) {
                    user.sendMessage(m("Corporation.NotEnoughMoney"));
                    return true;
                }

                VaultBridge.addBalance(user.getPlayer(), price);

                try {
                    sellStocks(user, stock.getId(), stock.getAmount(), amount, true);
                } catch (SQLException e) {
                    e.printStackTrace();
                    user.sendMessage(ChatColor.DARK_RED + "Error: " + ChatColor.RED + e.getMessage());
                    return true;
                }

                user.sendMessage(GENERAL_SUCCESS.format());
                break;
            }

            case "forceupdate": {
                if (!user.hasPermission("ReallifePlugin.StockMarket.ForceUpdate")) {
                    throw new NotEnoughPermissionsException();
                }

                if (StockMarket.getInstance().onStocksUpdate(getModule(), corpModule)) {
                    user.sendMessage(GENERAL_SUCCESS.format());
                } else {
                    user.sendMessage(m("StockMarket.ForceFailed"));
                }

                break;
            }

            case "transfer": {
                if (args.length < 5) {
                    user.sendMessage("/sm transfer <user> <stock> <amount> <price>");
                    break;
                }

                if (transferCooldown.containsKey(user.getUniqueId())) {
                    if (transferCooldown.get(user.getUniqueId()) < System.currentTimeMillis()) {
                        user.sendMessage(m("StockMarket.TransferCooldown", pendingCooldown)); //todo make this configurable
                        return true;
                    } else {
                        transferCooldown.remove(user.getUniqueId());
                    }
                }

                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[1]);
                String tag = args[2].toUpperCase();
                int amount = Integer.valueOf(args[3]);
                int price = Integer.valueOf(args[4]);

                Stock stock = StockMarket.getInstance().getStock(getModule(), corpModule, tag);
                if (stock == null) {
                    user.sendMessage(m("Corporation.DoesntExists", tag)); //Todo: stock not found
                    return true;
                }

                if (StockMarket.getInstance().getStocksAmount(getModule(), corpModule, stock, user) < amount) {
                    user.sendMessage(m("StockMarket.NotEnoughStocksLeft"));
                    return true;
                }

                if (target.getUniqueId().equals(user.getUniqueId())) {
                    user.sendMessage(m("StockMarket.TransferWithSelf"));
                    return true;
                }

                if (target == null || !target.isOnline()) {
                    throw new UserNotOnlineException(args[0]);
                }

                for (Transfer transfer : pendingTransfers) {
                    if (transfer.buyer.equals(target.getUniqueId())) {
                        user.sendMessage(m("StockMarket.PendingTransferNotDone"));
                        return true;
                    }
                }

                Transfer transfer = new Transfer();
                transfer.buyer = target.getUniqueId();
                transfer.seller = user.getUniqueId();
                transfer.amount = amount;
                transfer.price = price;
                transfer.stock = stock;

                user.sendMessage(m("StockMarket.WaitingForTransfer"));
                target.sendMessage(m("StockMarket.TransferRequest", user.getDisplayName(), amount, stock.getId(), price));
                pendingTransfers.add(transfer);
                transferCooldown.put(user.getUniqueId(), System.currentTimeMillis() + pendingCooldown * 60 * 1000);
                break;
            }

            case "transferdecline": {
                Transfer transfer = null;
                for (Transfer t : pendingTransfers) {
                    if (t.buyer.equals(user.getUniqueId())) {
                        transfer = t;
                    }
                }

                if (transfer == null) {
                    user.sendMessage(m("StockMarket.NoPendingTransfers"));
                    return true;
                }

                Player seller = Bukkit.getPlayer(transfer.seller);
                if (seller != null && seller.isOnline()) {
                    seller.sendMessage(m("StockMarket.TransferDeclined", user.getDisplayName()));
                }

                pendingTransfers.remove(transfer);
                user.sendMessage(GENERAL_SUCCESS.format());
                break;
            }

            case "transferaccept": {
                Transfer transfer = null;
                for (Transfer t : pendingTransfers) {
                    if (t.buyer.equals(user.getUniqueId())) {
                        transfer = t;
                    }
                }

                if (transfer == null) {
                    user.sendMessage(m("StockMarket.NoPendingTransfers"));
                    return true;
                }

                IngameUser seller = SinkLibrary.getInstance().getIngameUser(transfer.seller);
                if (StockMarket.getInstance().getStocksAmount(getModule(), corpModule, transfer.stock, seller) < transfer.amount) {
                    user.sendMessage(m("StockMarket.InvalidTransfer"));
                    pendingTransfers.remove(transfer);
                    return true;
                }

                if (!user.addBalance(-transfer.price)) {
                    user.sendMessage(GENERAL_NOT_ENOUGH_MONEY.format());
                    return true;
                }

                seller.addBalance(transfer.price);
                try {
                    processTransfer(transfer);
                    pendingTransfers.remove(transfer);
                    user.sendMessage(GENERAL_SUCCESS.format());
                    seller.sendMessage(m("StockMarket.TransferAccepted", user.getDisplayName(), transfer.amount));
                } catch (Exception e) {
                    e.printStackTrace();
                    //revert transactions
                    seller.addBalance(-transfer.price);
                    user.addBalance(transfer.price);
                    user.sendMessage(m("StockMarket.TransferFailed"));
                }
                break;
            }

            case "listmine": {
                Collection<StockUserRow> stocks = StockMarket.getInstance().getAllStocks(getModule(), corpModule, user, null);
                if (stocks.size() < 1) {
                    user.sendMessage(prefix + ChatColor.RED + "No stocks found"); //Todo
                    return true;
                }
                for (StockUserRow userStock : stocks) {
                    if (userStock.amount < 1) {
                        continue;
                    }

                    Stock stock = StockMarket.getInstance().getStock(getModule(), corpModule, userStock.stockId);

                    double percent = 0;
                    String a = null;
                    try {
                        try {
                            percent = StockMarket.getInstance().calculateStockQuotation(corpModule, stock);
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

                        percent = MathUtil.round(Math.abs(percent));

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

                    double holdingPercent = MathUtil.round((100 * userStock.amount) / getStocksAmount(stock));

                    s +=
                            ChatColor.GRAY + " - " + "Dividenden: " + ChatColor.GOLD + stock.getDividend() + ChatColor.GRAY + " - "
                            + "Im Besitz: " + ChatColor.GOLD + userStock.amount + ChatColor.YELLOW + " (" + holdingPercent + "%)";
                    user.sendMessage(prefix + s);
                }
                break;
            }

            case "list": {
                Collection<Stock> stocks = StockMarket.getInstance().getAllStocks(getModule(), corpModule);
                if (stocks.size() < 1) {
                    user.sendMessage(prefix + ChatColor.RED + "No stocks found"); //Todo
                    return true;
                }
                for (Stock stock : StockMarket.getInstance().getAllStocks(getModule(), corpModule)) {
                    double percent = 0;
                    String a = null;
                    try {
                        try {
                            percent = StockMarket.getInstance().calculateStockQuotation(corpModule, stock);
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

                        percent = MathUtil.round(Math.abs(percent));

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

                    double holdingPercent = MathUtil.round((100 * stock.getAmount()) / getStocksAmount(stock));

                    s +=
                            ChatColor.GRAY + " - " + "Dividenden: " + ChatColor.GOLD + stock.getDividend() + ChatColor.GRAY + " - "
                            + "Anzahl: " + ChatColor.GOLD + stock.getAmount() + ChatColor.YELLOW + " (" + holdingPercent + "%)";
                    user.sendMessage(prefix + s);
                }

                break;
            }

            default:
                sendHelp(user);
                break;
        }

        return true;
    }


    public void sendHelp(IngameUser user) {
        user.sendMessage(ChatColor.DARK_GRAY + "=== Stockmarket ===");
        user.sendMessage(ChatColor.GOLD + "/sm buy <Stock> <Amount>");
        user.sendMessage(ChatColor.GOLD + "/sm sell <Stock> <Amount>");
        user.sendMessage(ChatColor.GOLD + "/sm list");
        user.sendMessage(ChatColor.GOLD + "/sm listmine");
        user.sendMessage(ChatColor.GOLD + "/sm transfer <user> <stock> <amount> <price>");
        user.sendMessage(ChatColor.GOLD + "/sm transferaccept");
        user.sendMessage(ChatColor.GOLD + "/sm transferdecline");
        user.sendMessage(ChatColor.GOLD + "/sm enablebuying");
        user.sendMessage(ChatColor.GOLD + "/sm disablebuying");
        user.sendMessage(ChatColor.GOLD + "/sm gopublic <stock amount> <price> <dividend> <share> [Tag]");
    }
    private void processTransfer(Transfer transfer) throws SQLException {
        StockUsersTable usersTable = Module.getTable(getModule(), StockUsersTable.class);

        IngameUser seller = SinkLibrary.getInstance().getIngameUser(transfer.seller);
        IngameUser buyer = SinkLibrary.getInstance().getIngameUser(transfer.buyer);

        CorpUserRow tmp = CorporationManager.getInstance().getCorpUser(seller);

        StockUserRow[] rows = usersTable.get("SELECT * FROM `{TABLE}` WHERE `user_id` = ? AND `stock_id` = ?", tmp.id, transfer.stock.getId());
        if (rows.length > 0) {
            StockUserRow row = rows[0];
            int newAmount = row.amount - transfer.amount;
            if (newAmount < 0) {
                throw new IllegalStateException("Seller doesn't have enough stocks");
            }
            usersTable.executeUpdate("UPDATE `{TABLE}` SET `amount` = ? WHERE `id` = ?", newAmount, row.id);
        } else {
            throw new IllegalStateException("Seller doesn't have any stocks");
        }

        tmp = CorporationManager.getInstance().getCorpUser(buyer);

        rows = usersTable.get("SELECT * FROM `{TABLE}` WHERE `user_id` = ? AND `stock_id` = ?", tmp.id, transfer.stock.getId());
        if (rows.length > 0) {
            StockUserRow row = rows[0];
            int newAmount = row.amount + transfer.amount;
            usersTable.executeUpdate("UPDATE `{TABLE}` SET `amount` = ? WHERE `id` = ?", newAmount, row.id);
            return;
        }

        StockUserRow row = new StockUserRow();
        row.amount = transfer.amount;
        row.stockId = transfer.stock.getId();
        row.userId = tmp.id;
        usersTable.insert(row);
    }

    private void sellStocks(IngameUser user, int stockId, int stockAmount, int amount, boolean addToStock) throws SQLException {
        int newAmount = stockAmount + amount;
        if (addToStock) {
            StocksTable table = Module.getTable(getModule(), StocksTable.class);
            table.executeUpdate("UPDATE `{TABLE}` SET `amount` = ? WHERE `id` = ?", newAmount, stockId);
        }

        CorpUserRow tmp = CorporationManager.getInstance().getCorpUser(user);

        StockUsersTable usersTable = Module.getTable(getModule(), StockUsersTable.class);

        StockUserRow[] rows = usersTable.get("SELECT * FROM `{TABLE}` WHERE `user_id` = ? AND `stock_id` = ?", tmp.id, stockId);
        if (rows.length > 0) {
            StockUserRow row = rows[0];
            newAmount = row.amount - amount;
            if (newAmount < 0) {
                throw new IllegalStateException("User doesn't have enough stocks");
            }
            usersTable.executeUpdate("UPDATE `{TABLE}` SET `amount` = ? WHERE `id` = ?", newAmount, row.id);
            return;
        }
        throw new IllegalStateException("User doesn't have any stocks");
    }

    public int getStocksAmount(Stock stock) {
        int allStocks = stock.getAmount();
        StockUserRow[]
                rows =
                Module.getTable(getModule(), StockUsersTable.class)
                        .get("SELECT * FROM `{TABLE}` where `stock_id` = ?", stock.getId());
        for (StockUserRow row : rows) {
            allStocks += row.amount;
        }
        return allStocks;
    }

    private void addStocks(IngameUser user, int stockId, int stockAmount, int amount, boolean removeFromStock) {
        int newAmount = stockAmount - amount;
        if (removeFromStock && newAmount < 0) {
            throw new IllegalStateException("Corporation doesn't have enough stocks");
        }
        if (removeFromStock) {
            StocksTable table = Module.getTable(getModule(), StocksTable.class);
            table.executeUpdate("UPDATE `{TABLE}` SET `amount` = ? WHERE `id` = ?", newAmount, stockId);
        }

        CorpUserRow tmp = CorporationManager.getInstance().getCorpUser(user);

        StockUsersTable usersTable = Module.getTable(getModule(), StockUsersTable.class);

        StockUserRow[] rows = usersTable.get("SELECT * FROM `{TABLE}` WHERE `user_id` = ? AND `stock_id` = ?", tmp.id, stockId);
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
