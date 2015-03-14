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

package de.static_interface.reallifeplugin.module.insurance;

import de.static_interface.reallifeplugin.entry.InsuranceEntry;
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.ModuleListener;
import de.static_interface.reallifeplugin.module.payday.event.PayDayEvent;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.user.IngameUser;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class InsuranceListener extends ModuleListener {

    HashMap<String, ItemStack[]> inventories = new HashMap<>();
    List<String> activatedPlayers = new ArrayList<>();

    public InsuranceListener(Module module) {
        super(module);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        InsuranceCommand.createVars((IngameUser) SinkLibrary.getInstance().getUser(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!InsuranceCommand.isActive(event.getEntity())) {
            return;
        }

        inventories.put(event.getEntity().getName(), event.getEntity().getInventory().getContents());

        Random random = new Random();
        int r = random.nextInt(3);

        if (r == 1) {
            activatedPlayers.add(event.getEntity().getName());
            event.getDrops().clear();
        } else {
            activatedPlayers.remove(event.getEntity().getName());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerResawpn(PlayerRespawnEvent event) {
        if (!inventories.keySet().contains(event.getPlayer().getName())) {
            return;
        }
        if (!activatedPlayers.contains(event.getPlayer().getName())) {
            event.getPlayer().sendMessage(ChatColor.DARK_RED + "[Versicherung]" + ChatColor.GOLD + " Dein Inventar konnte nicht gerettet werden!");
            return;
        }
        event.getPlayer().sendMessage(ChatColor.DARK_RED + "[Versicherung]" + ChatColor.GOLD + " Dein Inventar wurde gerettet!");
        for (ItemStack stack : inventories.get(event.getPlayer().getName())) {
            event.getPlayer().getInventory().addItem(stack);
        }
        activatedPlayers.remove(event.getPlayer().getName());
    }

    @EventHandler
    public void onPayDay(PayDayEvent event) {
        if (!InsuranceCommand.isActive(event.getPlayer())) {
            return;
        }
        InsuranceEntry entry = new InsuranceEntry(event.getPlayer(), event.getGroup());
        event.addEntry(entry);
    }
}
