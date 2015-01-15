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

package de.static_interface.reallifeplugin.corporation;

import static de.static_interface.reallifeplugin.ReallifeLanguageConfiguration.m;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.BukkitUtil;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.database.table.row.CorpRow;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.user.SinkUser;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.Debug;
import de.static_interface.sinklibrary.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

public class CorporationUtil {

    public static final BlockFace[]
            SIGN_FACES = {BlockFace.SELF, BlockFace.DOWN, BlockFace.UP, BlockFace.EAST, BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH};
    private static List<Corporation> corporations;

    public static Corporation getUserCorporation(UUID uuid) {
        for (Corporation corporation : corporations) {
            if (corporation.getAllMembers().contains(uuid)) {
                return corporation;
            }
        }
        return null;
    }

    @Nullable
    public static Integer getUserId(IngameUser user) {
        Database db = ReallifeMain.getInstance().getDB();
        try {
            return db.getCorpUsersTable().get("SELECT * FROM `{TABLE}` WHERE `uuid`=?", user.getUniqueId().toString())[0].id;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void registerCorporationsFromDatabase() {
        Database db = ReallifeMain.getInstance().getDB();
        CorpRow[] rows;
        try {
            rows = db.getCorpsTable().get("SELECT * FROM `{TABLE}` WHERE `isdeleted`=0");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        corporations = new ArrayList<>();
        for (CorpRow row : rows) {
            Debug.log("Registering corporation: " + row.corpName);
            Corporation corp = new Corporation(db, row.id);
            register(corp);
        }
    }

    public static String getFormattedName(UUID uuid) {
        IngameUser user = SinkLibrary.getInstance().getIngameUser(uuid);

        String name =
                ChatColor.stripColor(user.getDisplayName() == null ? user.getName() : user.getDisplayName());

        if (StringUtil.isEmptyOrNull(name) || name.equals("null")) {
            return null;
        }

        Corporation corp = getUserCorporation(uuid);
        if (corp == null) {
            return ChatColor.GOLD + name;
        }
        String rank = corp.getRank(uuid);
        if (ChatColor.stripColor(rank).isEmpty()) {
            return rank + name;
        }
        return rank + " " + name;
    }

    @Nullable
    public static Corporation getCorporation(String name) {
        for (Corporation corporation : corporations) {
            if (corporation.getName().equalsIgnoreCase(name)) {
                return corporation;
            }
        }

        return null;
    }

    @Nullable
    public static Corporation getCorporation(Location location) {
        for (Corporation corporation : corporations) {
            ProtectedRegion region = corporation.getBaseRegion();
            Vector vec = BukkitUtil.toVector(location);
            if (region.contains(vec)) {
                return corporation;
            }
        }
        return null;
    }

    @Nullable
    public static Chest findConnectedChest(Block block) {
        for (BlockFace bf : SIGN_FACES) {
            Block faceBlock = block.getRelative(bf);
            if (faceBlock.getState() instanceof Chest) {
                return (Chest) faceBlock.getState();
            }
        }
        return null;
    }

    public static boolean canAddItemStack(Inventory inv) {
        for (ItemStack item : inv.getContents()) {
            if (item == null) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasCeoPermissions(IngameUser user, Corporation corporation) {
        return corporation != null && (corporation.getCEO().equals(user.getUniqueId())
                                       || corporation.getCoCEOs().contains(user.getUniqueId()));
    }

    public static boolean createCorporation(@Nullable SinkUser user, String name, UUID ceo, String base, World world) {
        if (name.equalsIgnoreCase("ceo") || name.equalsIgnoreCase("admin") || name.equalsIgnoreCase("help")
            || name.equals("deposit") || name.equals("list") || name.equals("leave")) {
            if (user != null) {
                user.sendMessage(m("Corporation.InvalidName"));
            }
            return false;
        }

        if (getCorporation(name) != null) {
            if (user != null) {
                user.sendMessage(m("Corporation.Exists"));
            }
            return false;
        }

        CorpRow row = new CorpRow();
        row.base_id = base;
        row.base_world = world.getName();
        row.ceo = ceo;
        row.corpName = name;
        row.creationTime = System.currentTimeMillis();
        row.isdeleted = false;

        Database db = ReallifeMain.getInstance().getDB();
        try {
            row = db.getCorpsTable().insert(row);
        } catch (SQLException e) {
            if (user != null) {
                user.sendMessage("Error: " + e.getMessage());
            }
            throw new RuntimeException(e);
        }

        Corporation corporation = new Corporation(db, row.id);
        register(corporation);
        return true;
    }

    private static void register(Corporation corporation) {
        corporations.add(corporation);
    }

    private static void unregister(Corporation corporation) {
        corporations.remove(corporation);
    }

    public static boolean deleteCorporation(SinkUser user, Corporation corporation) {
        if (corporation == null) {
            user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), ""));
            return false;
        }

        Database db = ReallifeMain.getInstance().getDB();
        try {
            db.getCorpsTable().get("UPDATE `{TABLE}` SET `isdeleted`=1 WHERE `id`=?", corporation.getId());
            unregister(corporation);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    public static void sendMessage(Corporation corp, String message) {
        message = ChatColor.GRAY + "[" + corp.getFormattedName() + ChatColor.GRAY + "] " + message;
        for (UUID uuid : corp.getAllMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) {
                continue;
            }
            p.sendMessage(message);
        }

        SinkLibrary.getInstance().getConsoleUser().sendMessage(message);
    }

    public static List<Corporation> getCorporations() {
        return corporations;
    }
}
