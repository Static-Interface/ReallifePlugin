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

import de.static_interface.sinklibrary.api.configuration.Configuration;
import de.static_interface.sinklibrary.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.File;

import javax.annotation.Nullable;

public class ReallifeLanguageConfiguration extends Configuration {

    private static ReallifeLanguageConfiguration instance;

    public ReallifeLanguageConfiguration() {
        super(new File(Bukkit.getPluginManager().getPlugin("ReallifePlugin").getDataFolder(), "Language.yml"));
    }

    public static ReallifeLanguageConfiguration getInstance() {
        if (instance == null) {
            instance = new ReallifeLanguageConfiguration();
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
        return m(path, null);
    }

    /**
     * Get language as String from key
     *
     * @param path Path to language variable
     * @param paramValues Varargs for {@link StringUtil#format(String, Object...)}
     * @return Language String
     */
    public static String m(String path, @Nullable Object... paramValues) {
        String s = (String) getInstance().get(path);
        if (paramValues != null) {
            s = StringUtil.format(s, paramValues);
        }
        s = s.replace("\\n", System.lineSeparator());
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @Override
    public void addDefaults() {
        addDefault("General.NotEnoughMoney", "&4You don't have enough money.");
        addDefault("General.UnknownSubCommand", "&4Unknown subcommand: {0}");
        addDefault("General.InvalidValue", "Invalid Value: {0}");
        addDefault("General.Success", "&aSuccess");

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
        addDefault("Corporation.NotCEO", "&4Error: &cYou're not the CEO!");
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
        addDefault("Corporation.AlreadyMember", "&c{0} is already a member of your corporation!");
        addDefault("Corporation.AlreadyMemberOther", "&c{0} is already a member of another corporation!");
        addDefault("Corporation.NotMember", "&c{0} is not a member of your corporation!");
        addDefault("Corporation.RankSet", "&2Succesfully set {0}'s rank to {1}&2!");
        addDefault("Corporation.NotCoCEO", "&4{0} is not a Co CEO!");
        addDefault("Corporation.AlreadyCoCEO", "&4{0} is already a Co CEO!");
        addDefault("Corporation.UserLeftCorporation", "&4{DISPLAYNAME} has left the corporation");
        addDefault("Corporation.LeftCorporation", "&4You left the corporation");
        addDefault("Corporation.CoCeoAdded", "&2Successfully added {0} as co-CEO!");
        addDefault("Corporation.CoCeoRemoved", "&2Successfully removed {0} as co-CEO!");
        addDefault("Corporation.Withdraw", "&4{0} has withdrawn {1} {CURRENCY} from corporation account");
        addDefault("Corporation.Deposit", "&4{0} has deposited {1} {CURRENCY} to corporation account");
        addDefault("Corporation.NotEnoughMoney", "&4 The Corporation doesn't have enough money!");
        addDefault("Corporation.InvalidName", "Invalid Corporation Name");

        addDefault("Ad.Message", "&7[&6Ad&7]&f {DISPLAYNAME} &6{MESSAGE}");
        addDefault("Ad.Timout", "&4Error: &cYou can use this command only every {0} minutes. Please wait {1} minutes before using this command again.");

        addDefault("Payday.Taxes", "{0}% Taxes");
        addDefault("Payday.Payday", "Payday ({0} {CURRENCY})");
        addDefault("AntiPvPEscape.BanMessage", "You have been banned for {0} Minutes. Reason: Escape from PvP by quit");
    }
}
