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

import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.level.hook.VaultPermissionsHook;
import de.static_interface.reallifeplugin.module.level.listener.LevelCommandListener;
import de.static_interface.reallifeplugin.module.level.listener.PlayerJoinListener;
import de.static_interface.sinklibrary.api.configuration.Configuration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class LevelModule extends Module<ReallifeMain> {

    public static final String NAME = "Level";
    private List<Level> levelList;
    private VaultPermissionsHook permissionsHook;

    public LevelModule(ReallifeMain plugin, @Nullable Database db) {
        super(plugin, new Configuration(new File(plugin.getDataFolder(), "levelmodule.yml")) {
            @Override
            public void onCreate() {
                //Example Level 1
                set("level.1.name", "Level 1");
                List<String> unblockedCmds = new ArrayList<>();
                unblockedCmds.add("cmd1");
                unblockedCmds.add("cmd2");
                set("level.1.unblockedCommands", unblockedCmds);

                List<String> permissions = new ArrayList<>();
                permissions.add("plugin.permission1");
                permissions.add("plugin.permission2");
                set("level.1.unblockedPermissions", permissions);
                if (isPluginAvailable("AdventuriaPlugin")) {
                    set("level.1.condition.likes", 0);
                    set("level.1.condition.posts", 0);
                    set("level.1.condition.activitypoints", 0);
                }
                set("level.1.condition.permission", "none");
                set("level.1.condition.time", 5);
                set("level.1.condition.cost", 0);

                //Example Level 2
                set("level.2.name", "Level 2");
                unblockedCmds = new ArrayList<>();
                unblockedCmds.add("cmd3");
                unblockedCmds.add("cmd4");
                set("level.2.unblockedCommands", unblockedCmds);

                permissions = new ArrayList<>();
                permissions.add("plugin.permission3");
                permissions.add("plugin.permission4");
                set("level.2.unblockedPermissions", permissions);
                if (isPluginAvailable("AdventuriaPlugin")) {
                    set("level.2.condition.likes", 0);
                    set("level.2.condition.posts", 0);
                    set("level.2.condition.activitypoints", 0);
                }
                set("level.2.condition.permission", "someplugin.perms");
                set("level.2.condition.time", 5);
                set("level.2.condition.cost", 0);
            }

            @Override
            public void addDefaults() {
                addDefault("baseLevelName", "Level 0");
            }
        }, db, NAME, true);
        levelList = new ArrayList<>();
    }

    @Override
    public void onEnable() {
        permissionsHook = new VaultPermissionsHook();

        if (isPluginAvailable("Vault")) {
            getPlugin().getLogger().info("[LevelModule-PermissionsHook] Hooking into Vault");
            Bukkit.getServicesManager()
                    .register(net.milkbowl.vault.permission.Permission.class, permissionsHook, getPlugin(), ServicePriority.Highest);
        } else {
            getPlugin().getLogger().info("[LevelModule-PermissionsHook] Vault not found, not hooking into vault");
        }

        String baseLevelName = getValue("baseLevelName").toString();
        Level level = new Level(0, baseLevelName, new LevelCondition()); //base level (level 0)
        levelList.add(level);

        int i = 1;
        for (String key : getConfig().getYamlConfiguration().getConfigurationSection("level").getKeys(false)) {
            int requiredLikes = 0;
            int requiredPosts = 0;
            int requiredActivityPoints = 0;
            if (isPluginAvailable("AdventuriaPlugin")) {
                requiredLikes = Integer.valueOf(getValue("level." + key + ".condition.likes", 0).toString());
                requiredPosts = Integer.valueOf(getValue("level." + key + ".condition.posts", 0).toString());
                requiredActivityPoints = Integer.valueOf(getValue("level." + key + ".condition.activitypoints", 0).toString());
            }

            int requiredTime = Integer.valueOf(getValue("level." + key + ".condition.time", 0).toString());
            int cost = Integer.valueOf(getValue("level." + key + ".condition.cost", 0).toString());
            String requiredPermission = getValue("level." + key + ".condition.permission", "none").toString();

            LevelCondition levelCondition = new LevelCondition()
                    .setRequiredLikes(requiredLikes)
                    .setRequiredActivityPoints(requiredActivityPoints)
                    .setRequiredTime(requiredTime * 60 * 60 * 1000)
                    .setRequiredPosts(requiredPosts)
                    .setRequiredPermission(requiredPermission)
                    .setCost(cost);

            List<String> cmds = getConfig().getYamlConfiguration().getStringList("level." + key + ".unblockedCommands");
            List<String> permissions = getConfig().getYamlConfiguration().getStringList("level." + key + ".unblockedPermissions");
            String name = getValue("level." + key + ".name", "Level " + key).toString();
            level = new Level(i, name, levelCondition);
            level.setCommands(cmds);
            level.setPermissions(permissions);
            levelList.add(level);
            i++;
        }

        registerModuleCommand("level", new LevelCommand(this));
        registerModuleListener(new LevelCommandListener(this));
        registerModuleListener(new PlayerJoinListener(this));
        for(Player p : Bukkit.getOnlinePlayers()) {
            PlayerJoinListener.updateAttachment(p, getPlugin());
        }
    }

    @Override
    public void onDisable() {
        levelList.clear();
        Level.Cache.clearCache();
        if(permissionsHook != null)
            Bukkit.getServer().getServicesManager().unregister(permissionsHook);
        PlayerJoinListener.clearAttachments();
    }
}
