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

package de.static_interface.reallifeplugin.corporation;

import de.static_interface.sinklibrary.User;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CorporationUtil
{
    private static List<Corporation> corporations = new ArrayList<>();
    private static CorporationConfig config;

    public static Corporation getUserCorporation(UUID uuid)
    {
        for(Corporation corporation : corporations )
        {
            if (corporation.getMembers().contains(uuid))
            {
                return corporation;
            }
        }
        return null;
    }

    public static void registerCorporationsFromConfig()
    {
        YamlConfiguration yconfig = config.getYamlConfiguration();
        for(String corpName : yconfig.getConfigurationSection("Configrations").getKeys(false))
        {
            Corporation corp = new Corporation(config, corpName);
            register(corp);
        }
    }

    public static CorporationConfig getCorporationConfig()
    {
        if (config == null)
        {
            config = new CorporationConfig();
        }
        return config;
    }

    public static Corporation getCorporation(String name)
    {
        for(Corporation corporation : corporations)
        {
            if(corporation.getName().equals(name)) return corporation;
        }

        return null;
    }

    public static boolean isCEO(User user, Corporation corporation)
    {
        return corporation.getCEO() == user.getUniqueId();
    }

    public static boolean createCorporation(String[] args)
    {
        if ( getCorporation(args[0]) != null)
        {
            return false;
        }

        String name = args[1];
        String leader = args[2];

        String pathPrefix = "Corporations." + name + ".";

        CorporationConfig config = new CorporationConfig();
        config.set(pathPrefix + CorporationValues.CEO, leader);
        config.set(pathPrefix + CorporationValues.BASE, "");
        config.set(pathPrefix + CorporationValues.MEMBERS, "");
        config.save();

        Corporation corporation = new Corporation(config, name);
        register(corporation);
        return true;
    }

    private static void register(Corporation corporation)
    {
        corporations.add(corporation);
    }

    private static void unregister(Corporation corporation)
    {
        corporations.remove(corporation);
    }

    public static boolean deleteCorporation(String name)
    {
        Corporation corporation = getCorporation(name);
        if (corporation == null) return false;

        unregister(corporation);
        return false;
    }
}
