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

package de.static_interface.reallifeplugin.listener;

import static de.static_interface.reallifeplugin.ReallifeLanguageConfiguration.*;

import com.sk89q.worldedit.*;
import com.sk89q.worldguard.bukkit.BukkitUtil;
import com.sk89q.worldguard.protection.regions.*;
import de.static_interface.reallifeplugin.corporation.*;
import de.static_interface.sinklibrary.*;
import de.static_interface.sinklibrary.user.*;
import de.static_interface.sinklibrary.util.*;
import org.bukkit.*;
import org.bukkit.Location;
import org.bukkit.block.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;

public class CorporationListener implements Listener {

    @EventHandler
    public static void onSignChange(SignChangeEvent event) {
        Block signBlock = event.getBlock();
        String[] lines = event.getLines();
        IngameUser user = (IngameUser) SinkLibrary.getInstance().getUser(event.getPlayer());
        if (!(signBlock.getState() instanceof Sign)) {
            return;
        }

        if (!validateSign(lines, signBlock.getLocation(), user)) {
            return;
        }

        createCorpSign(lines);

    }

    private static void createCorpSign(String[] lines) {

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static boolean validateSign(String[] lines, Location location, IngameUser user) {
        Corporation corp = CorporationUtil.getUserCorporation(user.getUniqueId());
        if (corp == null) {
            return false;
        }

        try {
            //Validate sign prefix
            if (!lines[0].equals("[Corp]")) {
                return false;
            }

            //Validate items (are names valid? are the items present?)
            // Todo

            //Validate price
            Double.parseDouble(lines[2]);

            //Validate location
            ProtectedRegion region = corp.getBase();
            Vector vec = BukkitUtil.toVector(location);
            if (!region.contains(vec)) {
                user.sendMessage(""); //Todo: Can only create signs in base
                return false;
            }

            return true;
        } catch (Exception e) {
            Debug.log("Exception while trying to create sign: ", e);
            return false;
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getMaterial() != Material.SIGN && event.getMaterial() != Material.SIGN_POST) {
            return;
        }

        Sign sign = (Sign) event.getClickedBlock().getState();
        if (!ChatColor.stripColor(sign.getLine(1)).equals("[CBuy]")) {
            return;
        }

        String line2 = sign.getLine(2);
        String line3 = sign.getLine(3);
        String line4 = sign.getLine(4);

        ItemStack boughtItems = getItem(line2);
        double price = Double.valueOf(line3);
        Corporation corp = CorporationUtil.getCorporation(line4);

        IngameUser user = (IngameUser) SinkLibrary.getInstance().getUser(event.getPlayer());

        if (CorporationUtil.getUserCorporation(user.getUniqueId()) == corp) {
            user.sendMessage(m("Corporation.BuyingFromSameCorporation"));
            return;
        }

        if (!user.getPlayer().getCanPickupItems()) {
            user.sendMessage(m("Corporation.BuySign.CantPickup"));
            return;
        }
        user.getPlayer().getInventory().addItem(boughtItems);
        VaultBridge.addBalance(user.getPlayer(), -price);
        user.sendMessage(StringUtil.format(m("Corporation.BuySign.Bought"), boughtItems.getAmount() + boughtItems.getItemMeta().getDisplayName(),
                                           price + VaultBridge.getCurrenyName()));
    }

    private ItemStack getItem(String line3) {
        return null;
    }
}
