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

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.database.table.CorpUsersTable;
import de.static_interface.reallifeplugin.database.table.CorpsTable;
import de.static_interface.reallifeplugin.database.table.row.CorpRow;
import de.static_interface.reallifeplugin.database.table.row.CorpUserRow;
import de.static_interface.sinklibrary.util.BukkitUtil;
import de.static_interface.sinklibrary.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Corporation {

    private final int id;
    private final Database db;

    private CorpsTable corpsTable;
    private CorpUsersTable corpUsersTable;

    public Corporation(Database db, int id) {
        this.id = id;
        this.db = db;

        corpsTable = db.getCorpsTable();
        corpUsersTable = db.getCorpUsersTable();
    }

    public List<UUID> getCoCEOs() {
        List<UUID> tmp = new ArrayList<>();
        ResultSet resultSet;
        try {
            resultSet = db.getCorpUsersTable().executeQuery("SELECT * FROM `{TABLE}` WHERE `corp_id`=? AND `isCoCeo`=1",
                                                            id);
            while (resultSet.next()) {
                tmp.add(UUID.fromString(resultSet.getString("uuid")));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return tmp;
    }

    public void setBase(World world, String regionId) {
        try {
            corpsTable.executeQuery("UPDATE `{TABLE}` SET `base_id`=?, `base_world`=? WHERE `id`=?", regionId, world.getName(), id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getName() {
        return getBase().corpName;
    }

    public void addMember(UUID uuid) {
        removeMember(uuid);
        if (getBaseRegion() != null) {
            DefaultDomain rgMembers = getBaseRegion().getMembers();
            rgMembers.addPlayer(BukkitUtil.getNameByUniqueId(uuid));
            getBaseRegion().setMembers(rgMembers);
        }
    }

    public void removeMember(UUID uuid) {
        resetUser(uuid);
        if (getBaseRegion() != null && getBaseRegion().getMembers() != null) {
            DefaultDomain rgMembers = getBaseRegion().getMembers();
            rgMembers.removePlayer(BukkitUtil.getNameByUniqueId(uuid));
            getBaseRegion().setMembers(rgMembers);
        }
    }

    public void resetUser(UUID uuid) {
        removeCoCeo(uuid);
        setRank(uuid, CorporationRanks.RANK_DEFAULT);
    }

    public double getBalance() {
        return getBase().balance;
    }

    private CorpRow getBase() {
        try {
            return db.getCorpsTable().get("SELECT * FROM `{TABLE}` WHERE `id`=?", id)[0];
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean addBalance(double amount) {
        return addBalance(amount, true);
    }

    public boolean addBalance(double amount, boolean checkAmount) {
        if (checkAmount && getBalance() < amount) {
            return false;
        }

        double newAmount = getBalance() + amount;

        try {
            corpsTable.executeQuery("UPDATE `{TABLE}` SET `balance`=? WHERE `id`=?", newAmount, id);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public ProtectedRegion getBaseRegion() {
        return ReallifeMain.getInstance().getWorldGuardPlugin().getRegionManager(Bukkit.getWorld(getBase().base_world)).getRegion(getBase().base_id);
    }

    public String getFormattedName() {
        return ChatColor.DARK_GREEN + getName().replace("_", " ") + ChatColor.RESET;
    }

    public Set<UUID> getMembers() {
        Set<UUID> members = getAllMembers();
        for (UUID member : members) {
            if (isCeo(member) || isCoCeo(member)) {
                members.remove(member);
            }
        }
        return members;
    }

    public Set<UUID> getAllMembers() {
        Set<UUID> members = new HashSet<>();
        try {
            CorpUserRow[] result = db.getCorpUsersTable().get("SELECT `uuid` from `{TABLE}` WHERE `corp_id`=?", id);
            for (CorpUserRow row : result) {
                members.add(row.uuid);
            }
            return members;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public UUID getCEO() {
        return getBase().ceo;
    }

    public void setCEO(UUID uuid) {
        try {
            resetUser(uuid);
            addMember(uuid);
            corpsTable.executeQuery("UPDATE `{TABLE}` SET `ceo_uuid`=? WHERE `id`=?", uuid.toString(), id);
            setRank(uuid, CorporationRanks.RANK_CEO);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addCoCeo(UUID uuid) {
        try {
            if (!isMember(uuid)) {
                resetUser(uuid);
            }
            addMember(uuid);
            corpUsersTable.executeQuery("UPDATE `{TABLE}` SET `isCoCeo`=1 WHERE `uuid`=?", uuid.toString());
            setRank(uuid, CorporationRanks.RANK_CO_CEO);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isMember(UUID uuid) {
        return getMembers().contains(uuid);
    }

    public void removeCoCeo(UUID uuid) {
        try {
            corpUsersTable.executeQuery("UPDATE `{TABLE}` SET `isCoCeo`=0 WHERE `uuid`=?", uuid.toString());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        setRank(uuid, CorporationRanks.RANK_DEFAULT);
    }

    public boolean isCoCeo(UUID uuid) {
        return getCoCEOs().contains(uuid);
    }

    public void setRank(UUID uuid, String rank) {
        rank = ChatColor.translateAlternateColorCodes('&', rank);

        if (!rank.startsWith(ChatColor.COLOR_CHAR + "")) {
            rank = ChatColor.GOLD + rank;
        }

        try {
            corpUsersTable.executeQuery("UPDATE `{TABLE}` SET `rank`=? WHERE `uuid`=?", rank, uuid.toString());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getRank(UUID uuid) {
        try {
            CorpUserRow[] rows = db.getCorpUsersTable().get("SELECT `rank` FROM `{TABLE}` WHERE `uuid`=?", uuid.toString());
            String rank = rows[0].rank;

            if (StringUtil.isEmptyOrNull(rank)) {
                if (getCEO() == uuid) {
                    setRank(uuid, CorporationRanks.RANK_CEO);
                } else if (isCoCeo(uuid)) {
                    setRank(uuid, CorporationRanks.RANK_CO_CEO);
                } else {
                    setRank(uuid, CorporationRanks.RANK_DEFAULT);
                }
                return getRank(uuid);
            }

            return rank;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isCeo(UUID uniqueId) {
        return getCEO().equals(uniqueId);
    }

    public final int getId() {
        return id;
    }
}
