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

package de.static_interface.reallifeplugin.corporation;

import org.bukkit.ChatColor;

public class CorporationValues {

    public static final String VALUE_BASE = "Base";
    public static final String VALUE_CEO = "CEO";
    public static final String VALUE_CO_CEO = "CoCEOs";
    public static final String VALUE_MEMBERS = "Members";

    public static final String RANK_DEFAULT = ChatColor.GOLD.toString();
    public static final String RANK_CEO = ChatColor.DARK_RED + "CEO" + ChatColor.RED;
    public static final String RANK_CO_CEO = ChatColor.DARK_GREEN + "Co-CEO" + ChatColor.GREEN;
}
