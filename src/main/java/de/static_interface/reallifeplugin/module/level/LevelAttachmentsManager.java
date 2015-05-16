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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LevelAttachmentsManager {

    private static Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public static void updateAttachment(Player p, Plugin pl) {
        clearAttachment(p);

        PermissionAttachment attachment = p.addAttachment(pl);
        attachments.put(p.getUniqueId(), attachment);

        IngameUser user = SinkLibrary.getInstance().getIngameUser(p);
        Level level = LevelUtil.getLevel(user);
        if (level != null) {
            int i = level.getLevelId();
            while (i >= 0) {
                Level lvl = Level.Cache.getLevel(i);
                if (lvl == null) {
                    continue;
                }
                for (String s : lvl.getPermissions()) {
                    Permission perm = getPermission(s);
                    attachment.setPermission(perm, true);
                }
                i--;
            }
        }

        p.recalculatePermissions();
    }

    public static void clearAttachment(Player p) {
        UUID uuid = p.getUniqueId();
        PermissionAttachment attachment = attachments.get(uuid);
        if (attachment != null) {
            p.removeAttachment(attachment);
        }
    }

    public static void clearAttachments() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID uuid = p.getUniqueId();
            PermissionAttachment attachment = attachments.get(uuid);
            if (attachment != null) {
                p.removeAttachment(attachment);
            }
        }
        attachments.clear();
    }

    public static Permission getPermission(String perm) {
        Permission permObject = Bukkit.getPluginManager().getPermission(perm);
        if (permObject != null) {
            return permObject;
        }

        permObject = new Permission(perm, "Auto generated perm " + perm, PermissionDefault.FALSE) {
            @Override
            public void recalculatePermissibles() {
                // no-op
            }
        };
        Bukkit.getServer().getPluginManager().addPermission(permObject);
        return permObject;
    }
}
