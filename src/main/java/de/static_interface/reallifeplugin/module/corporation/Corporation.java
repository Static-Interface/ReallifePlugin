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

import static de.static_interface.sinksql.query.Query.eq;
import static de.static_interface.sinksql.query.Query.from;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.corporation.database.row.CorpRank;
import de.static_interface.reallifeplugin.module.corporation.database.row.CorpRow;
import de.static_interface.reallifeplugin.module.corporation.database.row.CorpUserRow;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpOptionsTable;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpRanksTable;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpUsersTable;
import de.static_interface.reallifeplugin.module.corporation.database.table.CorpsTable;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.MathUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

public class Corporation {
    /**
     * Todo: fix stack bug
     * Todo: add corporation chat
     * Todo: Fix protection for chests
     * Todo: send messages to ceo (and coceos) if someone sold something
     */
    private final int id;

    private CorpsTable corpsTable;
    private CorpUsersTable corpUsersTable;
    private CorporationModule module;

    public Corporation(CorporationModule module, int id) {
        this.id = id;
        this.module = module;
        corpsTable = Module.getTable(module, CorpsTable.class);
        corpUsersTable = Module.getTable(module, CorpUsersTable.class);
    }

    public int getMemberLimit() {
        return getOption(CorporationOptions.MEMBER_LIMIT, Integer.class, -1);
    }

    public void setMemberLimit(int limit) {
        setOption(CorporationOptions.MEMBER_LIMIT, limit);
    }

    public final int getId() {
        return id;
    }

    public boolean addBalance(double amount) {
        return addBalance(amount, true);
    }

    public boolean addBalance(double amount, boolean checkAmount) {
        if (amount == 0) {
            return true;
        }

        amount = MathUtil.round(amount);

        if (checkAmount && amount < 0 && getBalance() < Math.abs(amount)) {
            return false;
        }

        double newAmount = getBalance() + amount;

        from(corpsTable)
                .update()
                .set("balance", "?")
                .where("id", eq("?"))
                .execute(newAmount, id);
        return true;
    }

    public void addMember(IngameUser user, CorpRank rank) {
        Integer userId = CorporationManager.getInstance().getUserId(user);
        from(corpUsersTable)
                .update()
                .set("corp_id", "?")
                .set("corp_rank", "?")
                .where("id", eq("?"))
                .execute(getId(), rank.id, userId);

        onUpdateRank(user, rank);

        if (getBaseRegion() != null) {
            DefaultDomain rgMembers = getBaseRegion().getMembers();
            rgMembers.addPlayer(user.getUniqueId());
            getBaseRegion().setMembers(rgMembers);
        }
    }

    public void removeMember(IngameUser user) {
        resetUser(user);
        onUpdateRank(user, null);

        if (getBaseRegion() != null && getBaseRegion().getMembers() != null) {
            DefaultDomain rgMembers = getBaseRegion().getMembers();
            rgMembers.removePlayer(user.getUniqueId());
            getBaseRegion().setMembers(rgMembers);
        }
    }

    private void resetUser(IngameUser user) {
        from(corpUsersTable)
                .update()
                .set("corp_rank", "?")
                .set("corp_id", "?")
                .where("id", eq("?"))
                .execute(null, null, CorporationManager
                        .getInstance().getUserId(user));
    }

    public Set<IngameUser> getMembers() {
        Set<IngameUser> members = new HashSet<>();
        CorpUserRow[] result = from(corpUsersTable)
                .select("uuid")
                .where("corp_id", eq("?"))
                .getResults(id);

        for (CorpUserRow row : result) {
            IngameUser member = SinkLibrary.getInstance().getIngameUser(UUID.fromString(row.uuid));
            members.add(member);
        }
        return members;
    }

    public double getBalance() {
        return getBase().balance;
    }

    private CorpRow getBase() {
        return from(corpsTable)
                .select()
                .where("id", eq("?"))
                .get(id);
    }

    public ProtectedRegion getBaseRegion() {
        return ReallifeMain.getInstance().getWorldGuardPlugin().getRegionManager(Bukkit.getWorld(getBase().base_world)).getRegion(getBase().baseId);
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
        tag = tag.toUpperCase();
        if (tag.length() < 2 || tag.length() > 5) {
            throw new IllegalArgumentException("Min Tag Length: 2 , Max Tag length: 5");
        }

        from(corpsTable)
                .update()
                .set("tag", "?")
                .where("id", eq("?"))
                .execute(tag, id);
    }

    public CorpRank getRank(IngameUser user) {
        CorpUserRow row = getUserInternal(user);
        if (row == null) {
            throw new RuntimeException(user.getName() + " is not a member of " + getName() + "!");
        }
        Integer rankId = row.corpRank;
        if (rankId == null) {
            return null;
        }

        return from(module.getTable(CorpRanksTable.class))
                .select()
                .where("id", eq("?"))
                .and("corp_id", eq("?"))
                .get(rankId, getId());
    }

    @Nullable
    public CorpRank getRank(String name) {
        return from(module.getTable(CorpRanksTable.class))
                .select()
                .where("name", eq("?"))
                .and("corp_id", eq("?"))
                .get(name, getId());
    }

    @Nullable
    public CorpRank getRank(int rankId) {
        return from(module.getTable(CorpRanksTable.class))
                .select()
                .where("id", eq("?"))
                .and("corp_id", eq("?"))
                .get(rankId, getId());
    }

    @Nullable
    private CorpUserRow getUserInternal(IngameUser user) {
        for (CorpUserRow row : getUsersInternal()) {
            if (Objects.equals(row.uuid, user.getUniqueId().toString()) && Objects.equals(row.corpId, getBase().id)) {
                return row;
            }
        }
        return null;
    }

