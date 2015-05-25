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

import static de.static_interface.reallifeplugin.config.ReallifeLanguageConfiguration.m;

import de.static_interface.reallifeplugin.module.ModuleListener;
import de.static_interface.reallifeplugin.module.level.Level;
import de.static_interface.reallifeplugin.module.level.LevelModule;
import de.static_interface.reallifeplugin.module.level.LevelUtil;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.Debug;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class LevelCommandListener extends ModuleListener<LevelModule> {

    public LevelCommandListener(LevelModule module) {
        super(module);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String cmd = event.getMessage().split(" ")[0];

        Debug.log("cmd: " + cmd);

        if (cmd.startsWith("/")) {
            cmd = cmd.replaceFirst("/", "");
        }
        if (cmd.contains(":")) {
            cmd = cmd.split(":")[1];
        }

        IngameUser user = SinkLibrary.getInstance().getIngameUser(event.getPlayer());
        Level userLevel = LevelUtil.getLevel(user);

        Debug.log("userLevel: " + userLevel.getLevelId());

        Level commandLevel = Level.Cache.getCommandLevel(cmd);
        Debug.log("commandLevel: " + commandLevel.getLevelId());

        if (userLevel.getLevelId() < commandLevel.getLevelId()) {
            Debug.log("level check failed");
            user.sendMessage(m("Level.NotEnoughLevel", commandLevel.getLevelName(), userLevel.getLevelName()));
            event.setCancelled(true);
        }

        Debug.log("level check passed");
    }
}
