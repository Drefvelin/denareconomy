package net.tfminecraft.DenarEconomy.Managers;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.tfminecraft.DenarEconomy.Data.PlayerData;
import net.tfminecraft.DenarEconomy.Database.Database;

public class PlayerManager implements Listener{
	private HashMap<Player, PlayerData> data = new HashMap<>();
	
	public boolean exists(Player p) {
		return data.containsKey(p);
	}
	
	public PlayerData get(Player p) {
		if(!exists(p)) add(p);
		return data.get(p);
	}

	public PlayerData get(String id) {
		Player p = Bukkit.getPlayer(UUID.fromString(id));
		if(p != null && p.isOnline()){
			if(exists(p)) return get(p);
		}
		return null;
	}
	
	public void add(Player p) {
		if(exists(p)) return;
		if(Database.hasPlayerData(p.getUniqueId())) data.put(p, Database.loadPlayerData(p));
		else data.put(p, new PlayerData(p));
	}
	
	public void init(Player p) {
		if(exists(p)) return;
		add(p);
	}
	
	public void start() {
		for(Player p : Bukkit.getOnlinePlayers()) {
			init(p);
		}
	}

	public double getBankBal(String id) {
		Player p = Bukkit.getPlayer(UUID.fromString(id));
		if(p != null && p.isOnline()){
			if(exists(p)) return get(p).getBank().getBal();
		} else if(Database.hasPlayerData(UUID.fromString(id))){
			return Database.getBankBalance(UUID.fromString(id));
		}
		return 0.0;
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		init(p);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e){
		Player p = e.getPlayer();
		if(!exists(p)) return;
		Database.savePlayerData(get(p));
		data.remove(p);
	}
}
