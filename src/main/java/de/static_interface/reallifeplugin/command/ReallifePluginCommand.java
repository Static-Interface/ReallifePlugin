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
import de.static_interface.sinklibrary.api.command.SinkSubCommand;
import de.static_interface.sinklibrary.api.command.annotation.Aliases;
import de.static_interface.sinklibrary.api.command.annotation.DefaultPermission;
import de.static_interface.sinklibrary.api.command.annotation.Description;
import de.static_interface.sinklibrary.api.command.annotation.Usage;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.Collection;

@DefaultPermission
@Description("Manage the ReallifePlugin")
@Aliases("rp")
public class ReallifePluginCommand extends SinkCommand {

    public ReallifePluginCommand(Plugin plugin) {
        super(plugin);
        getCommandOptions().setIrcOpOnly(true);
    }

    @Override
    protected boolean onExecute(CommandSender commandSender, String s, String[] strings) throws ParseException {
        return false;
    }

    @Override
    public void onRegistered() {
        registerSubCommand(new SinkSubCommand<ReallifePluginCommand>(this, "payday") {
            @Description("Run manually a payday event")
            @DefaultPermission
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                if (Module.isEnabled(PaydayModule.class)) {
                    boolean skipTimeCheck = getCommandLine().hasOption('f');
                    PaydayModule module = Module.getModule(PaydayModule.class);
                    module.getPaydayTask().run(!skipTimeCheck);
                } else {
                    sender.sendMessage("Payday module not enabled");
                }
                return true;
            }

            @Override
            public void onRegistered() {
                Options options = new Options();
                Option skiptimecheck = Option.builder("f")
                        .desc("Skip online time checks")
                        .longOpt("force")
                        .build();
                options.addOption(skiptimecheck);
                getCommandOptions().setCliOptions(options);
            }
        });

        SinkSubCommand moduleSubCommand = new SinkSubCommand<ReallifePluginCommand>(this, "module") {
            @DefaultPermission
            @Description("Manage the modules")
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                return false;
            }
        };

        registerSubCommand(moduleSubCommand);

        SinkSubCommand moduleListSubCommand = new SinkSubCommand(moduleSubCommand, "list") {
            @DefaultPermission
            @Description("List all modules")
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
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

                return true;
            }
        };
        moduleSubCommand.registerSubCommand(moduleListSubCommand);

        SinkSubCommand moduleEnableSubCommand = new SinkSubCommand(moduleSubCommand, "enable") {
            @DefaultPermission
            @Description("Enable a module")
            @Usage("<module>")
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                if (args.length < 1) {
                    return false;
                }
                Module module = Module.getModule(args[0]);
                if (module == null) {
                    sender.sendMessage(ChatColor.RED + "Module not found");
                    return true;
                }

                if (module.isEnabled()) {
                    sender.sendMessage(ChatColor.RED + "Module already enabled");
                    return true;
                }

                module.setValue("Enabled", true);
                module.enable();
                sender.sendMessage(ChatColor.GREEN + "Enabled " + module.getName());
                return true;
            }
        };
        moduleSubCommand.registerSubCommand(moduleEnableSubCommand);

        SinkSubCommand moduleDisableSubCommand = new SinkSubCommand(moduleSubCommand, "disable") {
            @DefaultPermission
            @Description("Disable a module")
            @Usage("<module>")
            @Override
            protected boolean onExecute(CommandSender sender, String label, String[] args) throws ParseException {
                if (args.length < 1) {
                    return false;
                }

                Module module = Module.getModule(args[2]);
                if (module == null) {
                    sender.sendMessage(ChatColor.RED + "Module not found");
                    return true;
                }

                if (!module.isEnabled()) {
                    sender.sendMessage(ChatColor.RED + "Module already disabled");
                    return true;
                }

                module.setValue("Enabled", false);
                module.disable();
                sender.sendMessage(ChatColor.GREEN + "Disabled " + module.getName());
                return true;
            }
        };
        moduleSubCommand.registerSubCommand(moduleDisableSubCommand);
    }
}
