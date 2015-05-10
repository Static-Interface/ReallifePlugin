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
import de.static_interface.sinklibrary.user.IngameUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Level {

    private LevelCondition levelCondition;
    private List<String> commands;
    private List<String> permissions;
    private int levelId;
    private String name;

    public Level(int levelId, String name, @Nonnull LevelCondition levelCondition) {
        this.levelCondition = levelCondition;
        this.commands = new ArrayList<>();
        this.permissions = new ArrayList<>();
        this.levelId = levelId;
        this.name = name;
        Level.Cache.setLevel(levelId, this);
    }

    public static Level getLevel(IngameUser user) {
        Level baseLevel = Level.Cache.getLevel(0);
        Object t = user.getConfiguration().get("Level.Id");
        if (t == null) {
            return baseLevel;
        }

        try {
            return Cache.getLevel(Integer.parseInt(t.toString()));
        } catch (NumberFormatException e) {
            ReallifeMain.getInstance().getLogger()
                    .severe("Couldn't get level for user: " + user.getName() + ": couldn't parse level: " + t.toString() + "; defaulting to zero");
            e.printStackTrace();
            setLevel(user, baseLevel);
            return baseLevel;
        }
    }

    public static void setLevel(IngameUser user, Level level) {
        user.getConfiguration().set("Level.Id", level.getLevelId());
    }

    public String getLevelName() {
        return name;
    }

    public LevelCondition getLevelCondition() {
        return levelCondition;
    }

    public List<String> getCommands() {
        return commands;
    }

    public Level setCommands(@Nullable List<String> commands) {
        if (commands == null) {
            commands = new ArrayList<>();
        }

        this.commands = commands;
        Level.Cache.addCommands(this, commands);
        return this;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public Level setPermissions(@Nullable List<String> permissions) {
        if (permissions == null) {
            permissions = new ArrayList<>();
        }

        this.permissions = permissions;
        Level.Cache.addPermissions(this, permissions);
        return this;
    }

    public int getLevelId() {
        return levelId;
    }

    public static class Cache {

        private static HashMap<String, Integer> commands = new HashMap<>();
        private static HashMap<String, Integer> permissions = new HashMap<>();

        private static HashMap<Integer, Level> levels = new HashMap<>();

        public static void addCommand(Level level, String command) {
            if (command == null) {
                return;
            }
            commands.put(command, level.getLevelId());
        }

        public static void addCommands(Level level, List<String> commandsToPut) {
            if (commandsToPut == null || commandsToPut.size() == 0) {
                return;
            }
            for (String cmd : commandsToPut) {
                addCommand(level, cmd);
            }
        }

        public static void addPermission(Level level, String permission) {
            if (permission == null) {
                return;
            }
            permissions.put(permission, level.getLevelId());
        }

        public static void addPermissions(Level level, List<String> permissionsToPut) {
            if (permissionsToPut == null || permissionsToPut.size() == 0) {
                return;
            }
            for (String perm : permissionsToPut) {
                addPermission(level, perm);
            }
        }

        public static void clearCache() {
            commands.clear();
            permissions.clear();
            levels.clear();
        }

        public static Level getCommandLevel(String command) {
            Integer level = commands.get(command);
            if (level == null) {
                return getLevel(0);
            }
            return getLevel(level);
        }

        public static Level getPermissionLevel(String permission) {
            Integer level = permissions.get(permission);
            if (level == null) {
                return getLevel(0);
            }
            return getLevel(level);
        }

        public static void setLevel(int levelId, Level level) {
            levels.put(levelId, level);
        }

        @Nullable
        public static Level getLevel(int levelId) {
            return levels.get(levelId);
        }
    }
}
