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

package de.static_interface.reallifeplugin.module.level;

import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.user.IngameUser;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class LevelPlayerTimer {

    private static Map<Player, Long> playTime = new HashMap<>();

    public static void startPlayerTime(Player p, long time) {
        playTime.put(p, time);
    }

    public static void stopPlayerTime(Player p) {
        if (playTime.get(p) == null) {
            return;
        }
        IngameUser user = SinkLibrary.getInstance().getIngameUser(p);
        LevelUtil.setPlayTime(user, LevelUtil.getPlayTime(user));
        playTime.remove(p);
    }

    public static long getSessionTime(Player p) {
        Long val = playTime.get(p);
        if (val == null) {
            return 0;
        }
        return System.currentTimeMillis() - val;
    }
}
