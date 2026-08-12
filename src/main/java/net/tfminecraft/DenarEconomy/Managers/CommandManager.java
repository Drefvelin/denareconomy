package net.tfminecraft.DenarEconomy.Managers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.Plugins.TLibs.Utils.ParseUtils;
import net.tfminecraft.DenarEconomy.DenarEconomy;
import net.tfminecraft.DenarEconomy.Data.Account;
import net.tfminecraft.DenarEconomy.Data.PlayerData;
import net.tfminecraft.DenarEconomy.Database.BalTopEntry;
import net.tfminecraft.DenarEconomy.Database.Database;
import net.tfminecraft.DenarEconomy.Loaders.MessageLoader;
import net.tfminecraft.DenarEconomy.event.PlayerBankPulseEvent;
import net.tfminecraft.DenarEconomy.event.PlayerDepositMaterialsEvent;

public class CommandManager implements CommandExecutor, TabCompleter {

    public String cmd1 = "deco";
    public String cmd2 = "pouch";


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageLoader.send(sender, "general.players-only");
            return false;
        }

        Player p = (Player) sender;

        if (cmd.getName().equalsIgnoreCase(cmd2)) {
            DenarEconomy.getMoneyManager().showPouch(p);
            return true;
        }

        if (cmd.getName().equalsIgnoreCase(cmd1)) {
            if (args.length == 0) {
                sendError(p);
                return false;
            }

            String sub = args[0].toLowerCase();

            switch (sub) {
                case "bal":
                    handleBalance(p);
                    break;

                case "pay":
                    handlePay(p, args);
                    break;

                case "toitem":
                    handleToItem(p, args);
                    break;

                case "deposit":
                    handleDeposit(p, args);
                    break;

                case "withdraw":
                    handleWithdraw(p, args);
                    break;

                default:
                    sendError(p);
                    break;
                case "baltop":
                    handleBalTop(p);
                    break;
            }
            return true;
        }
        return false;
    }

    private void handleBalTop(Player p) {
        int limit = 20;
        List<BalTopEntry> topList = Database.getTopBalances(limit);

        MessageLoader.send(p, "baltop.header", "limit", limit);
        int rank = 1;
        for (BalTopEntry entry : topList) {
            MessageLoader.send(p, "baltop.entry",
                "rank", rank++,
                "name", entry.getName(),
                "amount", String.format("%.2f", entry.getTotal()));
        }
        MessageLoader.send(p, "baltop.footer");
    }


    private void handleBalance(Player p) {
        PlayerData pd = DenarEconomy.getPlayerManager().get(p);
        MessageLoader.send(p, "balance.pouch", "amount", pd.getPouch().getBal());
        MessageLoader.send(p, "balance.bank", "amount", pd.getBank().getBal());
    }

    private void handlePay(Player p, String[] args) {
        if (args.length < 2) {
            MessageLoader.send(p, "errors.no-amount");
            return;
        }

        Double amount = ParseUtils.parseDouble(args[1]);

        if (!ParseUtils.isPositive(amount)) {
            MessageLoader.send(p, "errors.invalid-amount");
            return;
        }

        DenarEconomy.getMoneyManager().pay(p, amount);
    }

    private void handleToItem(Player p, String[] args) {
        if (args.length < 2) {
            MessageLoader.send(p, "errors.no-amount");
            return;
        }

        Double amount = ParseUtils.parseDouble(args[1]);

        if (!ParseUtils.isPositive(amount)) {
            MessageLoader.send(p, "errors.invalid-amount");
            return;
        }

        Account pouch = DenarEconomy.getPlayerManager().get(p).getPouch();

        if (pouch.getBal() < amount) {
            MessageLoader.send(p, "errors.not-enough-pouch");
            return;
        }

        pouch.change(-amount);
        List<ItemStack> items = DenarEconomy.getMoneyManager().amountToItems(amount);
        for (ItemStack i : items) {
            if (p.getInventory().firstEmpty() == -1) {
                p.getWorld().dropItem(p.getLocation(), i);
            } else {
                p.getInventory().addItem(i);
            }
        }
    }

    private void handleDeposit(Player p, String[] args) {
        PlayerBankPulseEvent event = new PlayerBankPulseEvent(p);
		Bukkit.getPluginManager().callEvent(event);
        if(event.isCancelled()) return;

        if (args.length < 2) {
            MessageLoader.send(p, "errors.no-amount");
            return;
        }

        Double amount = ParseUtils.parseDouble(args[1]);

        if (!ParseUtils.isPositive(amount)) {
            MessageLoader.send(p, "errors.invalid-amount");
            return;
        }

        PlayerData pd = DenarEconomy.getPlayerManager().get(p);
        if (pd.getPouch().getBal() < amount) {
            MessageLoader.send(p, "errors.not-enough-pouch");
            return;
        }

        MoneyManager.transfer(pd.getPouch(), pd.getBank(), amount);
        sendBankReport(p, MessageLoader.get("bank.deposited"), amount, pd);
    }

    private void handleWithdraw(Player p, String[] args) {
        PlayerBankPulseEvent event = new PlayerBankPulseEvent(p);
		Bukkit.getPluginManager().callEvent(event);
        if(event.isCancelled()) return;

        if (args.length < 2) {
            MessageLoader.send(p, "errors.no-amount");
            return;
        }

        Double amount = ParseUtils.parseDouble(args[1]);

        if (!ParseUtils.isPositive(amount)) {
            MessageLoader.send(p, "errors.invalid-amount");
            return;
        }

        PlayerData pd = DenarEconomy.getPlayerManager().get(p);
        if (pd.getBank().getBal() < amount) {
            MessageLoader.send(p, "errors.not-enough-bank");
            return;
        }

        MoneyManager.transfer(pd.getBank(), pd.getPouch(), amount);
        sendBankReport(p, MessageLoader.get("bank.withdrew"), amount, pd);
    }

    private void sendBankReport(Player p, String action, double amount, PlayerData pd) {
        MessageLoader.send(p, "bank.header");
        MessageLoader.send(p, "bank.action", "action", action, "amount", amount);
        MessageLoader.send(p, "bank.new-bank", "amount", pd.getBank().getBal());
        MessageLoader.send(p, "bank.new-pouch", "amount", pd.getPouch().getBal());
        MessageLoader.send(p, "bank.footer");
        p.playSound(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
    }

    private void sendError(Player p) {
        MessageLoader.send(p, "general.unknown-subcommand");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (cmd.getName().equalsIgnoreCase(cmd1)) {
            if (args.length == 1) {
                completions.add("bal");
                completions.add("pay");
                completions.add("toitem");
                completions.add("deposit");
                completions.add("withdraw");
                completions.add("baltop");
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("pay") || args[0].equalsIgnoreCase("toitem") ||
                    args[0].equalsIgnoreCase("deposit") || args[0].equalsIgnoreCase("withdraw")) {
                    completions.add("<amount>");
                }
            }
        } else if (cmd.getName().equalsIgnoreCase(cmd2)) {
            // "pouch" has no subcommands, so nothing to suggest
            completions = null;
        }
        if (completions == null) return null;

        // Filter completions based on current input (for better experience)
        return completions.stream()
                .filter(c -> c.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
