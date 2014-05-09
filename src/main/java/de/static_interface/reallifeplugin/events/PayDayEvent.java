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

package de.static_interface.reallifeplugin.events;

import de.static_interface.reallifeplugin.model.Entry;
import de.static_interface.reallifeplugin.model.Group;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.List;

/**
 * This event is fired when a payday starts
 * Its fired seperatly for each player, so not only one time
 */
public class PayDayEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();
    List<Entry> entries = new ArrayList<>();
    private Player player;
    private Group group;
    private boolean cancelled;

    public PayDayEvent(Player player, Group group)
    {
        this.player= player;
        this.group = group;
    }

    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    public Player getPlayer()
    {
        return player;
    }

    public Group getGroup()
    {
        return group;
    }

    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }

    public void addEntry(Entry entry)
    {
        entries.add(entry);
    }

    public List<Entry> getEntries()
    {
        return entries;
    }

    public boolean isCancelled()
    {
        return cancelled;
    }

    public void setCancelled(boolean cancel)
    {
        cancelled = cancel;
    }
}
