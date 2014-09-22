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

import static org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import de.static_interface.reallifeplugin.BanHelper;
import de.static_interface.sinklibrary.SinkLibrary;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AntiQuitListener implements Listener {

    public static final int COOLDOWN = 10 * 1000;
    HashMap<UUID, Long> cooldowns = new HashMap<>();
    HashMap<UUID, DamageCause> damageCauses = new HashMap<>();

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player) || !isValidCause(event.getCause())) {
            return;
        }

        Player player = (Player) event.getEntity();
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        damageCauses.put(player.getUniqueId(), event.getCause());
    }

    private boolean isValidCause(DamageCause cause) {
        return cause != DamageCause.SUICIDE;
    }

    private boolean isEntityCause(DamageCause cause) {
        return cause == DamageCause.ENTITY_ATTACK || cause == DamageCause.ENTITY_EXPLOSION;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        try {
            long cooldownTime = System.currentTimeMillis() - cooldowns.get(event.getPlayer().getUniqueId());

            if (cooldownTime > COOLDOWN) {
                cooldowns.remove(event.getPlayer().getUniqueId());
                return;
            }

            long timeLeft = TimeUnit.MILLISECONDS.toSeconds(cooldownTime - COOLDOWN);

            event.getPlayer().sendMessage(
                    ChatColor.DARK_RED + "Teleport wurde abgebrochen da du innerhalb der letzten " + TimeUnit.MILLISECONDS.toSeconds(COOLDOWN)
                    + " Minuten Schaden bekommen hast. Du musst noch " + timeLeft + " Sekunden warten!");

            event.setCancelled(true);
        } catch (NullPointerException e) {
            SinkLibrary.getInstance().getCustomLogger().debug(e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        try {
            long cooldownTime = System.currentTimeMillis() - cooldowns.get(event.getPlayer().getUniqueId());

            if (cooldownTime > COOLDOWN) {
                cooldowns.remove(event.getPlayer().getUniqueId());
                return;
            }

            DamageCause cause = damageCauses.get(event.getPlayer().getUniqueId());
            if (!isEntityCause(cause)) {
                cooldowns.remove(event.getPlayer().getUniqueId());
                return;
            }

            int banMinutes = 5;

            long unbanTimeStamp = System.currentTimeMillis() + (banMinutes * 60 * 1000);

            BanHelper.banPlayer(event.getPlayer().getUniqueId(), ChatColor.RED + "Du wurdest temporär für " + banMinutes
                                                                 + " Minuten gesperrt. Grund: Offline gegangen nach dem du Schaden bekommen hast",
                                unbanTimeStamp);
        } catch (NullPointerException e) {
            SinkLibrary.getInstance().getCustomLogger().debug(e.getMessage());
        }
    }
}
