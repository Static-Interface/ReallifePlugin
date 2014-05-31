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

import de.static_interface.sinklibrary.configuration.ConfigurationBase;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.File;

public class LanguageConfiguration extends ConfigurationBase
{
    private static LanguageConfiguration instance;

    public LanguageConfiguration()
    {
        super(new File(Bukkit.getPluginManager().getPlugin("ReallifePlugin").getDataFolder(), "Language.yml")); //DIRTY
    }

    public static LanguageConfiguration getInstance()
    {
        if ( instance == null )
        {
            instance = new LanguageConfiguration();
            instance.load();
        }
        return instance;
    }

    @Override
    public void addDefaults()
    {
        addDefault("Fractions.Fraction", "&6Fraction");
        addDefault("Fractions.DoesntExists", "&4Error: &cCouldn't find fraction: %s!");
        addDefault("Fractions.BaseSet", "&6Base has been updated!");
        addDefault("Fractions.NotInFraction", "&4Error: &cYou're not a member of any fraction!");
        addDefault("Fractions.NotLeader", "&4Error: &cYou're not a leader!");
        addDefault("Fractions.Added", "&4You've been added to the %s fraction!");
        addDefault("Fractions.Kick", "&eYou've been kicked from the %s fraction!");
        addDefault("Fractions.Created", "&eYou've successfully created %s!");
        addDefault("Fractions.CreationFailed", "&4Error: &cCouldn't create %s!");
        addDefault("Fractions.Deleted", "&eYou've successfully deleted %s!");
        addDefault("Fractions.DeletionFailed", "&4Error: &cCouldn't delete %s!");
    }

    /**
     * Get language as String from key
     *
     * @param path Path to language variable
     * @return Language String
     */
    public static String _(String path)
    {
        return ChatColor.translateAlternateColorCodes('&', (String) getInstance().get(path));
    }
}
