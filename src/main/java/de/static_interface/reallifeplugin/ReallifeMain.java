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

import com.sk89q.worldguard.bukkit.*;
import de.static_interface.reallifeplugin.commands.*;
import de.static_interface.reallifeplugin.corporation.*;
import de.static_interface.reallifeplugin.listener.*;
import de.static_interface.sinklibrary.*;
import org.bukkit.*;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.*;
import org.bukkit.scheduler.*;

import java.util.logging.*;

public class ReallifeMain extends JavaPlugin {

    static WorldGuardPlugin wgp;
    private static ReallifeMain instance;
    private Settings settings = null;
    private PayDayRunnable payDayRunnable = null;
    private BukkitTask payDayTask;

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

        registerCommands();
        registerListeners();

        if (CorporationUtil.getCorporationConfig().isEnabled()) {
            wgp = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard");
            CorporationUtil.registerCorporationsFromConfig();
        }
    }

    @Override
    public void onDisable() {
        if (payDayTask != null) {
            payDayTask.cancel();
        }
        instance = null;
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
        SinkLibrary.getInstance().registerCommand("corporation", new CorporationCommand(this));
        SinkLibrary.getInstance().registerCommand("ad", new AdCommand(this));
    }

    private void registerListeners() {
        if (getSettings().isInsuranceEnabled()) {
            Bukkit.getPluginManager().registerEvents(new InsuranceListener(), this);
        }
        if (getSettings().isAntiEscapeEnabled()) {
            Bukkit.getPluginManager().registerEvents(new AntiEscapeListener(), this);
        }

        Bukkit.getPluginManager().registerEvents(new OnlineTimeListener(), this);
    }
}
