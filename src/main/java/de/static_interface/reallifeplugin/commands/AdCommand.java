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

package de.static_interface.reallifeplugin.commands;


import static de.static_interface.reallifeplugin.ReallifeLanguageConfiguration.*;

import de.static_interface.reallifeplugin.*;
import de.static_interface.sinklibrary.*;
import de.static_interface.sinklibrary.api.command.*;
import de.static_interface.sinklibrary.user.*;
import de.static_interface.sinklibrary.util.*;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.plugin.*;

import java.util.*;
import java.util.concurrent.*;

public class AdCommand extends SinkCommand {

    HashMap<UUID, Long> timeouts = new HashMap<>();

    public AdCommand(Plugin plugin) {
        super(plugin);
        getCommandOptions().setPlayerOnly(true);
    }

    @Override
    protected boolean onExecute(CommandSender sender, String label, String[] args) {
        IngameUser user = (IngameUser) SinkLibrary.getInstance().getUser(sender);
        Player p = (Player) sender;

        double price = ReallifeMain.getInstance().getSettings().getAdPrice();
        if (user.getBalance() < price) {
            user.sendMessage(m("General.NotEnoughMoney"));
            return true;
        }

        long currenttime = System.currentTimeMillis();
        long settingsTimeout = ReallifeMain.getInstance().getSettings().getAdTimeout() * 1000 * 60;

        Long timeout = timeouts.get(p.getUniqueId());
        if (timeout != null && timeout > currenttime) {
            long timeleft = TimeUnit.MILLISECONDS.toMinutes(currenttime - timeout);
            p.sendMessage(StringUtil.format(m("Ad.Timout"), TimeUnit.MILLISECONDS.toMinutes(settingsTimeout), (Math.abs(timeleft) + 1)));
            return true;
        }

        if (args.length < 1) {
            return false;
        }

        String message = ChatColor.translateAlternateColorCodes('&', StringUtil.formatArrayToString(args, " "));

        user.addBalance(-price);
        BukkitUtil.broadcastMessage(StringUtil.format(m("Ad.Message"), p, message), false);
        if (settingsTimeout > 0) {
            timeouts.put(p.getUniqueId(), currenttime + settingsTimeout);
        }
        return true;
    }
}