package de.static_interface.reallifeplugin.commands;

import de.static_interface.reallifeplugin.ReallifeMain;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReallifePluginCommand implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if ( args.length < 1 )
        {
            return false;
        }
        switch ( args[0] )
        {
            case "payday":
                ReallifeMain.getPayDayRunnable().run();
                break;
            case "reload":
                ReallifeMain.getSettings().reload();
                break;
            default:
                return false;
        }
        return true;
    }
}
