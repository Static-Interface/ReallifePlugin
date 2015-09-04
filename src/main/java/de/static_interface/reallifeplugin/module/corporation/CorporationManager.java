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

import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.BukkitUtil;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.corporation.database.row.CorpRank;
import de.static_interface.reallifeplugin.module.corporation.database.row.CorpRow;
import de.static_interface.reallifeplugin.module.corporation.database.row.CorpUserRow;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpRankPermissionsTable;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpRanksTable;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpUsersTable;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpsTable;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.user.SinkUser;
import de.static_interface.sinklibrary.database.permission.Permission;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.StringUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

public class CorporationManager {

    public static final BlockFace[]
            SIGN_FACES = {BlockFace.SELF, BlockFace.DOWN, BlockFace.UP, BlockFace.EAST, BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH};
    private static CorporationManager instance;
    private final CorporationModule module;

    private CorporationManager(CorporationModule module) {
        this.module = module;
    }

    static void init(CorporationModule module) {
        instance = new CorporationManager(module);
    }

    public static CorporationManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Instance equals null. init() was not called yet");
        }
        return instance;
    }

    public static void unload() {
        instance = null;
    }

    @Nullable
    public Corporation getUserCorporation(IngameUser user) {
        if (user == null) {
            return null;
        }

        CorpUserRow row = getCorpUser(user);
        if (row == null) {
            return null;
        }
        Integer corpId = row.corpId;
        if (corpId == null) {
            return null;
        }
        return getCorporation(corpId);
    }

    /**
     * Returns the internal database ID of an user
     * @param user The user whichs id will returned
     * @return Id of the duser
     */
    @Nullable
    public Integer getUserId(IngameUser user) {
        CorpUserRow[] rows = module.getTable(CorpUsersTable.class).get("SELECT * FROM `{TABLE}` WHERE `uuid`=?", user.getUniqueId().toString());
        if (rows.length > 0) {
            return rows[0].id;
        }

        CorpUserRow row = new CorpUserRow();
        row.corpId = null;
        row.corpRank = null;
        row.uuid = user.getUniqueId().toString();
        return module.getTable(CorpUsersTable.class).insert(row).id;
    }

    public CorpUserRow getCorpUser(int userId) {
        return module.getTable(CorpUsersTable.class).get("SELECT * FROM `{TABLE}` WHERE `id`=?", userId)[0];
    }

    public CorpUserRow getCorpUser(IngameUser user) {
        int id = getUserId(user);
        CorpUserRow[]
                rows =
                module.getTable(CorpUsersTable.class).get("SELECT * FROM `{TABLE}` WHERE `id`=?", id);
        return rows[0];
    }

    public boolean hasEntry(CorporationModule module, IngameUser user) {
        return module.getTable(CorpUsersTable.class)
                       .get("SELECT * FROM `{TABLE}` WHERE `uuid`=?", user.getUniqueId().toString()).length > 0;
    }

    public String getFormattedName(IngameUser user) {
        String name =
                ChatColor.stripColor(user.getDisplayName() == null ? user.getName() : user.getDisplayName());

        if (StringUtil.isEmptyOrNull(name) || name.equals("null")) {
            return null;
        }

        Corporation corp = getUserCorporation(user);
        if (corp == null) {
            return ChatColor.GOLD + name;
        }
        CorpRank rank = corp.getRank(user);
        if (ChatColor.stripColor(rank.prefix).isEmpty()) {
            return rank.prefix + name;
        }
        return rank.prefix + " " + name;
    }

    @Nullable
    public Corporation getCorporation(String name) {
        return getCorporation(name, false);
    }

    @Nullable
    public Corporation getCorporation(String name, boolean exact) {
        for (Corporation corporation : getCorporations()) {
            if (corporation.getName().equalsIgnoreCase(name) || (!exact && corporation.getName().startsWith(name))) {
                return corporation;
            }
        }

        return null;
    }

    @Nullable
    public Corporation getCorporation(Location location) {
        for (Corporation corporation : getCorporations()) {
            ProtectedRegion region = corporation.getBaseRegion();
            if (region == null) {
                continue;
            }
            Vector vec = BukkitUtil.toVector(location);
            if (region.contains(vec)) {
                return corporation;
            }
        }
        return null;
    }

    @Nullable
    public Corporation getCorporation(int id) {
        for (Corporation corporation : getCorporations()) {
            if (corporation.getId() == id) {
                return corporation;
            }
        }
        return null;
    }

    @Nullable
    public Chest findConnectedChest(Block block) {
        for (BlockFace bf : SIGN_FACES) {
            Block faceBlock = block.getRelative(bf);
            if (faceBlock.getState() instanceof Chest) {
                return (Chest) faceBlock.getState();
            }
        }
        return null;
    }

    public boolean canAddItemStack(Inventory inv) {
        for (ItemStack item : inv.getContents()) {
            if (item == null) {
                return true;
            }
        }
        return false;
    }

    public boolean isValidCorporationName(String name) {
        name = name.trim();
        return name.length() <= 35 && !(name.contains("&") || name.equalsIgnoreCase("ceo") || name.equalsIgnoreCase("admin") || name
                .equalsIgnoreCase("help") || name.equalsIgnoreCase("?") || name
                                                .contains(" ")
                                        || name.contains("§") || name.equalsIgnoreCase("deposit") || name.equalsIgnoreCase("user") || name
                                                .equalsIgnoreCase("list") || name
                                                .equalsIgnoreCase("leave"));
    }

    public boolean createCorporation(CorporationModule module, @Nullable SinkUser user, String name, String username, String base,
                                     World world) {
        if (!isValidCorporationName(name)) {
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

        if (username == null) {
            return false;
        }

        IngameUser ceo = SinkLibrary.getInstance().getIngameUser(username, true);

        CorpRow row = new CorpRow();
        row.baseId = base;
        row.base_world = world.getName();
        row.ceoUuid = ceo.getUniqueId().toString();
        row.corpName = name;
        row.time = System.currentTimeMillis();
        row = module.getTable(CorpsTable.class).insert(row);

        Corporation corp = getCorporation(row.id);

        //We need an initial CEO rank
        CorpRank ceoRank = new CorpRank();
        ceoRank.name = "CEO";
        ceoRank.prefix = ChatColor.DARK_RED.toString();
        ceoRank.description = "CEO of " + row.corpName;
        ceoRank.priority = 0;
        ceoRank.corpId = row.id;

        ceoRank = module.getTable(CorpRanksTable.class).insert(ceoRank);

        // Lets add some permission to this rank
        setPermission(ceoRank, CorporationPermissions.ALL, true);

        //We also need a default rank with default values
        CorpRank defaultRank = new CorpRank();
        defaultRank.name = "Member";
        defaultRank.prefix = ChatColor.GOLD.toString();
        defaultRank.description = "Member of this corporation";
        defaultRank.priority = 10;
        defaultRank.corpId = row.id;
        defaultRank = module.getTable(CorpRanksTable.class).insert(defaultRank);

        //Lets set it as default rank for new members
        corp.setOption(CorporationOptions.DEFAULT_RANK, defaultRank.id);

        corp.addMember(ceo, ceoRank);
        return true;
    }

    public void setPermission(CorpRank rank, Permission permission, Object value) {
        CorpRankPermissionsTable permsTable = module.getTable(CorpRankPermissionsTable.class);
        permsTable.setOption(permission.getPermissionString(), value, rank.id);
    }

    public boolean hasCorpPermission(IngameUser user, Permission permission) {
        Corporation corp = getUserCorporation(user);
        if (corp == null) {
            return false;
        }

        CorpRank rank = corp.getRank(user);
        return hasCorpPermission(rank, permission);
    }

    public boolean hasCorpPermission(CorpRank rank, Permission permission) {
        if (permission != CorporationPermissions.ALL && permission.includeInAllPermission() && hasCorpPermission(rank, CorporationPermissions.ALL)) {
            return true;
        }

        CorpRankPermissionsTable permsTable = module.getTable(CorpRankPermissionsTable.class);

        if (permission.getValueType() != boolean.class && permission.getValueType() != Boolean.class) {
            return permsTable.getOption(permission.getPermissionString(), rank.id, Object.class, null) != null;
        }

        return permsTable.getOption(permission.getPermissionString(), rank.id, boolean.class, false);
    }

    public boolean deleteCorporation(SinkUser user, Corporation corporation) {
        if (corporation == null) {
            user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), ""));
            return false;
        }

        module.getTable(CorpsTable.class).executeUpdate("DELETE FROM `{TABLE}` WHERE `id`=?", corporation.getId());

        return true;
    }

    public Collection<Corporation> getCorporations() {
        if (!Module.isEnabled(CorporationModule.NAME) || module == null || module.getDatabase() == null) {
            return new ArrayList<>();
        }

        CorpRow[] rows = module.getTable(CorpsTable.class).get("SELECT * FROM `{TABLE}`");
        List<Corporation> corporations = new ArrayList<>();
        for (CorpRow row : rows) {
            Corporation corp = new Corporation(module, row.id);
            corporations.add(corp);
        }

        return corporations;
    }

    public boolean renameCorporation(@Nullable SinkUser user, Corporation corp, String newName) {
        if (!isValidCorporationName(newName)) {
            if (user != null) {
                user.sendMessage(m("Corporation.InvalidName"));
            }
            return false;
        }

        module.getTable(CorpsTable.class).executeUpdate("UPDATE `{TABLE}` SET `corp_name` = ? WHERE id = ?", newName, corp.getId());

        return true;
    }
}
