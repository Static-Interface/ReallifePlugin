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

package de.static_interface.reallifeplugin.command;

import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.payday.PaydayModule;
import de.static_interface.sinklibrary.api.command.SinkCommand;
import de.static_interface.sinklibrary.api.exception.NotEnoughArgumentsException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.Collection;

public class ReallifePluginCommand extends SinkCommand {

    public ReallifePluginCommand(Plugin plugin) {
        super(plugin);
        getCommandOptions().setIrcOpOnly(true);
    }

    @Override
    public boolean onExecute(CommandSender sender, String label, String[] args) {
        if (args.length < 1) {
            throw new NotEnoughArgumentsException();
        }

        switch (args[0]) {
            case "payday":
                if (Module.isEnabled(PaydayModule.NAME)) {
                    boolean skipTimeCheck = false;

                    for (String s : args) {
                        if (s.equalsIgnoreCase("skiptimecheck")) {
                            skipTimeCheck = true;
                            break;
                        }
                    }

                    PaydayModule module = Module.getModule(PaydayModule.NAME, PaydayModule.class);
                    module.getPayDayTask().run(!skipTimeCheck);
                    break;
                }
            case "module":
                if (args.length < 2) {
                    throw new NotEnoughArgumentsException();
                }

                switch (args[1]) {
                    case "list":
                        sender.sendMessage(ChatColor.GOLD + "ReallifePlugin Modules: ");
                        Collection<Module> moduleCollection = Module.getModules();
                        String s = "";
                        Module[] modules = moduleCollection.toArray(new Module[moduleCollection.size()]);
                        for (Module module : modules) {
                            String name;
                            if (module.isEnabled()) {
                                name = ChatColor.GREEN + module.getName();
                            } else {
                                name = ChatColor.RED + module.getName();
                            }

                            if (s.equals("")) {
                                s = name;
                                continue;
                            }

                            s += ChatColor.GRAY + ", " + name;
                        }
                        sender.sendMessage(s);
                        break;

                    case "enable": {
                        if (args.length < 3) {
                            throw new NotEnoughArgumentsException();
                        }
                        Module module = Module.getModule(args[2]);
                        if (module == null) {
                            sender.sendMessage(ChatColor.RED + "Module not found");
                            break;
                        }

                        if (module.isEnabled()) {
                            sender.sendMessage(ChatColor.RED + "Module already enabled");
                            break;
                        }

                        module.setValue("Enabled", true);
                        module.enable();
                        sender.sendMessage(ChatColor.GREEN + "Enabled " + module.getName());
                        break;

                    }

                    case "disable": {
                        if (args.length < 3) {
                            throw new NotEnoughArgumentsException();
                        }

                        Module module = Module.getModule(args[2]);
                        if (module == null) {
                            sender.sendMessage(ChatColor.RED + "Module not found");
                            break;
                        }

                        if (!module.isEnabled()) {
                            sender.sendMessage(ChatColor.RED + "Module already disabled");
                            break;
                        }

                        module.setValue("Enabled", false);
                        module.disable();
                        sender.sendMessage(ChatColor.GREEN + "Disabled " + module.getName());
                        break;
                    }
                }

                break;

            default:
                sender.sendMessage("Unknown subcommand: " + args[0]);
                return false;
        }
        return true;
    }
}
