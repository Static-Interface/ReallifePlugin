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

package de.static_interface.reallifeplugin.module.level;

import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.module.level.condition.LevelConditions;
import de.static_interface.sinklibrary.user.IngameUser;
import eu.adventuria.adventuriaplugin.api.forum.model.response.UserStatsResponse;

public class LevelUtil {

    public static Level getLevel(IngameUser user) {
        Level baseLevel = Level.Cache.getLevel(0);
        Object t = user.getConfiguration().get("Level.Id");
        if (t == null) {
            return baseLevel;
        }

        try {
            return Level.Cache.getLevel(Integer.parseInt(t.toString()));
        } catch (NumberFormatException e) {
            ReallifeMain.getInstance().getLogger()
                    .severe("Couldn't get level for user: " + user.getName() + ": couldn't parse level: " + t.toString() + "; defaulting to zero");
            e.printStackTrace();
            setLevel(user, baseLevel);
            return baseLevel;
        }
    }

    public static void setLevel(IngameUser user, Level nextLevel) {
        user.getConfiguration().set("Level.Id", nextLevel.getLevelId());
        LevelAttachmentsManager.updateAttachment(user.getPlayer(), ReallifeMain.getInstance());

        //Reset playTime for next level
        if (nextLevel.getLevelId() > getLevel(user).getLevelId()) {
            resetPlayTime(user);
        }
    }

    public static void resetPlayTime(IngameUser user) {
        if (user.isOnline()) {
            LevelPlayerTimer.stopPlayerTime(user.getPlayer());
            setPlayTime(user, 0);
            LevelPlayerTimer.startPlayerTime(user.getPlayer(), System.currentTimeMillis());
            return;
        }
        setPlayTime(user, 0);
    }

    public static long getPlayTime(IngameUser user) {
        if (user.isOnline()) {
            LevelPlayerTimer.stopPlayerTime(user.getPlayer());
        }
        long value = Long.parseLong(user.getConfiguration().get("Level.PlayTime", "0").toString());
        if (user.isOnline()) {
            LevelPlayerTimer.startPlayerTime(user.getPlayer(), System.currentTimeMillis());
        }
        return value;
    }

    public static void setPlayTime(IngameUser user, long time) {
        user.getConfiguration().set("Level.PlayTime", time);
    }

    public static boolean canLevelUp(IngameUser user, LevelConditions conditions, UserStatsResponse response) {
        if (conditions.getRequiredActivityPoints() > 0) {
            if (response.activityPoints < conditions.getRequiredActivityPoints()) {
                return false;
            }
        }

        if (conditions.getRequiredLikes() > 0) {
            if (response.likesReceived < conditions.getRequiredLikes()) {
                return false;
            }
        }

        if (conditions.getCost() > 0) {
            if (user.getBalance() < conditions.getCost()) {
                return false;
            }
        }

        if (conditions.getRequiredPosts() > 0) {
            if (response.posts < conditions.getRequiredPosts()) {
                return false;
            }
        }

        if (conditions.getRequiredTime() > 0) {
            if (LevelUtil.getPlayTime(user) < conditions.getRequiredTime()) {
                return false;
            }
        }

        if (conditions.getRequiredPermission() != null) {
            if (!user.hasPermission(conditions.getRequiredPermission())) {
                return false;
            }
        }

        return true;
    }
}
