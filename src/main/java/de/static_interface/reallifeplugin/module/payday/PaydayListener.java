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

import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.ModuleListener;
import de.static_interface.reallifeplugin.module.payday.event.PayDayEvent;
import de.static_interface.sinklibrary.util.BukkitUtil;
import de.static_interface.sinklibrary.util.Debug;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PaydayListener extends ModuleListener {

    public HashMap<UUID, Long> onlineTimes = new HashMap<>();

    public PaydayListener(Module module) {
        super(module);
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
        long minTime = TimeUnit.MINUTES.toMillis(((PaydayModule) getModule()).getMinOnlineTime());

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
