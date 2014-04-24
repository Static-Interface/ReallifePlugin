package de.static_interface.reallifeplugin;

import de.static_interface.reallifeplugin.entries.PayDayEntry;
import de.static_interface.reallifeplugin.entries.TaxesEntry;
import de.static_interface.reallifeplugin.events.PayDayEvent;
import de.static_interface.reallifeplugin.model.Entry;
import de.static_interface.reallifeplugin.model.EntryResult;
import de.static_interface.reallifeplugin.model.Group;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PayDayRunnable implements Runnable
{
    public void givePayDay(Player player, Group group)
    {
        User user = SinkLibrary.getUser(player);
        List<Entry> entries = new ArrayList<>();
        entries.add(new PayDayEntry(user, group));
        entries.add(new TaxesEntry(user, group));

        PayDayEvent event = new PayDayEvent(user, group);
        Bukkit.getServer().getPluginManager().callEvent(event);

        if ( event.isCancelled() ) return;

        entries.addAll(event.getEntries());
        List<String> out = new ArrayList<>();

        double result = 0;
        out.add(ChatColor.BLUE + "--------------------" + ChatColor.BLUE + " Zahltag " + ChatColor.BLUE + "--------------------");

        for ( Entry entry : entries )
        {
            EntryResult entryResult = handleEntry(entry);
            out.add(entryResult.out);
            result += entryResult.amount;
        }

        if ( result == 0 )
        {
            return;
        }

        String resultPrefix = (result < 0 ? ChatColor.DARK_RED : ChatColor.DARK_GREEN) + "";
        String moneyPrefix = (user.getMoney() < 0 ? ChatColor.DARK_RED : ChatColor.DARK_GREEN) + "";

        out.add(ChatColor.AQUA + String.format("|- Summe: " + resultPrefix + "%s Euro", result));
        out.add(ChatColor.AQUA + String.format("|- Geld: " + moneyPrefix + "%s Euro", user.getMoney()));

        String seperator = ChatColor.BLUE + "------------------------------------------------";
        out.add(seperator);
        player.sendMessage(out.toArray(new String[out.size()]));
    }

    private EntryResult handleEntry(Entry entry)
    {
        EntryResult result = new EntryResult();
        double amount = entry.getAmount();

        boolean negative = amount < 0;
        String text;

        String entryPrefix = "|- ";
        if ( negative )
        {
            text = String.format(ChatColor.RED + entryPrefix + "-%s Euro wurden abgezogen. (Grund: %s)", -amount, entry.getReason());
        }
        else
        {
            text = String.format(ChatColor.GREEN + entryPrefix + "+%s Euro wurden hinzugefuegt. (Grund: %s)", amount, entry.getReason());
        }

        result.out = text;
        result.amount = amount;

        User from = SinkLibrary.getUser(entry.from());
        from.addBalance(amount);

        if ( entry.sendToTarget() )
        {
            User target = SinkLibrary.getUser(entry.target());
            target.addBalance(-amount);
        }
        return result;
    }

    @Override
    public void run()
    {
        for ( Player player : Bukkit.getOnlinePlayers() )
        {
            boolean isInGroup = false;
            User user = SinkLibrary.getUser(player);
            for ( Group group : ReallifeMain.getSettings().readGroups() )
            {
                if ( ChatColor.stripColor(user.getPrimaryGroup()).equals(group.name) )
                {
                    givePayDay(player, group);
                    isInGroup = true;
                    break;
                }
            }
            if ( !isInGroup )
            {
                givePayDay(player, getDefaultGroup(user));
            }
        }
    }

    public Group getDefaultGroup(User user)
    {
        Group group = new Group();
        group.payday = ReallifeMain.getSettings().getDefaultPayday();
        group.taxesmodifier = ReallifeMain.getSettings().getDefaultTaxesModifier();
        group.shownName = user.getPrimaryGroup();
        group.name = ChatColor.stripColor(user.getPrimaryGroup());
        group.excluded = ReallifeMain.getSettings().getDefaultExcluded();
        return group;
    }
}
