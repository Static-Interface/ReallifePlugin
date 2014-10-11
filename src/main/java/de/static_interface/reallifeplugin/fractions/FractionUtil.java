/*
 * Copyright (c) 2014 http://adventuria.eu, http://static-interface.de and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.static_interface.reallifeplugin.fractions;

import de.static_interface.sinklibrary.user.IngameUser;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FractionUtil {

    private static List<Fraction> fractions = new ArrayList<>();
    private static FractionConfig config;

    public static Fraction getUserFraction(UUID uuid) {
        for (Fraction fraction : fractions) {
            if (fraction.getMembers().contains(uuid)) {
                return fraction;
            }
        }
        return null;
    }

    public static FractionConfig getFractionConfig() {
        if (config == null) {
            config = new FractionConfig();
        }
        return config;
    }

    public static Fraction getFraction(String name) {
        return new Fraction(getFractionConfig(), name);
    }

    public static boolean isLeader(IngameUser user, Fraction fraction) {
        return fraction.getLeader() == user.getUniqueId();
    }

    public static boolean createFraction(String[] args) {
        if (getFraction(args[0]) != null) {
            return false;
        }

        String name = args[1];
        String leader = args[2];
        boolean isPvPEnabled = Boolean.parseBoolean(args[3]);
        boolean isGriefingAllowed = Boolean.parseBoolean(args[4]);

        String pathPrefix = "Fractions." + name + ".";

        FractionConfig config = new FractionConfig();
        config.set(pathPrefix + FractionValues.LEADER, leader);
        config.set(pathPrefix + FractionValues.BASE, "");
        config.set(pathPrefix + FractionValues.GRIEFING_ALOWED, isGriefingAllowed);
        config.set(pathPrefix + FractionValues.PVP_ALLOWED, isPvPEnabled);
        config.set(pathPrefix + FractionValues.MEMBERS, "");
        config.save();

        Fraction fraction = new Fraction(config, name);
        register(fraction);
        return true;
    }

    private static void register(Fraction fraction) {
        fractions.add(fraction);
    }

    private static void unregister(Fraction fraction) {
        fractions.remove(fraction);
    }

    public static boolean deleteFraction(String name) {
        Fraction fraction = getFraction(name);
        if (fraction == null) {
            return false;
        }

        unregister(fraction);
        return false;
    }
}
