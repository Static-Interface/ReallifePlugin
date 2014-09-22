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

import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.events.PayDayEvent;
import de.static_interface.sinklibrary.SinkLibrary;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class OnlineTimeListener implements Listener
{
    public HashMap<UUID, Long> onlineTimes = new HashMap<>();

    public OnlineTimeListener()
    {
        for (Player player : Bukkit.getOnlinePlayers()) // Case of reload or something, where PlayerJoin is not fired
        {
            onlineTimes.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        onlineTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        try
        {
            onlineTimes.remove(event.getPlayer().getUniqueId());
        }
        catch(Exception e)
        {
            SinkLibrary.getInstance().getCustomLogger().debug(e.getMessage()); //Will this be thrown?
        }
    }

    @EventHandler
    public void onPayDay(PayDayEvent event)
    {
        long minTime = TimeUnit.MINUTES.toMillis(ReallifeMain.getSettings().getMinOnlineTime());

        if (minTime <= 0) return; //Don't check...

        long onlineTime = System.currentTimeMillis() - onlineTimes.get(event.getPlayer().getUniqueId());

        event.getPlayer().getPlayerTimeOffset();

        if ( onlineTime > minTime )
        {
            return;
        }

        long timeLeft = TimeUnit.MILLISECONDS.toMinutes(onlineTime - minTime);

        event.getPlayer().sendMessage(ChatColor.DARK_RED + "Du hast kein Geld bekommen, da du nicht mindestens "
                + TimeUnit.MILLISECONDS.toMinutes(minTime) + " Minuten online warst. Du musst noch mindestens " +  (Math.abs(timeLeft) + 1)+ " Minuten online sein!");

        event.setCancelled(true);
    }
}
