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

package de.static_interface.reallifeplugin.corporation;

import static de.static_interface.reallifeplugin.ReallifeLanguageConfiguration.*;

import com.google.gson.*;
import de.static_interface.sinklibrary.*;
import de.static_interface.sinklibrary.api.user.*;
import de.static_interface.sinklibrary.user.*;
import de.static_interface.sinklibrary.util.*;
import org.bukkit.*;
import org.bukkit.configuration.*;
import org.bukkit.configuration.file.*;
import org.bukkit.entity.*;

import java.util.*;

public class CorporationUtil {

    private static List<Corporation> corporations;
    private static CorporationConfig config;

    public static Corporation getUserCorporation(UUID uuid) {
        for (Corporation corporation : corporations) {
            if (corporation.getAllMembers().contains(uuid)) {
                return corporation;
            }
        }
        return null;
    }

    public static void registerCorporationsFromConfig() {
        corporations = new ArrayList<>();
        YamlConfiguration yconfig = config.getYamlConfiguration();
        ConfigurationSection section = yconfig.getConfigurationSection("Corporations");
        if (section == null) {
            return;
        }
        for (String corpName : section.getKeys(false)) {
            Debug.log("Registering corporation: " + corpName);
            Corporation corp = new Corporation(config, corpName);
            register(corp);
        }
    }

    public static String getFormattedName(UUID uuid) {
        IngameUser user = SinkLibrary.getInstance().getIngameUser(uuid);

        String name =
                ChatColor.stripColor(user.getDisplayName() == null ? user.getName() : user.getDisplayName());

        if (StringUtil.isStringEmptyOrNull(name) || name.equals("null")) {
            return null;
        }

        Corporation corp = getUserCorporation(uuid);
        if (corp == null) {
            return ChatColor.GOLD + name;
        }
        String rank = corp.getRank(uuid);
        if (ChatColor.stripColor(rank).isEmpty()) {
            return rank + name;
        }
        return rank + " " + name;
    }

    public static CorporationConfig getCorporationConfig() {
        if (config == null) {
            config = new CorporationConfig();
        }
        return config;
    }

    public static Corporation getCorporation(String name) {
        for (Corporation corporation : corporations) {
            if (corporation.getName().equalsIgnoreCase(name)) {
                return corporation;
            }
        }

        return null;
    }

    public static boolean isCEO(IngameUser user, Corporation corporation) {
        return corporation != null && (corporation.getCEO().equals(user.getUniqueId())
                                       || corporation.getCoCEOs().contains(user.getUniqueId()));
    }

    public static boolean createCorporation(SinkUser user, String name, UUID ceo, String base, World world) {
        if (name.equalsIgnoreCase("ceo") || name.equalsIgnoreCase("admin") || name.equalsIgnoreCase("help")
            || name.equals("deposit") || name.equals("list")) {
            if (user != null) {
                user.sendMessage(m("Corporation.InvalidName"));
            }
            return false;
        }

        if (getCorporation(name) != null) {
            if (user != null) {
                user.sendMessage(m("Corporation.Exists"));
            }
            return false;
        }

        String pathPrefix = "Corporations." + name + ".";

        CorporationConfig config = new CorporationConfig();
        config.set(pathPrefix + CorporationValues.VALUE_CEO, ceo.toString());
        config.set(pathPrefix + CorporationValues.VALUE_BASE, world.getName() + ":" + base);
        HashMap<String, String> members = new HashMap<>();
        members.put(ceo.toString(), CorporationValues.RANK_CEO);

        config.set(pathPrefix + CorporationValues.VALUE_MEMBERS, new Gson().toJson(members));
        config.save();

        Corporation corporation = new Corporation(config, name);
        register(corporation);
        return true;
    }

    private static void register(Corporation corporation) {
        corporations.add(corporation);
    }

    private static void unregister(Corporation corporation) {
        corporations.remove(corporation);
    }

    public static boolean deleteCorporation(SinkUser user, Corporation corporation) {
        if (corporation == null) {
            user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), ""));
            return false;
        }
        unregister(corporation);
        config.getYamlConfiguration().set("Corporations." + corporation.getName(), null);
        config.save();
        return true;
    }

    public static void sendMessage(Corporation corp, String message) {
        message = ChatColor.GRAY + "[" + corp.getFormattedName() + ChatColor.GRAY + "] " + message;
        for (UUID uuid : corp.getAllMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) {
                continue;
            }
            p.sendMessage(message);
        }

        SinkLibrary.getInstance().getConsoleUser().sendMessage(message);
    }

    public static List<Corporation> getCorporations() {
        return corporations;
    }
}
