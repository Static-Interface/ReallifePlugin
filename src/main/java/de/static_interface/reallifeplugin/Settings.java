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

package de.static_interface.reallifeplugin;

import de.static_interface.reallifeplugin.model.Group;
import de.static_interface.sinklibrary.configuration.ConfigurationBase;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Settings extends ConfigurationBase
{
    public static final String PAYDAY = "payday";
    public static final String TAXES_MODIFIER = "taxesmodifier";
    public static final String SHOWN_NAME = "shownname";
    public static final String EXCLUDED = "excluded";
    public Settings(Plugin plugin)
    {
        super(new File(plugin.getDataFolder(), "Settings.yml"));
    }

    @Override
    public void addDefaults()
    {
        addDefault("General.Taxesbase", 0.1);
        addDefault("General.PaydayTime", 60);
        addDefault("General.TaxAccount", "Staatskasse");
        addDefault("General.InsuranceAccount", "Versicherung");

        addDefault("Default.PayDay", 0);
        addDefault("Default.TaxesModifier", 1);
        addDefault("Default.Excluded", false);
        addDefault("Default.ShownName", "%name%");

        addDefault("Groups.Guest.Payday", 0);
        addDefault("Groups.Guest.TaxesModifier", 1);
        addDefault("Groups.Guest.Excluded", true);
        addDefault("Groups.Guest.ShownName", "%name%");

        addDefault("Groups.Member.Payday", 500);
        addDefault("Groups.Member.TaxesModifier", 1);
        addDefault("Groups.Member.Excluded", false);
        addDefault("Groups.Member.ShownName", "Arbeitsloser");
    }

    public List<Group> readGroups()
    {
        List<Group> groups = new ArrayList<>();

        Map<String, Object> values = yamlConfiguration.getConfigurationSection("Groups").getValues(false);

        for ( String groupName : values.keySet() )
        {
            Group group = new Group();
            group.name = groupName;
            Map<String, Object> groupValues = yamlConfiguration.getConfigurationSection("Groups." + groupName).getValues(false);
            for ( String s : groupValues.keySet() )
            {
                switch ( s.toLowerCase() )
                {
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

    private Group validateGroup(Group group)
    {
        if ( group.name == null || group.name.isEmpty() )
        {
            throw new RuntimeException("Name is null");
        }
        if ( group.shownName == null || group.shownName.isEmpty() || group.shownName.equals("%name%") )
        {
            group.shownName = group.name;
        }
        if ( group.payday < 0 )
        {
            group.payday = getDefaultPayday();
        }
        if ( group.taxesmodifier < 0 )
        {
            group.taxesmodifier = getDefaultTaxesModifier();
        }
        return group;
    }

    public int getDefaultPayday()
    {
        return (int) get("Default.PayDay");
    }

    public double getDefaultTaxesModifier()
    {
        return (double) get("Default.TaxesModifier");
    }

    public boolean getDefaultExcluded()
    {
        return (boolean) get("Default.Excluded");
    }

    public double getTaxesBase()
    {
        return (double) get("General.Taxesbase");
    }

    public int getPaydayTime()
    {
        return (int) get("General.PaydayTime");
    }

    public String getEconomyAccount()
    {
        return (String) get("General.TaxAccount");
    }

    public String getInsuranceAccount()
    {
        return (String) get("General.InsuranceAccount");
    }
}
