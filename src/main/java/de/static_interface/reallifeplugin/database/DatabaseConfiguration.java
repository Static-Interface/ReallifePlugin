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

package de.static_interface.reallifeplugin.database;

import de.static_interface.sinklibrary.api.configuration.Configuration;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class DatabaseConfiguration extends Configuration {

    private final Plugin plugin;

    public DatabaseConfiguration(Plugin plugin) {
        super(new File(plugin.getDataFolder(), "Database.yml"), true);
        this.plugin = plugin;
    }

    @Override
    public void addDefaults() {
        addDefault("Type", "MySQL", "Only MySQL is supported ATM");
        addDefault("Address", "localhost");
        addDefault("Port", 3306);
        addDefault("Username", "root");
        addDefault("Password", "");
        addDefault("TablePrefix", "RP_");
        addDefault("DatabaseName", plugin.getName().replace(" ", ""));
    }

    public DatabaseType getType() {
        return DatabaseType.MYSQL;
        //return DatabaseType.valueOf((String)get("Type"));
    }

    public String getAddress() {
        return (String) get("Address");
    }

    public int getPort() {
        return Integer.valueOf(String.valueOf(get("Port")));
    }

    public String getUsername() {
        return (String) get("Username");
    }

    public String getPassword() {
        return (String) get("Password");
    }

    public String getTablePrefix() {
        return (String) get("TablePrefix");
    }

    public String getDatabaseName() {
        return (String) get("DatabaseName");
    }
}
