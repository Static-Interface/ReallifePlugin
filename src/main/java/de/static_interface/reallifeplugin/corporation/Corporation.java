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

package de.static_interface.reallifeplugin.corporation;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.sinklibrary.configuration.ConfigurationBase;
import de.static_interface.sinklibrary.util.VaultHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Corporation {

    final String name;
    final ConfigurationBase config;

    protected ProtectedRegion base;
    protected List<UUID> members;
    protected UUID ceo;

    public Corporation(ConfigurationBase config, String name) {
        this.config = config;
        this.name = name;
        String[] baseraw = ((String) getValue(CorporationValues.BASE)).split(":");
        String world = baseraw[0];
        String regionId = baseraw[1];

        base = ReallifeMain.getWorldGuardPlugin().getRegionManager(Bukkit.getWorld(world)).getRegion(regionId);
        members = getMembersFromConfig();
        ceo = UUID.fromString((String) getValue(CorporationValues.CEO));
    }

    public void setBase(World world, String regionId) {
        this.base = ReallifeMain.getWorldGuardPlugin().getRegionManager(world).getRegion(regionId);
        setValue(CorporationValues.BASE, world.getName() + ":" + base);
        save();
    }

    public String getName() {
        return name;
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
        List<String> tmp = new ArrayList<>();
        for (UUID member : members) {
            if (member == uuid) {
                tmp.add(uuid.toString() + ":" + CorporationValues.DEFAULT_RANK);
            } else {
                tmp.add(member.toString() + ":" + getRank(member));
            }
        }
        setValue(CorporationValues.MEMBERS, tmp);
        DefaultDomain rgMembers = getBase().getMembers();
        rgMembers.addPlayer(Bukkit.getOfflinePlayer(uuid).getName());
        getBase().setMembers(rgMembers);
        save();
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        List<String> tmp = new ArrayList<>();
        for (UUID member : members) {
            tmp.add(member.toString() + ":" + getRank(member));
        }
        setValue(CorporationValues.MEMBERS, tmp);
        DefaultDomain rgMembers = getBase().getMembers();
        rgMembers.removePlayer(Bukkit.getOfflinePlayer(uuid).getName());
        getBase().setMembers(rgMembers);
        save();
    }

    public void save() {
        config.save();
    }

    public double getMoney() {
        return VaultHelper.getBalance(name);
    }

    public void addBalance(double amount) {
        VaultHelper.addBalance(name, amount);
    }

    public ProtectedRegion getBase() {
        return base;
    }

    public String getFormattedName() {
        return ChatColor.DARK_GREEN + name.replace("_", " ");
    }

    public List<UUID> getMembers() {
        return members;
    }

    public UUID getCEO() {
        return ceo;
    }

    public void setCEO(UUID ceo) {
        this.ceo = ceo;
        setValue(CorporationValues.CEO, ceo.toString());
        save();
    }

    private List<UUID> getMembersFromConfig() {
        List<UUID> tmp = new ArrayList<>();

        for (String s : config.getYamlConfiguration().getStringList("Corporations." + getName() + "." + CorporationValues.MEMBERS)) {
            tmp.add(UUID.fromString(s.split(":")[0]));
        }
        return tmp;
    }

    public Object getValue(String path) {
        return config.get("Corporations." + getName() + "." + path);
    }

    public void setValue(String path, Object value) {
        config.set("Corporations." + getName() + "." + path, value);
    }

    public String getRank(UUID user) {
        String line = null;
        for (String s : config.getYamlConfiguration().getStringList("Corporations." + getName() + "." + CorporationValues.MEMBERS)) {
            if (s.split(":")[0].equals(user.toString())) {
                line = s;
                break;
            }
        }
        if (line == null) {
            throw new AssertionError("This shouldn't happen :(");
        }
        return ChatColor.GOLD + line.split(":")[1];
    }
}
