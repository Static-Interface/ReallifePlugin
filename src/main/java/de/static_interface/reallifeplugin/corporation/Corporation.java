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
import de.static_interface.reallifeplugin.database.table.impl.corp.CorpUsersTable;
import de.static_interface.reallifeplugin.database.table.impl.corp.CorpsTable;
import de.static_interface.reallifeplugin.database.table.row.corp.CorpRow;
import de.static_interface.reallifeplugin.database.table.row.corp.CorpUserRow;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

public class Corporation {

    private final int id;

    private CorpsTable corpsTable;
    private CorpUsersTable corpUsersTable;

    public Corporation(Database db, int id) {
        this.id = id;

        corpsTable = db.getCorpsTable();
        corpUsersTable = db.getCorpUsersTable();
    }

    public boolean addBalance(double amount) {
        return addBalance(amount, true);
    }

    public boolean addBalance(double amount, boolean checkAmount) {
        if (amount == 0) {
            return true;
        }

        if (checkAmount && amount < 0 && getBalance() < amount) {
            return false;
        }

        double newAmount = getBalance() + amount;

        try {
            corpsTable.executeUpdate("UPDATE `{TABLE}` SET `balance`=? WHERE `id`=?", newAmount, id);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void addCoCeo(IngameUser user) {
        try {
            if (!isMember(user)) {
                resetUser(user, false);
                addMember(user);
            }
            corpUsersTable.executeUpdate("UPDATE `{TABLE}` SET `isCoCeo`=1 WHERE `uuid`=?", user.getUniqueId().toString());
            setRank(user, CorporationRanks.RANK_CO_CEO);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addMember(IngameUser user) {
        resetUser(user, true);
        CorporationUtil.insertUser(user, CorporationRanks.RANK_DEFAULT);
        try {
            corpUsersTable.executeUpdate("UPDATE `{TABLE}` SET `corp_id`=? WHERE `uuid`=?", id, user.getUniqueId().toString());
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        setRank(user, CorporationRanks.RANK_DEFAULT);

        if (getBaseRegion() != null) {
            DefaultDomain rgMembers = getBaseRegion().getMembers();
            rgMembers.addPlayer(user.getUniqueId());
            getBaseRegion().setMembers(rgMembers);
        }
    }

    public void removeMember(IngameUser user) {
        resetUser(user, false);
        setRank(user, CorporationRanks.RANK_DEFAULT);

        if (getBaseRegion() != null && getBaseRegion().getMembers() != null) {
            DefaultDomain rgMembers = getBaseRegion().getMembers();
            rgMembers.removePlayer(user.getUniqueId());
            getBaseRegion().setMembers(rgMembers);
        }
    }

    public void removeCoCeo(IngameUser user) {
        try {
            if (!isMember(user)) {
                throw new IllegalArgumentException(user.toString() + " is not a member of the corporation: " + getName() + "!");
            }
            corpUsersTable.executeUpdate("UPDATE `{TABLE}` SET `isCoCeo`=0 WHERE `uuid`=?", user.getUniqueId().toString());
            setRank(user, CorporationRanks.RANK_DEFAULT);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void resetUser(IngameUser user, boolean ignoreFailure) {
        if (isCoCeo(user)) {
            removeCoCeo(user);
        }
        setRank(user, CorporationRanks.RANK_DEFAULT);

        try {
            corpUsersTable.executeUpdate("UPDATE `{TABLE}` SET `isCoCeo`=0 , `corp_id`=NULL WHERE `uuid`=?", user.getUniqueId().toString());
        } catch (Exception e) {
            if (!ignoreFailure) {
                throw new RuntimeException(e);
            }
        }
    }

    public Set<IngameUser> getMembers(boolean excludeCeo) {
        Set<IngameUser> members = new HashSet<>();
        try {
            CorpUserRow[] result = corpUsersTable.get("SELECT `uuid` from `{TABLE}` WHERE `corp_id`=?", id);
            for (CorpUserRow row : result) {
                IngameUser member = SinkLibrary.getInstance().getIngameUser(row.uuid);
                if (excludeCeo && (isCeo(member) || isCoCeo(member))) {
                    continue;
                }
                members.add(member);
            }
            return members;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public double getBalance() {
        return getBase().balance;
    }

    private CorpRow getBase() {
        try {
            return corpsTable.get("SELECT * FROM `{TABLE}` WHERE `id`=?", id)[0];
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ProtectedRegion getBaseRegion() {
        return ReallifeMain.getInstance().getWorldGuardPlugin().getRegionManager(Bukkit.getWorld(getBase().baseWorld)).getRegion(getBase().baseId);
    }

    public IngameUser getCEO() {
        return SinkLibrary.getInstance().getIngameUser(getBase().ceoUniqueId);
    }

    public void setCEO(IngameUser user) {
        try {
            resetUser(user, true);
            addMember(user);
            corpsTable.executeUpdate("UPDATE `{TABLE}` SET `ceo_uuid`=? WHERE `id`=?", user.getUniqueId().toString(), id);
            setRank(user, CorporationRanks.RANK_CEO);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isCoCeo(IngameUser user) {
        return getCoCEOs().contains(user);
    }

    public List<IngameUser> getCoCEOs() {
        List<IngameUser> tmp = new ArrayList<>();
        CorpUserRow[] result;
        try {
            result = corpUsersTable.get("SELECT * FROM `{TABLE}` WHERE `corp_id`=? AND `isCoCeo`=1",
                                        id);

            for (CorpUserRow row : result) {
                tmp.add(SinkLibrary.getInstance().getIngameUser(row.uuid));
            }

            return tmp;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getFormattedName() {
        return ChatColor.DARK_GREEN + getName().replace("_", " ") + ChatColor.RESET;
    }

    public String getName() {
        return getBase().corpName;
    }

    @Nullable
    public String getTag() {
        return getBase().tag;
    }

    public void setTag(String tag) {
        if (tag.length() < 2 || tag.length() > 4) {
            throw new IllegalArgumentException("Min Tag Length: 2 , Max Tag length: 4");
        }

        try {
            corpsTable.executeUpdate("UPDATE `{TABLE}` SET `tag`=? WHERE `id`=?", tag, id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getRank(IngameUser user) {
        try {
            CorpUserRow[] rows = corpUsersTable.get("SELECT `rank` FROM `{TABLE}` WHERE `uuid`=?", user.getUniqueId().toString());
            String rank = rows[0].rank;

            if (StringUtil.isEmptyOrNull(rank)) {
                if (isCeo(user)) {
                    setRank(user, CorporationRanks.RANK_CEO);
                } else if (isCoCeo(user)) {
                    setRank(user, CorporationRanks.RANK_CO_CEO);
                } else {
                    setRank(user, CorporationRanks.RANK_DEFAULT);
                }
                return getRank(user);
            }

            return rank;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isMember(IngameUser user) {
        try {
            return corpUsersTable.get("SELECT * from `{TABLE}` WHERE `uuid`=? AND `corp_id`=?", user.getUniqueId().toString(), id).length > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setBase(World world, String regionId) {
        try {
            corpsTable.executeUpdate("UPDATE `{TABLE}` SET `base_id`=?, `base_world`=? WHERE `id`=?", regionId, world.getName(), id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setRank(IngameUser user, String rank) {
        rank = ChatColor.translateAlternateColorCodes('&', rank);

        if (!rank.startsWith(ChatColor.COLOR_CHAR + "")) {
            rank = ChatColor.GOLD + rank;
        }

        try {
            corpUsersTable.executeUpdate("UPDATE `{TABLE}` SET `rank`=? WHERE `uuid`=?", rank, user.getUniqueId().toString());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isCeo(IngameUser user) {
        return getCEO().getUniqueId().equals(user.getUniqueId());
    }

    public final int getId() {
        return id;
    }
}
