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

import static de.static_interface.reallifeplugin.ReallifeLanguageConfiguration.*;
import static org.bukkit.event.entity.EntityDamageEvent.*;

import de.static_interface.reallifeplugin.*;
import de.static_interface.reallifeplugin.model.*;
import de.static_interface.sinklibrary.*;
import de.static_interface.sinklibrary.user.*;
import de.static_interface.sinklibrary.util.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;

import java.util.*;
import java.util.concurrent.*;

public class AntiEscapeListener implements Listener {

    public static final int COOLDOWN = 10 * 1000;
    HashMap<UUID, Damage> damageInstances = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getEntity() instanceof Player) || !isValidCause(event.getCause())) {
            return;
        }

        Player player = (Player) event.getEntity();

        //if(player.hasPermission("reallifeplugin.escapebypass"))
        //{
        //    return;
        //}

        Damage damage = new Damage();
        if (damageInstances.containsKey(player.getUniqueId())) {
            damage = damageInstances.get(player.getUniqueId());
        }

        damage.damager = event.getDamager();
        damage.millis = System.currentTimeMillis();
        damageInstances.put(player.getUniqueId(), damage);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getEntity() instanceof Player) || !isValidCause(event.getCause())) {
            return;
        }

        Player player = (Player) event.getEntity();

        if (player.hasPermission("reallifeplugin.escapebypass")) {
            return;
        }

        Damage damage = new Damage();
        if (damageInstances.containsKey(player.getUniqueId())) {
            damage = damageInstances.get(player.getUniqueId());
        }

        damage.millis = System.currentTimeMillis();
    }

    private boolean isValidCause(DamageCause cause) {
        return cause != DamageCause.SUICIDE;
    }

    private boolean isPlayerDamager(Entity entity) {
        return entity instanceof Player;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!damageInstances.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }
        try {
            Damage damage = damageInstances.get(event.getPlayer().getUniqueId());
            long cooldownTime = System.currentTimeMillis() - damage.millis;

            if (cooldownTime > COOLDOWN) {
                damageInstances.remove(event.getPlayer().getUniqueId());
                return;
            }

            long timeLeft = TimeUnit.MILLISECONDS.toSeconds(cooldownTime - COOLDOWN);

            event.getPlayer().sendMessage(ChatColor.DARK_RED + "Teleport wurde abgebrochen da du innerhalb der letzten "
                                          + TimeUnit.MILLISECONDS.toSeconds(COOLDOWN) + " Sekunden Schaden bekommen hast. Du musst noch "
                                          + (Math.abs(timeLeft) + 1) + " Sekunden warten!");

            event.setCancelled(true);
        } catch (NullPointerException e) {
            Debug.log(e);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!damageInstances.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }
        try {
            Damage damage = damageInstances.get(event.getPlayer().getUniqueId());
            long cooldownTime = System.currentTimeMillis() - damage.millis;

            if (cooldownTime > COOLDOWN) {
                damageInstances.remove(event.getPlayer().getUniqueId());
                return;
            }

            Entity damager = damage.damager;

            if (!isPlayerDamager(damager)) {
                damageInstances.remove(event.getPlayer().getUniqueId());
                return;
            }

            int banMinutes = ReallifeMain.getInstance().getSettings().getPvPEscapeBanTime();

            long unbanTimeStamp = System.currentTimeMillis() + (banMinutes * 60 * 1000);

            IngameUser user = SinkLibrary.getInstance().getIngameUser(event.getPlayer());
            user.ban(m("AntiPvPEscape.BanMessage", banMinutes), unbanTimeStamp);

        } catch (NullPointerException e) {
            Debug.log(e);
        }
    }
}
