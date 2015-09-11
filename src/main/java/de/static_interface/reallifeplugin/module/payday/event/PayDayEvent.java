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
import de.static_interface.reallifeplugin.module.payday.model.PaydayPlayer;
import org.bukkit.event.HandlerList;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * This event is fired when a payday starts
 * Its fired seperatly for each player, so not only one time
 */
public class PaydayEvent extends ModuleEvent<PaydayModule> {

    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled;
    private List<PaydayPlayer> players;

    public PaydayEvent(PaydayModule module, List<PaydayPlayer> players) {
        super(module);
        this.players = players;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @Nullable
    public PaydayPlayer getPaydayPlayer(UUID uuid) {
        for (PaydayPlayer p : players) {
            if (p.getPlayer().getUniqueId().equals(uuid)) {
                return p;
            }
        }

        return null;
    }

    public List<PaydayPlayer> getPlayers() {
        return players;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }
}