    private List<CorpUserRow> getUsersInternal() {
        CorpUserRow[] result = from(corpUsersTable)
                .select()
                .where("corp_id", eq("?"))
                .getResults(getBase().id);
        return Arrays.asList(result);
    }

    public boolean isMember(IngameUser user) {
        return from(corpUsersTable)
                       .select()
                       .where("uuid", eq("?"))
                       .and("corp_id", eq("?"))
                       .getResults(user.getUniqueId().toString(), id).length > 0;
    }

    public void setBase(World world, String regionId) {
        ProtectedRegion rg = getBaseRegion();
        if (rg != null) {
            DefaultDomain rgMembers = rg.getMembers();
            for (IngameUser member : getMembers()) {
                try {
                    rgMembers.removePlayer(member.getUniqueId());
                } catch (Exception e) {
                    ReallifeMain.getInstance().getLogger()
                            .warning("Couldn't remove player from WorldGuard region " + rg.getId() + " :" + member.getName());
                    e.printStackTrace();
                }
            }
        }

        ProtectedRegion newRegion;
        try {
            newRegion = ReallifeMain.getInstance().getWorldGuardPlugin().getRegionManager(world).getRegion(regionId);
            if (newRegion == null) {
                throw new NullPointerException("Base not found");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        DefaultDomain rgMembers = newRegion.getMembers();
        for (IngameUser member : getMembers()) {
            try {
                rgMembers.addPlayer(member.getUniqueId());
            } catch (Exception e) {
                ReallifeMain.getInstance().getLogger()
                        .warning("Couldn't remove player from WorldGuard region " + rg.getId() + " :" + member.getName());
                e.printStackTrace();
            }
        }

        from(corpsTable).update()
                .set("base_id", "?")
                .set("base_world", "?")
                .where("id", eq("?"))
                .execute(regionId, world.getName(), id);
    }

    public void setRank(IngameUser target, CorpRank rank) {
        if (!isMember(target)) {
            throw new IllegalStateException("Couldn't set rank for player: " + target.getName() + " is not a member of " + getName());
        }

        onUpdateRank(target, rank);

        int userId = CorporationManager.getInstance().getUserId(target);
        from(module.getTable(CorpUsersTable.class))
                .update()
                .set("corp_rank", "?")
                .where("id", eq("?"))
                .execute(rank.id, userId);
    }

    public void onUpdateRank(IngameUser user, @Nullable CorpRank rank) {
        if (rank != null && CorporationManager.getInstance().hasCorpPermission(rank, CorporationPermissions.REGION_OWNER)) {
            addRegionOwner(user);
        } else {
            removeRegionOwner(user);
        }
    }

    public int getMemberCount() {
        return from(module.getTable(CorpUsersTable.class))
                .select()
                .where("corp_id", eq("?"))
                .getResults(getId())
                .length;
    }

    private void addRegionOwner(IngameUser user) {
        if (getBaseRegion() != null) {
            DefaultDomain rgOwners = getBaseRegion().getOwners();
            rgOwners.addPlayer(user.getUniqueId());
            getBaseRegion().setOwners(rgOwners);
        }
    }

    private void removeRegionOwner(IngameUser user) {
        if (getBaseRegion() != null) {
            DefaultDomain rgOwners = getBaseRegion().getOwners();
            rgOwners.removePlayer(user.getUniqueId());
            getBaseRegion().setOwners(rgOwners);
        }
    }

    public boolean isRegionOwner(IngameUser user) {
        return getBaseRegion() != null && CorporationManager.getInstance().hasCorpPermission(user, CorporationPermissions.REGION_OWNER);
    }

    public void announce(String message) {
        message = ChatColor.GRAY + "[" + getFormattedName() + ChatColor.GRAY + "] " + message;
        for (IngameUser user : getOnlineMembers()) {
            Player p = Bukkit.getPlayer(user.getUniqueId());
            if (p == null) {
                continue;
            }
            p.sendMessage(message);
        }

        SinkLibrary.getInstance().getConsoleUser().sendMessage(message);
    }

    public List<IngameUser> getOnlineMembers() {
        List<IngameUser> onlineMembers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            IngameUser user = SinkLibrary.getInstance().getIngameUser(p);
            if (isMember(user)) {
                onlineMembers.add(user);
            }
        }
        return onlineMembers;
    }

    public List<CorpRank> getRanks() {
        List<CorpRank>
                rows =
                Arrays.asList(
                        from(module.getTable(CorpRanksTable.class))
                                .select()
                                .where("corp_id", eq("?"))
                                .getResults(getId()));

        Collections.sort(rows);
        return rows;
    }

    public CorpRank getDefaultRank() {
        int defaultRankId = getOption(CorporationOptions.DEFAULT_RANK, Integer.class, null);
        return getRank(defaultRankId);
    }

    public boolean isPublic() {
        return getOption(CorporationOptions.PUBLIC, boolean.class, false);
    }

    public List<CorpUserRow> getRankUsers(CorpRank rank) {
        CorpUserRow[]
                result =
                from(module.getTable(CorpUsersTable.class))
                        .select().where("corp_id", eq("?"))
                        .and("corp_rank", eq("?"))
                        .getResults(getId(), rank.id);

        return Arrays.asList(result);
    }

    public <T> T getOption(CorporationOptions option, Class<T> returnClazz, T defaultValue) {
        return module.getTable(CorpOptionsTable.class).getOption(option.getIdentifier(), getId(), returnClazz, defaultValue);
    }

    public void setOption(CorporationOptions option, Object value) {
        module.getTable(CorpOptionsTable.class).setOption(option.getIdentifier(), value, getId());
    }
}
