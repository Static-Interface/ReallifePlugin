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

package de.static_interface.reallifeplugin.config;

import de.static_interface.reallifeplugin.module.payday.model.Group;
import de.static_interface.sinklibrary.api.configuration.Configuration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RpSettings extends Configuration {

    public static final String PAYDAY = "payday";
    public static final String TAXES_MODIFIER = "taxesmodifier";
    public static final String SHOWN_NAME = "shownname";
    public static final String EXCLUDED = "excluded";

    public RpSettings(File baseFolder, Plugin plugin) {
        super(new File(baseFolder, "Settings.yml"), true);
    }

    @Override
    public void addDefaults() {
        addDefault("General.TaxAccount", "TaxAccount");

        addDefault("Groups.Default.PayDay", 0);
        addDefault("Groups.Default.TaxesModifier", 1);
        addDefault("Groups.Default.Excluded", true);
        addDefault("Groups.Default.ShownName", "%name%");

        addDefault("Ad.Price", 500.0);
        addDefault("Ad.Timeout", 30, "In minutes");
        addDefault("Ad.GlobalCooldown", 45, "In seconds");
    }

    @Override
    public void onCreate() {
        set("Groups.Guest.Payday", 0);
        set("Groups.Guest.TaxesModifier", 1);
        set("Groups.Guest.Excluded", true);
        set("Groups.Guest.ShownName", "%name%");

        set("Groups.Member.Payday", 500);
        set("Groups.Member.TaxesModifier", 1);
        set("Groups.Member.Excluded", false);
        set("Groups.Member.ShownName", "Arbeitsloser");
    }


    public List<Group> readGroups() {
        List<Group> groups = new ArrayList<>();

        Map<String, Object> values = yamlConfiguration.getConfigurationSection("Groups").getValues(false);

        for (String groupName : values.keySet()) {
            if (groupName.equals("Default")) {
                continue;
            }

            Group group = new Group();
            group.name = groupName;
            Map<String, Object> groupValues = yamlConfiguration.getConfigurationSection("Groups." + groupName).getValues(false);
            for (String s : groupValues.keySet()) {
                switch (s.toLowerCase()) {
                    case PAYDAY:
                        group.payday = (int) get("Groups." + groupName + "." + s);
                        break;
                    case TAXES_MODIFIER:
                        group.taxesmodifier = (int) get("Groups." + groupName + "." + s);
                        break;
                    case SHOWN_NAME:
                        group.shownName = (String) get("Groups." + groupName + "." + s);
                        break;
                    case EXCLUDED:
                        group.excluded = (boolean) get("Groups." + groupName + "." + s);
                        break;
                }
            }

            group = validateGroup(group);

            groups.add(group);
        }
        return groups;
    }

    private Group validateGroup(Group group) {
        if (group.name == null || group.name.isEmpty()) {
            throw new RuntimeException("Name is null");
        }
        if (group.shownName == null || group.shownName.isEmpty() || group.shownName.equals("%name%")) {
            group.shownName = group.name;
        }
        if (group.payday < 0) {
            group.payday = getDefaultPayday();
        }
        if (group.taxesmodifier < 0) {
            group.taxesmodifier = getDefaultTaxesModifier();
        }
        return group;
    }

    public int getDefaultPayday() {
        return (int) get("Groups.Default.PayDay");
    }

    public double getDefaultTaxesModifier() {
        return Double.parseDouble(get("Groups.Default.TaxesModifier").toString());
    }

    public boolean isDefaultExcluded() {
        return (boolean) get("Groups.Default.Excluded");
    }

    public String getTaxAccount() {
        return (String) get("General.TaxAccount");
    }

    public double getAdPrice() {
        return Double.valueOf(String.valueOf(get("Ad.Price")));
    }

    public int getAdTimeout() {
        return Integer.valueOf(String.valueOf(get("Ad.Timeout")));
    }
}
