/*
 * Copyright (c) 2014 http://adventuria.eu, http://static-interface.de and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.static_interface.reallifeplugin.commands;

import static de.static_interface.reallifeplugin.ReallifeLanguageConfiguration.m;

import de.static_interface.reallifeplugin.corporation.Corporation;
import de.static_interface.reallifeplugin.corporation.CorporationUtil;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.command.SinkCommand;
import de.static_interface.sinklibrary.api.user.SinkUser;
import de.static_interface.sinklibrary.configuration.LanguageConfiguration;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.user.IrcUser;
import de.static_interface.sinklibrary.util.Debug;
import de.static_interface.sinklibrary.util.StringUtil;
import de.static_interface.sinklibrary.util.VaultBridge;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class CorporationCommand extends SinkCommand {

    public CorporationCommand(Plugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onExecute(CommandSender sender, String label, String[] args) {
        SinkUser user = SinkLibrary.getInstance().getUser(sender);
        Corporation userCorp = null;
        if (user instanceof IngameUser) {
            userCorp = CorporationUtil.getUserCorporation(((IngameUser) user).getUniqueId());
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
                    //Todo: check for ceo / co-ceo
                    if (userCorp == null) {
                        user.sendMessage(m("Corporation.NotInCorporation"));
                        break;
                    }

                    userCorp.removeMember(((IngameUser) user).getUniqueId());

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
                Corporation corporation = CorporationUtil.getCorporation(args[0]);
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

    private void sendCorporationsList(SinkUser user, boolean listByMoney) {
        user.sendMessage(ChatColor.GOLD + "Corporations: ");
        String msg = "";
        for (Corporation corporation : CorporationUtil.getCorporations()) {
            int data;
            if (listByMoney) {
                data = (int) corporation.getBalance();
            } else {
                data = corporation.getAllMembers().size();
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

    private void handleAdminCommand(SinkUser user, String[] args) {
        switch (args[0].toLowerCase()) {
            case "?":
            case "help":
                sendAdminHelp(user);
                break;

            case "new": {
                if (args.length < 4 || (user instanceof IngameUser && args.length < 5)) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }
                //Todo: remove ceo from any corporation
                //Todo cancel if ceo is already a ceo of another corporation

                //Todo check for "." (problems with yaml files) and "&" (problems with color codes)

                String name = args[1];
                if (name.contains(".") || name.contains("&")) {
                    sender.sendMessage(m("Corporation.InvalidName"));
                    return;
                }

                UUID ceo = SinkLibrary.getInstance().getIngameUser(args[2]).getUniqueId();
                String base = args[3];
                World world;
                if (user instanceof IngameUser) {
                    world = ((IngameUser) user).getPlayer().getWorld();
                } else {
                    world = Bukkit.getWorld(args[4]);
                }

                boolean successful = CorporationUtil.createCorporation(user, name, ceo, base, world);
                Corporation corp = CorporationUtil.getCorporation(name);
                String msg = successful ? m("Corporation.Created") : m("Corporation.CreationFailed");
                msg = StringUtil.format(msg, corp.getFormattedName());
                user.sendMessage(msg);
                break;
            }

            case "delete": {
                if (args.length < 2) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }

                Corporation corp = CorporationUtil.getCorporation(args[1]);
                boolean successful = CorporationUtil.deleteCorporation(user, corp);

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

                Corporation corporation = CorporationUtil.getCorporation(args[1]);

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
                Corporation corporation = CorporationUtil.getCorporation(args[1]);
                if (corporation == null) {
                    user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), args[1]));
                    return;
                }
                IngameUser newCEO = SinkLibrary.getInstance().getIngameUser(args[2]);
                corporation.setCEO(newCEO.getUniqueId());
                user.sendMessage(m("Corporation.CEOSet"));
                break;
            }

            case "give": {
                if (args.length < 3) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }
                Corporation corporation = CorporationUtil.getCorporation(args[1]);
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
                corporation.addBalance(amount);
                user.sendMessage(m("General.Success"));
                break;
            }

            case "take": {
                if (args.length < 3) {
                    user.sendMessage(LanguageConfiguration.m("General.CommandMisused.Arguments.TooFew"));
                    return;
                }
                Corporation corporation = CorporationUtil.getCorporation(args[1]);
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
        if (!CorporationUtil.isCEO(user, corporation)) {
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
                //Todo: Check if targt is already in a corporation
                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[1]);

                Corporation targetCorporation = CorporationUtil.getUserCorporation(target.getUniqueId());
                if (targetCorporation != null) {
                    user.sendMessage(m("Corporation.AlreadyMemberOther", target.getName()));
                    return;
                }

                if (corporation.getAllMembers().contains(target.getUniqueId())) {
                    user.sendMessage(StringUtil.format(m("Corporation.AlreadyMember"), target.getName()));
                    break;
                }

                corporation.addMember(target.getUniqueId());
                if (target.isOnline()) {
                    target.sendMessage(StringUtil.format(m("Corporation.Added"), corporation.getName()));
                }
                user.sendMessage(StringUtil.format(m("Corporation.CEOAdded"), CorporationUtil.getFormattedName(target.getUniqueId())));
                break;
            }

            case "kick": {
                if (args.length < 2) {
                    user.sendMessage(ChatColor.RED + "/corp ceo help");
                    break;
                }
                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[1]);

                if (!corporation.getAllMembers().contains(target.getUniqueId())) {
                    user.sendMessage(StringUtil.format(m("Corporation.NotMember"), target.getName()));
                    break;
                }

                corporation.removeMember(target.getUniqueId());
                if (target.isOnline()) {
                    target.sendMessage(StringUtil.format(m("Corporation.Kicked"), corporation.getName()));
                }
                user.sendMessage(StringUtil.format(m("Corporation.CEOKicked"), CorporationUtil.getFormattedName(target.getUniqueId())));
                break;
            }
            case "acc":
            case "addcoceo": {
                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[1]);
                if (corporation.isCoCeo(target.getUniqueId())) {
                    user.sendMessage(m("Corporation.NotCEO"));
                    break;
                }

                if (corporation.getCoCEOs().contains(target.getUniqueId())) {
                    //Already Co-CEO
                    user.sendMessage(m("Corporation.AlreadyCoCEO", target.getDisplayName()));
                    break;
                }

                corporation.addCoCeo(target.getUniqueId());
                user.sendMessage(m("Corporation.CoCeoAdded", target.getDisplayName()));
                break;
            }

            case "rcc":
            case "removecoceo": {
                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[1]);
                if (!corporation.getCEO().equals(target.getUniqueId())) {
                    user.sendMessage(m("Corporation.NotCEO"));
                    return;
                }
                if (!corporation.getCoCEOs().contains(target.getUniqueId())) {
                    //Already removed from Co-CEO?
                    user.sendMessage(m("Corporation.NotCoCEO", target.getDisplayName()));
                    break;
                }

                corporation.removeCoCeo(target.getUniqueId());
                user.sendMessage(m("Corporation.CoCeoRemoved", target.getDisplayName()));
                break;
            }

            case "setrank": {
                IngameUser target = SinkLibrary.getInstance().getIngameUser(args[1]);
                String rank = ChatColor.translateAlternateColorCodes('&', StringUtil.formatArrayToString(args, " ", 2));
                corporation.setRank(target.getUniqueId(), rank);
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

    private void sendCorporationInfo(SinkUser user, Corporation corporation) {
        if (corporation == null) {
            user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), ""));
            return;
        }

        user.sendMessage("");
        String text = ChatColor.GOLD + " Corporation: " + corporation.getFormattedName();
        String divider = "";
        for (int i = 0; i < 32; i++) {
            divider += "-";
        }
        user.sendMessage(ChatColor.RED + text);
        user.sendMessage(ChatColor.GOLD + divider);
        user.sendMessage(ChatColor.GRAY + "CEO: " + CorporationUtil.getFormattedName(corporation.getCEO()));
        String coCeos = "";
        if (corporation.getCoCEOs().size() > 0) {
            for (UUID coCeo : corporation.getCoCEOs()) {
                String name = CorporationUtil.getFormattedName(coCeo);
                if (StringUtil.isStringEmptyOrNull(name)) {
                    Debug.log(Level.WARNING, "Empty or null name at CorporationCommand: " + name);
                    continue;
                }
                if (coCeos.equals("")) {
                    coCeos = name;
                    continue;
                }
                coCeos += ChatColor.GRAY + ", " + name;
            }
            user.sendMessage(ChatColor.GRAY + "Co-CEO's: " + coCeos);
        }

        if (corporation.getBase() != null) {
            user.sendMessage(ChatColor.GRAY + "Base: " + ChatColor.GOLD + corporation.getBase().getId());
        }

        user.sendMessage(ChatColor.GRAY + "Money: " + ChatColor.GOLD + corporation.getBalance() + " " + VaultBridge.getCurrenyName());
        Set<UUID> members = corporation.getMembers();
        if (members.size() > 0) {
            String membersList = "";
            for (UUID member : members) {
                String name = CorporationUtil.getFormattedName(member);
                if (StringUtil.isStringEmptyOrNull(name)) {
                    Debug.log(Level.WARNING, "Empty or null name at CorporationCommand: " + name);
                    continue;
                }
                if (Bukkit.getOfflinePlayer(member).getName() == null) {
                    Debug.log(Level.WARNING, "Couldn't find user: " + member.toString() + ": Wrong UUID?");
                    continue;
                }

                if (StringUtil.isStringEmptyOrNull(name)) {
                    membersList = name;
                    continue;
                }
                membersList += ChatColor.GRAY + ", " + name;
            }
            user.sendMessage(ChatColor.GRAY + "Members: " + ChatColor.GOLD + membersList);
        }
        user.sendMessage(ChatColor.GOLD + divider);
        user.sendMessage("");
    }
}
