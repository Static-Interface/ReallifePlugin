/*
 * Copyright (c) 2013 - 2015 <http://static-interface.de> and contributors
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

package de.static_interface.reallifeplugin.module.politics;

import de.static_interface.reallifeplugin.module.politics.database.row.PartyRank;
import de.static_interface.reallifeplugin.module.politics.database.row.PartyRow;
import de.static_interface.reallifeplugin.module.politics.database.row.PartyUser;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyOptionsTable;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyRanksTable;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyTable;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyUsersTable;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.BukkitUtil;
import de.static_interface.sinklibrary.util.MathUtil;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;

public class Party implements Comparable<Party> {

    private final int id;
    private PoliticsModule module;

    public Party(int id, PoliticsModule module) {
        this.module = module;
        this.id = id;
    }

    public boolean exists() {
        return getBase() != null;
    }

    private PartyRow getBase() {
        PartyRow[] result = module.getTable(PartyTable.class).get("SELECT * FROM `{TABLE}` WHERE `id`=?", id);
        if (result == null || result.length < 1) {
            return null;
        }
        return result[0];
    }

    @Nullable
    public String getDescription() {
        return getBase().description;
    }

    public List<IngameUser> getMembers() {
        List<IngameUser> tmp = new ArrayList<>();
        for (PartyUser user : getUsersInternal()) {
            tmp.add(SinkLibrary.getInstance().getIngameUser(UUID.fromString(user.uuid)));
        }
        return tmp;
    }

    @Nullable
    public PartyRank getRank(IngameUser user) {
        PartyUser row = getUserInternal(user);
        if (row == null) {
            throw new RuntimeException(user.getName() + " is not a member of " + getName() + "!");
        }
        Integer rankId = row.partyRank;
        if (rankId == null) {
            return null;
        }
        PartyRank[] result = module.getTable(PartyRanksTable.class).get("SELECT * FROM `{TABLE}` WHERE `id` = ?", rankId);
        if (result == null || result.length < 1) {
            return null;
        }
        return result[0];
    }

    public List<PartyUser> getRankUsers(PartyRank rank) {
        PartyUser[] result =
                module.getTable(PartyUsersTable.class).get("SELECT * FROM `{TABLE}` WHERE `party_id` = ? AND `party_rank` = ?", getId(), rank.id);
        if (result == null || result.length < 1) {
            return new ArrayList<>();
        }

        return Arrays.asList(result);
    }

    @Nullable
    public PartyRank getRank(String name) {
        PartyRank[] result = module.getTable(PartyRanksTable.class).get("SELECT * FROM `{TABLE}` WHERE `name` = ? AND `party_id` = ?", name, getId());
        if (result == null || result.length < 1) {
            return null;
        }
        return result[0];
    }

    @Nullable
    public PartyRank getRank(int id) {
        PartyRank[] result = module.getTable(PartyRanksTable.class).get("SELECT * FROM `{TABLE}` WHERE `id` = ?", id);
        if (result == null || result.length < 1) {
            return null;
        }
        return result[0];
    }

    public PartyRank getDefaultRank() {
        int defaultRankId = module.getTable(PartyOptionsTable.class)
                .getOption(PartyOptions.DEFAULT_RANK.getIdentifier(), getId(), Integer.class, null);
        return getRank(defaultRankId);
    }

    private List<PartyUser> getUsersInternal() {
        return Arrays.asList(module.getTable(PartyUsersTable.class).get("SELECT * FROM `{TABLE}` WHERE `party_id` = ?", getBase().id));
    }

    @Nullable
    private PartyUser getUserInternal(IngameUser user) {
        for (PartyUser row : getUsersInternal()) {
            if (Objects.equals(row.uuid, user.getUniqueId().toString()) && Objects.equals(row.partyId, getBase().id)) {
                return row;
            }
        }
        return null;
    }

    public String getName() {
        return getBase().name;
    }

    public String getFormattedName() {
        return ChatColor.translateAlternateColorCodes('&', getName().replace("_", " "));
    }

    public int getId() {
        return id;
    }

    public double getBalance() {
        return getBase().balance;
    }

    public void addMember(UUID user, PartyRank rank) {
        int userId = PartyManager.getInstance().getUserId(user);
        module.getTable(PartyUsersTable.class)
                .executeUpdate("UPDATE `{TABLE}` SET `party_id` = ?, `party_rank` = ? WHERE `id` = ?", getId(), rank.id, userId);
    }

    public boolean isMember(UUID user) {
        for (IngameUser member : getMembers()) {
            if (member.getUniqueId().equals(user)) {
                return true;
            }
        }

        return false;
    }

    public void removeMember(UUID user) {
        if (!isMember(user)) {
            throw new IllegalArgumentException(
                    "User " + BukkitUtil.getNameByUniqueId(user) + " is not a member of party " + getName() + ", can't remove him!");
        }
        int userId = PartyManager.getInstance().getUserId(user);
        module.getTable(PartyUsersTable.class)
                .executeUpdate("UPDATE `{TABLE}` SET `party_id` = ?, `party_rank` = ? WHERE `id` = ?", null, null, userId);
    }

    public String getTag() {
        return getBase().tag;
    }

    public List<PartyRank> getRanks() {
        List<PartyRank>
                rows =
                Arrays.asList(module.getTable(PartyRanksTable.class).get("SELECT * FROM `{TABLE}` WHERE `party_id` = ?", getId()));
        Collections.sort(rows);
        return rows;
    }

    public boolean isPublic() {
        return module.getTable(PartyOptionsTable.class).getOption(PartyOptions.PUBLIC.getIdentifier(), boolean.class, false);
    }

    public void announce(String message) {
        for (IngameUser user : getMembers()) {
            if (user.isOnline()) {
                user.sendMessage(message);
            }
        }
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

        module.getTable(PartyTable.class).executeUpdate("UPDATE `{TABLE}` SET `balance`=? WHERE `id`=?", newAmount, getId());
        return true;
    }

    @Override
    public int compareTo(Party o) {
        if (o == null) {
            return 1;
        }
        return Integer.valueOf(o.getMembers().size()).compareTo(getMembers().size());
    }

    public void setRank(IngameUser target, PartyRank rank) {
        if (!isMember(target.getUniqueId())) {
            throw new IllegalArgumentException("Couldn't set rank for player: " + target.getName() + " is not a member of " + getName());
        }
        int userId = PartyManager.getInstance().getUserId(target.getUniqueId());
        module.getTable(PartyUsersTable.class).executeUpdate("UPDATE `{TABLE}` SET `party_rank`=? WHERE `id` = ?", rank.id, userId);
    }
}
