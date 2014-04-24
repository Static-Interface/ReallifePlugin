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

package de.static_interface.reallifeplugin.entries;

import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.model.Entry;
import de.static_interface.reallifeplugin.model.Group;
import de.static_interface.sinklibrary.User;

public class InsuranceEntry extends Entry
{
    User user;
    Group group;

    public InsuranceEntry(User user, Group group)
    {
        this.user = user;
        this.group = group;
    }

    @Override
    public String from()
    {
        return user.getName();
    }

    @Override
    public String getReason()
    {
        return "Versicherung";
    }

    @Override
    public double getAmount()
    {
        return -750;
    }

    @Override
    public boolean sendToTarget()
    {
        return true;
    }

    @Override
    public String target()
    {
        return ReallifeMain.getSettings().getInsuranceAccount();
    }
}
