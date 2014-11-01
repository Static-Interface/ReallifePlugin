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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.static_interface.reallifeplugin.ReallifeMain;
import de.static_interface.sinklibrary.api.configuration.Configuration;
import de.static_interface.sinklibrary.util.BukkitUtil;
import de.static_interface.sinklibrary.util.StringUtil;
import de.static_interface.sinklibrary.util.VaultBridge;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Corporation {

    final String accountName;
    final String name;
    final Configuration config;

    public Corporation(Configuration config, String name) {
        this.config = config;
        this.name = name;
        this.accountName = "Corp_" + name.replace("_", "");

        if (!VaultBridge.isAccountAvailable(accountName)) {
            VaultBridge.createAccount(accountName);
        }
    }

    public List<UUID> getCoCEOs() {
        List<UUID> tmp = new ArrayList<>();

        for (String s : config.getYamlConfiguration().getStringList("Corporations." + getName() + "." + CorporationValues.VALUE_CO_CEO)) {
            tmp.add(UUID.fromString(s));
        }
        return tmp;
    }

    public void setBase(World world, String regionId) {
        setValue(CorporationValues.VALUE_BASE, world.getName() + ":" + regionId);
        save();
    }

    public String getName() {
        return name;
    }

    public String getAccountName() {
        return accountName;
    }

    public void addMember(UUID uuid) {
        HashMap<UUID, String> allMembers = getMembersFromConfig();
        allMembers.put(uuid, CorporationValues.RANK_DEFAULT);
        Gson gson = new Gson();
        setValue(CorporationValues.VALUE_MEMBERS, gson.toJson(uuidHashMapToStringHashMap(allMembers)));
        if (getBase() != null) {
            DefaultDomain rgMembers = getBase().getMembers();
            rgMembers.addPlayer(BukkitUtil.getNameByUniqueId(uuid));
            getBase().setMembers(rgMembers);
        }
        save();
    }

    public void removeMember(UUID uuid) {
        HashMap<UUID, String> allMembers = getMembersFromConfig();
        allMembers.remove(uuid);
        Gson gson = new Gson();
        setValue(CorporationValues.VALUE_MEMBERS, gson.toJson(uuidHashMapToStringHashMap(allMembers)));
        if (getBase() != null && getBase().getMembers() != null) {
            DefaultDomain rgMembers = getBase().getMembers();
            rgMembers.removePlayer(BukkitUtil.getNameByUniqueId(uuid));
            getBase().setMembers(rgMembers);
        }
        save();
    }

    public void save() {
        config.save();
    }

    public double getBalance() {
        return VaultBridge.getBalance(accountName);
    }

    public void addBalance(double amount) {
        VaultBridge.addBalance(accountName, amount);
    }

    public ProtectedRegion getBase() {
        String[] baseraw = ((String) getValue(CorporationValues.VALUE_BASE)).split(":");
        String world = baseraw[0];
        String regionId = baseraw[1];

        return ReallifeMain.getInstance().getWorldGuardPlugin().getRegionManager(Bukkit.getWorld(world)).getRegion(regionId);
    }

    public String getFormattedName() {
        return ChatColor.DARK_GREEN + name.replace("_", " ") + ChatColor.RESET;
    }

    public Set<UUID> getMembers() {
        Set<UUID> tmp = getMembersFromConfig().keySet();
        tmp.removeAll(getCoCEOs());
        tmp.remove(getCEO());
        return tmp;
    }

    public Set<UUID> getAllMembers() {
        return getMembersFromConfig().keySet();
    }

    public UUID getCEO() {
        return UUID.fromString((String) getValue(CorporationValues.VALUE_CEO));
    }

    public void setCEO(UUID uuid) {
        setValue(CorporationValues.VALUE_CEO, uuid.toString());
        setRank(uuid, CorporationValues.RANK_CEO);
    }

    public void addCoCeo(UUID uuid) {
        Set<String> tmp = new HashSet<>();
        for (UUID key : getCoCEOs()) {
            tmp.add(key.toString());
        }
        tmp.add(uuid.toString());
        ArrayList<String> asdf = new ArrayList<>();
        asdf.addAll(tmp);
        setValue(CorporationValues.VALUE_CO_CEO, asdf);
        setRank(uuid, CorporationValues.RANK_CO_CEO);
    }

    public void removeCoCeo(UUID uuid) {
        Set<String> tmp = new HashSet<>();
        for (UUID key : getCoCEOs()) {
            tmp.add(key.toString());
        }
        tmp.remove(uuid.toString());
        ArrayList<String> asdf = new ArrayList<>();
        asdf.addAll(tmp);
        setValue(CorporationValues.VALUE_CO_CEO, asdf);
        setRank(uuid, CorporationValues.RANK_DEFAULT);
    }

    public boolean isCoCeo(UUID uuid) {
        return getCoCEOs().contains(uuid);
    }

    public void setRank(UUID user, String rank) {
        HashMap<UUID, String> members = getMembersFromConfig();
        members.put(user, rank);
        Gson gson = new Gson();
        setValue(CorporationValues.VALUE_MEMBERS, gson.toJson(uuidHashMapToStringHashMap(members)));
    }

    private HashMap<UUID, String> getMembersFromConfig() {
        Gson gson = new Gson();
        String json = (String) getValue(CorporationValues.VALUE_MEMBERS);
        Type typetoken = new TypeToken<HashMap<String, String>>() {
        }.getType();
        HashMap<String, String> map = gson.fromJson(json, typetoken);
        return stringHashMapToUuidHashMap(map);
    }

    private HashMap<String, String> uuidHashMapToStringHashMap(HashMap<UUID, String> convertMap) {
        HashMap<String, String> tmp = new HashMap<>();
        for (UUID uuid : convertMap.keySet()) {
            tmp.put(uuid.toString(), convertMap.get(uuid));
        }
        return tmp;
    }

    private HashMap<UUID, String> stringHashMapToUuidHashMap(HashMap<String, String> convertMap) {
        HashMap<UUID, String> tmp = new HashMap<>();
        for (String key : convertMap.keySet()) {
            tmp.put(UUID.fromString(key), convertMap.get(key));
        }
        return tmp;
    }

    public Object getValue(String path) {
        return config.get("Corporations." + getName() + "." + path);
    }

    public void setValue(String path, Object value) {
        config.set("Corporations." + getName() + "." + path, value);
        save();
    }

    public String getRank(UUID user) {
        String rank = getMembersFromConfig().get(user);
        if (StringUtil.isStringEmptyOrNull(rank)) {
            if (getCEO() == user) {
                setRank(user, CorporationValues.RANK_CEO);
            } else if (getCoCEOs().contains(user)) {
                setRank(user, CorporationValues.RANK_CO_CEO);
            } else {
                setRank(user, CorporationValues.RANK_DEFAULT);
            }
            return getRank(user);
        }
        return ChatColor.GOLD + rank;
    }
}
