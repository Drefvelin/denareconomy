package net.tfminecraft.denareconomy;

import java.io.File;

import org.bukkit.plugin.java.JavaPlugin;

import net.tfminecraft.denareconomy.loaders.CoinLoader;
import net.tfminecraft.denareconomy.managers.CommandManager;
import net.tfminecraft.denareconomy.managers.MoneyManager;
import net.tfminecraft.denareconomy.managers.PlayerManager;

public class DenarEconomy extends JavaPlugin {
	
	public static DenarEconomy plugin;
	
	private final CommandManager commands = new CommandManager();
	
	private final CoinLoader coinLoader = new CoinLoader();
	
	private static final PlayerManager playerManager = new PlayerManager();
	private static final MoneyManager moneyManager = new MoneyManager();
	
	@Override
	public void onEnable() {
		plugin = this;
		createFolders();
		createConfigs();
		loadConfigs();
		registerListeners();
		getCommand(commands.cmd1).setExecutor(commands);
		playerManager.start();
	}
	
	public void loadConfigs() {
		coinLoader.loadCoins(new File(getDataFolder(), "coins.yml"));
	}
	
	public void registerListeners() {
		getServer().getPluginManager().registerEvents(playerManager, this);
		getServer().getPluginManager().registerEvents(moneyManager, this);
	}
	public void createFolders() {
		if (!getDataFolder().exists()) getDataFolder().mkdir();
		File subFolder = new File(getDataFolder(), "Data");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "PlayerData");
		if(!subFolder.exists()) subFolder.mkdir();
	}
	public void createConfigs() {
		String[] files = {
				"coins.yml"
				};
		for(String s : files) {
			File newConfigFile = new File(getDataFolder(), s);
	        if (!newConfigFile.exists()) {
	        	newConfigFile.getParentFile().mkdirs();
	            saveResource(s, false);
	        }
		}
	}
	
	public static PlayerManager getPlayerManager() {
		return playerManager;
	}
	public static MoneyManager getMoneyManager() {
		return moneyManager;
	}
}
