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

import static de.static_interface.reallifeplugin.ReallifeLanguageConfiguration.m;

import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.corporation.Corporation;
import de.static_interface.reallifeplugin.corporation.CorporationUtil;
import de.static_interface.reallifeplugin.database.table.row.CorpTradesRow;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.Debug;
import de.static_interface.sinklibrary.util.StringUtil;
import de.static_interface.sinklibrary.util.VaultBridge;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CorporationListener implements Listener {

    @EventHandler
    public static void onSignChange(SignChangeEvent event) {
        Block signBlock = event.getBlock();
        String[] lines = event.getLines();
        IngameUser user = SinkLibrary.getInstance().getIngameUser(event.getPlayer());
        if (!(signBlock.getState() instanceof Sign)) {
            return;
        }

        if (!isCorpTradesSign(lines)) {
            return;
        }

        Chest chest = CorporationUtil.findConnectedChest(signBlock);
        if (chest == null) {
            user.sendMessage(m("Corporation.Sign.InvalidChest"));
            event.setLine(1, ChatColor.DARK_RED + ChatColor.stripColor(lines[0]));
            return;
        }


        if (!validateSign(lines, signBlock.getLocation(), user)) {
            event.setLine(1, ChatColor.DARK_RED + ChatColor.stripColor(lines[0]));
            return;
        }

        event.setLine(1, ChatColor.BLUE + ChatColor.stripColor(lines[0]));

        /* Protect chest with LWC if available */
        if (ReallifeMain.getInstance().isLwcAvailable()) {
            com.griefcraft.model.Protection.Type type = com.griefcraft.model.Protection.Type.PRIVATE;
            com.griefcraft.model.Protection protection = ReallifeMain.getInstance().getLWC().getPhysicalDatabase().
                    registerProtection(signBlock.getTypeId(), type, signBlock.getWorld().toString(), event.getPlayer().toString(), "",
                                       (int) signBlock.getLocation().getX(), (int) signBlock.getLocation().getY(),
                                       (int) signBlock.getLocation().getZ());
            ReallifeMain.getInstance().getLWC().wrapPlayer(event.getPlayer()).addAccessibleProtection(protection);
        }
    }

    private static boolean isCorpTradesSign(String[] lines) {
        return ChatColor.stripColor(lines[0]).equals("[CBuy]") ||
               ChatColor.stripColor(lines[0]).equals("[CSell]");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static boolean validateSign(String[] lines, Location location, IngameUser user) {
        Corporation corp = CorporationUtil.getUserCorporation(user.getUniqueId());
        if (corp == null) {
            user.sendMessage(m("Corporation.NotInCorporation"));
            return false;
        }

        if (!corp.isCoCeo(user.getUniqueId()) && !corp.isCeo(user.getUniqueId())) {
            user.sendMessage(m("Corporation.NotCEO"));
            return false;
        }

        try {
            int amount;

            try {
                amount = Integer.valueOf(lines[1]);
            } catch (NumberFormatException e) {
                user.sendMessage(ChatColor.RED + "Invalid Amount: " + lines[1]);
                return false;
            }

            try {
                Double.parseDouble(lines[2]);
            } catch (NumberFormatException e) {
                user.sendMessage(ChatColor.RED + "Invalid price: " + lines[1]);
                return false;
            }

            if (amount > 64 || amount < 1) {  //Invalid amount ( amount must be [1,64) )
                throw new NumberFormatException();
            }

            //Check if Material is valid
            try {
                String material_name = lines[3];
                getItemStack(material_name, amount);
            } catch (Exception e) {
                user.sendMessage(ChatColor.RED + "Unknown item or block: " + lines[3]);
                return false;
            }

            //Validate location
            if (CorporationUtil.getCorporation(location) != corp) {
                user.sendMessage(m("Corporation.Sign.InvalidCreateLocation"));
                return false;
            }
        } catch (Exception e) {
            Debug.log("Exception while trying to create sign: ", e);
            return false;
        }
        return true;
    }

    private static ItemStack getItemStack(String materialName, int amount) {
        Material material = Material.valueOf(materialName);
        if (material == null) {
            throw new NullPointerException("Unknown material: " + materialName);
        }
        return new ItemStack(material, amount);
    }

    @EventHandler
    public void onPlayerInteractCSell(PlayerInteractEvent event) {
        IngameUser user = SinkLibrary.getInstance().getIngameUser(event.getPlayer());

        try {
            if (event.getMaterial() != Material.SIGN && event.getMaterial() != Material.SIGN_POST) {
                return;
            }

            Sign sign = (Sign) event.getClickedBlock().getState();
            if (!sign.getLine(1).trim().equals(ChatColor.BLUE + "[CSell]")) {
                return;
            }

            Corporation corp = CorporationUtil.getCorporation(event.getClickedBlock().getLocation());
            if (corp == null) {
                user.sendMessage(m("Corporation.NotInCorporation"));
                return;
            }

            if (CorporationUtil.getUserCorporation(user.getUniqueId()) != corp) {
                user.sendMessage(m("Corporation.Sign.InvalidSellCorporation"));
                return;
            }

            int amount = Integer.valueOf(sign.getLine(2));
            Material toSell = Material.valueOf(sign.getLine(4));

            double pricePerItem = (double) amount / Double.valueOf(sign.getLine(3));

            Inventory inv = event.getPlayer().getInventory();
            ItemStack stack = null;
            int stackamount = 0;
            for (ItemStack invStack : inv.getContents()) {
                if (invStack.getType() == toSell && invStack.getAmount() > stackamount) {
                    stack = invStack;
                    stackamount = invStack.getAmount();
                }
            }

            if (stack == null) {
                user.sendMessage(m("Corporation.Sign.NoItemsFound"));
                return;
            }

            Chest chest = CorporationUtil.findConnectedChest(sign.getBlock());
            if (chest == null) {
                user.sendMessage(m("Corporation.Sign.InvalidChest"));
                return;
            }

            if (!CorporationUtil.canAddItemStack(chest.getBlockInventory())) {
                user.sendMessage(m("Corporation.Sign.ChestFull"));
                return;
            }

            double price = pricePerItem * stackamount;

            if (corp.getBalance() > price && corp.addBalance(-price)) {
                inv.remove(stack);
                chest.getBlockInventory().addItem(stack);
                user.addBalance(price);
                user.sendMessage(
                        StringUtil.format(m("Corporation.Sign.Sold"), stack.getAmount(), stack.getItemMeta().getDisplayName(),
                                          price));

                int newAmount = 0;
                for (ItemStack invStack : chest.getBlockInventory().getContents()) {
                    if (invStack.getType() == stack.getType()) {
                        newAmount += invStack.getAmount();
                    }
                }

                CorpTradesRow row = new CorpTradesRow();
                row.id = null;
                row.amount = amount;
                row.corp_id = corp.getId();
                row.location = sign.getLocation();
                row.material = stack.getType();
                row.new_amount = newAmount;
                row.price = price;
                row.soldAmount = stackamount;
                row.time = System.currentTimeMillis();
                row.type = "csell";
                row.userId = CorporationUtil.getUserId(user);
                ReallifeMain.getInstance().getDB().getCorpTradesTable().insert(row);

                //Todo: add MySQL entry
                return;
            }

            user.sendMessage(m("Corporation.NotEnoughMoney"));
        } catch (Exception e) {
            user.sendMessage(ChatColor.DARK_RED + "Error: " + ChatColor.RED + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerInteractCBuy(PlayerInteractEvent event) {
        IngameUser user = SinkLibrary.getInstance().getIngameUser(event.getPlayer());
        try {
            if (event.getMaterial() != Material.SIGN && event.getMaterial() != Material.SIGN_POST) {
                return;
            }

            Sign sign = (Sign) event.getClickedBlock().getState();
            if (!sign.getLine(1).trim().equals(ChatColor.BLUE + "[CBuy]")) {
                return;
            }

            Corporation corp = CorporationUtil.getCorporation(event.getClickedBlock().getLocation());
            if (corp == null) {
                user.sendMessage(m("Corporation.NotInCorporation"));
                return;
            }

            if (CorporationUtil.getUserCorporation(user.getUniqueId()) != corp) {
                user.sendMessage(m("Corporation.Sign.InvalidBuyCorporation"));
                return;
            }

            int amount = Integer.valueOf(sign.getLine(2));
            String material_name = sign.getLine(4);
            Material toBuy = Material.valueOf(material_name);

            if (!user.getPlayer().getCanPickupItems()) {
                user.sendMessage(m("Corporation.Sign.CantPickup"));
                return;
            }

            Chest chest = CorporationUtil.findConnectedChest(sign.getBlock());
            if (chest == null) {
                user.sendMessage(m("Corporation.Sign.InvalidChest"));
                return;
            }

            double pricePerItem = (double) amount / Double.valueOf(sign.getLine(3));

            Inventory inv = event.getPlayer().getInventory();
            ItemStack stack = null;
            int stackamount = 0;
            for (ItemStack invStack : inv.getContents()) {
                if (invStack.getType() == toBuy && invStack.getAmount() > stackamount) {
                    stack = invStack;
                    stackamount = invStack.getAmount();
                }
            }

            double price = pricePerItem * stack.getAmount();

            if (stack == null) {
                user.sendMessage(m("Corporation.Sign.NoItemsFound"));
                return;
            }

            if (VaultBridge.getBalance(user.getPlayer()) > price && VaultBridge.addBalance(user.getPlayer(), -price)) {
                inv.remove(stack);
                user.getPlayer().getInventory().addItem(stack);
                corp.addBalance(price);
                user.sendMessage(
                        StringUtil.format(m("Corporation.Sign.Bought"), stack.getAmount(), stack.getItemMeta().getDisplayName(),
                                          price));
                int newAmount = 0;
                for (ItemStack invStack : chest.getBlockInventory().getContents()) {
                    if (invStack.getType() == stack.getType()) {
                        newAmount += invStack.getAmount();
                    }
                }

                CorpTradesRow row = new CorpTradesRow();
                row.id = null;
                row.amount = amount;
                row.corp_id = corp.getId();
                row.location = sign.getLocation();
                row.material = stack.getType();
                row.new_amount = newAmount;
                row.price = price;
                row.soldAmount = stackamount;
                row.time = System.currentTimeMillis();
                row.type = "cbuy";
                row.userId = CorporationUtil.getUserId(user);

                ReallifeMain.getInstance().getDB().getCorpTradesTable().insert(row);
                return;
            }

            user.sendMessage(m("Corporation.Sign.NotEnoughMoney"));
        } catch (Exception e) {
            user.sendMessage(ChatColor.DARK_RED + "Error: " + ChatColor.RED + e.getMessage());
            e.printStackTrace();
        }
    }
}
