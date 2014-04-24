package de.static_interface.reallifeplugin.commands;

import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.User;
import de.static_interface.sinklibrary.configuration.PlayerConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.concurrent.TimeUnit;

public class InsuranceCommand implements CommandExecutor
{
    public static final String ACTIVATED_PATH = "Insurance.Activated";
    public static final String TIMEOUT_TIMESTAMP = "Insurance.Timestamp";

    public static void createVars(User user)
    {
        PlayerConfiguration config = user.getPlayerConfiguration();
        if ( config.get(ACTIVATED_PATH) == null )
        {
            config.set(ACTIVATED_PATH, false);
        }
        if ( config.get(TIMEOUT_TIMESTAMP) == null )
        {
            config.set(TIMEOUT_TIMESTAMP, 0);
        }
    }

    public static boolean isActive(User user)
    {
        return (boolean) user.getPlayerConfiguration().get(ACTIVATED_PATH);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        User user = SinkLibrary.getUser(sender);

        if ( user.isConsole() || args.length < 1 )
        {
            return false;
        }

        PlayerConfiguration config = user.getPlayerConfiguration();

        createVars(user);

        boolean active = (boolean) config.get(ACTIVATED_PATH);
        long timestamp = Long.parseLong(String.valueOf(config.get(TIMEOUT_TIMESTAMP)));

        switch ( args[0].toLowerCase() )
        {
            case "on":
                if ( active )
                {
                    user.sendMessage(ChatColor.DARK_RED + "Fehler: " + ChatColor.RED + "Die Versicherung ist schon aktiviert!");
                    return true;
                }

                config.set(ACTIVATED_PATH, true);
                config.set(TIMEOUT_TIMESTAMP, System.currentTimeMillis() + 12 * 60 * 60 * 1000); //12 h

                user.sendMessage(ChatColor.DARK_GREEN + "Die Versicherung wurde erfolgreich aktiviert");
                break;

            case "off":
                if ( !active )
                {
                    user.sendMessage(ChatColor.DARK_RED + "Fehler: " + ChatColor.RED + "Die Versicherung ist schon deaktiviert!");
                    return true;
                }

                if ( timestamp != 0 && timestamp > System.currentTimeMillis() )
                {
                    user.sendMessage(ChatColor.DARK_RED + "Fehler: " + ChatColor.RED + "Die Versicherung kann erst nach 12 Stunden deaktiviert werden.");

                    long millis = timestamp - System.currentTimeMillis();

                    String timeleft = String.format("Es sind noch %d Stunde(n) und %d Minute(n) uebrig!", TimeUnit.MILLISECONDS.toHours(millis), TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)));

                    user.sendMessage(ChatColor.RED + timeleft);
                    return true;
                }

                config.set(ACTIVATED_PATH, false);
                config.set(TIMEOUT_TIMESTAMP, 0);

                user.sendMessage(ChatColor.DARK_GREEN + "Die Versicherung wurde erfolgreich deaktiviert");
                break;
        }
        return true;
    }
}
