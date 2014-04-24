package de.static_interface.reallifeplugin;

import de.static_interface.reallifeplugin.commands.InsuranceCommand;
import de.static_interface.reallifeplugin.commands.ReallifePluginCommand;
import de.static_interface.reallifeplugin.listener.InsuranceListener;
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
            getLogger().log(Level.WARNING, "This Plugin requires SinkLibrary!");
            Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }

        if ( !SinkLibrary.initialized )
        {
            throw new NotInitializedException("SinkLibrary is not initialized!");
        }
        return true;
    }

    private void registerCommands()
    {
        Bukkit.getPluginCommand("reallifeplugin").setExecutor(new ReallifePluginCommand());
        Bukkit.getPluginCommand("insurance").setExecutor(new InsuranceCommand());
    }

    private void registerListeners()
    {
        Bukkit.getPluginManager().registerEvents(new InsuranceListener(), this);
    }
}
