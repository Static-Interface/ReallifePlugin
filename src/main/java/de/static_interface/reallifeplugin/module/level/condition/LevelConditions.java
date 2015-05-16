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

package de.static_interface.reallifeplugin.module.level.condition;

public class LevelConditions {

    private int requiredLikes = 0;
    private int requiredPosts = 0;
    private int requiredActivityPoints = 0;
    private int requiredTime = 0;
    private int cost = 0;
    private String requiredPermission = null;

    public int getRequiredLikes() {
        return requiredLikes;
    }

    public LevelConditions setRequiredLikes(int requiredLikes) {
        this.requiredLikes = requiredLikes;
        return this;
    }

    public int getRequiredPosts() {
        return requiredPosts;
    }

    public LevelConditions setRequiredPosts(int requiredPosts) {
        this.requiredPosts = requiredPosts;
        return this;
    }

    public int getRequiredActivityPoints() {
        return requiredActivityPoints;
    }

    public LevelConditions setRequiredActivityPoints(int requiredActivityPoints) {
        this.requiredActivityPoints = requiredActivityPoints;
        return this;
    }

    public int getRequiredTime() {
        return requiredTime;
    }

    public LevelConditions setRequiredTime(int requiredTime) {
        this.requiredTime = requiredTime;
        return this;
    }

    public int getCost() {
        return cost;
    }

    public LevelConditions setCost(int cost) {
        this.cost = cost;
        return this;
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }

    public LevelConditions setRequiredPermission(String requiredPermission) {
        if (requiredPermission != null) {
            requiredPermission = requiredPermission.trim();
            if (requiredPermission.equals("") || requiredPermission.equals("none")) {
                requiredPermission = null;
            }
        }
        this.requiredPermission = requiredPermission;
        return this;
    }
}
