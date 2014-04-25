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

package de.static_interface.reallifeplugin;

import de.static_interface.sinklibrary.SinkLibrary;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.entity.Player;

public class VaultBridge
{
    public static String getPlayerGroup(Player player)
    {
        Permission perm = SinkLibrary.getPermissions();
        String[] groups = perm.getPlayerGroups(player);
        return groups[0];
    }

    public static double getBalance(Player player)
    {
        Economy economy = SinkLibrary.getEconomy();
        return economy.getBalance(player.getName());
    }

    public static boolean addBalance(Player player, double amount)
    {
        Economy economy = SinkLibrary.getEconomy();
        String name = player.getName();
        double roundedAmount = MathHelper.round(amount);
        EconomyResponse response;
        if ( roundedAmount > 0 )
        {
            SinkLibrary.getCustomLogger().debug("econ.withDrawPlayer(" + name + ", " + -roundedAmount + ");");
            response = economy.withdrawPlayer(name, -roundedAmount);
        }
        else if ( roundedAmount < 0 )
        {
            SinkLibrary.getCustomLogger().debug("econ.depositPlayer(" + name + ", " + roundedAmount + ");");
            response = economy.depositPlayer(name, roundedAmount);
        }
        else
        {
            return true;
        }
        boolean result = response.transactionSuccess();
        SinkLibrary.getCustomLogger().debug("result = " + result);
        return response.transactionSuccess();
    }
}