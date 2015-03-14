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

package de.static_interface.reallifeplugin.module.payday;

import de.static_interface.reallifeplugin.PayDayTask;
import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.database.Database;
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.sinklibrary.Constants;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;

public class PaydayModule extends Module {

    public static final String NAME = "Payday";
    private static PaydayModule instance;

    private PayDayTask payDayTask;
    private BukkitTask payDayBukkitTask;

    public PaydayModule(Plugin plugin, @Nullable Database db) {
        super(plugin, ReallifeMain.getInstance().getSettings(), db, NAME, true);
    }

    public static PaydayModule getInstance() {
        return instance;
    }

    @Override
    protected void onEnable() {
        instance = this;
        addDefaultValue("Time", 60, "Time in minutes");
        addDefaultValue("MinOnlineTime", 30);
        addDefaultValue("Taxesbase", 0.1);
        addListener(new PaydayListener(this));

        long delay = PaydayModule.getInstance().getPaydayTime() * 60 * (long) Constants.TICK;
        payDayTask = new PayDayTask(getDatabase());
        payDayBukkitTask = Bukkit.getScheduler().runTaskTimer(getPlugin(), payDayTask, delay, delay);
    }

    @Override
    protected void onDisable() {
        instance = null;
        if (payDayBukkitTask != null) {
            payDayBukkitTask.cancel();
        }
    }

    public int getPaydayTime() {
        return (int) getValue("Time");
    }

    public int getMinOnlineTime() {
        return (int) getValue("MinOnlineTime");
    }

    public double getTaxesBase() {
        return (double) getValue("Taxesbase");
    }

    public PayDayTask getPayDayTask() {
        return payDayTask;
    }
}
