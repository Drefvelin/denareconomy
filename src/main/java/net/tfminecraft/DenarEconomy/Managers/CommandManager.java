package net.tfminecraft.DenarEconomy.Managers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.Plugins.SimpleFactions.Managers.FactionManager;
import me.Plugins.SimpleFactions.Objects.Faction;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import me.Plugins.TLibs.Utils.ParseUtils;
import net.tfminecraft.DenarEconomy.DenarEconomy;
import net.tfminecraft.DenarEconomy.Data.Account;
import net.tfminecraft.DenarEconomy.Data.PlayerData;
import net.tfminecraft.DenarEconomy.Database.BalTopEntry;
import net.tfminecraft.DenarEconomy.Database.Database;

public class CommandManager implements CommandExecutor, TabCompleter {

    public String cmd1 = "deco";
    public String cmd2 = "pouch";


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
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
        List<BalTopEntry> topList = Database.getTopBalances(20);

        p.sendMessage("§e========== §6[Balance Top 20] §e==========");
        int rank = 1;
        for (BalTopEntry entry : topList) {
            String name = entry.getName();
            double total = entry.getTotal();
            p.sendMessage(StringFormatter.formatHex(
                String.format("§a%d §e%s §7- #b39122%.2f#dbaf1dd", rank++, name, total)
            ));
        }
        p.sendMessage("§e==================================");
    }


    private void handleBalance(Player p) {
        PlayerData pd = DenarEconomy.getPlayerManager().get(p);
        p.sendMessage(StringFormatter.formatHex("#3ce8c9Current Pouch Balance: #b39122" + pd.getPouch().getBal() + "#dbaf1dd"));
        p.sendMessage(StringFormatter.formatHex("#3ce8c9Current Bank Balance: #b39122" + pd.getBank().getBal() + "#dbaf1dd"));
    }

    private void handlePay(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§a[DenarEconomy] §cNo amount specified");
            return;
        }

        Double amount = ParseUtils.parseDouble(args[1]);

        if (!ParseUtils.isPositive(amount)) {
            p.sendMessage("§a[DenarEconomy] §cInvalid amount");
            return;
        }

        DenarEconomy.getMoneyManager().pay(p, amount);
    }

    private void handleToItem(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage("§a[DenarEconomy] §cNo amount specified");
            return;
        }

        Double amount = ParseUtils.parseDouble(args[1]);

        if (!ParseUtils.isPositive(amount)) {
            p.sendMessage("§a[DenarEconomy] §cInvalid amount");
            return;
        }

        Account pouch = DenarEconomy.getPlayerManager().get(p).getPouch();

        if (pouch.getBal() < amount) {
            p.sendMessage(StringFormatter.formatHex("#a33d1dNot enough funds in pouch"));
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
        if (!isInBankChunk(p)) return;

        if (args.length < 2) {
            p.sendMessage("§a[DenarEconomy] §cNo amount specified");
            return;
        }

        Double amount = ParseUtils.parseDouble(args[1]);

        if (!ParseUtils.isPositive(amount)) {
            p.sendMessage("§a[DenarEconomy] §cInvalid amount");
            return;
        }

        PlayerData pd = DenarEconomy.getPlayerManager().get(p);
        if (pd.getPouch().getBal() < amount) {
            p.sendMessage(StringFormatter.formatHex("#a33d1dNot enough funds in pouch"));
            return;
        }

        MoneyManager.transfer(pd.getPouch(), pd.getBank(), amount);
        sendBankReport(p, "Deposited", amount, pd);
    }

    private void handleWithdraw(Player p, String[] args) {
        if (!isInBankChunk(p)) return;

        if (args.length < 2) {
            p.sendMessage("§a[DenarEconomy] §cNo amount specified");
            return;
        }

        Double amount = ParseUtils.parseDouble(args[1]);

        if (!ParseUtils.isPositive(amount)) {
            p.sendMessage("§a[DenarEconomy] §cInvalid amount");
            return;
        }

        PlayerData pd = DenarEconomy.getPlayerManager().get(p);
        if (pd.getBank().getBal() < amount) {
            p.sendMessage(StringFormatter.formatHex("#a33d1dNot enough funds in bank"));
            return;
        }

        MoneyManager.transfer(pd.getBank(), pd.getPouch(), amount);
        sendBankReport(p, "Withdrew", amount, pd);
    }

    private boolean isInBankChunk(Player p) {
        Faction f = FactionManager.getByMember(p.getName());
        if (f == null) {
            p.sendMessage("§a[DenarEconomy] §cYou must be in a faction");
            return false;
        }
        if (f.getBank() == null) {
            p.sendMessage("§a[DenarEconomy] §cYour faction has no bank chunk");
            return false;
        }
        if (!f.getBank().getChunk().equals(p.getLocation().getChunk())) {
            p.sendMessage("§a[DenarEconomy] §cYou must be inside the bank chunk to deposit/withdraw");
            return false;
        }
        return true;
    }

    private void sendBankReport(Player p, String action, double amount, PlayerData pd) {
        p.sendMessage("§e============§6[Bank Report]§e==============");
        p.sendMessage(StringFormatter.formatHex("#6ab05a" + action + ": #b39122" + amount + "#dbaf1dd"));
        p.sendMessage(StringFormatter.formatHex("#3ce8c9New Bank Balance: #b39122" + pd.getBank().getBal() + "#dbaf1dd"));
        p.sendMessage(StringFormatter.formatHex("#3ce8c9New Pouch Balance: #b39122" + pd.getPouch().getBal() + "#dbaf1dd"));
        p.sendMessage("§e=====================================");
        p.playSound(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
    }

    private void sendError(Player p) {
        p.sendMessage("§a[DenarEconomy] §cUnknown subcommand or wrong usage.");
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
