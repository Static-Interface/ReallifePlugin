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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.BukkitUtil;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.corporation.database.row.CorpRow;
import de.static_interface.reallifeplugin.module.corporation.database.row.CorpUserRow;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpUsersTable;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpsTable;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.configuration.Configuration;
import de.static_interface.sinklibrary.api.user.SinkUser;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.StringUtil;
import de.static_interface.sinklibrary.util.VaultBridge;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CorporationUtil {

    public static final BlockFace[]
            SIGN_FACES = {BlockFace.SELF, BlockFace.DOWN, BlockFace.UP, BlockFace.EAST, BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH};

    @Nullable
    public static Corporation getUserCorporation(CorporationModule module, IngameUser user) {
        if (user == null) {
            return null;
        }

        for (Corporation corporation : getCorporations(module)) {
            for (IngameUser member : corporation.getMembers(false)) {
                if (member == null) {
                    continue;
                }
                if (member.getUniqueId().equals(user.getUniqueId())) {
                    return corporation;
                }
            }
        }
        return null;
    }

    /**
     * Returns the internal database ID of an user
     * @param user The user whichs id will returned
     * @return id of the user if the user is registered, null if no entrys were found or the user never joined a corporation
     */
    @Nullable
    public static Integer getUserId(CorporationModule module, IngameUser user) {
        try {
            return module.getTable(CorpUsersTable.class).get("SELECT * FROM `{TABLE}` WHERE `uuid`=?", user.getUniqueId().toString())[0].id;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static CorpUserRow getCorpUser(CorporationModule module, int userId) {
        try {
            return module.getTable(CorpUsersTable.class).get("SELECT * FROM `{TABLE}` WHERE `id`=?", userId)[0];
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public static CorpUserRow getCorpUser(CorporationModule module, IngameUser user) {
        try {
            CorpUserRow[]
                    rows =
                    module.getTable(CorpUsersTable.class).get("SELECT * FROM `{TABLE}` WHERE `uuid`=?", user.getUniqueId().toString());
            if (rows.length < 1) {
                return null;
            }
            return rows[0];
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean hasEntry(CorporationModule module, IngameUser user) {
        try {
            return module.getTable(CorpUsersTable.class)
                           .get("SELECT * FROM `{TABLE}` WHERE `uuid`=?", user.getUniqueId().toString()).length > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static CorpUserRow insertUser(CorporationModule module, IngameUser user, @Nullable String rank) {
        if (hasEntry(module, user)) {
            return getCorpUser(module, user);
        }

        if (rank == null) {
            rank = CorporationRanks.RANK_DEFAULT;
        }
        try {
            CorpUserRow row = new CorpUserRow();
            row.id = null;
            row.corpId = null;
            row.isCoCeo = false;
            row.rank = rank;
            row.uuid = user.getUniqueId().toString();

            return module.getTable(CorpUsersTable.class).insert(row);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getFormattedName(CorporationModule module, IngameUser user) {
        String name =
                ChatColor.stripColor(user.getDisplayName() == null ? user.getName() : user.getDisplayName());

        if (StringUtil.isEmptyOrNull(name) || name.equals("null")) {
            return null;
        }

        Corporation corp = getUserCorporation(module, user);
        if (corp == null) {
            return ChatColor.GOLD + name;
        }
        String rank = corp.getRank(user);
        if (ChatColor.stripColor(rank).isEmpty()) {
            return rank + name;
        }
        return rank + " " + name;
    }

    @Nullable
    public static Corporation getCorporation(CorporationModule module, String name) {
        for (Corporation corporation : getCorporations(module)) {
            if (corporation.getName().equalsIgnoreCase(name)) {
                return corporation;
            }
        }

        return null;
    }

    @Nullable
    public static Corporation getCorporation(CorporationModule module, Location location) {
        for (Corporation corporation : getCorporations(module)) {
            ProtectedRegion region = corporation.getBaseRegion();
            Vector vec = BukkitUtil.toVector(location);
            if (region.contains(vec)) {
                return corporation;
            }
        }
        return null;
    }

    @Nullable
    public static Corporation getCorporation(CorporationModule module, int id) {
        for (Corporation corporation : getCorporations(module)) {
            if (corporation.getId() == id) {
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
        return corporation != null && (corporation.getCEO().equals(user))
               || corporation.getCoCEOs().contains(user);
    }

    public static boolean isValidCorporationName(String name) {
        name = name.trim();
        return name.length() <= 35 && !(name.contains("&") || name.equalsIgnoreCase("ceo") || name.equalsIgnoreCase("admin") || name
                .equalsIgnoreCase("help") || name.equalsIgnoreCase("?") || name
                .contains(" ")
                                        || name.contains("§") || name.equalsIgnoreCase("deposit") || name.equalsIgnoreCase("user") || name
                .equalsIgnoreCase("list") || name
                                                .equalsIgnoreCase("leave"));
    }

    public static boolean createCorporation(CorporationModule module, @Nullable SinkUser user, String name, String username, String base,
                                            World world) {
        if (!isValidCorporationName(name)) {
            if (user != null) {
                user.sendMessage(m("Corporation.InvalidName"));
            }
            return false;
        }

        if (getCorporation(module, name) != null) {
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
        row.isDeleted = false;

        try {
            insertUser(module, ceo, CorporationRanks.RANK_CEO);
            module.getTable(CorpsTable.class).insert(row);
        } catch (Exception e) {
            if (user != null) {
                user.sendMessage("Error: " + e.getMessage());
            }
            throw new RuntimeException(e);
        }

        getCorporation(module, name).setCEO(ceo);
        return true;
    }

    public static boolean migrate(CorporationModule module, SinkUser user) {
        Configuration
                config =
                new Configuration(new File(Bukkit.getPluginManager().getPlugin("ReallifePlugin").getDataFolder(), "Corporations.yml"), true) {
                    @Override
                    public void addDefaults() {
                    }
                };

        if (!config.exists()) {
            return false;
        }
        YamlConfiguration yconfig = config.getYamlConfiguration();
        ConfigurationSection section = yconfig.getConfigurationSection("Corporations");
        if (section == null) {
            return false;
        }
        for (String corpName : section.getKeys(false)) {
            try {
                if (getCorporation(module, corpName) != null) {
                    continue;
                }

                user.sendMessage(ChatColor.DARK_GREEN + "Migrating: " + corpName);
                double balance = VaultBridge.getBalance("Corp_" + corpName.replace("_", ""));

                List<UUID> addedUsers = new ArrayList<>();
                UUID ceo = Bukkit.getOfflinePlayer(
                        UUID.fromString((String) config.get("Corporations." + corpName + ".CEO"))).getUniqueId();
                addedUsers.add(ceo);

                String[] baseraw = ((String) config.get("Corporations." + corpName + ".Base")).split(":");

                World world = Bukkit.getWorld(baseraw[0]);
                String regionId = baseraw[1];

                ProtectedRegion region = ReallifeMain.getInstance().getWorldGuardPlugin().getRegionManager(world).getRegion(regionId);
                region.setMembers(new DefaultDomain());

                createCorporation(module, null, corpName, Bukkit.getOfflinePlayer(ceo).getName(), regionId, world);
                Corporation corp = getCorporation(module, corpName);

                module.getTable(CorpsTable.class).executeUpdate("UPDATE `{TABLE}` SET `balance`=? WHERE `id`=?", balance, corp.getId());

                for (String s : config.getYamlConfiguration().getStringList("Corporations." + corpName + ".CoCEOs")) {
                    IngameUser coceo = SinkLibrary.getInstance().getIngameUser(UUID.fromString(s));
                    if (addedUsers.contains(coceo.getUniqueId())) {
                        continue;
                    }
                    corp.addCoCeo(coceo);
                    addedUsers.add(coceo.getUniqueId());
                }

                Gson gson = new Gson();

                String json = (String) config.get("Corporations." + corpName + ".Members");
                Type typetoken = new TypeToken<HashMap<String, String>>() {
                }.getType();
                HashMap<String, String> map = gson.fromJson(json, typetoken);

                for (String id : map.keySet()) {
                    IngameUser member;
                    try {
                        member = SinkLibrary.getInstance().getIngameUser(UUID.fromString(id));
                    } catch (Exception e) {
                        continue;
                    }

                    if (member == null || !member.hasPlayedBefore()) {
                        continue;
                    }

                    if (addedUsers.contains(member.getUniqueId())) {
                        continue;
                    }
                    corp.addMember(member);
                    corp.setRank(member, map.get(id));
                    addedUsers.add(member.getUniqueId());
                }
            } catch (Exception e) {
                e.printStackTrace();
                user.sendMessage(ChatColor.DARK_RED + "Migrating failed: " + corpName);
            }
        }

        return true;
    }

    public static boolean deleteCorporation(CorporationModule module, SinkUser user, Corporation corporation) {
        if (corporation == null) {
            user.sendMessage(StringUtil.format(m("Corporation.DoesntExists"), ""));
            return false;
        }

        try {
            module.getTable(CorpsTable.class).executeUpdate("UPDATE `{TABLE}` SET `isdeleted`=1 WHERE `id`=?", corporation.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    public static void sendMessage(Corporation corp, String message) {
        message = ChatColor.GRAY + "[" + corp.getFormattedName() + ChatColor.GRAY + "] " + message;
        for (IngameUser user : corp.getMembers(false)) {
            Player p = Bukkit.getPlayer(user.getUniqueId());
            if (p == null) {
                continue;
            }
            p.sendMessage(message);
        }

        SinkLibrary.getInstance().getConsoleUser().sendMessage(message);
    }

    public static Collection<Corporation> getCorporations(CorporationModule module) {
        if (!Module.isEnabled(CorporationModule.NAME) || module == null || module.getDatabase() == null) {
            return new ArrayList<>();
        }

        CorpRow[] rows;
        try {
            rows = module.getTable(CorpsTable.class).get("SELECT * FROM `{TABLE}` WHERE `isdeleted`=0");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        List<Corporation> corporations = new ArrayList<>();
        for (CorpRow row : rows) {
            Corporation corp = new Corporation(module, row.id);
            corporations.add(corp);
        }

        return corporations;
    }

    public static boolean renameCorporation(@Nonnull CorporationModule module, @Nullable SinkUser user, Corporation corp, String newName) {
        if (!isValidCorporationName(newName)) {
            if (user != null) {
                user.sendMessage(m("Corporation.InvalidName"));
            }
            return false;
        }

        try {
            module.getTable(CorpsTable.class).executeUpdate("UPDATE `{TABLE}` SET `corp_name` = ? WHERE id = ?", newName, corp.getId());
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
