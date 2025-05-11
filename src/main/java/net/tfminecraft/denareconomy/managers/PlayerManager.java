package net.tfminecraft.DenarEconomy.Managers;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import net.tfminecraft.DenarEconomy.Data.PlayerData;

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
		data.put(p, new PlayerData(p));
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
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		init(p);
	}
}
