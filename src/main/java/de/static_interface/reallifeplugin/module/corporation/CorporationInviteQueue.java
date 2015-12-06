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

package de.static_interface.reallifeplugin.module.corporation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CorporationInviteQueue {

    private static Map<UUID, List<Integer>> invites = new HashMap<>();

    public static void add(UUID user, Corporation corp) {
        List<Integer> inviteIds = invites.get(user);
        if (inviteIds == null) {
            inviteIds = new ArrayList<>();
        }
        inviteIds.add(corp.getId());
        invites.put(user, inviteIds);
    }

    public static void remove(UUID user, Corporation corp) {
        List<Integer> inviteIds = invites.get(user);
        if (inviteIds == null) {
            return;
        }

        inviteIds.remove(Integer.valueOf(corp.getId()));
        if (inviteIds.size() == 0) {
            invites.remove(user);
        } else {
            invites.put(user, inviteIds);
        }
    }

    public static boolean hasInvite(UUID user, Corporation corp) {
        return invites.get(user) != null && invites.get(user).contains(corp.getId());
    }
}
