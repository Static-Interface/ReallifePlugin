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

package de.static_interface.reallifeplugin.module.payday.event;

import de.static_interface.reallifeplugin.module.ModuleEvent;
import de.static_interface.reallifeplugin.module.payday.PaydayModule;
import de.static_interface.reallifeplugin.module.payday.model.Entry;
import de.static_interface.reallifeplugin.module.payday.model.Group;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.List;

/**
 * This event is fired when a payday starts
 * Its fired seperatly for each player, so not only one time
 */
public class PayDayEvent extends ModuleEvent<PaydayModule> {

    private static final HandlerList handlers = new HandlerList();
    List<Entry> entries = new ArrayList<>();
    private Player player;
    private boolean checkTime;
    private Group group;
    private boolean cancelled;

    public PayDayEvent(PaydayModule module, Player player, Group group, boolean checkTime) {
        super(module);
        this.player = player;
        this.group = group;
        this.checkTime = checkTime;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Player getPlayer() {
        return player;
    }

    public Group getGroup() {
        return group;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public void addEntry(Entry entry) {
        entries.add(entry);
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

    public boolean isCheckTimeEnabled() {
        return checkTime;
    }

    public void setCheckTimeEnabled(boolean checkTime) {
        this.checkTime = checkTime;
    }
}
