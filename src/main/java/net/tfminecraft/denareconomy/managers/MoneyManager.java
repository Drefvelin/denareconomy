package net.tfminecraft.DenarEconomy.Managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import me.Plugins.SimpleFactions.Managers.FactionManager;
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
import net.tfminecraft.DenarEconomy.Item.Coin;
import net.tfminecraft.DenarEconomy.Loaders.CoinLoader;

public class MoneyManager implements Listener{
	private PlayerManager pm = DenarEconomy.getPlayerManager();
	
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
	
	public double doTaxes(String p, double amount) {
		double paidTax = 0;
		if(FactionManager.getByMember(p) != null) {
			Faction f = FactionManager.getByMember(p);
			if(f.getTaxRate() > 0) {
				if(f.getBank() != null) {
					double tax = f.getTaxRate()/100.0*amount;
					paidTax += tax;
					f.getBank().deposit(tax);
				}
			}
			for(FactionModifier mod : f.getModifiers()) {
				if(paidTax >= amount) break;
				if(mod.getFrom() == null) continue;
				if(!mod.getType().equals(FactionModifiers.TAX)) continue;
				double tax = mod.getAmount()/100*amount;
				Faction from = mod.getFrom();
				if(from.getBank() == null) continue;
				paidTax += tax;
				from.getBank().deposit(tax);
			}
		}
		return Math.round(paidTax*100.0)/100.0;
	}
	
	public void addMoney(Player p, double amount, boolean silent, boolean taxable) {
		PlayerData pd = pm.get(p);
		addMoneyToAccount(p.getUniqueId().toString(), amount, silent, taxable, pd.getPouch());
	}

	public void addMoneyToBank(String id, double amount, boolean silent, boolean taxable) {
		PlayerData pd = pm.get(Bukkit.getPlayer(UUID.fromString(id)));
		addMoneyToAccount(id, amount, silent, taxable, pd.getBank());
	}

	public void addMoneyToAccount(String id, double amount, boolean silent, boolean taxable, Account a) {
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
		a.change(amount);
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

	
	public Item spawnMoney(Player p, ItemStack i, boolean silent) {
		if(silent) {
			ItemMeta m = i.getItemMeta();
			NamespacedKey key = new NamespacedKey(DenarEconomy.plugin, "silent");
			m.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
			i.setItemMeta(m);
		}
		Item item = p.getWorld().dropItem(p.getEyeLocation().clone().add(0, -0.4, 0), i);
		p.swingMainHand();
		item.setVelocity(p.getLocation().getDirection().clone().normalize().multiply(0.5));
		return item;
	}
	
	public void pay(Player p, double amount) {
	    PlayerData pd = pm.get(p);
	    Account pouch = pd.getPouch();

	    if (pouch.getBal() < amount) {
	        p.sendMessage(StringFormatter.formatHex("#a33d1dNot enough funds in pouch"));
	        return;
	    }

	    pouch.change(-amount);
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

	                NamespacedKey senderKey = new NamespacedKey(DenarEconomy.plugin, "sender");
	                m.getPersistentDataContainer().set(senderKey, PersistentDataType.STRING, p.getUniqueId().toString());

	                NamespacedKey valueKey = new NamespacedKey(DenarEconomy.plugin, "customValue");

	                if (!named[0]) {
	                    named[0] = true;
	                    m.getPersistentDataContainer().set(valueKey, PersistentDataType.DOUBLE, amount);
	                    item.setItemMeta(m);
	                    Item first = spawnMoney(p, item, false);
	                    idHolder[0] = first.getUniqueId();
	                    firstItemHolder[0] = first.getItemStack();
	                } else {
	                    if (idHolder[0] == null) {
	                        p.sendMessage("Error: First money item not initialized.");
	                        return;
	                    }

	                    m.getPersistentDataContainer().set(valueKey, PersistentDataType.DOUBLE, 0.0);
	                    NamespacedKey chain = new NamespacedKey(DenarEconomy.plugin, "chained");
	                    m.getPersistentDataContainer().set(chain, PersistentDataType.STRING, idHolder[0].toString());
	                    item.setItemMeta(m);

	                    Item newItem = spawnMoney(p, item, true);

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


	
	@EventHandler
	public void pickupCoin(EntityPickupItemEvent e) {
		if(!(e.getEntity() instanceof Player)) return;
		Player p = (Player) e.getEntity();
		ItemStack item = e.getItem().getItemStack();
		Coin c = getCoin(item);
		if(c == null) return;
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
	public void coinSpawn(EntitySpawnEvent e) {
		if(!(e.getEntity() instanceof Item)) return;
		Item i = (Item) e.getEntity();
		Coin c = getCoin(i.getItemStack());
		if(c == null) return;
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
