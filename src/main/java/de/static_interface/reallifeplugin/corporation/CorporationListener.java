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

import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.BukkitUtil;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.static_interface.reallifeplugin.VaultBridge;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.User;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import static de.static_interface.reallifeplugin.LanguageConfiguration.m;

public class CorporationListener implements Listener
{
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if(event.getMaterial() != Material.SIGN && event.getMaterial() != Material.SIGN_POST)
        {
            return;
        }

        Sign sign = (Sign) event.getClickedBlock().getState();
        if ( !ChatColor.stripColor(sign.getLine(1)).equals("[CBuy]") ) return;

        String line2 = sign.getLine(2);
        String line3 = sign.getLine(3);
        String line4 = sign.getLine(4);

        ItemStack boughtItems = getItem(line2);
        double price = Double.valueOf(line3);
        Corporation corp = CorporationUtil.getCorporation(line4);

        User user = SinkLibrary.getUser(event.getPlayer());

        if(CorporationUtil.getUserCorporation(user.getUniqueId()) == corp)
        {
            user.sendMessage(m("Corporation.BuyingFromSameCorporation"));
            return;
        }

        if(!user.getPlayer().getCanPickupItems())
        {
            user.sendMessage(m("Corporation.BuySign.CantPickup"));
            return;
        }
        user.getPlayer().getInventory().addItem(boughtItems);
        VaultBridge.addBalance(user.getName(), -price);
        user.sendMessage(String.format(m("Corporation.BuySign.Bought"), boughtItems.getAmount() + boughtItems.getItemMeta().getDisplayName(), price + VaultBridge.getCurrenyName()));
    }

    private ItemStack getItem(String line3)
    {
        return null;
    }

    @EventHandler
    public static void onSignChange(SignChangeEvent event)
    {
        Block signBlock = event.getBlock();
        String[] lines = event.getLines();
        User user = SinkLibrary.getUser(event.getPlayer());
        if (!(signBlock.getState() instanceof Sign))
        {
            return;
        }

        if (!validateSign(lines, signBlock.getLocation(), user))
        {
            return;
        }

        createCorpSign(lines);

    }

    private static void createCorpSign(String[] lines)
    {

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static boolean validateSign(String[] lines, Location location, User user)
    {
        Corporation corp = CorporationUtil.getUserCorporation(user.getUniqueId());
        if (corp == null) return false;

        try
        {
            //Validate sign prefix
            if ( !lines[0].equals("[Corp]") )
            {
                return false;
            }

            //Validate items (are names valid? are the items present?)
            // Todo

            //Validate price
            Double.parseDouble(lines[2]);

            //Validate location
            ProtectedRegion region = corp.getBase();
            Vector vec = BukkitUtil.toVector(location);
            if (!region.contains(vec))
            {
                user.sendMessage(""); //Todo: Can only create signs in base
                return false;
            }


            return true;
        }
        catch(Exception e)
        {
            SinkLibrary.getCustomLogger().debug("Exception while trying to create sign:" + e);
            return false;
        }
    }
}
