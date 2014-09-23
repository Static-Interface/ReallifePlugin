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

package de.static_interface.reallifeplugin.commands;


import static de.static_interface.reallifeplugin.LanguageConfiguration.m;

import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.SinkUser;
import de.static_interface.sinklibrary.command.Command;
import de.static_interface.sinklibrary.util.BukkitUtil;
import de.static_interface.sinklibrary.util.StringUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class AdCommand extends Command {

    public AdCommand(Plugin plugin) {
        super(plugin);
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }

    @Override
    protected boolean onExecute(CommandSender sender, String label, String[] args) {
        if(args.length < 1) {
            return false;
        }
        SinkUser user = SinkLibrary.getInstance().getUser(sender);
        Player p = (Player) sender;
        double price = ReallifeMain.getSettings().getAdPrice();
        if (user.getBalance() < price) {
            user.sendMessage(m("General.NotEnoughMoney"));
            return true;
        }

        String message = StringUtil.formatArrayToString(args, " ");

        user.addBalance(-price);
        BukkitUtil.broadcastMessage(StringUtil.format(m("Ad.Message"), p, message), false);
        return true;
    }
}