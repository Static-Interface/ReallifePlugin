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

package de.static_interface.reallifeplugin.module.payday;

import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.payday.entry.PaydayEntry;
import de.static_interface.reallifeplugin.module.payday.entry.TaxesEntry;
import de.static_interface.reallifeplugin.module.payday.event.PaydayEvent;
import de.static_interface.reallifeplugin.module.payday.model.Entry;
import de.static_interface.reallifeplugin.module.payday.model.EntryResult;
import de.static_interface.reallifeplugin.module.payday.model.Group;
import de.static_interface.reallifeplugin.module.payday.model.PaydayPlayer;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.database.Database;
import de.static_interface.sinklibrary.util.BukkitUtil;
import de.static_interface.sinklibrary.util.MathUtil;
import de.static_interface.sinklibrary.util.StringUtil;
import de.static_interface.sinklibrary.util.VaultBridge;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PaydayTask implements Runnable {

    private Database db;
    private PaydayModule module;

    public PaydayTask(PaydayModule module, Database db) {
        this.db = db;
        this.module = module;
    }

    public void givePayDay(List<PaydayPlayer> players) {
        PaydayEvent event = new PaydayEvent(module, players);
        Bukkit.getServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        for (PaydayPlayer player : players) {
            if (player.isCancelled()) {
                continue;
            }

            List<Entry> entries = new ArrayList<>();
            entries.add(new PaydayEntry(player.getPlayer(), player.getGroup()));
            entries.add(new TaxesEntry(module, player.getPlayer(), player.getGroup()));

            entries.addAll(player.getEntries());

            List<Entry> queue = PaydayQueue.getPlayerQueue(player.getPlayer().getUniqueId());
            if (queue != null) {
                entries.addAll(queue);
            }

            List<String> out = new ArrayList<>();

            double result = 0;
            out.add(ChatColor.BLUE + "--------------------" + ChatColor.BLUE + " Zahltag " + ChatColor.BLUE + "--------------------");

            Collections.sort(entries);

            for (Entry entry : entries) {
                try {
                    EntryResult entryResult = handleEntry(entry);
                    out.add(entryResult.out);
                    result += entryResult.amount;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (result == 0) {
                return;
            }

            double money = VaultBridge.getBalance(player.getPlayer());

            String resultPrefix = (result < 0 ? ChatColor.DARK_RED : ChatColor.DARK_GREEN) + "";
            String moneyPrefix = (money < 0 ? ChatColor.DARK_RED : ChatColor.DARK_GREEN) + "";

            String curreny = VaultBridge.getCurrenyName();

            out.add(ChatColor.AQUA + StringUtil.format("|- Gesamt: " + resultPrefix + "{0} " + curreny, MathUtil.round(result)));
            out.add(ChatColor.AQUA + StringUtil.format("|- Geld: " + moneyPrefix + "{0} " + curreny, money));

            String seperator = ChatColor.BLUE + "------------------------------------------------";
            out.add(seperator);
            player.getPlayer().sendMessage(out.toArray(new String[out.size()]));
        }
    }

    private EntryResult handleEntry(Entry entry) {
        EntryResult result = new EntryResult();
        double amount = entry.getAmount();

        boolean negative = amount < 0;
        String text;

        String curreny = VaultBridge.getCurrenyName();

        String entryPrefix = "|- ";
        if (negative) {
            text = StringUtil.format(ChatColor.RED + entryPrefix + "-{0} " + curreny + ": {1}", -amount, entry.getReason());
        } else {
            text =
                    StringUtil.format(ChatColor.GREEN + entryPrefix + "+{0} " + curreny + ": {1}", amount,
                                      entry.getReason());
        }

        result.out = text;
        result.amount = amount;

        String source = entry.getSourceAccount();
        if (!StringUtil.isEmptyOrNull(source)) {
            VaultBridge.addBalance(source, amount);
        }
        if (entry.sendToTarget() && !StringUtil.isEmptyOrNull(entry.getTargetAccount())) {
            String target = entry.getTargetAccount();
            VaultBridge.addBalance(target, -amount);
        }
        return result;
    }

    @Override
    public void run() {
        run(true);
    }

    public void run(boolean checktime) {
        if (!Module.isEnabled(PaydayModule.class)) {
            return;
        }

        List<PaydayPlayer> players = new ArrayList<>();
        //Todo: create MessageStream
        BukkitUtil.broadcastMessage(ChatColor.DARK_GREEN + "Es ist Zahltag! Dividenden und Gehalt werden nun ausgezahlt.");
        for (Player player : Bukkit.getOnlinePlayers()) {
            PaydayPlayer p = null;
            boolean isInGroup = false;
            for (Group group : ReallifeMain.getInstance().getSettings().readGroups()) {
                if (ChatColor.stripColor(SinkLibrary.getInstance().getUser(player).getPrimaryGroup()).equals(group.name)) {
                    p = new PaydayPlayer(player, group, checktime);
                    isInGroup = true;
                    break;
                }
            }
            if (!isInGroup) {
                p = new PaydayPlayer(player, getDefaultGroup(player), checktime);
            }
            if (p == null) {
                continue;
            }
            players.add(p);
        }
        givePayDay(players);
    }

    public Group getDefaultGroup(Player player) {
        Group group = new Group();
        group.payday = ReallifeMain.getInstance().getSettings().getDefaultPayday();
        group.taxesmodifier = ReallifeMain.getInstance().getSettings().getDefaultTaxesModifier();
        group.shownName = SinkLibrary.getInstance().getUser(player).getPrimaryGroup();
        group.name = ChatColor.stripColor(SinkLibrary.getInstance().getUser(player).getPrimaryGroup());
        group.excluded = ReallifeMain.getInstance().getSettings().isDefaultExcluded();
        return group;
    }
}
