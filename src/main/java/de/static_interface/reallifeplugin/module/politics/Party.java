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
import org.bukkit.ChatColor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;

public class Party {

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
        try {
            PartyRow[] result = module.getTable(PartyTable.class).get("SELECT * FROM `{TABLE}` WHERE `id`=?", id);
            if (result == null || result.length < 1) {
                return null;
            }
            return result[0];
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public String getDescription() {
        return ChatColor.translateAlternateColorCodes('&', getBase().description);
    }

    public List<IngameUser> getMembers() {
        List<IngameUser> tmp = new ArrayList<>();
        for (PartyUser user : getUsersInternal()) {
            tmp.add(SinkLibrary.getInstance().getIngameUser(UUID.fromString(user.uuid)));
        }
        return tmp;
    }

    @Nullable
    public PartyRank getUserRank(IngameUser user) {
        PartyUser row = getUserInternal(user);
        if (row == null) {
            throw new RuntimeException(user.getName() + " is not a member of " + getName() + "!");
        }
        Integer rankId = row.partyRank;
        if (rankId == null) {
            return null;
        }
        PartyRank[] result;
        try {
            result = module.getTable(PartyRanksTable.class).get("SELECT * FROM `{TABLE}` WHERE `id` = ?", rankId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (result == null || result.length < 1) {
            return null;
        }
        return result[0];
    }

    @Nullable
    public PartyRank getRank(String name) {
        PartyRank[] result;
        try {
            result = module.getTable(PartyRanksTable.class).get("SELECT * FROM `{TABLE}` WHERE `name` = ?", name);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (result == null || result.length < 1) {
            return null;
        }
        return result[0];
    }

    @Nullable
    public PartyRank getRank(int id) {
        PartyRank[] result;
        try {
            result = module.getTable(PartyRanksTable.class).get("SELECT * FROM `{TABLE}` WHERE `id` = ?", id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (result == null || result.length < 1) {
            return null;
        }
        return result[0];
    }

    public PartyRank getDefaultRank() {
        int defaultRankId = module.getTable(PartyOptionsTable.class)
                .getOption(PartyOptions.DEFAULT_RANK, getId(), Integer.class, null);
        return getRank(defaultRankId);
    }

    private List<PartyUser> getUsersInternal() {
        try {
            return Arrays.asList(module.getTable(PartyUsersTable.class).get("SELECT * FROM `{TABLE}` WHERE `party_id` = ?", getBase().id));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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

    public void addMember(UUID user, int rankId) {

    }
}
