/*
 * Copyright (c) 2013 - 2015 <http://static-interface.de> and contributors
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

package de.static_interface.reallifeplugin.module.level.listener;

import de.static_interface.reallifeplugin.module.ModuleListener;
import de.static_interface.reallifeplugin.module.level.LevelAttachmentsManager;
import de.static_interface.reallifeplugin.module.level.LevelModule;
import de.static_interface.reallifeplugin.module.level.LevelPlayerTimer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener extends ModuleListener<LevelModule> {
    public PlayerJoinListener(LevelModule module) {
        super(module);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        LevelAttachmentsManager.updateAttachment(event.getPlayer(), getModule().getPlugin());
        LevelPlayerTimer.startPlayerTime(event.getPlayer(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        LevelAttachmentsManager.clearAttachment(event.getPlayer());
        LevelPlayerTimer.stopPlayerTime(event.getPlayer());
    }
}
