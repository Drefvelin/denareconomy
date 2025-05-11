package net.tfminecraft.DenarEconomy.Managers;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.tfminecraft.DenarEconomy.DenarEconomy;
import net.tfminecraft.DenarEconomy.Data.PlayerData;

public class CommandManager implements CommandExecutor{
	
	public String cmd1 = "deco";

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(cmd.getName().equalsIgnoreCase(cmd1)) {
			if(!(sender instanceof Player)) return false;
			Player p = (Player) sender;
			if(args[0].equalsIgnoreCase("bal")) {
				PlayerData pd = DenarEconomy.getPlayerManager().get(p);
				p.sendMessage(StringFormatter.formatHex("#3ce8c9Current Pouch Balance: #b39122"+pd.getPouch().getBal()+"#dbaf1dd"));
				p.sendMessage(StringFormatter.formatHex("#3ce8c9Current Bank Balance: #b39122"+pd.getBank().getBal()+"#dbaf1dd"));
				return true;
			} else if(args[0].equalsIgnoreCase("pay")) {
				if(args.length < 2) {
					p.sendMessage("§a[DenarEconomy] §cNo amount specified");
					return false;
				}
				double amount = Double.parseDouble(args[1]);
				DenarEconomy.getMoneyManager().pay(p, amount);
				return true;
			} else if(args[0].equalsIgnoreCase("deposit")) {
				if(FactionManager.getByMember(p.getName()) == null) {
					p.sendMessage("§a[DenarEconomy] §cNeed to be in a faction to use the banking system");
					return false;
				}
				Faction f = FactionManager.getByMember(p.getName());
				if(f.getBank() == null) {
					p.sendMessage("§a[DenarEconomy] §cYour faction has no bank chunk");
					return false;
				}
				if(!f.getBank().getChunk().equals(p.getLocation().getChunk())) {
					p.sendMessage("§a[DenarEconomy] §cYou need to be in your faction's bank chunk to deposit/withdraw");
					return false;
				}
				if(args.length < 2) {
					p.sendMessage("§a[DenarEconomy] §cNo amount specified");
					return false;
				}
				double amount = Double.parseDouble(args[1]);
				PlayerData pd = DenarEconomy.getPlayerManager().get(p);
				if(pd.getPouch().getBal() < amount) {
					p.sendMessage(StringFormatter.formatHex("#a33d1dNot enough funds in pouch"));
			        return false;
				}
				MoneyManager.transfer(pd.getPouch(), pd.getBank(), amount);
				p.sendMessage("§e============§6[Bank Report]§e==============");
				p.sendMessage(StringFormatter.formatHex("#6ab05aDeposited: #b39122"+amount+"#dbaf1dd"));
				p.sendMessage(StringFormatter.formatHex("#3ce8c9New Bank Balance: #b39122"+pd.getBank().getBal()+"#dbaf1dd"));
				p.sendMessage(StringFormatter.formatHex("#3ce8c9New Pouch Balance: #b39122"+pd.getPouch().getBal()+"#dbaf1dd"));
				p.sendMessage("§e=====================================");
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
				return true;
			} else if(args[0].equalsIgnoreCase("withdraw")) {
				if(FactionManager.getByMember(p.getName()) == null) {
					p.sendMessage("§a[DenarEconomy] §cNeed to be in a faction to use the banking system");
					return false;
				}
				Faction f = FactionManager.getByMember(p.getName());
				if(f.getBank() == null) {
					p.sendMessage("§a[DenarEconomy] §cYour faction has no bank chunk");
					return false;
				}
				if(!f.getBank().getChunk().equals(p.getLocation().getChunk())) {
					p.sendMessage("§a[DenarEconomy] §cYou need to be in your faction's bank chunk to deposit/withdraw");
					return false;
				}
				if(args.length < 2) {
					p.sendMessage("§a[DenarEconomy] §cNo amount specified");
					return false;
				}
				double amount = Double.parseDouble(args[1]);
				PlayerData pd = DenarEconomy.getPlayerManager().get(p);
				if(pd.getBank().getBal() < amount) {
					p.sendMessage(StringFormatter.formatHex("#a33d1dNot enough funds in bank"));
			        return false;
				}
				MoneyManager.transfer(pd.getBank(), pd.getPouch(), amount);
				p.sendMessage("§e============§6[Bank Report]§e==============");
				p.sendMessage(StringFormatter.formatHex("#6ab05aWithdrew: #b39122"+amount+"#dbaf1dd"));
				p.sendMessage(StringFormatter.formatHex("#3ce8c9New Bank Balance: #b39122"+pd.getBank().getBal()+"#dbaf1dd"));
				p.sendMessage(StringFormatter.formatHex("#3ce8c9New Pouch Balance: #b39122"+pd.getPouch().getBal()+"#dbaf1dd"));
				p.sendMessage("§e=====================================");
				p.playSound(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
				return true;
			}
			p.sendMessage("§a[DenarEconomy] §cError with command format, perhaps you spelled something wrong?");
		}
		return false;
	}
}
