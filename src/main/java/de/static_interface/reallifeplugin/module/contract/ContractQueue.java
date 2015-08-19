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

package de.static_interface.reallifeplugin.module.contract;

import static de.static_interface.reallifeplugin.config.ReallifeLanguageConfiguration.m;

import de.static_interface.reallifeplugin.module.contract.database.row.ContractRow;
import de.static_interface.reallifeplugin.module.contract.database.table.ContractUsersTable;
import de.static_interface.sinklibrary.user.IngameUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class ContractQueue {

    private static Map<IngameUser, List<ContractRow>> queue = new HashMap<>();
    private static Map<ContractRow, List<IngameUser>> deniedRequests = new HashMap<>();

    public static void addToQueue(ContractModule module, ContractRow row) {

        //Todo: remove users from row
        for (int id : ContractDatabaseUtil.splitIds(row.userIds)) {
            IngameUser user = Contract.getUserFromUserId(module, id);
            List<ContractRow> rows = queue.get(user);
            if (rows == null) {
                rows = new ArrayList<>();
            }
            rows.add(row);
            queue.put(user, rows);
        }
    }

    public static void removeFromQueue(IngameUser user, ContractRow row) {
        List<ContractRow> rows = queue.get(user);
        if (rows == null) {
            return;
        }

        rows.remove(row);
        if (rows.size() == 0) {
            queue.remove(user);
        } else {
            queue.put(user, rows);
        }

        boolean queueLeft = false;
        for (IngameUser queueUser : queue.keySet()) {
            List<ContractRow> queueRows = queue.get(queueUser);

            if (queueRows == null) {
                queue.remove(queueUser);
                continue;
            }
            if (queueRows.contains(row)) {
                queueLeft = true;
                break;
            }
        }

        if (!queueLeft) {
            onQueueFinish(row);
        }
    }

    public static List<ContractRow> getQueue(IngameUser user) {
        List<ContractRow> queueRows = queue.get(user);
        if (queueRows == null) {
            queueRows = new ArrayList<>();
        }
        return queueRows;
    }

    public static void addDeny(IngameUser user, ContractRow row) {
        removeFromQueue(user, row);
        List<IngameUser> denies = deniedRequests.get(row);
        if (denies == null) {
            denies = new ArrayList<>();
        }
        denies.add(user);
        deniedRequests.put(row, denies);
    }

    public static void cancelQueue(ContractModule module, ContractRow row) {
        for (IngameUser user : queue.keySet()) {
            List<ContractRow> rows = queue.get(user);
            if (rows == null || !rows.contains(row)) {
                queue.remove(user);
                continue;
            }
            rows.remove(row);
            user.sendMessage(m("")); // contract cancelled
        }

        deniedRequests.remove(row);

        module.getTable(ContractUsersTable.class).executeUpdate("DELETE FROM `{TABLE}` WHERE `id` = ?", row.id);
    }

    @Nullable
    public static ContractRow getCreatorContractRow(ContractModule module, IngameUser user) {
        for (List<ContractRow> rows : queue.values()) {
            for (ContractRow row : rows) {
                if (Contract.getUserFromUserId(module, row.creatorId).equals(user)) {
                    return row;
                }
            }
        }
        return null;
    }

    public static void onQueueFinish(ContractRow contract) {

        deniedRequests.remove(contract);
    }
}
