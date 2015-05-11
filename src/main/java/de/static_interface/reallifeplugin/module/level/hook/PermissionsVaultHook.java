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

package de.static_interface.reallifeplugin.module.level.hook;

import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.level.Level;
import de.static_interface.reallifeplugin.module.level.LevelModule;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.user.IngameUser;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class PermissionsVaultHook extends Permission {

    @Override
    public String getName() {
        return "ReallifePlugin-Level-PermissionsHook";
    }

    @Override
    public boolean isEnabled() {
        return Module.getModule(LevelModule.class) != null && Module.getModule(LevelModule.class).isEnabled();
    }

    @Override
    public boolean hasSuperPermsCompat() {
        return false;
    }

    @Override
    public boolean playerHas(String worldName, OfflinePlayer player, String permission) {
        return playerHas(worldName, player.getName(), permission);
    }

    @Override
    public boolean has(Player player, String permission) {
        return playerHas(player.getWorld().getName(), player.getName(), permission);
    }

    @Override
    public boolean playerHas(String worldName, String playerName, String permission) {
        if (Module.getModule(LevelModule.class) == null || !Module.getModule(LevelModule.class).isEnabled()) {
            return true;
        }

        IngameUser user = SinkLibrary.getInstance().getIngameUser(playerName);
        Level level = Level.getLevel(user);
        Level permLevel = Level.Cache.getPermissionLevel(permission);

        boolean value;

        if (permission.equals("Debug")) {
            value = false;
        } else {
            value = level.getLevelId() >= permLevel.getLevelId();
        }

        Bukkit.getLogger().info("playerHas DEBUG: player: " + user.getName() + ", permission: " + permission + ", value: " + value);

        return value;
    }

    @Override
    public boolean playerAdd(String s, String s1, String s2) {
        return true; /* not supported */
    }

    @Override
    public boolean playerRemove(String s, String s1, String s2) {
        return true; /* not supported */
    }

    /*********************************************
     *                   GROUPS                   *
     *********************************************/

    @Override
    public boolean hasGroupSupport() {
        return false;
    }

    @Override
    public boolean groupHas(String s, String s1, String s2) {
        return false; /* not supported */
    }

    @Override
    public boolean groupAdd(String s, String s1, String s2) {
        return false; /* not supported */
    }

    @Override
    public boolean groupRemove(String s, String s1, String s2) {
        return false; /* not supported */
    }

    @Override
    public boolean playerInGroup(String s, String s1, String s2) {
        return false; /* not supported */
    }

    @Override
    public boolean playerAddGroup(String s, String s1, String s2) {
        return false; /* not supported */
    }

    @Override
    public boolean playerRemoveGroup(String s, String s1, String s2) {
        return false; /* not supported */
    }

    @Override
    public String[] getPlayerGroups(String s, String s1) {
        return null;  /* not supported */
    }

    @Override
    public String getPrimaryGroup(String s, String s1) {
        return null; /* not supported */
    }

    @Override
    public String[] getGroups() {
        return null; /* not supported */
    }
}
