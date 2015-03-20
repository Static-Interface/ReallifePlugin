/*
 * Copyright (c) 2013 - 2014 <http://static-interface.de> and contributors
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

package de.static_interface.reallifeplugin.module.corporation;

import static de.static_interface.reallifeplugin.config.ReallifeLanguageConfiguration.m;

import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.corporation.Corporation;
import de.static_interface.reallifeplugin.corporation.CorporationRanks;
import de.static_interface.reallifeplugin.corporation.CorporationUtil;
import de.static_interface.reallifeplugin.database.table.impl.corp.CorpTradesTable;
import de.static_interface.reallifeplugin.database.table.row.corp.CorpTradesRow;
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.ModuleCommand;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.exception.UserNotFoundException;
import de.static_interface.sinklibrary.api.user.SinkUser;
import de.static_interface.sinklibrary.configuration.LanguageConfiguration;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.user.IrcUser;
import de.static_interface.sinklibrary.util.BukkitUtil;
import de.static_interface.sinklibrary.util.Debug;
import de.static_interface.sinklibrary.util.MathUtil;
import de.static_interface.sinklibrary.util.StringUtil;
import de.static_interface.sinklibrary.util.VaultBridge;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class CorporationCommand extends ModuleCommand<CorporationModule> {

    public CorporationCommand(CorporationModule module) {
        super(module);
        getCommandOptions().setPlayerOnly(true);
    }

    @Override
    public boolean onExecute(CommandSender sender, String label, String[] args) {
        SinkUser user = SinkLibrary.getInstance().getUser(sender);
        Corporation userCorp = null;
        if (user instanceof IngameUser) {
            userCorp = CorporationUtil.getUserCorporation(getModule(), (IngameUser) user);
        }

        if (args.length < 1 && user instanceof IngameUser) {
            if (userCorp != null) {
                sendCorporationInfo(user, userCorp);
                return true;
            }
            user.sendMessage(m("Corporation.NotInCorporation"));
            return true;
        } else if (args.length < 1 && !(user instanceof IngameUser)) {
            return false;
        }

        List<String> tmp = new ArrayList<>(Arrays.asList(args));
        tmp.remove(args[0]);
        String[] moreArgs = tmp.toArray(new String[tmp.size()]);

        switch (args[0].toLowerCase()) {
            case "?":
            case "help":
                sendHelp(user);
                break;

            case "user":
                if (args.length < 2) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    break;
                }

                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[1]);
                if (!target.hasPlayedBefore()) {
                    throw new UserNotFoundException(args[1]);
                }
                sendUserInfo(user, target);

                break;
            case "ceo": {
                if (user instanceof IngameUser) {
                    if (moreArgs.length < 1) {
                        user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                        break;
                    }
                    handleCEOCommand((IngameUser) user, moreArgs, userCorp);
                    break;
                }
            }

            case "admin": {
                if (user instanceof IngameUser || (user instanceof IrcUser && user.isOp())) {
                    if (!user.hasPermission("reallifeplugin.corporations.admin")) {
                        user.sendMessage(LanguageConfiguration.m("Permissions.General"));
                        break;
                    }
                    if (moreArgs.length < 1) {
                        user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                        break;
                    }
                    handleAdminCommand(user, moreArgs);
                    break;
                }
            }

            case "list": {
                boolean listByMoney = false;
                if (args.length > 1) {
                    listByMoney = args[1].equalsIgnoreCase("money");
                }
                sendCorporationsList(user, listByMoney);
                break;
            }

            case "leave": {
                if (user instanceof IngameUser) {
                    //Todo: check for ceo_user_id / co-ceo_user_id
                    if (userCorp == null) {
                        user.sendMessage(m("Corporation.NotInCorporation"));
                        break;
                    }

                    userCorp.removeMember((IngameUser) user);

                    CorporationUtil.sendMessage(userCorp, StringUtil.format(m("Corporation.UserLeftCorporation"), user, null, null));
                    user.sendMessage(m("Corporation.LeftCorporation"));
                    break;
                }
            }

            case "deposit":
                if (user instanceof IngameUser) {

                    if (args.length < 2) {
                        user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                        break;
                    }
                    try {
                        if (userCorp == null) {
                            user.sendMessage(m("Corporation.NotInCorporation"));
                            break;
                        }
                        deposit((IngameUser) user, userCorp, Double.valueOf(args[1]));
                    } catch (NumberFormatException ignored) {
                        user.sendMessage(m("General.InvalidValue", args[1]));
                    }
                    break;
                }

            default: {
                Corporation corporation = CorporationUtil.getCorporation(getModule(), args[0]);
                sendCorporationInfo(user, corporation);
                break;
            }
        }

        return true;
    }

    private void deposit(IngameUser user, Corporation corp, double amount) {
        if (amount < 1) {
            user.sendMessage(m("General.InvalidValue", amount));
            return;
        }

        if (VaultBridge.getBalance(user.getPlayer()) < amount) {
            user.sendMessage(m("General.NotEnoughMoney"));
            return;
        }

        VaultBridge.addBalance(user.getPlayer(), -amount);
        corp.addBalance(amount);

        CorporationUtil.sendMessage(corp, m("Corporation.Deposit", user.getDisplayName(), amount));
    }

    private void withdraw(IngameUser user, Corporation corp, double amount) {
        if (amount < 1) {
            user.sendMessage(m("General.InvalidValue", amount));
            return;
        }

        if (corp.getBalance() < amount) {
            user.sendMessage(m("Corporation.NotEnoughMoney"));
            return;
        }

        VaultBridge.addBalance(user.getPlayer(), amount);
        corp.addBalance(-amount);

        CorporationUtil.sendMessage(corp, m("Corporation.Withdraw", user.getDisplayName(), amount));
    }

    private void handleAdminCommand(SinkUser user, String[] args) {
        switch (args[0].toLowerCase()) {
            case "?":
            case "help":
                sendAdminHelp(user);
                break;

            case "new": {
                if ((user instanceof IngameUser && args.length < 4) || (!(user instanceof IngameUser) && args.length < 5)) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }
                //Todo: remove ceo_user_id from any corporation
                //Todo cancel if ceo_user_id is already a ceo_user_id of another corporation

                String name = args[1];
                if (name.contains("&")) {
                    sender.sendMessage(m("Corporation.InvalidName"));
                    return;
                }

                String base = args[3];
                World world;
                if (user instanceof IngameUser) {
                    world = ((IngameUser) user).getPlayer().getWorld();
                } else {
                    world = Bukkit.getWorld(args[4]);
                }

                boolean successful = CorporationUtil.createCorporation(getModule(), user, name, args[2], base, world);
                Corporation corp = CorporationUtil.getCorporation(getModule(), name);
                String msg = successful ? m("Corporation.Created") : m("Corporation.CreationFailed");
                msg = StringUtil.format(msg, corp.getFormattedName());
                user.sendMessage(msg);
                break;
            }

            case "migrate": {
                user.sendMessage("" + CorporationUtil.migrate(getModule(), user));
                break;
            }

            case "delete": {
                if (args.length < 2) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }

                Corporation corp = CorporationUtil.getCorporation(getModule(), args[1]);
                boolean successful = CorporationUtil.deleteCorporation(getModule(), user, corp);

                if (successful) {
                    user.sendMessage(StringUtil.format(m("Corporation.Deleted"), corp.getFormattedName()));
                }
                break;
            }

            case "setbase": {
                if (args.length < 3 || (!(user instanceof IngameUser) && args.length < 4)) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }

                Corporation corporation = CorporationUtil.getCorporation(getModule(), args[1]);

                World world;
                if (user instanceof IngameUser) {
                    world = ((IngameUser) user).getPlayer().getWorld();
                } else {
                    world = Bukkit.getWorld(args[3]);
                }

                if (corporation == null) {
                    user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[1]));
                    return;
                }
                corporation.setBase(world, args[2]);
                user.sendMessage(m("Corporation.BaseSet"));
                break;
            }

            case "setceo": {
                if (args.length < 3) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }
                Corporation corporation = CorporationUtil.getCorporation(getModule(), args[1]);
                if (corporation == null) {
                    user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[1]));
                    return;
                }
                IngameUser newCEO = SinkLibrary.getInstance().getIngameUser(args[2]);
                corporation.setCEO(newCEO);
                user.sendMessage(m("Corporation.CEOSet"));
                break;
            }

            case "give": {
                if (args.length < 3) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }
                Corporation corporation = CorporationUtil.getCorporation(getModule(), args[1]);
                double amount;
                try {
                    amount = Double.valueOf(args[2]);
                    if (amount < 1) {
                        user.sendMessage(m("General.InvalidValue", amount));
                        return;
                    }
                } catch (NumberFormatException e) {
                    user.sendMessage(m("General.InvalidValue", args[1]));
                    return;
                }

                if (corporation.addBalance(amount)) {
                    user.sendMessage(m("General.Success"));
                } else {
                    user.sendMessage(ChatColor.DARK_RED + "Failure"); //Todo
                }

                break;
            }

            case "take": {
                if (args.length < 3) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }
                Corporation corporation = CorporationUtil.getCorporation(getModule(), args[1]);
                double amount;
                try {
                    amount = Double.valueOf(args[2]);
                    if (amount < 1) {
                        user.sendMessage(m("General.InvalidValue", amount));
                        return;
                    }
                    amount = -amount;
                } catch (NumberFormatException e) {
                    user.sendMessage(m("General.InvalidValue", args[1]));
                    return;
                }
                corporation.addBalance(amount);
                user.sendMessage(m("General.Success"));
                break;
            }

            default: {
                user.sendMessage(m("General.UnknownSubCommand", args[0]));
                break;
            }
        }
    }

    private void handleCEOCommand(IngameUser user, String[] args, Corporation corporation) {
        if (!CorporationUtil.hasCeoPermissions(user, corporation)) {
            user.sendMessage(m("Corporation.NotCEO"));
            return;
        }
        switch (args[0].toLowerCase()) {
            case "?":
            case "help":
                sendCeoHelp(user);
                break;

            case "add": {
                if (args.length < 2) {
                    user.sendMessage(ChatColor.RED + "/corp ceo help");
                    break;
                }

                UUID uuid = BukkitUtil.getUniqueIdByName(args[1]);
                ReallifeMain.getInstance().getLogger().log(Level.INFO, "[Debug] UUID of " + args[1] + ": " + uuid.toString());
                IngameUser target = SinkLibrary.getInstance().getIngameUser(uuid);

                if (!target.hasPlayedBefore()) {
                    throw new UserNotFoundException(args[1]);
                }

                Corporation targetCorporation = CorporationUtil.getUserCorporation(getModule(), target);
                if (targetCorporation != null) {
                    user.sendMessage(m("Corporation.AlreadyMemberOther", target.getName()));
                    return;
                }

                if (corporation.getMembers(false).contains(target)) {
                    user.sendMessage(StringUtil.format(m("Corporation.AlreadyMember"), target.getName()));
                    break;
                }

                corporation.addMember(target);
                if (target.isOnline()) {
                    target.sendMessage(StringUtil.format(m("Corporation.Added"), corporation.getName()));
                }
                user.sendMessage(StringUtil.format(m("Corporation.CEOAdded"), CorporationUtil.getFormattedName(getModule(), target)));
                break;
            }

            case "kick": {
                if (args.length < 2) {
                    user.sendMessage(ChatColor.RED + "/corp ceo help");
                    break;
                }

                boolean success = false;
                for (IngameUser u : corporation.getMembers(false)) {
                    if (ChatColor.stripColor(u.getDisplayName()).equalsIgnoreCase(args[1])
                        || u.getName().equalsIgnoreCase(args[1])) {
                        corporation.removeMember(u);
                        if (u.isOnline()) {
                            u.sendMessage(StringUtil.format(m("Corporation.Kicked"), corporation.getName()));
                            user.sendMessage(StringUtil.format(m("Corporation.CEOKicked"), CorporationUtil.getFormattedName(getModule(), u)));
                        }
                        success = true;
                        break;
                    }
                }
                if (!success) {
                    user.sendMessage(StringUtil.format(m("Corporation.NotMember"), args[1]));
                    break;
                }

                break;
            }
            case "acc":
            case "addcoceo": {
                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[1]);
                if (!corporation.getCEO().equals(user)) {
                    user.sendMessage(m("Corporation.NotCEO"));
                    break;
                }

                if (corporation.getCoCEOs().contains(target)) {
                    //Already Co-CEO
                    user.sendMessage(m("Corporation.AlreadyCoCEO", target.getDisplayName()));
                    break;
                }

                corporation.addCoCeo(target);
                user.sendMessage(m("Corporation.CoCeoAdded", target.getDisplayName()));
                break;
            }

            case "rcc":
            case "removecoceo": {
                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[1]);
                if (!corporation.getCEO().equals(user)) {
                    user.sendMessage(m("Corporation.NotCEO"));
                    return;
                }
                if (!corporation.getCoCEOs().contains(target)) {
                    //Already removed from Co-CEO?
                    user.sendMessage(m("Corporation.NotCoCEO", target.getDisplayName()));
                    break;
                }

                corporation.removeCoCeo(target);
                user.sendMessage(m("Corporation.CoCeoRemoved", target.getDisplayName()));
                break;
            }

            case "setrank": {
                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[1]);
                String rank = ChatColor.translateAlternateColorCodes('&', StringUtil.formatArrayToString(args, " ", 2));
                corporation.setRank(target, rank);
                user.sendMessage(m("Corporation.RankSet", target.getDisplayName(), rank));
                break;
            }

            case "withdraw":
                if (args.length < 2) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    break;
                }
                try {
                    withdraw(user, corporation, Double.valueOf(args[1]));
                } catch (NumberFormatException ignored) {
                    user.sendMessage(m("General.InvalidValue", args[1]));
                }
                break;

            default: {
                user.sendMessage(m("General.UnknownSubCommand", args[0]));
                break;
            }
        }
    }

    private void sendHelp(SinkUser user) {
        user.sendMessage(ChatColor.RED + "Corp Commands: ");
        user.sendMessage(ChatColor.GOLD + "/corp");
        user.sendMessage(ChatColor.GOLD + "/corp <corp>");
        user.sendMessage(ChatColor.GOLD + "/corp help");
        user.sendMessage(ChatColor.GOLD + "/corp leave");
        user.sendMessage(ChatColor.GOLD + "/corp list");
        user.sendMessage(ChatColor.GOLD + "/corp deposit <amount>");
        user.sendMessage(ChatColor.GOLD + "/corp ceo help");

        if (user.hasPermission("reallifeplugin.corporations.admin")) {
            user.sendMessage(ChatColor.GOLD + "/corp admin help");
        }
    }

    private void sendAdminHelp(SinkUser user) {
        user.sendMessage(ChatColor.RED + "Admin Commands: ");
        user.sendMessage(ChatColor.GOLD + "/corp admin new <corp> <ceo> <base>");
        user.sendMessage(ChatColor.GOLD + "/corp admin delete <corp>");
        user.sendMessage(ChatColor.GOLD + "/corp admin setbase <corp> <region>");
        user.sendMessage(ChatColor.GOLD + "/corp admin setceo <corp> <player>");
        user.sendMessage(ChatColor.GOLD + "/corp admin give <corp> <amount>");
        user.sendMessage(ChatColor.GOLD + "/corp admin take <corp> <amount>");
    }

    private void sendCeoHelp(SinkUser user) {
        user.sendMessage(ChatColor.RED + "CEO Commands: ");
        user.sendMessage(ChatColor.GOLD + "/corp ceo add <name>");
        user.sendMessage(ChatColor.GOLD + "/corp ceo kick <name>");
        user.sendMessage(ChatColor.GOLD + "/corp ceo addcoceo <name>");
        user.sendMessage(ChatColor.GOLD + "/corp ceo removecoceo <name>");
        user.sendMessage(ChatColor.GOLD + "/corp ceo setrank <name> <rank>");
        user.sendMessage(ChatColor.GRAY + "/corp ceo withdraw <amount>");
    }

    private void sendCorporationsList(SinkUser user, boolean listByMoney) {
        user.sendMessage(ChatColor.GOLD + "Corporations: ");
        String msg = "";
        for (Corporation corporation : CorporationUtil.getCorporations(getModule())) {
            int data;
            if (listByMoney) {
                data = (int) corporation.getBalance();
            } else {
                data = corporation.getMembers(false).size();
            }
            String formattedName = corporation.getFormattedName() + ChatColor.WHITE + "[" +
                                   data + "]" + ChatColor.RESET;
            if (msg.equals("")) {
                msg = formattedName;
                continue;
            }
            msg += " " + formattedName;
        }
        user.sendMessage(msg);
    }


    private void sendUserInfo(SinkUser user, IngameUser target) {
        user.sendMessage("");
        String divider = ChatColor.GOLD + "";
        for (int i = 0; i < 32; i++) {
            divider += "-";
        }
        user.sendMessage("");
        user.sendMessage(ChatColor.GOLD + " User: " + CorporationUtil.getFormattedName(getModule(), target));
        user.sendMessage(divider);
        Corporation corp = CorporationUtil.getUserCorporation(getModule(), target);
        user.sendMessage(ChatColor.GRAY + "Corporation: " + ChatColor.GOLD + (corp == null ? "-" : corp.getFormattedName()));
        String rank;
        if (corp == null) {
            rank = ChatColor.GOLD + "-";
        } else if (corp.getCEO().equals(target)) {
            rank = CorporationRanks.RANK_CEO;
        } else if (corp.getCoCEOs().contains(target)) {
            rank = CorporationRanks.RANK_CO_CEO;
        } else {
            rank = CorporationRanks.RANK_DEFAULT + "Member";
        }
        user.sendMessage(ChatColor.GRAY + "Rank: " + rank);

        try {
            String soldItems = "-";
            Integer userId = CorporationUtil.getUserId(getModule(), target);
            if (corp != null && userId != null) {
                //Todo make time configurable
                int days = 3;
                long maxTime = System.currentTimeMillis() - 1000 * 60 * 60 * 24 * days;
                CorpTradesRow[] rows =
                        Module.getTable(getModule(), CorpTradesTable.class).get(
                                "SELECT * FROM `{TABLE}` WHERE `user_id` = ? AND `corp_id` = ? AND `time` > ?", userId,
                                corp.getId(), maxTime);
                if (rows.length > 0) {
                    int i = 0;
                    for (CorpTradesRow row : rows) {
                        if (row.type != 0) {
                            continue; //wasn't selling items
                        }
                        i += row.changedAmount;
                    }
                    soldItems = m("Corporation.ItemsSold", target.getDisplayName(), i, days + " " + LanguageConfiguration.m("TimeUnit.Days"));
                }
            }
            user.sendMessage(ChatColor.GRAY + "Items sold: " + ChatColor.GOLD + soldItems);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        user.sendMessage(divider);
        user.sendMessage("");
    }


    private void sendCorporationInfo(SinkUser user, Corporation corporation) {
        if (corporation == null) {
            user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), ""));
            return;
        }

        user.sendMessage("");
        String text = ChatColor.GOLD + " Corporation: " + corporation.getFormattedName();
        String divider = ChatColor.GOLD + "";
        for (int i = 0; i < 32; i++) {
            divider += "-";
        }
        user.sendMessage(ChatColor.RED + text);
        user.sendMessage(divider);
        String ceo = ChatColor.GOLD + "-";
        if (corporation.getCEO() != null) {
            ceo = CorporationUtil.getFormattedName(getModule(), corporation.getCEO());
        }
        user.sendMessage(ChatColor.GRAY + "CEO: " + ceo);
        String coCeos = "-";
        if (corporation.getCoCEOs().size() > 0) {
            for (IngameUser coCeo : corporation.getCoCEOs()) {
                String name = CorporationUtil.getFormattedName(getModule(), coCeo);
                if (StringUtil.isEmptyOrNull(name)) {
                    Debug.log(Level.WARNING, "Empty or null name at CorporationCommand: " + name);
                    continue;
                }
                if (coCeos.equals("-")) {
                    coCeos = name;
                    continue;
                }
                coCeos += ChatColor.GRAY + ", " + name;
            }
        }
        user.sendMessage(ChatColor.GRAY + "Co-CEO's: " + ChatColor.GOLD + coCeos);
        if (corporation.getBaseRegion() != null) {
            user.sendMessage(ChatColor.GRAY + "Base: " + ChatColor.GOLD + corporation.getBaseRegion().getId());
        }

        user.sendMessage(ChatColor.GRAY + "Money: " + ChatColor.GOLD + MathUtil.round(corporation.getBalance()) + " " + VaultBridge.getCurrenyName());
        Set<IngameUser> members = corporation.getMembers(true);
        String membersFormatted = "-";
        if (members.size() > 0) {
            for (IngameUser member : members) {
                try {
                    String name = CorporationUtil.getFormattedName(getModule(), member);
                    if (StringUtil.isEmptyOrNull(name)) {
                        Debug.log(Level.WARNING, "Empty or null name at CorporationCommand: " + name);
                        continue;
                    }
                    if (!member.hasPlayedBefore()) {
                        Debug.log(Level.WARNING, "Couldn't find user: " + member.toString() + ": Wrong UUID?");
                        continue;
                    }

                    if (membersFormatted.equals("-")) {
                        membersFormatted = name;
                        continue;
                    }
                    membersFormatted += ChatColor.GRAY + ", " + name;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        user.sendMessage(ChatColor.GRAY + "Members: " + ChatColor.GOLD + membersFormatted);
        user.sendMessage(divider);
        user.sendMessage("");
    }
}
