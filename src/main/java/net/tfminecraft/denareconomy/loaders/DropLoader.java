package net.tfminecraft.DenarEconomy.Loaders;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import net.tfminecraft.DenarEconomy.Drop.Drop;

public class DropLoader {
	public static HashMap<Material, Drop> drops = new HashMap<>();
	public static HashMap<Material, Drop> get(){
		return drops;
	}
	public static Drop getByBlock(Material id) {
		if(!drops.containsKey(id)) return null;
		return drops.get(id);
	}
	public void load(File configFile) {
		FileConfiguration config = new YamlConfiguration();
        try {
        	config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
		Set<String> set = config.getKeys(false);

		List<String> list = new ArrayList<String>(set);
		
		for(String key : list) {
			Drop c = new Drop(key, config.getConfigurationSection(key));
			drops.put(c.getBlock(), c);
		}
	}
}