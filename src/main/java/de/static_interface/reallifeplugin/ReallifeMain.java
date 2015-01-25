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
import de.static_interface.reallifeplugin.commands.AdCommand;
import de.static_interface.reallifeplugin.commands.CorporationCommand;
import de.static_interface.reallifeplugin.commands.InsuranceCommand;
import de.static_interface.reallifeplugin.commands.ReallifePluginCommand;
import de.static_interface.reallifeplugin.commands.StockMarketCommand;
import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.database.DatabaseConfiguration;
import de.static_interface.reallifeplugin.database.DatabaseType;
import de.static_interface.reallifeplugin.database.impl.H2Database;
import de.static_interface.reallifeplugin.database.impl.MySqlDatabase;
import de.static_interface.reallifeplugin.listener.AntiEscapeListener;
import de.static_interface.reallifeplugin.listener.CorporationListener;
import de.static_interface.reallifeplugin.listener.InsuranceListener;
import de.static_interface.reallifeplugin.listener.OnlineTimeListener;
import de.static_interface.reallifeplugin.stockmarket.StockMarketUtil;
import de.static_interface.sinklibrary.Constants;
import de.static_interface.sinklibrary.SinkLibrary;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.logging.Level;

import javax.annotation.Nullable;

public class ReallifeMain extends JavaPlugin {

    static WorldGuardPlugin wgp;
    private static ReallifeMain instance;
    private Settings settings = null;
    private PayDayRunnable payDayRunnable = null;
    private BukkitTask payDayTask;
    private Database db;
    private BukkitTask stocksTask;

    public static ReallifeMain getInstance() {
        return instance;
    }

    public PayDayRunnable getPayDayRunnable() {
        return payDayRunnable;
    }

    public Settings getSettings() {
        return settings;
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

        settings = new Settings(this);

        long delay = settings.getPaydayTime() * 60 * (long) Constants.TICK;

        payDayRunnable = new PayDayRunnable();
        payDayTask = Bukkit.getScheduler().runTaskTimer(this, payDayRunnable, delay, delay);

        DatabaseConfiguration config = new DatabaseConfiguration(getDataFolder());
        DatabaseType type = config.getDatabaseType();
        switch (type) {
            case H2:
                db = new H2Database(config, this);
                break;
            case MYSQL:
                db = new MySqlDatabase(config, this);
                break;

            case INVALID:
                getLogger().log(Level.WARNING, "Invalid Database type: " + config.get("Type"));
            case NONE:
                db = null;
                getSettings().setCorporationsEnabled(false);
                break;
        }

        if (db != null) {
            try {
                db.setupConfig();
                db.connect();
                db.initTables();
            } catch (SQLException e) {
                getLogger().log(Level.SEVERE, "Database connection failed. Disabling database-based features.");
                e.printStackTrace();
                getSettings().setCorporationsEnabled(false);
                db = null;
            }
        }

        if (db != null && getSettings().isStockMarketEnabled()) {
            stocksTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
                @Override
                public void run() {
                    StockMarketUtil.onStocksUpdate();
                }
            }, 0, 20 * 60 * 60);
        }

        registerCommands();
        registerListeners();

        wgp = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard");
    }

    @Nullable
    public Database getDB() {
        return db;
    }

    @Override
    public void onDisable() {
        if (payDayTask != null) {
            payDayTask.cancel();
        }

        if (stocksTask != null) {
            stocksTask.cancel();
        }

        if (db != null) {
            try {
                db.close();
            } catch (SQLException e) {
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
        Bukkit.getPluginCommand("reallifeplugin").setExecutor(new ReallifePluginCommand());
        if (getSettings().isInsuranceEnabled()) {
            Bukkit.getPluginCommand("insurance").setExecutor(new InsuranceCommand(this));
        }
        if (getSettings().isCorporationsEnabled()) {
            SinkLibrary.getInstance().registerCommand("corporation", new CorporationCommand(this));
        }

        SinkLibrary.getInstance().registerCommand("ad", new AdCommand(this));

        if (getSettings().isStockMarketEnabled()) {
            SinkLibrary.getInstance().registerCommand("stockmarket", new StockMarketCommand(this));
        }
    }

    private void registerListeners() {
        if (getSettings().isInsuranceEnabled()) {
            Bukkit.getPluginManager().registerEvents(new InsuranceListener(), this);
        }
        if (getSettings().isAntiEscapeEnabled()) {
            Bukkit.getPluginManager().registerEvents(new AntiEscapeListener(), this);
        }
        if (getSettings().isCorporationsEnabled()) {
            Bukkit.getPluginManager().registerEvents(new CorporationListener(), this);
        }
        Bukkit.getPluginManager().registerEvents(new OnlineTimeListener(), this);
    }

    public boolean isLwcAvailable() {
        return Bukkit.getPluginManager().getPlugin("LWC") != null;
    }
}
