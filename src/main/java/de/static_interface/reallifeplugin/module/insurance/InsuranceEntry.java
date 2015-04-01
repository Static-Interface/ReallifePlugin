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

package de.static_interface.reallifeplugin.module.insurance;

import de.static_interface.reallifeplugin.module.payday.model.Entry;
import de.static_interface.reallifeplugin.module.payday.model.Group;
import org.bukkit.entity.Player;

public class InsuranceEntry extends Entry {

    Player player;
    Group group;
    InsuranceModule module;

    public InsuranceEntry(InsuranceModule module, Player player, Group group) {
        this.player = player;
        this.group = group;
        this.module = module;
    }


    @Override
    public String getSourceAccount() {
        return player.getName();
    }

    @Override
    public String getReason() {
        return "Versicherung";
    }

    @Override
    public double getAmount() {
        return -750;
    }

    @Override
    public boolean sendToTarget() {
        return true;
    }

    @Override
    public String getTargetAccount() {
        return module.getInsuranceAccount();
    }
}
