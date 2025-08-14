package net.tfminecraft.DenarEconomy.Managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Managers.RelationManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.SimpleFactions.Objects.FactionModifier;
import me.Plugins.SimpleFactions.enums.FactionModifiers;
import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Enums.APIType;
import me.Plugins.TLibs.Objects.API.ItemAPI;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.Indyuce.mmoitems.MMOItems;
import net.tfminecraft.DenarEconomy.DenarEconomy;
import net.tfminecraft.DenarEconomy.Data.Account;
import net.tfminecraft.DenarEconomy.Data.PlayerData;
import net.tfminecraft.DenarEconomy.Database.Database;
import net.tfminecraft.DenarEconomy.Drop.Drop;
import net.tfminecraft.DenarEconomy.Enum.Accounts;
import net.tfminecraft.DenarEconomy.Item.Coin;
import net.tfminecraft.DenarEconomy.Loaders.CoinLoader;
import net.tfminecraft.DenarEconomy.Loaders.DropLoader;

public class MoneyManager implements Listener{
	private PlayerManager pm = DenarEconomy.getPlayerManager();

	private final Map<UUID, ArmorStand> standMap = new HashMap<>();
	private final Map<UUID, Integer> taskMap = new HashMap<>();

	public Map<UUID, ArmorStand> getStandMap() {
		return standMap;
	}

	public Map<UUID, Integer> getTaskMap() {
		return taskMap;
	}

	
	public Coin getCoin(ItemStack i) {
		ItemAPI api = (ItemAPI) TLibs.getApiInstance(APIType.ITEM_API);
		for(Coin c : CoinLoader.get()) {
			if(api.getChecker().checkItemWithPath(i, c.getItem())) return c;
		}
		return null;
	}
	
	public double combinedValue(Coin c, ItemStack i) {
		double v = c.getValue()*100.0*i.getAmount();
		return Math.round(v)/100.0;
	}
	
	public static void transfer(Account from, Account to, double amount) {
		from.change(amount*-1);
		to.change(amount);
	}

