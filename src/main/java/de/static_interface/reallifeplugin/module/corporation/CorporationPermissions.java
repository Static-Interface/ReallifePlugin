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

import de.static_interface.reallifeplugin.permission.Permission;
import de.static_interface.reallifeplugin.permission.Permissions;

public class CorporationPermissions extends Permissions {
    public static final Permission ALL = new Permission("*", "All permissions", false);
    public static final Permission SET_RANK = new Permission("setrank", "Set ranks", false);
    public static final Permission MANAGE_RANKS = new Permission("manageranks", "Create and configure ranks", false);
    public static final Permission DEPOSIT = new Permission("deposit", "Deposit money", true);
    public static final Permission WITHDRAW = new Permission("withdraw", "Withdraw money", false);
    public static final Permission INVITE = new Permission("invite", "Invite people to the corporation", false);
    public static final Permission KICK = new Permission("kick", "Kick members from the corporation", false);
    public static final Permission DELETE = new Permission("delete", "Delete this corporation", false);
    public static final Permission CSELL_SIGN = new Permission("csellsign", "Create csell signs", false);
    public static final Permission CBUY_SIGN = new Permission("cbuysign", "Create cbuy signs", false);
    public static final Permission REGION_OWNER = new Permission("regionowner", "Makes the user region owner", false);

    private static CorporationPermissions instance;
    private CorporationPermissions() {
        add(ALL, SET_RANK, MANAGE_RANKS, DEPOSIT, WITHDRAW, INVITE, KICK, DELETE, CSELL_SIGN, CBUY_SIGN);
    }

    public static void init() {
        instance = new CorporationPermissions();
    }

    public static CorporationPermissions getInstance() {
        return instance;
    }

    public static void unload() {
        instance = null;
    }
}
