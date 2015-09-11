/*
 * Copyright (c) 2013 - 2015 <http://static-interface.de> and contributors
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

package de.static_interface.reallifeplugin.module.payday.model;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PaydayPlayer {

    List<Entry> entries = new ArrayList<>();
    private Player player;
    private boolean checkTime;
    private Group group;
    private boolean cancelled;

    public PaydayPlayer(Player player, Group group, boolean checkTime) {
        this.player = player;
        this.group = group;
        this.checkTime = checkTime;
    }

    public void addEntry(Entry entry) {
        entries.add(entry);
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isCheckTime() {
        return checkTime;
    }

    public Group getGroup() {
        return group;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public boolean isCheckTimeEnabled() {
        return checkTime;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
