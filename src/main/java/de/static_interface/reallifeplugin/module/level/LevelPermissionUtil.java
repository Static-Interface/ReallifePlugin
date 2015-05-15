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

import static de.static_interface.reallifeplugin.config.ReallifeLanguageConfiguration.m;

import de.static_interface.sinklibrary.user.IngameUser;

public class LevelPermissionUtil {

    public static boolean hasPermission(IngameUser user, String permission) {
        Level level = Level.getLevel(user);
        Level permLevel = Level.Cache.getPermissionLevel(permission);

        if (level.getLevelId() >= permLevel.getLevelId()) {
            return true;
        }

        user.sendMessage(m("Level.CommandNotEnoughLevel", level.getLevelName(), permLevel.getLevelName()));
        return false;
    }
}
