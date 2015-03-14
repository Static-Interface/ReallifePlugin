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

package de.static_interface.reallifeplugin.module.insurance;

import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.payday.PaydayModule;
import de.static_interface.sinklibrary.SinkLibrary;
import org.bukkit.plugin.Plugin;

public class InsuranceModule extends Module {

    public static final String NAME = "Insurance";

    public InsuranceModule(Plugin plugin) {
        super(plugin, ReallifeMain.getInstance().getSettings(), null, NAME, true);
    }

    @Override
    protected void onEnable() {
        if (!Module.isEnabled(PaydayModule.NAME)) {
            getPlugin().getLogger().warning("Payday module not active, deactivating...");
            disable();
            return;
        }

        //Todo: add whitelist/blacklist regions for insurances
        addDefaultValue("Account", "Insurances");
        addListener(new InsuranceListener(this));
        SinkLibrary.getInstance().registerCommand("insurance", new InsuranceCommand(this));
    }

    public String getInsuranceAccount() {
        return (String) getValue("Account");
    }
}
