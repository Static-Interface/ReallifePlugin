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

package de.static_interface.reallifeplugin.module.payday.model;

public abstract class Entry implements Comparable<Entry> {

    public abstract String getSourceAccount();

    public abstract String getReason();

    public abstract double getAmount();

    public abstract boolean sendToTarget();

    public abstract String getTargetAccount();

    @Override
    public int compareTo(Entry entry) {
        if (entry == null) {
            throw new NullPointerException();
        }

        if (getAmount() < entry.getAmount()) {
            return 1;
        }
        if (getAmount() == entry.getAmount()) {
            return 0;
        } else {
            return -1;
        }
    }
}
