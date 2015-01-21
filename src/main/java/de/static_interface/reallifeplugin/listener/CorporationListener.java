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
import de.static_interface.reallifeplugin.database.table.row.corp.CorpTradesRow;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.Debug;
import de.static_interface.sinklibrary.util.MathUtil;
import de.static_interface.sinklibrary.util.StringUtil;
import de.static_interface.sinklibrary.util.VaultBridge;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class CorporationListener implements Listener {

    private static boolean isCorpTradesSign(String[] lines) {
        return ChatColor.stripColor(lines[0]).equals("[CBuy]") ||
               ChatColor.stripColor(lines[0]).equals("[CSell]");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static boolean validateSign(SignChangeEvent event, String[] lines, Location location, IngameUser user) {
        Corporation corp = CorporationUtil.getUserCorporation(user);
        if (corp == null) {
            user.sendMessage(m("Corporation.NotInCorporation"));
            return false;
        }

        if (!CorporationUtil.hasCeoPermissions(user, corp)) {
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

            if (amount > 64 || amount < 1) {  //Invalid stack_amount ( stack_amount must be [1,64) )
                throw new NumberFormatException();
            }

            //Check if Material is valid
            try {
                String material_name = lines[3].toUpperCase();
                getItemStack(material_name, amount);
                event.setLine(3, Material.valueOf(material_name).toString());
            } catch (Exception e) {
                user.sendMessage(ChatColor.RED + "Unknown item or block: " + lines[3]);
                return false;
            }

            //Validate location
            Corporation locationCorporation = CorporationUtil.getCorporation(location);

            if (locationCorporation.getId() != corp.getId()) {
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
    public void onSignChange(SignChangeEvent event) {
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
            event.setLine(0, ChatColor.DARK_RED + ChatColor.stripColor(lines[0]));
            return;
        }

        if (!validateSign(event, lines, signBlock.getLocation(), user)) {
            event.setLine(0, ChatColor.DARK_RED + ChatColor.stripColor(lines[0]));
            return;
        }

        event.setLine(0, ChatColor.BLUE + ChatColor.stripColor(lines[0]));

        user.sendMessage(m("General.Success"));
        registerBlock(chest.getBlock(), event.getPlayer());
        registerBlock(signBlock, event.getPlayer());

    }

    private void registerBlock(Block block, Player player) {
        if (ReallifeMain.getInstance().isLwcAvailable()) {
            com.griefcraft.model.Protection.Type type = com.griefcraft.model.Protection.Type.PRIVATE;
            com.griefcraft.model.Protection protection = ReallifeMain.getInstance().getLWC().getPhysicalDatabase().
                    registerProtection(block.getTypeId(), type, block.getWorld().toString(), player.getName(), "",
                                       (int) block.getLocation().getX(), (int) block.getLocation().getY(),
                                       (int) block.getLocation().getZ());
            ReallifeMain.getInstance().getLWC().wrapPlayer(player).addAccessibleProtection(protection);
        }
    }

    @EventHandler
    public void onPlayerInteractCSell(PlayerInteractEvent event) {
        IngameUser user = SinkLibrary.getInstance().getIngameUser(event.getPlayer());

        try {
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                return;
            }

            if (event.getClickedBlock().getType() != Material.WALL_SIGN && event.getClickedBlock().getType() != Material.SIGN
                && event.getClickedBlock().getType() != Material.SIGN_POST) {
                return;
            }

            if (!(event.getClickedBlock().getState() instanceof Sign)) {
                return;
            }

            Sign sign = (Sign) event.getClickedBlock().getState();
            if (!sign.getLine(0).trim().equals(ChatColor.BLUE + "[CSell]")) {
                return;
            }

            Corporation corp = CorporationUtil.getCorporation(event.getClickedBlock().getLocation());
            if (corp == null) {
                user.sendMessage(ChatColor.DARK_RED + "Invalid Sign"); //Todo
                sign.setLine(0, ChatColor.DARK_RED + "[CSell]");
                return;
            }

            if (CorporationUtil.getUserCorporation(user) == null) {
                user.sendMessage(m("Corporation.NotInCorporation"));
                return;
            }

            if (CorporationUtil.getUserCorporation(user).getId() != corp.getId()) {
                user.sendMessage(CorporationUtil.getUserCorporation(user).getName() + ":" + corp.getName());
                user.sendMessage(m("Corporation.Sign.InvalidSellCorporation"));
                return;
            }

            int amount = Integer.valueOf(sign.getLine(1));
            double price = Double.valueOf(sign.getLine(2));

            Material toSell = Material.valueOf(sign.getLine(3));

            double pricePerItem = price / (double) amount;

            Inventory inv = event.getPlayer().getInventory();
            ItemStack stack = null;
            int stackamount = 0;
            for (ItemStack invStack : inv.getContents()) {
                if (invStack != null && invStack.getType() == toSell && invStack.getAmount() > stackamount) {
                    stack = invStack;
                    stackamount = invStack.getAmount();
                }
            }

            if (stack == null) {
                user.sendMessage(m("Corporation.Sign.NoItemsFound"));
                return;
            }

            if (stack.getAmount() > amount) {
                stack.setAmount(amount);
                inv.addItem(new ItemStack(stack.getType(), stack.getAmount() - amount));
            } else if (stack.getAmount() < amount) { //Todo
                user.sendMessage(m("Corporation.Sign.NoItemsFound"));
                return;
            }

            Chest chest = CorporationUtil.findConnectedChest(sign.getBlock());
            if (chest == null) {
                user.sendMessage(m("Corporation.Sign.InvalidChest"));
                return;
            }

            if (!CorporationUtil.canAddItemStack(chest.getInventory())) {
                user.sendMessage(m("Corporation.Sign.ChestFull"));
                return;
            }

            price = pricePerItem * stackamount;

            price = MathUtil.round(price);

            if (corp.getBalance() > price && corp.addBalance(-price)) {
                inv.remove(stack);
                chest.getInventory().addItem(stack);
                user.addBalance(price);
                user.sendMessage(
                        StringUtil.format(m("Corporation.Sign.Sold"), stack.getAmount(), stack.getType().toString(),
                                          price));

                int newAmount = 0;
                for (ItemStack invStack : chest.getInventory().getContents()) {
                    if (invStack != null && invStack.getType() == stack.getType()) {
                        newAmount += invStack.getAmount();
                    }
                }

                CorpTradesRow row = new CorpTradesRow();
                row.id = null;
                row.signAmount = amount;
                row.corpId = corp.getId();
                row.location = sign.getLocation();
                row.material = stack.getType();
                row.newAmount = newAmount;
                row.price = price;
                row.soldAmount = stackamount;
                row.time = System.currentTimeMillis();
                row.type = 0;
                row.userId = CorporationUtil.getUserId(user);
                ReallifeMain.getInstance().getDB().getCorpTradesTable().insert(row);
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
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                return;
            }

            if (event.getClickedBlock().getType() != Material.WALL_SIGN && event.getClickedBlock().getType() != Material.SIGN
                && event.getClickedBlock().getType() != Material.SIGN_POST) {
                return;
            }

            if (!(event.getClickedBlock().getState() instanceof Sign)) {
                return;
            }

            Sign sign = (Sign) event.getClickedBlock().getState();
            if (!sign.getLine(0).trim().equals(ChatColor.BLUE + "[CBuy]")) {
                return;
            }

            Corporation corp = CorporationUtil.getCorporation(event.getClickedBlock().getLocation());
            if (corp == null) {
                user.sendMessage(ChatColor.DARK_RED + "Invalid Sign"); //Todo
                sign.setLine(0, ChatColor.DARK_RED + "[CBuy]");
                return;
            }

            if (CorporationUtil.getUserCorporation(user) == null) {
                user.sendMessage(m("Corporation.NotInCorporation"));
                return;
            }

            if (CorporationUtil.getUserCorporation(user).getId() != corp.getId()) {
                user.sendMessage(m("Corporation.Sign.InvalidBuyCorporation"));
                return;
            }

            int amount = Integer.valueOf(sign.getLine(1));
            double price = Double.valueOf(sign.getLine(2));
            String material_name = sign.getLine(3);
            Material toBuy = Material.valueOf(material_name);

            if (!CorporationUtil.canAddItemStack(user.getPlayer().getInventory())) {
                user.sendMessage(m("Corporation.Sign.CantPickup"));
                return;
            }

            Chest chest = CorporationUtil.findConnectedChest(sign.getBlock());
            if (chest == null) {
                user.sendMessage(m("Corporation.Sign.InvalidChest"));
                return;
            }

            double pricePerItem = price / (double) amount;

            Inventory inv = chest.getInventory();
            ItemStack stack = null;
            int stackamount = 0;
            for (ItemStack invStack : inv.getContents()) {
                if (invStack != null && invStack.getType() == toBuy && invStack.getAmount() > stackamount) {
                    stack = invStack;
                    stackamount = invStack.getAmount();
                }
            }

            if (stack == null) {
                user.sendMessage(m("Corporation.Sign.NoItemsFound"));
                return;
            }

            if (stack.getAmount() > amount) {
                stack.setAmount(amount);
                inv.addItem(new ItemStack(stack.getType(), stack.getAmount() - amount));
            } else if (stack.getAmount() < amount) { //Todo
                user.sendMessage(m("Corporation.Sign.NoItemsFound"));
                return;
            }

            price = pricePerItem * stack.getAmount();

            price = MathUtil.round(price);

            if (VaultBridge.getBalance(user.getPlayer()) > price && VaultBridge.addBalance(user.getPlayer(), -price)) {
                inv.remove(stack);
                user.getPlayer().getInventory().addItem(stack);
                corp.addBalance(price);
                user.sendMessage(
                        StringUtil.format(m("Corporation.Sign.Bought"), stack.getAmount(), stack.getType().toString(),
                                          price));
                int newAmount = 0;
                for (ItemStack invStack : chest.getInventory().getContents()) {
                    if (invStack != null && invStack.getType() == stack.getType()) {
                        newAmount += invStack.getAmount();
                    }
                }

                CorpTradesRow row = new CorpTradesRow();
                row.id = null;
                row.corpId = corp.getId();
                row.location = sign.getLocation();
                row.material = stack.getType();
                row.newAmount = newAmount;
                row.price = price;
                row.soldAmount = stackamount;
                row.signAmount = amount;
                row.time = System.currentTimeMillis();
                row.type = 1;
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