	public void showPouch(Player p) {
		UUID uuid = p.getUniqueId();

		// Remove old stand if exists
		if (standMap.containsKey(uuid)) {
			ArmorStand old = standMap.remove(uuid);
			if (old != null && !old.isDead()) old.remove();

			Integer oldTask = taskMap.remove(uuid);
			if (oldTask != null) Bukkit.getScheduler().cancelTask(oldTask);
		}

		double a = pm.get(p).getPouch().getBal();
		double inv = 0;

		for (ItemStack i : p.getInventory().getContents()) {
			if (i == null || i.getType() == Material.AIR) continue;
			Coin c = getCoin(i);
			if (c == null || !c.canWithdraw()) continue;
			inv += combinedValue(c, i);
		}

		a += inv;
		final double amount = a;

		p.sendMessage("§6Showing "+amount+"d...");

		ArmorStand stand = p.getWorld().spawn(p.getLocation().add(0, 2.2, 0), ArmorStand.class, as -> {
			as.setCustomName(ChatColor.GOLD + "Balance: " + String.format("%.2f", amount) + "d");
			as.setCustomNameVisible(true);
			as.setVisible(false);
			as.setMarker(true);
			as.setGravity(false);
			as.setSmall(true);
		});

		standMap.put(uuid, stand);

		int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(DenarEconomy.plugin, () -> {
			if (!p.isOnline() || stand.isDead()) return;
			stand.teleport(p.getLocation().add(0, 2.2, 0));
		}, 0L, 2L);

		taskMap.put(uuid, taskId);

		// Cleanup after 5 seconds
		Bukkit.getScheduler().runTaskLater(DenarEconomy.plugin, () -> {
			ArmorStand s = standMap.remove(uuid);
			if (s != null && !s.isDead()) s.remove();

			Integer t = taskMap.remove(uuid);
			if (t != null) Bukkit.getScheduler().cancelTask(t);
		}, 20L * 5);
	}
	
	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent e) {
		Iterator<Map.Entry<UUID, ArmorStand>> iterator = standMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, ArmorStand> entry = iterator.next();
			ArmorStand stand = entry.getValue();
			if (stand != null && stand.getLocation().getChunk().equals(e.getChunk())) {
				stand.remove(); // remove entity from world
				iterator.remove(); // remove entry from map
				Integer taskId = taskMap.remove(entry.getKey());
				if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
			}
		}
	}




	
	public double doTaxes(String p, double amount) {
		double paidTax = 0;
		if(FactionManager.getByMember(p) != null) {
			Faction f = FactionManager.getByMember(p);
			if(f.getTaxRate() > 0) {
				if(f.getBank() != null) {
					double tax = f.getTaxRate()/100.0*amount;
					paidTax += tax;
					f.giveTax(tax);
				}
			}
			for(FactionModifier mod : f.getModifiers()) {
				if(paidTax >= amount) break;
				if(mod.getFrom() == null) continue;
				if(!mod.getType().equals(FactionModifiers.TAX)) continue;
				double tax = mod.getAmount()/100.0*amount;
				String overlord = RelationManager.getOverlord(f);
				if(overlord != null && overlord.equalsIgnoreCase(mod.getFrom().getId())) {
					tax = mod.getFrom().getVassalTaxRate()/100.0*tax;
				}
				Faction from = mod.getFrom();
				if(from.getBank() == null) continue;
				paidTax += tax;
				from.giveTax(tax);
			}
		}
		return Math.round(paidTax*100.0)/100.0;
	}
	
	public void addMoney(Player p, double amount, boolean silent, boolean taxable) {
		addMoneyToAccount(p.getUniqueId().toString(), amount, silent, taxable, Accounts.POUCH);
	}

	public double getServerBal(Accounts account) {
		return Database.getTotalAmount(account);
	}

	public void changeBal(String id, double amount, Accounts a){
		PlayerData pd = pm.get(UUID.fromString(id));
		Account account = null;
		switch (a) {
			case POUCH:
				account = pd.getPouch();
				break;
			case BANK:
				account = pd.getBank();
				break;
			default:
				break;
		}
		if(account == null) return;
		account.change(amount);
		Player p = Bukkit.getPlayer(UUID.fromString(id));
		if(p == null) {
			pm.save(UUID.fromString(id));
		}
	}

	public void addMoneyToAccount(String id, double amount, boolean silent, boolean taxable, Accounts a) {
		if(id == null){
			System.out.println("Error null ID");
			return;
		}
		Player p = Bukkit.getPlayer(UUID.fromString(id));
		String name = " ";
		if(p != null && p.isOnline()){
			name = p.getName();
		} else {
			name = Bukkit.getOfflinePlayer(UUID.fromString(id)).getName();
		}
		double tax = 0.0;
		if(taxable) {
			tax = doTaxes(name, amount);
			if(tax > 0) {
				amount -= tax;
			}
			amount = Math.round(amount*100.0)/100.0;
		}
		
		if(!silent && p != null) {
			p.sendMessage(StringFormatter.formatHex("#dbaf1d+#b39122"+amount+"#dbaf1dd"));
			if(tax > 0) p.sendMessage(StringFormatter.formatHex("#44524f("+tax+" in tax)"));
		}
		
		if(amount <= 0) return;
		changeBal(id, amount, a);
	}
	
	public List<ItemStack> amountToItems(double amount) {
	    Map<String, Integer> itemCounts = new HashMap<>();

	    for (Coin c : CoinLoader.getSortedCoins()) {
	        if (!c.canWithdraw()) continue;

	        int count = 0;
	        while (amount >= c.getValue()) {
	            amount -= c.getValue();
	            count++;
	        }

	        if (count > 0) {
	            itemCounts.put(c.getItem(), itemCounts.getOrDefault(c.getItem(), 0) + count);
	        }
	    }

	    List<ItemStack> items = new ArrayList<>();
	    for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
	        String[] parts = entry.getKey().split("\\.");
	        int count = entry.getValue();

	        if (parts[0].equalsIgnoreCase("v")) {
	            ItemStack item = new ItemStack(Material.valueOf(parts[1].toUpperCase()), count);
	            items.add(item);
	        } else if (parts[0].equalsIgnoreCase("m")) {
	            // MMOItems usually returns a new ItemStack each time, so we set the amount after fetching
	            ItemStack item = MMOItems.plugin.getItem(parts[1].toUpperCase(), parts[2].toUpperCase());
	            if (item != null) {
	                item.setAmount(count);
	                items.add(item);
	            }
	        }
	    }

	    return items;
	}

	
	public Item spawnMoney(Player p, Location loc, ItemStack i, boolean silent) {
		Location spawnLoc = loc;
		if(p != null) spawnLoc = p.getEyeLocation();
		if(silent) {
			ItemMeta m = i.getItemMeta();
			NamespacedKey key = new NamespacedKey(DenarEconomy.plugin, "silent");
			m.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
			i.setItemMeta(m);
		}
		Item item = spawnLoc.getWorld().dropItem(spawnLoc.clone().add(0, -0.4, 0), i);
		if(p != null) {
			p.swingMainHand();
			item.setVelocity(p.getLocation().getDirection().clone().normalize().multiply(0.5));
		}
		return item;
	}

	@SuppressWarnings("unused")
	public void dropItems(Player p, Location loc, double amount) {
		final Vector launchVector = new Vector(
			(Math.random() - 0.5) * 0.2, // small horizontal motion (left/right)
			0.2 + Math.random() * 0.1,   // slight upward motion
			(Math.random() - 0.5) * 0.2  // small horizontal motion (forward/back)
		);
		List<ItemStack> items = amountToItems(amount);
	    if (items.isEmpty()) return;

	    boolean[] named = {false}; // Use array to allow modification in inner class
	    final UUID[] idHolder = new UUID[1];
	    final ItemStack[] firstItemHolder = new ItemStack[1];

	    for (int i = 0; i < items.size(); i++) {
	        final ItemStack original = items.get(i);
	        final int index = i;

	        new BukkitRunnable() {
	            @Override
	            public void run() {
	                ItemStack item = original.clone(); // Clone to avoid shared meta issues
	                ItemMeta m = item.getItemMeta();
					if(p != null) {
						NamespacedKey senderKey = new NamespacedKey(DenarEconomy.plugin, "sender");
	                	m.getPersistentDataContainer().set(senderKey, PersistentDataType.STRING, p.getUniqueId().toString());
					}
	                

	                NamespacedKey valueKey = new NamespacedKey(DenarEconomy.plugin, "customValue");

	                if (!named[0]) {
	                    named[0] = true;
	                    m.getPersistentDataContainer().set(valueKey, PersistentDataType.DOUBLE, amount);
	                    item.setItemMeta(m);
	                    Item first = spawnMoney(p, loc, item, false);
						if(p == null) first.setVelocity(launchVector);
	                    idHolder[0] = first.getUniqueId();
	                    firstItemHolder[0] = first.getItemStack();
	                } else {
	                    if (idHolder[0] == null) {
	                        if(p != null) p.sendMessage("Error: First money item not initialized.");
	                        return;
	                    }

	                    m.getPersistentDataContainer().set(valueKey, PersistentDataType.DOUBLE, 0.0);
	                    NamespacedKey chain = new NamespacedKey(DenarEconomy.plugin, "chained");
	                    m.getPersistentDataContainer().set(chain, PersistentDataType.STRING, idHolder[0].toString());
	                    item.setItemMeta(m);

	                    Item newItem = spawnMoney(p, loc, item, true);
						if(p == null) newItem.setVelocity(launchVector);

	                    if (firstItemHolder[0] != null) {
	                        ItemMeta firstM = firstItemHolder[0].getItemMeta();
	                        NamespacedKey chainTo = new NamespacedKey(DenarEconomy.plugin, "chained_to");
	                        String current = firstM.getPersistentDataContainer().get(chainTo, PersistentDataType.STRING);
	                        if (current == null) {
	                            firstM.getPersistentDataContainer().set(chainTo, PersistentDataType.STRING, newItem.getUniqueId().toString());
	                        } else {
	                            firstM.getPersistentDataContainer().set(chainTo, PersistentDataType.STRING, current + ";" + newItem.getUniqueId().toString());
	                        }
	                        firstItemHolder[0].setItemMeta(firstM);
	                    }
	                }
	            }
	        }.runTaskLater(DenarEconomy.plugin, index);
	    }
	}
	
	public void pay(Player p, double amount) {
	    PlayerData pd = pm.get(p);
	    Account pouch = pd.getPouch();

	    if (pouch.getBal() < amount) {
	        p.sendMessage(StringFormatter.formatHex("#a33d1dNot enough funds in pouch"));
	        return;
	    }

	    pouch.change(-amount);
	    dropItems(p, null, amount);
	}

	@EventHandler
	public void breakBlock(BlockBreakEvent e) {
		if(e.getPlayer() == null) return;
		Block b = e.getBlock();
		Drop drop = DropLoader.getByBlock(b.getType());
		if(drop == null) return;

		// Check if it's a crop and fully grown
		BlockData data = b.getBlockData();
		if (data instanceof Ageable ageable) {
			if (ageable.getAge() < ageable.getMaximumAge()) {
				return; // Not fully grown
			}
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				if(b.getLocation().getBlock().getType().equals(drop.getBlock())) return;
				if(Math.random() < drop.getChance()) {
					dropItems(null, b.getLocation().clone().add(0.5, 0.1, 0.5), drop.getAmount());
				}
			}
		}.runTaskLater(DenarEconomy.plugin, 5);
	}

	@EventHandler
	public void depositMaterials(PlayerInteractEvent e) {
		if(!(e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK))) return;
		Player p = e.getPlayer();
		ItemStack i = p.getInventory().getItemInMainHand();
		Coin c = getCoin(i);
		if(c == null) return;
		if(c.canWithdraw()) return;
		if(FactionManager.getByMember(p.getName()) == null) return;
		Faction f = FactionManager.getByMember(p.getName());
		if(f.getBank() == null) return;
		if(!f.getBank().getChunk().equals(p.getLocation().getChunk())) return;
		addMoneyToAccount(p.getUniqueId().toString(), c.getValue()*i.getAmount(), false, true, Accounts.BANK);
		p.playSound(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
		i.setAmount(0);
	}
	
	@EventHandler
	public void pickupCoin(EntityPickupItemEvent e) {
		if(!(e.getEntity() instanceof Player)) return;
		Player p = (Player) e.getEntity();
		ItemStack item = e.getItem().getItemStack();
		Coin c = getCoin(item);
		if(c == null) return;
		if(!c.canWithdraw()) return;
		e.setCancelled(true);
		e.getItem().remove();
		ItemMeta m = item.getItemMeta();
		
		NamespacedKey chain = new NamespacedKey(DenarEconomy.plugin, "chained");
        String chained = m.getPersistentDataContainer().get(chain, PersistentDataType.STRING);
        if(chained != null) {
        	return;
        }
        chain = new NamespacedKey(DenarEconomy.plugin, "chained_to");
        chained = m.getPersistentDataContainer().get(chain, PersistentDataType.STRING);
        
        if(chained != null) {
        	String[] items = chained.split("\\;");
        	for(String id : items) {
        		Entity chainedEntity = Bukkit.getEntity(UUID.fromString(id));
        		if(chainedEntity != null) chainedEntity.remove();
        	}
        }
        
        p.playSound(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
		NamespacedKey key = new NamespacedKey(DenarEconomy.plugin, "silent");
		Integer silent = m.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
		
		NamespacedKey value = new NamespacedKey(DenarEconomy.plugin, "customValue");
		Double customValue = m.getPersistentDataContainer().get(value, PersistentDataType.DOUBLE);
		double amount = 0.0;
		if(customValue != null) {
			amount = customValue;
		} else {
			amount = combinedValue(c, item);
		}
		
		key = new NamespacedKey(DenarEconomy.plugin, "sender");
		String sender = m.getPersistentDataContainer().get(key, PersistentDataType.STRING);
		boolean tax = true;
		if(sender != null) {
			if(p.getUniqueId().toString().equalsIgnoreCase(sender)) tax = false;
		}
		
		addMoney(p, amount, silent != null, tax);
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player victim = event.getEntity();
		Player killer = victim.getKiller();

		if (killer == null || killer == victim) return; // Not PvP or suicide

		PlayerData pd = pm.get(victim);
		if (pd == null) return;

		Account pouch = pd.getPouch();
		double balance = pouch.getBal();
		if (balance <= 0) return;

		pouch.change(-balance); // Remove from pouch
		dropItems(null, victim.getLocation(), balance); // Drop money at death location
	}
	
	@EventHandler
	public void coinSpawn(EntitySpawnEvent e) {
		if(!(e.getEntity() instanceof Item)) return;
		Item i = (Item) e.getEntity();
		Coin c = getCoin(i.getItemStack());
		if(c == null) return;
		if(!c.canWithdraw()) return;
		ItemStack item = i.getItemStack();
		ItemMeta m = item.getItemMeta();
		
		NamespacedKey key = new NamespacedKey(DenarEconomy.plugin, "customValue");
		if(m.getPersistentDataContainer().get(key, PersistentDataType.DOUBLE) == null) {
			i.setCustomNameVisible(true);
			i.setCustomName(StringFormatter.formatHex("#b39122"+combinedValue(c, i.getItemStack())+"#dbaf1dd"));
		} else {
			double amount = m.getPersistentDataContainer().get(key, PersistentDataType.DOUBLE);
			if(amount > 0) {
				i.setCustomNameVisible(true);
				i.setCustomName(StringFormatter.formatHex("#b39122"+amount+"#dbaf1dd"));
			}
		}
		
		i.setPickupDelay(10);
		
		key = new NamespacedKey(DenarEconomy.plugin, "nonstack");
		m.getPersistentDataContainer().set(key, PersistentDataType.STRING, UUID.randomUUID().toString());
		item.setItemMeta(m);
	}
}
