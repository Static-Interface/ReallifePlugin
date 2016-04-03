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

package de.static_interface.reallifeplugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import de.static_interface.reallifeplugin.command.AdCommand;
import de.static_interface.reallifeplugin.command.ReallifePluginCommand;
import de.static_interface.reallifeplugin.config.RpLanguage;
import de.static_interface.reallifeplugin.config.RpSettings;
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.antiescape.AntiEscapeModule;
import de.static_interface.reallifeplugin.module.contract.ContractModule;
import de.static_interface.reallifeplugin.module.corporation.CorporationModule;
import de.static_interface.reallifeplugin.module.insurance.InsuranceModule;
import de.static_interface.reallifeplugin.module.level.LevelModule;
import de.static_interface.reallifeplugin.module.payday.PaydayModule;
import de.static_interface.reallifeplugin.module.politics.PoliticsModule;
import de.static_interface.reallifeplugin.module.stockmarket.StockMarketModule;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.database.DatabaseConfiguration;
import de.static_interface.sinksql.Database;
import de.static_interface.sinksql.impl.database.H2Database;
import de.static_interface.sinksql.impl.database.MySqlDatabase;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.annotation.Nullable;

public class ReallifeMain extends JavaPlugin {

    static WorldGuardPlugin wgp;
    private static ReallifeMain instance;
    private RpSettings rpSettings = null;
    private Database db;
    private File reallifeDirectory;

    public static ReallifeMain getInstance() {
        return instance;
    }

    public RpSettings getSettings() {
        return rpSettings;
    }

    public WorldGuardPlugin getWorldGuardPlugin() {
        return wgp;
    }

    @Override
    public void onEnable() {
        if (!checkDependencies()) {
            return;
        }

        instance = this;

        reallifeDirectory = new File(SinkLibrary.getInstance().getCustomDataFolder(), getName());

        rpSettings = new RpSettings(reallifeDirectory, this);
        new RpLanguage(reallifeDirectory).init();
        DatabaseConfiguration config = new DatabaseConfiguration(reallifeDirectory, "Database.yml", "ReallifePlugin", this);
        String type = config.getDatabaseType();

        switch (type.toUpperCase().trim()) {
            case "H2":
                db = new H2Database(new File(reallifeDirectory, "database.h2"), config.getTablePrefix());
                break;
            case "MARIADB":
            case "MYSQL":
                db = new MySqlDatabase(config);
                break;
            default:
                getLogger().log(Level.SEVERE, "Warning, an error occured while parsing the config:");
                getLogger().log(Level.SEVERE, "Invalid Database type: " + type);
                Bukkit.getPluginManager().disablePlugin(this);
                return;
        }

        if (db != null) {
            try {
                db.connect();
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Database connection failed. Disabling database-based features.");
                e.printStackTrace();
                db = null;
            }
        }

        new AntiEscapeModule(this).enable();
        new ContractModule(this, db).enable();
        new PaydayModule(this, db).enable();
        new InsuranceModule(this).enable();
        new CorporationModule(this, db).enable();
        new StockMarketModule(this, db).enable();
        new LevelModule(this, db).enable();
        new PoliticsModule(this, db).enable();

        registerCommands();

        wgp = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard");
    }

    @Nullable
    public Database getDB() {
        return db;
    }

    @Override
    public void onDisable() {
        if (db != null) {
            try {
                db.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        for (Module module : Module.getModules()) {
            try {
                module.disable();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        instance = null;
    }

    public com.griefcraft.lwc.LWC getLWC() {
        return com.griefcraft.lwc.LWC.getInstance();
    }

    private boolean checkDependencies() {
        Plugin sinkLibrary = Bukkit.getPluginManager().getPlugin("SinkLibrary");

        if (sinkLibrary == null) {
            Bukkit.getLogger().log(Level.WARNING, "This Plugin requires SinkLibrary!");
            Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }

        if (!SinkLibrary.getInstance().isEconomyAvailable()) {
            Bukkit.getLogger().log(Level.WARNING, "Economy not available. Please install vault and an economy plugin");
            Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }

        return SinkLibrary.getInstance().validateApiVersion(SinkLibrary.API_VERSION, this);
    }

    private void registerCommands() {
        SinkLibrary.getInstance().registerCommand("reallifeplugin", new ReallifePluginCommand(this));
        SinkLibrary.getInstance().registerCommand("advertisment", new AdCommand(this));
    }

    public boolean isLwcAvailable() {
        return Bukkit.getPluginManager().getPlugin("LWC") != null;
    }

    public File getCustomDataFolder() {
        return reallifeDirectory;
    }
}
