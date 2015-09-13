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

import de.static_interface.sinklibrary.api.configuration.Configuration;
import de.static_interface.sinklibrary.api.configuration.option.YamlI18nOption;
import de.static_interface.sinklibrary.api.configuration.option.YamlParentOption;
import de.static_interface.sinklibrary.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.File;
import java.util.Objects;

import javax.annotation.Nullable;

public class ReallifeLanguageConfiguration extends Configuration {


    public static final YamlParentOption CONTRACT_PARENT = new YamlParentOption("Contract");
    public static final YamlI18nOption CONTRACT_DENIED = new YamlI18nOption(CONTRACT_PARENT, "Denied", "&4The contract has been denied");
    public static final YamlI18nOption
            CONTRACT_DENIED_OWNER =
            new YamlI18nOption(CONTRACT_PARENT, "DeniedOwnerMessage", "&4{DISPLAYNAME}&r&4 has denied your contract: {0}");
    public static final YamlI18nOption CONTRACT_NOT_FOUND = new YamlI18nOption(CONTRACT_PARENT, "ContractNotFound", "Contract not found!", true);
    public static final YamlI18nOption CONTRACTS_NOT_FOUND = new YamlI18nOption(CONTRACT_PARENT, "NoContractsFound", "No contracts found!", true);
    public static final YamlI18nOption CONTRACT_ACCEPTED = new YamlI18nOption(CONTRACT_PARENT, "Accepted", "&aThe contract has been accepted");
    public static final YamlI18nOption
            CONTRACT_ACCEPTED_OWNER =
            new YamlI18nOption(CONTRACT_PARENT, "AcceptedOwner", "&2{DISPLAYNAME}&r&2 has accepted your contract: {0}");
    public static final YamlI18nOption
            CONTRACT_CANCELLED =
            new YamlI18nOption(CONTRACT_PARENT, "Cancalled", "&4The contract {0}&r&4 has been cancelled.");
    public static final YamlI18nOption
            CONTRACT_CREATED =
            new YamlI18nOption(CONTRACT_PARENT, "Created", "&2The contract {0} has been accepted by all participants!");
    public static YamlI18nOption
            CONTRACT_ACCEPT_MESSAGE =
            new YamlI18nOption(CONTRACT_PARENT, "AcceptMessage", "&7Use &2/caccept&7 to accept or &4/cdeny&7 to deny!");
    public static YamlI18nOption CONTRACT_ADDED = new YamlI18nOption(CONTRACT_PARENT, "Added", "&4{0}&r&2 has added you to the &c{1}&r&4 contract.");

    private static ReallifeLanguageConfiguration instance;
    public ReallifeLanguageConfiguration() {
        super(new File(Bukkit.getPluginManager().getPlugin("ReallifePlugin").getDataFolder(), "Language.yml"));
        instance = this;
    }

