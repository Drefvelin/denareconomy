package net.tfminecraft.denareconomy.managers;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import net.tfminecraft.denareconomy.data.PlayerData;

public class PlayerManager implements Listener{
	private HashMap<Player, PlayerData> data = new HashMap<>();
	
	public boolean exists(Player p) {
		return data.containsKey(p);
	}
	
	public PlayerData get(Player p) {
		if(!exists(p)) return null;
		return data.get(p);
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
