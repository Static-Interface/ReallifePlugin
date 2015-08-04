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

import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.database.AbstractTable;
import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.politics.command.PartyCommand;
import de.static_interface.reallifeplugin.module.politics.database.row.PartyRankRow;
import de.static_interface.reallifeplugin.module.politics.database.row.PartyRow;
import de.static_interface.reallifeplugin.module.politics.database.row.PartyUserRow;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyOptionsTable;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyRankPermissionsTable;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyRanksTable;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyTable;
import de.static_interface.reallifeplugin.module.politics.database.table.PartyUsersTable;
import de.static_interface.sinklibrary.user.IngameUser;
import org.bukkit.ChatColor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

public class PoliticsModule extends Module<ReallifeMain> {

    public static final String NAME = "Politics";
    List<Party> parties = new ArrayList<>();

    public PoliticsModule(ReallifeMain plugin, Database db) {
        super(plugin, ReallifeMain.getInstance().getSettings(), db, NAME, false);
    }

    @Override
    public void onEnable() {
        registerModuleCommand("party", new PartyCommand(this));
    }

    @Override
    protected Collection<AbstractTable> getTables() {
        List<AbstractTable> tables = new ArrayList<>();
        AbstractTable table = new PartyTable(getDatabase());
        tables.add(table);
        table = new PartyRanksTable(getDatabase());
        tables.add(table);
        table = new PartyUsersTable(getDatabase());
        tables.add(table);
        table = new PartyOptionsTable(getDatabase());
        tables.add(table);
        table = new PartyRankPermissionsTable(getDatabase());
        tables.add(table);
        return tables;
    }

    public List<Party> getParties() {
        try {
            for (PartyRow row : getTable(PartyTable.class).get("SELECT * FROM {TABLE}")) {
                if (isPartyCached(row.id)) {
                    continue;
                }
                parties.add(new Party(row.id, this));
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
    public Party getParty(String name) {
        name = ChatColor.stripColor(name).replace(" ", "_");
        for (Party p : getParties()) {
            if (p.getName().equalsIgnoreCase(name)) {
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

    public List<PartyRankRow> getRanks(PartyRow party) {
        try {
            List<PartyRankRow> rows = Arrays.asList(getTable(PartyRanksTable.class).get("SELECT * FROM `{TABLE}` WHERE `party_id` = ?", party.id));
            Collections.sort(rows);
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public Party getParty(IngameUser user) {
        PartyUserRow[] rows;
        try {
            rows = getTable(PartyUsersTable.class).get("SELECT * FROM `{TABLE}` WHERE `uuid` = ?", user.getUniqueId().toString());
        } catch (SQLException e) {
            return null;
        }
        if (rows == null || rows.length < 1) {
            return null;
        }

        int partyId = rows[0].partyId;
        return getParty(partyId);
    }
}