    public static ReallifeLanguageConfiguration getInstance() {
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
        Object o = getInstance().get(path);
        String s = Objects.toString(o);
        if (paramValues != null) {
            s = StringUtil.format(s, paramValues);
        }
        s = s.replace("\\n", System.lineSeparator());
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @Override
    public void addDefaults() {
        addDefault("General.UnknownSubCommand", "&4Unknown subcommand: {0}");

        addDefault("Corporation.Corporation", "&6Corporation");
        addDefault("Corporation.Exists", "&4Error: &cCorporation already exists!");
        addDefault("Corporation.DoesntExists", "&4Error: &cCouldn't find corporation {0}!");
        addDefault("Corporation.BaseSet", "&6Base has been updated!");
        addDefault("Corporation.CEOSet", "&6CEO has been updated!");
        addDefault("Corporation.NotInCorporation", "&4Error: &cYou're not a member of any corporation!");
        addDefault("Corporation.NotCEO", "&4Error: &cYou're not the CEO!");
        addDefault("Corporation.CorporationNotFound", "&4Corporation {0} not found");
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
        addDefault("Corporation.AlreadyMember", "&c{0} is already a member of your corporation!");
        addDefault("Corporation.AlreadyMemberOther", "&c{0} is already a member of another corporation!");
        addDefault("Corporation.NotMember", "&c{0} is not a member of your corporation!");
        addDefault("Corporation.UserNotMember", "&4Error: &c{0}&r&c is not a member of {1}&r&c!");
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
        addDefault("Corporation.NotInCorporation", "&4Error:&c You aren't in any corporation!");
        addDefault("Corporation.Sign.ItemAlreadySold", "&4This item has been already sold. You can't re-sell it");
        addDefault("Corporation.ItemsSold", "&6{0} has sold {1} items in the last {2}");
        addDefault("Corporation.UserHasBeenInvited", "&3{0}&r&3 has been invited");
        addDefault("Corporation.GotInvited", "&3You've got invited for corporation: {1}&r&3 by {0}.&r&3 Use /corporation join {2}&r&3 to accept.");
        addDefault("Corporation.NotInvited", "&4Error: &cYou're not invited to {0}&r&c!");
        addDefault("Corporation.AlreadyInvited", "&4Error: &c{0}&r&c is already invited!");
        addDefault("Corporation.AlreadyInCorporation", "&4Error: &cYou're already in a corporation: {0}");
        addDefault("Corporation.UserAlreadyInCorporation", "&4Error: &c{0}&r&c is already in a corporation: {1}&r&c!");
        addDefault("Corporation.Joined", "&3{0}&r&3 joined the corporation!");
        addDefault("Corporation.CorporationFull", "&4The corporation is full!");
        addDefault("Corporation.Sign.CantPickup", "&4Error:&c Your inventory is full!");
        addDefault("Corporation.Sign.Bought", "&aSuccessfully bought {0} {1} for {2}{CURRENCY}!");
        addDefault("Corporation.Sign.Sold", "&aSuccessfully sold {0} {1} for {2}{CURRENCY}!");
        addDefault("Corporation.Sign.InvalidCreateLocation", "&cYou can create trade signs only in your own corporation!");
        addDefault("Corporation.Sign.InvalidSellCorporation", "&cYou can only sell items to your own corporation!");
        addDefault("Corporation.Sign.InvalidBuyCorporation", "&cYou can only buy items from own corporation!");
        addDefault("Corporation.Sign.InvalidChest", "&cChest not found!");
        addDefault("Corporation.Sign.NoItemsFound", "&cNo Items left!");
        addDefault("Corporation.Sign.ChestFull", "&cThe chest is full!");
        addDefault("Corporation.Sign.NotEnoughMoney", "&cYou don't have enough money");
        addDefault("Corporation.Sign.SoldWatermark", "&4- &oSold");
        addDefault("Corporation.Sign.CantSell", "&cYou can't sell items!");
        addDefault("Corporation.Sign.CantBuy", "&cYou can't buy items!");

        addDefault("Corporation.UnknownPermission", "&4Error: &cUnknown permission: {0}&c!");
        addDefault("Corporation.DeletingDefaultRank",
                   "&4Can't delete a default rank. Please set an other rank as default rank before deleting this one!");
        addDefault("Corporation.RankNotFound", "&4Rank &c\"{0}\"&r&4 not found");
        addDefault("Corporation.NotEnoughPriority", "&4You can't modify or assign a rank with a higher priorty!");
        addDefault("StockMarket.NotEnoughStocksLeft", "&cNot enough stocks left!");
        addDefault("StockMarket.ForceFailed", "&4Couldn't update stocks");
        addDefault("StockMarket.PendingTransferNotDone", "&4User already has a pending transfer");
        addDefault("StockMarket.WaitingForTransfer", "&aWaiting for accept...");
        addDefault("StockMarket.TransferCooldown", "&4You've to wait {0} minutes before using this command again");
        addDefault("StockMarket.TransferRequest", "&7[&4StockTransfer&2]&2{0} wants to sell you {1} {2} for {3} {CURRENCY}");
        addDefault("StockMarket.TransferAccept", "&2Use /sm transferaccept to accept it");
        addDefault("StockMarket.TransferWithSelf", "&4You can't transfer stocks to yourself");
        addDefault("StockMarket.NoPendingTransfers", "&2You don't have pending transfers!");
        addDefault("StockMarket.TransferDeclined", "&2{0} declined tje transfer");
        addDefault("StockMarket.TransferFailed", "&2The transfer failed for some reason");
        addDefault("StockMarket.TransferAccepted", "&2{0} accepted the transfer. {1} {CURRENCY} has been added to your account.");
        addDefault("StockMarket.BuyingDisabled", "&2This corporation currently doesn't buy stocks");
        addDefault("StockMarket.MaxStocksAmount", "&4Error: &4Max stocks amount: &c{0}!");
        addDefault("StockMarket.MaxStocksPrice", "&4Error: &Max price for stocks: &c{0}!");
        addDefault("StockMarket.MinStocksDividend", "&4Error: &4Min dividend has to be equal or greater than: &c{0})");
        addDefault("StockMarket.MinStocksShare", "&4Error: &4Min Share: &c{0}");
        addDefault("StockMarket.PaydayEntry", "Dividends for stock: &a&l{0}");

        addDefault("Ad.Message", "&7[&6Ad&7]&f {DISPLAYNAME} &6{MESSAGE}");
        addDefault("Ad.Timout", "&4Error: &cYou can use this command only every {0} minutes. Please wait {1} minutes before using this command again.");

        addDefault("Payday.Taxes", "{0}% Taxes");
        addDefault("Payday.Payday", "Payday ({0} {CURRENCY})");
        addDefault("AntiEscape.BanMessage", "You have been banned for {0} Minutes. Reason: Escape from PvP by quit");

        addDefault("Level.NotEnoughLevel",
                   "&4Your level is not high enough to do this. You need &c{0} &4but are currently only on &c{1}&4!");
        addDefault("Level.NotLoggedIn", "&4Not logged in!");

        addDefault("Party.NotEnoughPriority", "&4You can't modify or assign a rank with a higher priorty!");
        addDefault("Party.RankNotFound", "&4Rank &c\"{0}\"&r&4 not found");
        addDefault("Party.DeletingDefaultRank", "&4Can't delete a default rank. Please set an other rank as default rank before deleting this one!");
        addDefault("Party.NotInParty", "&4Not in any party!");
        addDefault("Party.UserHasBeenInvited", "&3{0}&r&3 has been invited");
        addDefault("Party.GotInvited", "&3You've got invited for party: {1}&r&3 by {0}.&r&3 Use /party join {2}&r&3 to accept.");
        addDefault("Party.AlreadyInParty", "&4Error: &cYou're already in a party: {0}");
        addDefault("Party.PartyNotFound", "&4Error: &cParty \"{0}\"&r&c not found!");
        addDefault("Party.NotInvited", "&4Error: &cYou're not invited to {0}&r&c!");
        addDefault("Party.Joined", "&3{0}&r&3 joined the party!");
        addDefault("Party.Left", "&3{0}&r&3 left the party!");
        addDefault("Party.UserAlreadyInParty", "&4Error: &c{0}&r&c is already in a party: {1}&r&c!");
        addDefault("Party.UserNotMember", "&4Error: &c{0}&r&c is not a member of {1}&r&c!");
        addDefault("Party.Kicked", "&c{0}&r&4 has been kicked by &c{1}&r&4 from the party!");
        addDefault("Party.Deposit", "&4{0} has deposited {1} {CURRENCY} to party account");
        addDefault("Party.Withdraw", "&4{0} has withdrawn {1} {CURRENCY} from party account");
        addDefault("Party.NotEnoughMoney", "&4Error: &cThe party doesn't have enough money!");
        addDefault("Party.AlreadyInvited", "&4Error: &c{0}&r&c is already invited!");
        addDefault("Party.UnknownPermission", "&4Error: &cUnknown permission: {0}&c!");
    }
}
