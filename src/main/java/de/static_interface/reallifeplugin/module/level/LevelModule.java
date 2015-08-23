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
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.level.condition.LevelConditions;
import de.static_interface.reallifeplugin.module.level.hook.VaultPermissionsHook;
import de.static_interface.reallifeplugin.module.level.listener.LevelCommandListener;
import de.static_interface.reallifeplugin.module.level.listener.PlayerJoinListener;
import de.static_interface.sinklibrary.api.configuration.Configuration;
import de.static_interface.sinklibrary.database.Database;
import de.static_interface.sinklibrary.util.StringUtil;
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
    private boolean inited = false;
    public LevelModule(ReallifeMain plugin, @Nullable Database db) {
        super(plugin, new Configuration(new File(plugin.getDataFolder(), "LevelModule.yml"), false) {
            @Override
            public void addDefaults() {
                //Example Level 1
                addDefault("level.1.name", "Level 1");
                List<String> unblockedCmds = new ArrayList<>();
                unblockedCmds.add("cmd1");
                unblockedCmds.add("cmd2");
                addDefault("level.1.unblockedCommands", unblockedCmds);

                List<String> permissions = new ArrayList<>();
                permissions.add("plugin.permission1");
                permissions.add("plugin.permission2");
                addDefault("level.1.unblockedPermissions", permissions);
                if (isPluginAvailable("AdventuriaPlugin")) {
                    addDefault("level.1.condition.likes", 0);
                    addDefault("level.1.condition.posts", 0);
                    addDefault("level.1.condition.activitypoints", 0);
                }
                addDefault("level.1.condition.permission", "none");
                addDefault("level.1.condition.time", 5);
                addDefault("level.1.condition.cost", 0);
                addDefault("level.1.description", "You can use the cmd1 and cmd2 commands with this level!");

                //Example Level 2
                addDefault("level.2.name", "Level 2");
                unblockedCmds = new ArrayList<>();
                unblockedCmds.add("cmd3");
                unblockedCmds.add("cmd4");
                addDefault("level.2.unblockedCommands", unblockedCmds);

                permissions = new ArrayList<>();
                addDefault("level.2.unblockedPermissions", permissions, "Example for no permissions");
                if (isPluginAvailable("AdventuriaPlugin")) {
                    addDefault("level.2.condition.likes", 0);
                    addDefault("level.2.condition.posts", 0);
                    addDefault("level.2.condition.activitypoints", 0);
                }
                addDefault("level.2.condition.permission", "someplugin.perms");
                addDefault("level.2.condition.time", 5);
                addDefault("level.2.condition.cost", 0);
                addDefault("level.2.description", "You can use the cmd3 and cmd4 commands with this level!");

                addDefault("baseLevelName", "Level 0");
                addDefault("baseLevelDescription", "This is the baseLevel!");
            }
        }, db, NAME, true);
        levelList = new ArrayList<>();
    }

    @Override
    public void onEnable() {
        if (inited) {
            getConfig().reload();
        } else {
            getConfig().init();
        }
        permissionsHook = new VaultPermissionsHook();

        if (isPluginAvailable("Vault")) {
            getPlugin().getLogger().info("[LevelModule-PermissionsHook] Hooking into Vault");
            Bukkit.getServicesManager()
                    .register(net.milkbowl.vault.permission.Permission.class, permissionsHook, getPlugin(), ServicePriority.Highest);
        } else {
            getPlugin().getLogger().info("[LevelModule-PermissionsHook] Vault not found, not hooking into vault");
        }

        String baseLevelName = getValue("baseLevelName").toString();
        String description = getValue("baseLevelDescription").toString();

        Level level = new Level(0, baseLevelName, new LevelConditions()); //base level (level 0)
        if (!StringUtil.isEmptyOrNull(description)) {
            level.setDescription(description);
        }
        levelList.add(level);

        int i = 1;
        for (String key : getConfig().getYamlConfiguration().getConfigurationSection("level").getKeys(false)) {
            try {
                int id = Integer.valueOf(key);
                if (id != i) {
                    throw new RuntimeException("Invalid level config: level." + key + ": expected \"" + i + "\" but value was \"" + key + "\"");
                }
            } catch (NumberFormatException e) {
                getPlugin().getLogger().info("[LevelModule] Invalid level identifier: " + key + " (not a valid number)");
                break;
            }
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

            LevelConditions levelConditions = new LevelConditions()
                    .setRequiredLikes(requiredLikes)
                    .setRequiredActivityPoints(requiredActivityPoints)
                    .setRequiredTime(requiredTime * 60 * 60 * 1000)
                    .setRequiredPosts(requiredPosts)
                    .setRequiredPermission(requiredPermission)
                    .setCost(cost);

            List<String> cmds = getConfig().getYamlConfiguration().getStringList("level." + key + ".unblockedCommands");
            List<String> permissions = getConfig().getYamlConfiguration().getStringList("level." + key + ".unblockedPermissions");
            String name = getValue("level." + key + ".name", "Level " + key).toString();
            level = new Level(i, name, levelConditions);
            level.setCommands(cmds);
            level.setDescription(getValue("level." + key + ".description").toString());
            level.setPermissions(permissions);
            levelList.add(level);
            i++;
        }

        registerModuleCommand("level", new LevelCommand(this));
        registerModuleListener(new LevelCommandListener(this));
        registerModuleListener(new PlayerJoinListener(this));
        for(Player p : Bukkit.getOnlinePlayers()) {
            LevelAttachmentsManager.updateAttachment(p, getPlugin());
            LevelPlayerTimer.startPlayerTime(p, System.currentTimeMillis());
        }
        inited = true;
    }

    @Override
    public void onDisable() {
        levelList.clear();
        Level.Cache.clearCache();
        if(permissionsHook != null)
            Bukkit.getServer().getServicesManager().unregister(permissionsHook);
        LevelAttachmentsManager.clearAttachments();
        for (Player p : Bukkit.getOnlinePlayers()) {
            LevelPlayerTimer.stopPlayerTime(p);
        }
    }
}
