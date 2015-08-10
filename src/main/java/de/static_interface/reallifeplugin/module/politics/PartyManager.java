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
import de.static_interface.reallifeplugin.module.politics.database.table.PartyRankPermissionsTable;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyRanksTable;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyTable;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyUsersTable;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.user.IngameUser;
import org.bukkit.ChatColor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

public class PartyManager {

    private static PartyManager instance;
    private PoliticsModule module;
    private List<Party> parties;

    private PartyManager(PoliticsModule module) {
        this.module = module;
        parties = new ArrayList<>();
    }

    public static void init(PoliticsModule module) {
        instance = new PartyManager(module);
    }

    public static PartyManager getInstance() {
        return instance;
    }

    public void createNewPary(String partyName, String tag, UUID founder) throws SQLException {
        PartyRow partyRow = new PartyRow();
        partyRow.balance = 0;
        partyRow.creationTime = System.currentTimeMillis();
        partyRow.creatorUuid = founder.toString();
        partyRow.description = null;
        partyRow.name = partyName;
        partyRow.tag = tag;
        partyRow = module.getTable(PartyTable.class).insert(partyRow);

        //We need an initial rank
        PartyRank founderRank = new PartyRank();
        founderRank.name = "Founder";
        founderRank.prefix = ChatColor.DARK_RED.toString();
        founderRank.description = "Founder of the party";
        founderRank.priority = 0;
        founderRank.partyId = partyRow.id;

        founderRank = module.getTable(PartyRanksTable.class).insert(founderRank);

        // Lets add some permission to this rank
        setPermission(founderRank, PartyPermission.ALL, true);

        //We also need a default rank with default values
        PartyRank defaultRank = new PartyRank();
        defaultRank.name = "Member";
        defaultRank.prefix = ChatColor.GOLD.toString();
        defaultRank.description = "Member of this party";
        defaultRank.priority = 10;
        defaultRank.partyId = partyRow.id;
        defaultRank = module.getTable(PartyRanksTable.class).insert(defaultRank);

        //Lets set it as default rank for new members
        module.getTable(PartyOptionsTable.class).setOption(PartyOptions.DEFAULT_RANK.getIdentifier(), defaultRank.id, partyRow.id);

        // Ok, now add the founder to the database
        Party party = getParty(partyRow.id);
        party.addMember(founder, founderRank.id);
    }

    public boolean hasPartyPermission(UUID uuid, PartyPermission permission) {
        IngameUser user = SinkLibrary.getInstance().getIngameUser(uuid);

        Party party = getParty(SinkLibrary.getInstance().getIngameUser(uuid));
        if (party == null) {
            return false;
        }

        PartyRank rank = party.getRank(user);
        return hasPartyPermission(rank, permission);
    }

    public boolean hasPartyPermission(PartyRank rank, PartyPermission permission) {
        if (permission != PartyPermission.ALL && permission.includeInAllPermission() && hasPartyPermission(rank, PartyPermission.ALL)) {
            return true;
        }

        PartyRankPermissionsTable permsTable = module.getTable(PartyRankPermissionsTable.class);

        if (permission.getValueType() != boolean.class && permission.getValueType() != Boolean.class) {
            return permsTable.getOption(permission.getPermissionString(), rank.id, Object.class, null) != null;
        }

        return permsTable.getOption(permission.getPermissionString(), rank.id, boolean.class, false);
    }

    public void setPermission(PartyRank rank, PartyPermission permission, Object value) {
        PartyRankPermissionsTable permsTable = module.getTable(PartyRankPermissionsTable.class);
        permsTable.setOption(permission.getPermissionString(), value, rank.id);
    }

    public int getUserId(UUID uuid) throws SQLException {
        PartyUser[] rows = module.getTable(PartyUsersTable.class).get("SELECT * FROM `{TABLE}` WHERE `uuid`=?", uuid.toString());
        if (rows.length > 0) {
            return rows[0].id;
        }

        PartyUser row = new PartyUser();
        row.partyId = null;
        row.partyRank = null;
        row.uuid = uuid.toString();
        return module.getTable(PartyUsersTable.class).insert(row).id;
    }

    @Nullable
    public Party getParty(String name) {
        return getParty(name, true);
    }

    @Nullable
    public Party getParty(String name, boolean searchForTagToo) {
        name = ChatColor.stripColor(name).replace(" ", "_");
        for (Party p : getParties()) {
            if (p.getName().equalsIgnoreCase(name) || (searchForTagToo && p.getTag().equalsIgnoreCase(name))) {
                return p;
            }
        }

        return null;
    }

    @Nullable
    public Party getParty(int id) {
        for (Party p : getParties()) {
            if (p.getId() == id) {
                return p;
            }
        }

        return null;
    }

    public List<Party> getParties() {
        try {
            for (PartyRow row : module.getTable(PartyTable.class).get("SELECT * FROM {TABLE}")) {
                if (isPartyCached(row.id)) {
                    continue;
                }
                parties.add(new Party(row.id, module));
            }

            for (Party p : parties) {
                if (!p.exists()) {
                    parties.remove(p);
                }
            }
            return parties;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isPartyCached(int id) {
        for (Party p : parties) {
            if (p.getId() == id) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    public Party getParty(IngameUser user) {
        PartyUser[] rows;
        try {
            rows = module.getTable(PartyUsersTable.class).get("SELECT * FROM `{TABLE}` WHERE `uuid` = ?", user.getUniqueId().toString());
        } catch (SQLException e) {
            return null;
        }
        if (rows == null || rows.length < 1) {
            return null;
        }

        Integer partyId = rows[0].partyId;
        if (partyId == null) {
            return null;
        }
        return getParty(partyId);
    }

    public String getFormattedName(PartyUser user) {
        IngameUser ingameUser = SinkLibrary.getInstance().getIngameUser(UUID.fromString(user.uuid));
        Party party = getParty(ingameUser);
        if (party == null) {
            return ChatColor.GOLD + ChatColor.stripColor(ingameUser.getDisplayName()) + ChatColor.RESET;
        }
        PartyRank rank = party.getRank(ingameUser);
        return ChatColor.translateAlternateColorCodes('&', rank.prefix) + ChatColor.stripColor(ingameUser.getDisplayName()) + ChatColor.RESET;
    }
}
