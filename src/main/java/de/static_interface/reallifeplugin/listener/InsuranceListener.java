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

package de.static_interface.reallifeplugin.listener;

import de.static_interface.reallifeplugin.commands.InsuranceCommand;
import de.static_interface.reallifeplugin.entries.InsuranceEntry;
import de.static_interface.reallifeplugin.events.PayDayEvent;
import de.static_interface.sinklibrary.SinkLibrary;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class InsuranceListener implements Listener
{
    HashMap<String, ItemStack[]> inventories = new HashMap<>();
    List<String> re = new ArrayList<>();

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        InsuranceCommand.createVars(SinkLibrary.getUser(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event)
    {
        if ( !InsuranceCommand.isActive(SinkLibrary.getUser(event.getEntity())) ) return;

        inventories.put(event.getEntity().getName(), event.getEntity().getInventory().getContents());

        Random random = new Random();
        int r = random.nextInt(3);

        if ( r == 1 )
        {
            re.add(event.getEntity().getName());
            event.getDrops().clear();
        }
        else
        {
            re.remove(event.getEntity().getName());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerResawpn(PlayerRespawnEvent event)
    {
        if ( !inventories.keySet().contains(event.getPlayer().getName()) ) return;
        if ( !re.contains(event.getPlayer().getName()) )
        {
            event.getPlayer().sendMessage(ChatColor.DARK_RED + "[Versicherung]" + ChatColor.GOLD + " Dein Inventar konnte nicht gerettet werden!");
            return;
        }
        event.getPlayer().sendMessage(ChatColor.DARK_RED + "[Versicherung]" + ChatColor.GOLD + " Dein Inventar wurde gerettet!");
        for ( ItemStack stack : inventories.get(event.getPlayer().getName()) )
        {
            event.getPlayer().getInventory().addItem(stack);
        }
        re.remove(event.getPlayer().getName());
    }

    @EventHandler
    public void onPayDay(PayDayEvent event)
    {
        if ( !InsuranceCommand.isActive(event.getUser()) ) return;
        InsuranceEntry entry = new InsuranceEntry(event.getUser(), event.getGroup());
        event.addEntry(entry);
    }
}
