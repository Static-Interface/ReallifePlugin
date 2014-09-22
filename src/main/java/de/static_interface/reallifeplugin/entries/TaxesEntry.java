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
import de.static_interface.sinklibrary.util.MathUtil;
import de.static_interface.sinklibrary.util.StringUtil;
import de.static_interface.sinklibrary.util.VaultHelper;
import org.bukkit.entity.Player;

public class TaxesEntry extends Entry {

    Player player;
    Group group;

    public TaxesEntry(Player player, Group group) {
        this.player = player;
        this.group = group;
    }


    @Override
    public String getSourceAccount() {
        return player.getName();
    }

    @Override
    public String getReason() {
        return StringUtil.format("{0}%% Steuern", MathUtil.round(getTaxesModifier() * 100));
    }

    @Override
    public double getAmount() {
        return -MathUtil.round(getTaxes());
    }

    private double getTaxesModifier() {
        double money = VaultHelper.getBalance(player) + group.payday;
        double taxesBase = ReallifeMain.getSettings().getTaxesBase();

        double taxesmodifier = group.taxesmodifier;
        if (money <= 25000) {
            taxesmodifier *= 20;
        } else if (money <= 50000) {
            taxesmodifier *= 16;
        } else if (money <= 100000) {
            taxesmodifier *= 12;
        } else if (money <= 250000) {
            taxesmodifier *= 8;
        } else if (money <= 500000) {
            taxesmodifier *= 6;
        } else if (money <= 750000) {
            taxesmodifier *= 5;
        } else {
            taxesmodifier *= 2.5;
        }

        return (taxesBase * taxesmodifier) / 100;
    }

    private double getTaxes() {
        return getTaxesModifier() * (VaultHelper.getBalance(player) + group.payday);
    }

    @Override
    public boolean sendToTarget() {
        return true;
    }

    @Override
    public String getTargetAccount() {
        return ReallifeMain.getSettings().getEconomyAccount();
    }
}
