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

package de.static_interface.reallifeplugin.listener;

import de.static_interface.reallifeplugin.*;
import de.static_interface.reallifeplugin.events.*;
import de.static_interface.sinklibrary.util.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;

import java.util.*;
import java.util.concurrent.*;

public class OnlineTimeListener implements Listener {

    public HashMap<UUID, Long> onlineTimes = new HashMap<>();

    public OnlineTimeListener() {
        for (Player player : BukkitUtil.getOnlinePlayers()) // Case of reload or something, where PlayerJoin is not fired
        {
            onlineTimes.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        onlineTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        try {
            onlineTimes.remove(event.getPlayer().getUniqueId());
        } catch (Exception e) {
            Debug.log(e);
            //shouldnt happen?
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPayDay(PayDayEvent event) {
        long minTime = TimeUnit.MINUTES.toMillis(ReallifeMain.getInstance().getSettings().getMinOnlineTime());

        if (minTime <= 0 || !event.isCheckTimeEnabled()) {
            return; //Don't check...
        }

        long onlineTime = System.currentTimeMillis() - onlineTimes.get(event.getPlayer().getUniqueId());

        if (onlineTime > minTime) {
            return;
        }

        long timeLeft = TimeUnit.MILLISECONDS.toMinutes(onlineTime - minTime);

        event.getPlayer().sendMessage(ChatColor.DARK_RED + "Du hast kein Geld bekommen, da du nicht mindestens "
                                      + TimeUnit.MILLISECONDS.toMinutes(minTime) + " Minuten online warst. Du musst noch mindestens " + (
                Math.abs(timeLeft) + 1) + " Minuten online sein!");

        event.setCancelled(true);
    }
}
