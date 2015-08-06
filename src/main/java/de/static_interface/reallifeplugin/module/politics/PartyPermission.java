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
    SET_RANK("setrank", "Allows setting ranks", false),
    MANAGE_RANKS("manageranks", "Allows creating and configurin of ranks", false),
    DEPOSIT("desposit", "Allows depositing money", true),
    WITHDRAW("withdraw", "Allows withdrawing money", false),
    INVITE("invite", "Allows inviting people", false),
    KICK("kick", "Allows kicking members from a party", false),
    MEMBER_FEE("memberfee", "Set fee for beeing member", Integer.class, null),
    MEMBER_FEE_PERIOD("memberfeeperiod", "Period for fee in days", 7),;

    private final String description;
    private final Class valueType;
    private String permissionString;
    private Object defaultValue;

    PartyPermission(String permissionString, String description, @Nonnull Object defaultValue) {
        this.permissionString = permissionString;
        this.description = description;
        this.defaultValue = defaultValue;
        this.valueType = defaultValue.getClass();
    }

    PartyPermission(String permissionString, String description, Class valueType, Object defaultValue) {
        this.permissionString = permissionString;
        this.description = description;
        this.defaultValue = defaultValue;
        this.valueType = valueType;
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
}
