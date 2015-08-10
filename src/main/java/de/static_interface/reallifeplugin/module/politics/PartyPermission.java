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

package de.static_interface.reallifeplugin.module.politics;

import javax.annotation.Nonnull;

public enum PartyPermission {
    ALL("*", "All permissions", false),
    SET_RANK("setrank", "Set ranks", false),
    MANAGE_RANKS("manageranks", "Create and configure ranks", false),
    DEPOSIT("desposit", "Deposit money", true),
    WITHDRAW("withdraw", "Withdraw money", false),
    INVITE("invite", "Invite people to the party", false),
    KICK("kick", "Kick members from a party", false),
    MEMBER_FEE("memberfee", "Fee for beeing member", Integer.class, null, false),
    MEMBER_FEE_PERIOD("memberfeeperiod", "Period for fee in days", Integer.class, null, false),
    DELETE("delete", "Delete this party", false),;

    private final String description;
    private final Class valueType;
    private final boolean includeInAllPerms;
    private String permissionString;
    private Object defaultValue;

    PartyPermission(String permissionString, String description, @Nonnull Object defaultValue) {
        this.permissionString = permissionString;
        this.description = description;
        this.defaultValue = defaultValue;
        this.valueType = defaultValue.getClass();
        includeInAllPerms = true;
    }

    PartyPermission(String permissionString, String description, Class valueType, Object defaultValue, boolean includeInAllPerms) {
        this.permissionString = permissionString;
        this.description = description;
        this.defaultValue = defaultValue;
        this.valueType = valueType;
        this.includeInAllPerms = includeInAllPerms;
    }

    public String getDescription() {
        return description;
    }

    public String getPermissionString() {
        return permissionString;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Class getValueType() {
        return valueType;
    }

    public boolean includeInAllPermission() {
        return includeInAllPerms;
    }
}
