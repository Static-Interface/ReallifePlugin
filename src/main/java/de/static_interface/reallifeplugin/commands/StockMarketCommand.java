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

package de.static_interface.reallifeplugin.commands;

import static de.static_interface.reallifeplugin.ReallifeLanguageConfiguration.m;

import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.corporation.Corporation;
import de.static_interface.reallifeplugin.corporation.CorporationUtil;
import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.database.table.row.stockmarket.StockRow;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.api.command.SinkCommand;
import de.static_interface.sinklibrary.user.IngameUser;
import org.apache.commons.cli.ParseException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.SQLException;

public class StockMarketCommand extends SinkCommand {

    public StockMarketCommand(Plugin plugin) {
        super(plugin);
        getCommandOptions().setPlayerOnly(true);
    }

    @Override
    protected boolean onExecute(CommandSender commandSender, String label, String[] args) throws ParseException {
        if (args.length == 0) {
            return false;
        }

        String subcommand = args[0];

        IngameUser user = SinkLibrary.getInstance().getIngameUser((Player) sender);

        switch (subcommand.toLowerCase()) {
            case "add":
                if (args.length < 4) {
                    return false;
                }

                Corporation corp = CorporationUtil.getUserCorporation(user);

                if (!CorporationUtil.hasCeoPermissions(user, corp)) {
                    user.sendMessage(m("Corporation.NotCEO"));
                    return true;
                }

                if (corp.getTag() == null && args.length < 5) {
                    user.sendMessage(ChatColor.RED + "Tag not set!"); // Todo
                    return true;
                } else if (args.length >= 5) {
                    String tag = args[4];
                    if (tag.length() < 2 || tag.length() > 4) {
                        user.sendMessage(ChatColor.DARK_RED + "Min Tag Length: 2, Max Tag length: 4"); //Todo
                        return true;
                    }

                    corp.setTag(tag);
                }

                int amount = Integer.parseInt(args[1]);
                double price = Double.parseDouble(args[2]);
                double dividendPercent = Double.parseDouble(args[3]);

                Database db = ReallifeMain.getInstance().getDB();

                StockRow row = new StockRow();
                row.amount = amount;
                row.corpId = corp.getId();
                row.dividendPercent = dividendPercent;
                row.base = price;
                row.time = System.currentTimeMillis();

                try {
                    db.getStocksTable().insert(row);
                } catch (SQLException e) {
                    user.sendMessage(ChatColor.RED + "An internal error occured");
                    e.printStackTrace();
                }

                user.sendMessage(m("General.Success"));
                //Todo: broadcast?
                break;
            default:
                return false;
        }

        return true;
    }
}
