package net.tfminecraft.DenarEconomy.Data;

import java.util.UUID;

import org.bukkit.entity.Player;

public class PlayerData {
	private transient Player p;
	private UUID id;
	private Account pouch;
	private Account bank;
	
	public PlayerData(Player p) {
		this.p = p;
		this.id = p.getUniqueId();
		this.pouch = new Account(0);
		this.bank = new Account(0, false);
	}

	public void setPlayer(Player p){
		this.p = p;
	}

	public UUID getId(){
		return id;
	}

	public Player getPlayer() {
		return p;
	}

	public Account getPouch() {
		return pouch;
	}
	
	public Account getBank() {
		return bank;
	}
}
