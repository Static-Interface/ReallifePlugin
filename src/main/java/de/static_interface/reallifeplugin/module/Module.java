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

package de.static_interface.reallifeplugin.module;

import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.database.table.Table;
import de.static_interface.sinklibrary.api.configuration.Configuration;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public abstract class Module {

    private final static List<Module> modules = new ArrayList<>();
    private final String name;
    private final Plugin plugin;
    private final String[] requiredTables;
    private final List<ModuleListener> listeners = new ArrayList<>();
    private final Configuration config;
    private final Database db;
    private boolean enabled;
    private String modulePrefix;

    public Module(Plugin plugin, Configuration config,
                  @Nullable Database db, String name, boolean useConfigPrefix, @Nullable String... requiredTables) {
        name = name.trim();
        if (name.contains(".") || name.contains(" ")) {
            throw new IllegalArgumentException("Illegal name characters");
        }
        modulePrefix = useConfigPrefix ? "Module." + name + "." : "";
        this.db = db;
        this.name = name;
        this.plugin = plugin;
        this.requiredTables = requiredTables;
        this.config = config;
        modules.add(this);
    }

    @Nullable
    public static Module getModule(String name) {
        for (Module module : modules) {
            if (module.getName().equals(name)) {
                return module;
            }
        }
        return null;
    }

    public static boolean isEnabled(String name) {
        Module m = getModule(name);
        return m != null && m.isEnabled();
    }

    public final Object getValue(String path) {
        return getConfig().get(modulePrefix + path);
    }

    public final void setValue(String path, Object value) {
        getConfig().set(modulePrefix + path, value);
    }

    public final void addDefaultValue(String path, Object value) {
        getConfig().addDefault(modulePrefix + path, value);
    }

    public final void addDefaultValue(String path, Object value, String comment) {
        getConfig().addDefault(modulePrefix + path, value, comment);
    }

    public final void enable() {
        addDefaultValue("Enabled", false);

        if (getValue("Enabled") != Boolean.TRUE) {
            setValue("Enabled", false);
            return;
        }

        if (requiredTables != null) {
            if (getDatabase() == null) {
                throw new IllegalStateException("Database connection failed");
            }

            try {
                for (String table : requiredTables) {
                    Table tbl = db.getTable(table);
                    if (!tbl.exists()) {
                        tbl.create();
                    }
                }
            } catch (Exception e) {
                getPlugin().getLogger().warning("Couldn't enable module: " + getName() + ": ");
                e.printStackTrace();
                return;
            }
        }
        for (ModuleListener listener : listeners) {
            listener.register();
        }
        enabled = true;
        onEnable();
    }

    public final void disable() {
        if (listeners != null) {
            for (ModuleListener listener : listeners) {
                listener.unregister();
            }
        }
        enabled = false;
    }

    protected void onEnable() {

    }

    protected void onDisable() {

    }

    protected final void addListener(ModuleListener listener) {
        if (!listeners.contains(listener) && enabled) {
            listeners.add(listener);
            if (enabled) {
                listener.register();
            }
        }
    }

    public final Plugin getPlugin() {
        return plugin;
    }

    public final String getName() {
        return name;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final Configuration getConfig() {
        return config;
    }

    public final Database getDatabase() {
        return db;
    }
}
