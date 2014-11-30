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

import de.static_interface.reallifeplugin.model.*;

import java.util.*;

public class PayDayQueue {
    //Todo: Dump data with json when plugin is beeing unloaded, load with onEnable

    private static HashMap<UUID, List<Entry>> queue = new HashMap<>();

    public static void addToQueue(UUID uuid, Entry entry) {
        List<Entry> list;
        if (!queue.containsKey(uuid)) {
            list = new ArrayList<>();
        } else {
            list = queue.get(uuid);
        }
        list.add(entry);
        queue.put(uuid, list);
    }

    public static HashMap<UUID, List<Entry>> getQueues() {
        return queue;
    }

    public static List<Entry> getPlayerQueue(UUID uuid) {
        return queue.get(uuid);
    }
}
