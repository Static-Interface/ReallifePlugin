/*
 * Copyright (c) 2014 http://adventuria.eu, http://static-interface.de and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.static_interface.reallifeplugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import de.static_interface.reallifeplugin.commands.CorporationCommand;
import de.static_interface.reallifeplugin.commands.InsuranceCommand;
import de.static_interface.reallifeplugin.commands.ReallifePluginCommand;
import de.static_interface.reallifeplugin.corporation.CorporationUtil;
import de.static_interface.reallifeplugin.listener.AntiEscapeListener;
import de.static_interface.reallifeplugin.listener.BanListener;
import de.static_interface.reallifeplugin.listener.InsuranceListener;
import de.static_interface.reallifeplugin.listener.OnlineTimeListener;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.exceptions.NotInitializedException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Level;

public class ReallifeMain extends JavaPlugin
{
    public static final long TICKS = 20L;
    private static Settings settings = null;
    private static PayDayRunnable payDayRunnable = null;
    private BukkitTask payDayTask;

    public static PayDayRunnable getPayDayRunnable()
    {
        return payDayRunnable;
    }

    public static Settings getSettings()
    {
        return settings;
    }

    static WorldGuardPlugin wgp;

    public void onEnable()
    {
        if ( !checkDependencies() ) return;

        settings = new Settings(this);
        settings.load();

        long delay = settings.getPaydayTime() * 60 * TICKS;

        payDayRunnable = new PayDayRunnable();
        payDayTask = Bukkit.getScheduler().runTaskTimer(this, payDayRunnable, delay, delay);

        registerCommands();
        registerListeners();

        SinkLibrary.registerPlugin(this);

        SinkLibrary.getCustomLogger().info("Enabled");
        if( CorporationUtil.getCorporationConfig().isEnabled())
        {
            wgp = (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard");
            CorporationUtil.registerCorporationsFromConfig();
        }
    }

    public static WorldGuardPlugin getWorldGuardPlugin()
    {
        return wgp;
    }

    public void onDisable()
    {
        payDayTask.cancel();
    }

    private boolean checkDependencies()
    {
        Plugin sinkLibrary = Bukkit.getPluginManager().getPlugin("SinkLibrary");

        if ( sinkLibrary == null )
        {
            Bukkit.getLogger().log(Level.WARNING, "This Plugin requires SinkLibrary!");
            Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }

        if ( !SinkLibrary.initialized )
        {
            throw new NotInitializedException("SinkLibrary is not initialized!");
        }

        if( !SinkLibrary.isEconomyAvailable())
        {
            Bukkit.getLogger().log(Level.WARNING, "Economy not available. Please install vault and an economy plugin");
            Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }
        return true;
    }

    private void registerCommands()
    {
        Bukkit.getPluginCommand("reallifeplugin").setExecutor(new ReallifePluginCommand());
        if (getSettings().isInsuranceEnabled())
        {
            Bukkit.getPluginCommand("insurance").setExecutor(new InsuranceCommand());
        }
        Bukkit.getPluginCommand("corporation").setExecutor(new CorporationCommand());
    }

    private void registerListeners()
    {
        if (getSettings().isInsuranceEnabled())
        {
            Bukkit.getPluginManager().registerEvents(new InsuranceListener(), this);
        }
        if (getSettings().isAntiEscapeEnabled())
        {
            Bukkit.getPluginManager().registerEvents(new AntiEscapeListener(), this);
        }
        Bukkit.getPluginManager().registerEvents(new BanListener(), this);
        Bukkit.getPluginManager().registerEvents(new OnlineTimeListener(), this);
    }
}
