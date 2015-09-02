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

package de.static_interface.reallifeplugin.module.corporation;

import static de.static_interface.reallifeplugin.config.ReallifeLanguageConfiguration.m;

import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.module.ModuleListener;
import de.static_interface.reallifeplugin.module.corporation.database.row.CorpTradesRow;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpTradesTable;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.database.permission.Permission;
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
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CorporationListener extends ModuleListener<CorporationModule> {

    // http://hastebin.com/ripetuxafe.avrasm
    public static final String CURRENCY = "€"; //TODO

    public CorporationListener(CorporationModule module) {
        super(module);
    }

    private static boolean isCorpTradesSign(String[] lines) {
        return ChatColor.stripColor(lines[0]).equals("[CBuy]") ||
               ChatColor.stripColor(lines[0]).equals("[CSell]");
    }

    private static ItemStack getItemStack(String materialName, int amount) {
        Map.Entry<String, Short> entry = getItemIds(materialName);
        Material material = Material.matchMaterial(unformatItemName(entry.getKey()));
        ItemStack stack = new ItemStack(material, amount, entry.getValue());
        if (material == null || stack == null) {
            throw new NullPointerException("Unknown material: " + materialName);
        }
        return stack;
    }

    public static String formatItemName(String s) {
        s = s.replace("_", " ");
        String tmp = "";
        for (String word : s.split(" ")) {
            word = Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();

            if (tmp.equals("")) {
                tmp = word;
                continue;
            }
            tmp += " " + word;
        }

        return tmp;
    }

    public static String unformatItemName(String s) {
        s = s.replace(" ", "_");
        return s.toUpperCase();
    }

    public static Map.Entry<String, Short> getItemIds(String s) {
        String base = formatItemName(s.split(":")[0]);
        String sub = s.split(":").length > 1 ? s.split(":")[1] : null;

        Integer baseId;
        try {
            baseId = Integer.parseInt(base);
            base = formatItemName(Material.getMaterial(baseId).name());
        } catch (NumberFormatException ignored) {

        }

        Short subId = 0;
        if (sub != null) {
            try {
                subId = Short.parseShort(sub);
            } catch (NumberFormatException ignored) {

            }
        }

        final String finalBase = base;
        final Short finalSubId = subId;

        if (Material.matchMaterial(unformatItemName(base)) == null) {
            throw new NullPointerException();
        }

        return new Map.Entry<String, Short>() {
            @Override
            public String getKey() {
                return finalBase;
            }

            @Override
            public Short getValue() {
                return finalSubId;
            }

            @Override
            public Short setValue(Short value) {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static boolean equalsItemStack(ItemStack stack, String s) {
        Map.Entry<String, Short> entry = getItemIds(s);
        ItemStack sStack = new ItemStack(Material.matchMaterial(unformatItemName(entry.getKey())), stack.getAmount(), entry.getValue());
        return sStack.equals(stack);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public static void onBlockPlace(BlockPlaceEvent event) {
        ItemStack stack = event.getPlayer().getItemInHand();
        if (stack != null && isTagged(stack)) {
            event.setCancelled(true);
        }

        if (isInteractionRestricted(SinkLibrary.getInstance().getIngameUser(event.getPlayer()), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public static void untagListener(PlayerInteractEvent event) {
        IngameUser user = SinkLibrary.getInstance().getIngameUser(event.getPlayer());

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
        if (!sign.getLine(1).trim().equals(ChatColor.BLUE + "[CUntag]")) {
            return;
        }

        for (ItemStack stack : event.getPlayer().getInventory()) {
            untag(stack);
        }

        user.sendMessage(m("General.Success"));
    }

    public static boolean removeItem(@Nonnull Inventory inv, @Nonnull ItemStack stack, boolean preferNotSold) {
        boolean soldFound = false;
        ItemStack foundStack = null;

        for (ItemStack invStack : inv.getContents()) {
            if (invStack == null) {
                continue;
            }

            if (invStack.getType() != stack.getType()) {
                continue;
            }

            soldFound = isTagged(invStack);

            if (soldFound && preferNotSold) {
                continue;
            }

            if (foundStack != null && foundStack.getAmount() == stack.getAmount()) {
                continue;
            }

            foundStack = invStack;
        }

        if (foundStack != null) {
            inv.removeItem(foundStack);
            return true;
        }

        if (soldFound && preferNotSold) {
            throw new RuntimeException();
        }

        return false;
    }

    public static void tag(ItemStack stack) {
        List<String> lore = new ArrayList<>();
        ItemMeta meta = stack.getItemMeta();
        if (meta.hasLore()) {
            lore = meta.getLore();
        }

        lore.add(m("Corporation.Sign.SoldWatermark"));
        meta.setLore(lore);
        stack.setItemMeta(meta);
    }

    public static void untag(ItemStack stack) {
        if (!isTagged(stack)) {
            return;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        List<String> lore = meta.getLore();
        if (lore == null) {
            return;
        }
        lore.remove(m("Corporation.Sign.SoldWatermark"));
        meta.setLore(lore);
        stack.setItemMeta(meta);
    }

    public static boolean isTagged(ItemStack stack) {
        return stack != null && stack.getItemMeta().hasLore() && stack.getItemMeta().getLore().contains(m("Corporation.Sign.SoldWatermark"));
    }

    @EventHandler
    public static void onBlockBreak(BlockBreakEvent event) {
        if (isInteractionRestricted(SinkLibrary.getInstance().getIngameUser(event.getPlayer()), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    public static boolean isInteractionRestricted(IngameUser user, Block block) {
        if (true) {
            return false;
        }

        Corporation corp = CorporationManager.getInstance().getCorporation(block.getLocation());
        if (corp == null) {
            return false;
        }

        if (user.hasPermission("reallifeplugin.corporations.admin")) {
            return false;
        }

        if (!corp.isMember(user)) {
            return true;
        }

        if (CorporationManager.getInstance().hasCorpPermission(user, CorporationPermissions.REGION_OWNER)) {
            return false;
        }

        List<String> restrictedBlocks = corp.getOption(CorporationOptions.RESTRICTED_BLOCKS, ArrayList.class, new ArrayList());
        List<String> allowedBlocks = corp.getOption(CorporationOptions.ALLOWED_BLOCKS, ArrayList.class, new ArrayList());

        Material m = block.getType();

        if (restrictedBlocks.isEmpty()) {
            return false;
        }

        if (restrictedBlocks.contains("ALL") && !allowedBlocks.contains(m.name().toUpperCase())) {
            return true;
        }

        return restrictedBlocks.contains(m.name().toUpperCase());

    }

    @Nullable
    public ItemStack getItem(@Nonnull Inventory inv, String itemName, int amount, boolean preferNotSold) {
        int stackamount = 0;
        ItemStack stack = null;
        ItemStack soldItemStack = null;

        for (ItemStack invStack : inv.getContents()) {
            if (invStack == null) {
                continue;
            }

            if (invStack.getAmount() <= amount && stack != null && stack.getAmount() < invStack.getAmount()) {
                continue;
            }

            if (equalsItemStack(invStack, itemName) && invStack.getAmount() >= stackamount) {
                if (preferNotSold && isTagged(invStack)) {
                    soldItemStack = invStack;
                    continue;
                }

                stack = invStack;
            }
        }

        if (stack == null && soldItemStack != null) {
            return soldItemStack;
        }
        return stack;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private boolean validateSign(SignChangeEvent event, String[] lines, Location location, IngameUser user) {
        Corporation corp = CorporationManager.getInstance().getUserCorporation(user);
        if (corp == null) {
            user.sendMessage(m("Corporation.NotInCorporation"));
            return false;
        }

        Permission perm;
        if (ChatColor.stripColor(lines[0]).equals("[CBuy]")) {
            perm = CorporationPermissions.CBUY_SIGN;
        } else if (ChatColor.stripColor(lines[0]).equals("[CSell]")) {
            perm = CorporationPermissions.CSELL_SIGN;
        } else {
            throw new IllegalStateException("This shouldn't happen");
        }

        if (!CorporationManager.getInstance().hasCorpPermission(user, perm)) {
            user.sendMessage(m("Permissions.General"));
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
                Double.parseDouble(lines[2].split(" ")[0]);
            } catch (NumberFormatException e) {
                user.sendMessage(ChatColor.RED + "Invalid price: " + lines[2]);
                return false;
            }

            if (amount > 64 || amount < 1) {  //Invalid stack_amount ( stack_amount must be [1,64) )
                throw new NumberFormatException();
            }

            //Check if Material is valid
            try {
                String material_name = lines[3];
                getItemStack(material_name, amount);
                Map.Entry<String, Short> idData = getItemIds(material_name);
                String s = idData.getKey() + ":" + idData.getValue();
                if (idData.getValue() == 0) {
                    s = idData.getKey();
                }
                event.setLine(3, s);
            } catch (Exception e) {
                user.sendMessage(ChatColor.RED + "Unknown item or block: " + lines[3]);
                return false;
            }

            //Validate location
            Corporation locationCorporation = CorporationManager.getInstance().getCorporation(location);

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

    @EventHandler
    public void onSignChangeCorpTrades(SignChangeEvent event) {
        Block signBlock = event.getBlock();
        String[] lines = event.getLines();
        event.setLine(1, lines[1].trim());
        event.setLine(3, lines[3].trim());

        IngameUser user = SinkLibrary.getInstance().getIngameUser(event.getPlayer());
        if (!(signBlock.getState() instanceof Sign)) {
            return;
        }

        if (!isCorpTradesSign(lines)) {
            return;
        }

        event.setLine(2, lines[2].split(" ")[0] + " " + CURRENCY);

        lines = event.getLines();

        if (!validateSign(event, lines, signBlock.getLocation(), user)) {
            event.setLine(0, ChatColor.DARK_RED + ChatColor.stripColor(lines[0]));
            return;
        }

        Chest chest = CorporationManager.getInstance().findConnectedChest(signBlock);
        if (chest == null) {
            user.sendMessage(m("Corporation.Sign.InvalidChest"));
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

            //Todo
            if (!sign.getLine(2).endsWith("€")) {
                sign.setLine(2, sign.getLine(2).split(" ")[0] + " " + CURRENCY);
            }

            if (!sign.getLine(3).contains(":") && sign.getLine(3).toUpperCase().equals(sign.getLine(3))) {
                Material material = Material.matchMaterial(unformatItemName(sign.getLine(3)));
                sign.setLine(3, formatItemName(material.name()));
            }

            sign.update(true);

            Corporation corp = CorporationManager.getInstance().getCorporation(event.getClickedBlock().getLocation());
            if (corp == null) {
                user.sendMessage(ChatColor.DARK_RED + "Invalid Sign"); //Todo
                sign.setLine(0, ChatColor.DARK_RED + "[CSell]");
                return;
            }

            Corporation userCorp = CorporationManager.getInstance().getUserCorporation(user);

            if (userCorp == null) {
                user.sendMessage(m("Corporation.NotInCorporation"));
                return;
            }

            if (userCorp.getId() != corp.getId()) {
                user.sendMessage(CorporationManager.getInstance().getUserCorporation(user).getName() + ":" + corp.getName());
                user.sendMessage(m("Corporation.Sign.InvalidSellCorporation"));
                return;
            }

            if (CorporationManager.getInstance().hasCorpPermission(user, CorporationPermissions.CSELL_SIGN)) {
                user.sendMessage(m("Corporation.Sign.CantSell"));
                return;
            }

            int amount = Integer.valueOf(sign.getLine(1));
            double price = Double.valueOf(sign.getLine(2).split(" ")[0]);

            double pricePerItem = price / (double) amount;

            Inventory inv = event.getPlayer().getInventory();
            ItemStack stack = getItem(inv, sign.getLine(3), amount, true);

            if (stack == null) {
                user.sendMessage(m("Corporation.Sign.NoItemsFound"));
                return;
            }

            if (isTagged(stack)) {
                user.sendMessage(m("Corporation.Sign.ItemAlreadySold"));
                return;
            }

            Chest chest = CorporationManager.getInstance().findConnectedChest(sign.getBlock());
            if (chest == null) {
                user.sendMessage(m("Corporation.Sign.InvalidChest"));
                return;
            }

            if (!CorporationManager.getInstance().canAddItemStack(chest.getInventory())) {
                user.sendMessage(m("Corporation.Sign.ChestFull"));
                return;
            }

            price = pricePerItem * stack.getAmount();
            price = MathUtil.round(price);

            if (corp.getBalance() <= price) {
                user.sendMessage(m("Corporation.NotEnoughMoney"));
                return;
            }

            try {
                if (!removeItem(inv, stack, true)) {
                    user.sendMessage(m("Corporation.Sign.NoItemsFound"));
                    return;
                }
            } catch (RuntimeException e) {
                user.sendMessage(m("Corporation.Sign.ItemAlreadySold"));
                return;
            }

            user.getPlayer().updateInventory();

            chest.getInventory().addItem(stack);
            chest.update(true);

            for (ItemStack chestStack : chest.getInventory().getContents()) {
                if (chestStack.getType() == stack.getType() && chestStack.getItemMeta().equals(stack.getItemMeta())) {
                    if (!isTagged(chestStack)) {
                        tag(chestStack);
                        break;
                    }
                }
            }

            if (!corp.addBalance(-price)) {
                user.sendMessage(m("Corporation.NotEnoughMoney"));
                return;
            }

            user.addBalance(price);
            user.sendMessage(
                    StringUtil.format(m("Corporation.Sign.Sold"), stack.getAmount(), formatItemName(stack.getType().name()),
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
            row.x = sign.getLocation().getBlockX();
            row.y = sign.getLocation().getBlockY();
            row.z = sign.getLocation().getBlockZ();
            row.world = sign.getLocation().getWorld().getName();
            row.materialName = stack.getType().name();
            row.newAmount = newAmount;
            row.price = -price;
            row.changedAmount = stack.getAmount();
            row.time = System.currentTimeMillis();
            row.type = 0;
            row.userId = CorporationManager.getInstance().getUserId(user);
            getModule().getTable(CorpTradesTable.class).insert(row);
        } catch (Exception e) {
            user.sendMessage(ChatColor.DARK_RED + "Error: " + ChatColor.RED + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        IngameUser user = SinkLibrary.getInstance().getIngameUser(event.getPlayer());
        Corporation corp = CorporationManager.getInstance().getCorporation(event.getPlayer().getLocation());
        if (corp == null) {
            return;
        }

        Corporation userCorp = CorporationManager.getInstance().getUserCorporation(user);
        if (userCorp == null || userCorp.getId() != corp.getId()) {
            event.setCancelled(true);
            return;
        }

        if (!corp.getOption(CorporationOptions.FISHING, Boolean.class, true)) {
            event.setCancelled(true);
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

            if (!sign.getLine(2).endsWith(CURRENCY)) {
                sign.setLine(2, sign.getLine(2).split(" ")[0] + " " + CURRENCY);
            }
            sign.update(true);

            Corporation signCorp = CorporationManager.getInstance().getCorporation(event.getClickedBlock().getLocation());
            if (signCorp == null) {
                user.sendMessage(ChatColor.DARK_RED + "Invalid Sign"); //Todo
                sign.setLine(0, ChatColor.DARK_RED + "[CBuy]");
                return;
            }

            Corporation userCorp = CorporationManager.getInstance().getUserCorporation(user);

            if (userCorp == null) {
                user.sendMessage(m("Corporation.NotInCorporation"));
                return;
            }

            if (userCorp.getId() != signCorp.getId()) {
                user.sendMessage(m("Corporation.Sign.InvalidBuyCorporation"));
                return;
            }

            if (CorporationManager.getInstance().hasCorpPermission(user, CorporationPermissions.CBUY_SIGN)) {
                user.sendMessage(m("Corporation.Sign.CantBuy"));
                return;
            }

            int amount = Integer.valueOf(sign.getLine(1));
            double price = Double.valueOf(sign.getLine(2).split(" ")[0]);

            if (!CorporationManager.getInstance().canAddItemStack(user.getPlayer().getInventory())) {
                user.sendMessage(m("Corporation.Sign.CantPickup"));
                return;
            }

            Chest chest = CorporationManager.getInstance().findConnectedChest(sign.getBlock());
            if (chest == null) {
                user.sendMessage(m("Corporation.Sign.InvalidChest"));
                return;
            }

            double pricePerItem = price / (double) amount;

            Inventory inv = chest.getInventory();
            ItemStack stack = getItem(inv, sign.getLine(3), amount, false);

            if (stack == null) {
                user.sendMessage(m("Corporation.Sign.NoItemsFound"));
                return;
            }
            price = pricePerItem * stack.getAmount();

            price = MathUtil.round(price);

            if (VaultBridge.getBalance(user.getPlayer()) <= price) {
                user.sendMessage(m("Corporation.Sign.NotEnoughMoney"));
                return;
            }

            if (!removeItem(inv, stack, false)) {
                user.sendMessage(m("Corporation.Sign.NoItemsFound"));
                return;
            }

            if (!VaultBridge.addBalance(user.getPlayer(), -price)) {
                user.sendMessage(m("Corporation.Sign.NotEnoughMoney"));
                return;
            }

            chest.update(true);
            user.getPlayer().getInventory().addItem(stack);
            user.getPlayer().updateInventory();
            signCorp.addBalance(price);
            user.sendMessage(
                    StringUtil.format(m("Corporation.Sign.Bought"), stack.getAmount(), formatItemName(stack.getType().name()),
                                      price));
            int newAmount = 0;
            for (ItemStack invStack : chest.getInventory().getContents()) {
                if (invStack != null && invStack.getType() == stack.getType()) {
                    newAmount += invStack.getAmount();
                }
            }

            CorpTradesRow row = new CorpTradesRow();
            row.id = null;
            row.corpId = signCorp.getId();
            row.x = sign.getLocation().getBlockX();
            row.y = sign.getLocation().getBlockY();
            row.z = sign.getLocation().getBlockZ();
            row.world = sign.getLocation().getWorld().getName();
            row.materialName = stack.getType().name();
            row.newAmount = newAmount;
            row.price = price;
            row.changedAmount = -stack.getAmount();
            row.signAmount = amount;
            row.time = System.currentTimeMillis();
            row.type = 1;
            row.userId = CorporationManager.getInstance().getUserId(user);

            getModule().getTable(CorpTradesTable.class).insert(row);
        } catch (Exception e) {
            user.sendMessage(ChatColor.DARK_RED + "Error: " + ChatColor.RED + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onSignChangeUntag(SignChangeEvent event) {
        Block signBlock = event.getBlock();
        String[] lines = event.getLines();
        IngameUser user = SinkLibrary.getInstance().getIngameUser(event.getPlayer());

        if (!(signBlock.getState() instanceof Sign)) {
            return;
        }

        if (!ChatColor.stripColor(lines[1]).equals("[CUntag]")) {
            return;
        }

        if (!user.hasPermission("reallifeplugin.corporation.untag")) {
            event.setLine(1, ChatColor.DARK_RED + ChatColor.stripColor(lines[1]));
            return;
        }

        event.setLine(1, ChatColor.BLUE + ChatColor.stripColor(lines[1]));
    }
}
