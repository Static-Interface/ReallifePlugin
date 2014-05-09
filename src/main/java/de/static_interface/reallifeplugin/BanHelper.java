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

package de.static_interface.reallifeplugin;

import de.static_interface.sinklibrary.SinkLibrary;

import java.util.HashMap;
import java.util.UUID;

/**
 * Bans are stored in memory (HashMaps), <b>they will be erased on restarts/reloads</b>
 */

public class BanHelper
{
    static HashMap<UUID, Long> bannedPlayers = new HashMap<>();
    static HashMap<UUID, String> reasons = new HashMap<>();

    public static void banPlayer(UUID uuid, String reason, long timeout)
    {
        bannedPlayers.put(uuid, timeout);
        reasons.put(uuid, reason);
    }

    public static void unbanPlayer(UUID uuid)
    {
        try
        {
            bannedPlayers.remove(uuid);
            reasons.remove(uuid);
        }
        catch(Exception e)
        {
            SinkLibrary.getCustomLogger().debug(e.getMessage());
        }
    }

    public static boolean isBanned(UUID uuid)
    {
        try
        {
            long banTime = bannedPlayers.get(uuid);

            if (banTime > System.currentTimeMillis())
            {
                unbanPlayer(uuid);
                return true;
            }
            return false;
        }
        catch(Exception e)
        {
            SinkLibrary.getCustomLogger().debug(e.getMessage());
            return false;
        }
    }

    public static String getBanReason(UUID uuid)
    {
        return reasons.get(uuid);
    }
}
