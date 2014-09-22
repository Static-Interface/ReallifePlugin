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

public class LanguageConfiguration extends ConfigurationBase {

    private static LanguageConfiguration instance;

    public LanguageConfiguration() {
        super(new File(Bukkit.getPluginManager().getPlugin("ReallifePlugin").getDataFolder(), "Language.yml")); //DIRTY
    }

    public static LanguageConfiguration getInstance() {
        if (instance == null) {
            instance = new LanguageConfiguration();
            instance.load();
        }
        return instance;
    }

    /**
     * Get language as String from key
     *
     * @param path Path to language variable
     * @return Language String
     */
    public static String m(String path) {
        return ChatColor.translateAlternateColorCodes('&', (String) getInstance().get(path));
    }

    @Override
    public void addDefaults() {
        addDefault("General.NotEnoughMoney", "&4You don't have enough money.");

        addDefault("Fractions.Fraction", "&6Fraction");
        addDefault("Fractions.DoesntExists", "&4Error: &cCouldn't find fraction: {0}!");
        addDefault("Fractions.BaseSet", "&6Base has been updated!");
        addDefault("Fractions.NotInFraction", "&4Error: &cYou're not a member of any fraction!");
        addDefault("Fractions.NotLeader", "&4Error: &cYou're not a leader!");
        addDefault("Fractions.Added", "&4You've been added to the {0} fraction!");
        addDefault("Fractions.Kick", "&eYou've been kicked from the {0} fraction!");
        addDefault("Fractions.Created", "&eYou've successfully created {0}!");
        addDefault("Fractions.CreationFailed", "&4Error: &cCouldn't create {0}!");
        addDefault("Fractions.Deleted", "&eYou've successfully deleted {0}!");
        addDefault("Fractions.DeletionFailed", "&4Error: &cCouldn't delete {0}!");

        addDefault("Corporation.Corporation", "&6Corporation");
        addDefault("Corporation.Exists", "&4Error: &cCorporation already exists!");
        addDefault("Corporation.DoesntExists", "&4Error: &cCouldn't find corporation {0}!");
        addDefault("Corporation.BaseSet", "&6Base has been updated!");
        addDefault("Corporation.CEOSet", "&6CEO has been updated!");
        addDefault("Corporation.NotInCorporation", "&4Error: &cYou're not a member of any corporation!");
        addDefault("Corporation.NotCEO", "&4Error: &cYou're not a CEO!");
        addDefault("Corporation.CEOAdded", "{0} &ehas been added to the corporation!");
        addDefault("Corporation.Added", "&eYou've been added to the {0} corporation!");
        addDefault("Corporation.CEOKicked", "{DISPLAYNAME} &4has been kicked from the corporation!");
        addDefault("Corporation.Kicked", "&4You've been kicked from the {0} corporation!");
        addDefault("Corporation.Created", "&eYou've successfully created {0}!");
        addDefault("Corporation.CreationFailed", "&4Error: &cCouldn't create {0}!");
        addDefault("Corporation.Deleted", "&eYou've successfully deleted {0}!");
        addDefault("Corporation.DeletionFailed", "&4Error: &cCouldn't delete {0}!");
        addDefault("Corporation.InvalidName", "&4Error:&c Invalid name");
        addDefault("Corporation.BuyingFromSameCorporation", "&4Error:&c You can't buy from your corporation");
        addDefault("Corporation.BuySign.CantPickup", "&4Error:&c You can't pickup items!");
        addDefault("Corporation.BuySign.Bought", "&aSuccessfully bought {0} for {1}!");
        addDefault("Corporation.AlreadyMember", "&c{0} is already a member of your corporation");
        addDefault("Corporation.NotMember", "&c{0} is not a member of your corporation!");

        addDefault("Ad.Message", "&7[&6Ad&7]&f {DISPLAYNAME} &6{MESSAGE}");
    }
}
