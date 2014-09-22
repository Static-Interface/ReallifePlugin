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

package de.static_interface.reallifeplugin;

import de.static_interface.reallifeplugin.entries.PayDayEntry;
import de.static_interface.reallifeplugin.entries.TaxesEntry;
import de.static_interface.reallifeplugin.events.PayDayEvent;
import de.static_interface.reallifeplugin.model.Entry;
import de.static_interface.reallifeplugin.model.EntryResult;
import de.static_interface.reallifeplugin.model.Group;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.util.BukkitUtil;
import de.static_interface.sinklibrary.util.MathUtil;
import de.static_interface.sinklibrary.util.StringUtil;
import de.static_interface.sinklibrary.util.VaultHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PayDayRunnable implements Runnable {

    public void givePayDay(Player player, Group group) {
        PayDayEvent event = new PayDayEvent(player, group);
        Bukkit.getServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        List<Entry> entries = new ArrayList<>();
        entries.add(new PayDayEntry(player, group));
        entries.add(new TaxesEntry(player, group));
        entries.addAll(event.getEntries());
        List<Entry> queue = PayDayQueue.getPlayerQueue(player.getUniqueId());
        if (queue != null) {
            entries.addAll(queue);
        }

        List<String> out = new ArrayList<>();

        double result = 0;
        out.add(ChatColor.BLUE + "--------------------" + ChatColor.BLUE + " Zahltag " + ChatColor.BLUE + "--------------------");

        Collections.sort(entries);

        for (Entry entry : entries) {
            EntryResult entryResult = handleEntry(entry);
            out.add(entryResult.out);
            result += entryResult.amount;
        }

        if (result == 0) {
            return;
        }

        double money = VaultHelper.getBalance(player);

        String resultPrefix = (result < 0 ? ChatColor.DARK_RED : ChatColor.DARK_GREEN) + "";
        String moneyPrefix = (money < 0 ? ChatColor.DARK_RED : ChatColor.DARK_GREEN) + "";

        String curreny = VaultHelper.getCurrenyName();

        out.add(ChatColor.AQUA + StringUtil.format("|- Summe: " + resultPrefix + "{0} " + curreny, MathUtil.round(result)));
        out.add(ChatColor.AQUA + StringUtil.format("|- Geld: " + moneyPrefix + "{0} " + curreny, money));

        String seperator = ChatColor.BLUE + "------------------------------------------------";
        out.add(seperator);
        player.sendMessage(out.toArray(new String[out.size()]));
    }

    private EntryResult handleEntry(Entry entry) {
        EntryResult result = new EntryResult();
        double amount = entry.getAmount();

        boolean negative = amount < 0;
        String text;

        String curreny = VaultHelper.getCurrenyName();

        String entryPrefix = "|- ";
        if (negative) {
            text = StringUtil.format(ChatColor.RED + entryPrefix + "-{0} " + curreny + " wurden abgezogen. (Grund: {1})", -amount, entry.getReason());
        } else {
            text =
                    StringUtil.format(ChatColor.GREEN + entryPrefix + "+{0} " + curreny + " wurden hinzugefuegt. (Grund: {1})", amount,
                                      entry.getReason());
        }

        result.out = text;
        result.amount = amount;

        String source = entry.getSourceAccount();
        VaultHelper.addBalance(source, amount);
        if (entry.sendToTarget()) {
            String target = entry.getTargetAccount();
            VaultHelper.addBalance(target, -amount);
        }
        return result;
    }

    @Override
    public void run() {
        BukkitUtil.broadcastMessage(ChatColor.DARK_GREEN + "Es ist Zahltag! Dividenden und Gehalt werden nun ausgezahlt.", false);
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean isInGroup = false;
            for (Group group : ReallifeMain.getSettings().readGroups()) {
                if (ChatColor.stripColor(SinkLibrary.getInstance().getUser(player).getPrimaryGroup()).equals(group.name)) {
                    givePayDay(player, group);
                    isInGroup = true;
                    break;
                }
            }
            if (!isInGroup) {
                givePayDay(player, getDefaultGroup(player));
            }
        }
    }

    public Group getDefaultGroup(Player player) {
        Group group = new Group();
        group.payday = ReallifeMain.getSettings().getDefaultPayday();
        group.taxesmodifier = ReallifeMain.getSettings().getDefaultTaxesModifier();
        group.shownName = SinkLibrary.getInstance().getUser(player).getPrimaryGroup();
        group.name = ChatColor.stripColor(SinkLibrary.getInstance().getUser(player).getPrimaryGroup());
        group.excluded = ReallifeMain.getSettings().isDefaultExcluded();
        return group;
    }
}
