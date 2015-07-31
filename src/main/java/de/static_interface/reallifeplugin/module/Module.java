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

import de.static_interface.reallifeplugin.database.AbstractTable;
import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.configuration.Configuration;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public abstract class Module<T extends Plugin> {

    private final static List<Module> modules = new ArrayList<>();
    private final String name;
    private final T plugin;
    private final List<ModuleListener> listeners = new ArrayList<>();
    private final Configuration config;
    private final Database db;
    private final Map<String, ModuleCommand> commands = new HashMap<>();

    private Collection<AbstractTable> requiredTables;
    private boolean enabled;
    private String modulePrefix;
    private boolean isStandAloneConfig;
    @SuppressWarnings("FieldCanBeLocal")
    private boolean registered = false;

    public Module(T plugin, Configuration config,
                  @Nullable Database db, String name, boolean isStandAloneConfig) {
        if (Module.getModule(name) != null && !registered) {
            throw new IllegalStateException("A module with the name \"" + name + "\" is already registered");
        }
        name = name.trim();
        if (name.contains(".") || name.contains(" ")) {
            throw new IllegalArgumentException("Illegal name characters");
        }
        this.isStandAloneConfig = isStandAloneConfig;
        modulePrefix = isStandAloneConfig ? "" : "Module." + name + ".";
        this.db = db;
        this.name = name;
        this.plugin = plugin;
        this.requiredTables = getTables();
        if (requiredTables == null) {
            requiredTables = new ArrayList<>();
        }
        this.config = config;
        modules.add(this);
        registered = true;
    }

    public static boolean isPluginAvailable(String name) {
        return Bukkit.getPluginManager().getPlugin(name) != null;
    }

    @Nullable
    public static <E extends AbstractTable> E getTable(Module module, Class<E> classOfE) {
        if (module.requiredTables.size() == 0) {
            throw new IllegalStateException("This module doesn't have any tables");
        }

        for (Object o : module.getRequiredTables()) {
            if (o.getClass().equals(classOfE)) {
                return (E) o;
            }
        }

        return null;
    }

    @Nullable
    public static Module getModule(String name) {
        name = name.trim().replace(" ", "_");

        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public static boolean isEnabled(String name) {
        Module m = getModule(name);
        return m != null && m.isEnabled();
    }

    public static <T extends Module> T getModule(Class<T> classOfT) {
        for (Module module : modules) {
            if (module.getClass().getName().equals(classOfT.getName())) {
                return (T) module;
            }
        }
        return null;
    }

    public static Collection<Module> getModules() {
        return Collections.unmodifiableCollection(modules);
    }

    public final Collection<ModuleListener> getModuleListenesr() {
        return Collections.unmodifiableCollection(listeners);
    }

    public final Map<String, ModuleCommand> getModuleCommands() {
        return Collections.unmodifiableMap(commands);
    }

    public final Object getValue(String path) {
        return getConfig().get(modulePrefix + path);
    }

    public final Object getValue(String path, Object defaultValue) {
        return getConfig().get(modulePrefix + path, defaultValue);
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
        getPlugin().getLogger().info("[" + getName() + "-Module]: Enabling...");
        if (isStandAloneConfig) {
            getConfig().init();
        }
        addDefaultValue("Enabled", false);

        if (getValue("Enabled") != Boolean.TRUE) {
            setValue("Enabled", false);
            return;
        }

        if (requiredTables != null && requiredTables.size() > 0) {
            if (getDatabase() == null) {
                throw new IllegalStateException("Database connection failed");
            }

            try {
                for (AbstractTable table : requiredTables) {
                    if (table != null && !table.exists()) {
                        table.create();
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
        getPlugin().getLogger().info("[" + getName() + "-Module]: Disabling...");
        for (ModuleListener listener : listeners) {
            listener.unregister();
        }

        //for(String cmd : commands.keySet()) {
        //    SinkLibrary.getInstance().unregisterCommand(cmd, getPlugin());
        //}

        onDisable();

        enabled = false;
    }

    protected Collection<AbstractTable> getRequiredTables() {
        return requiredTables;
    }

    protected void onEnable() {

    }

    protected void onDisable() {

    }

    @Nullable
    protected Collection<AbstractTable> getTables() {
        return new ArrayList<>();
    }

    protected final void registerModuleListener(ModuleListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            if (enabled) {
                listener.register();
            }
        }
    }

    protected final void registerModuleCommand(String name, ModuleCommand command) {
        if (!commands.keySet().contains(name) && enabled) {
            commands.put(name, command);
            if (enabled) {
                SinkLibrary.getInstance().registerCommand(name, command);
            }
        }
    }

    public final T getPlugin() {
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

    @Override
    public String toString() {
        return getName();
    }
}
