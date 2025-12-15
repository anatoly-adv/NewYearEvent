package com.yourname.newyearevent.commands;

import com.yourname.newyearevent.NewYearEventPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SnowflakesCommand implements CommandExecutor {
    
    private final NewYearEventPlugin plugin;
    
    public SnowflakesCommand(NewYearEventPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /snowflakes - показать свой баланс
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cЭта команда только для игроков!");
                return true;
            }
            
            Player player = (Player) sender;
            int balance = plugin.getCurrencyManager().getBalance(player.getUniqueId());
            
            player.sendMessage("§b§l❄ СНЕЖИНКИ");
            player.sendMessage("§7Ваш баланс: §b" + balance + " ❄");
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        // /snowflakes balance [игрок] - посмотреть баланс
        if (subCommand.equals("balance") || subCommand.equals("bal")) {
            if (args.length == 1) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cУкажите имя игрока!");
                    return true;
                }
                
                Player player = (Player) sender;
                int balance = plugin.getCurrencyManager().getBalance(player.getUniqueId());
                sender.sendMessage("§7Ваш баланс: §b" + balance + " ❄");
            } else {
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                int balance = plugin.getCurrencyManager().getBalance(target.getUniqueId());
                sender.sendMessage("§7Баланс игрока §e" + target.getName() + "§7: §b" + balance + " ❄");
            }
            return true;
        }
        
        // /snowflakes give <игрок> <количество> - выдать снежинки
        if (subCommand.equals("give")) {
            if (!sender.hasPermission("newyear.admin.currency")) {
                sender.sendMessage("§cУ вас нет прав!");
                return true;
            }
            
            if (args.length < 3) {
                sender.sendMessage("§cИспользование: /snowflakes give <игрок> <количество>");
                return true;
            }
            
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            int amount;
            
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cНеверное количество: " + args[2]);
                return true;
            }
            
            plugin.getCurrencyManager().addBalance(target.getUniqueId(), amount);
            sender.sendMessage("§aВы выдали §b" + amount + " ❄ §aигроку §e" + target.getName());
            
            if (target.isOnline()) {
                target.getPlayer().sendMessage("§aВам выдали §b" + amount + " ❄ §aснежинок!");
            }
            
            return true;
        }
        
        // /snowflakes take <игрок> <количество> - забрать снежинки
        if (subCommand.equals("take")) {
            if (!sender.hasPermission("newyear.admin.currency")) {
                sender.sendMessage("§cУ вас нет прав!");
                return true;
            }
            
            if (args.length < 3) {
                sender.sendMessage("§cИспользование: /snowflakes take <игрок> <количество>");
                return true;
            }
            
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            int amount;
            
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cНеверное количество: " + args[2]);
                return true;
            }
            
            if (plugin.getCurrencyManager().takeBalance(target.getUniqueId(), amount)) {
                sender.sendMessage("§aВы забрали §b" + amount + " ❄ §aу игрока §e" + target.getName());
                
                if (target.isOnline()) {
                    target.getPlayer().sendMessage("§cУ вас забрали §b" + amount + " ❄ §cснежинок!");
                }
            } else {
                sender.sendMessage("§cУ игрока недостаточно снежинок!");
            }
            
            return true;
        }
        
        // /snowflakes set <игрок> <количество> - установить баланс
        if (subCommand.equals("set")) {
            if (!sender.hasPermission("newyear.admin.currency")) {
                sender.sendMessage("§cУ вас нет прав!");
                return true;
            }
            
            if (args.length < 3) {
                sender.sendMessage("§cИспользование: /snowflakes set <игрок> <количество>");
                return true;
            }
            
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            int amount;
            
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cНеверное количество: " + args[2]);
                return true;
            }
            
            plugin.getCurrencyManager().setBalance(target.getUniqueId(), amount);
            sender.sendMessage("§aВы установили баланс игрока §e" + target.getName() + " §aна §b" + amount + " ❄");
            
            return true;
        }
        
        // /snowflakes display <sidebar/actionbar/off> - изменить отображение
        if (subCommand.equals("display")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cЭта команда только для игроков!");
                return true;
            }
            
            if (args.length < 2) {
                sender.sendMessage("§cИспользование: /snowflakes display <sidebar/actionbar/off>");
                return true;
            }
            
            Player player = (Player) sender;
            String mode = args[1].toLowerCase();
            
            if (!mode.equals("sidebar") && !mode.equals("actionbar") && !mode.equals("off")) {
                sender.sendMessage("§cДоступные режимы: sidebar, actionbar, off");
                return true;
            }
            
            plugin.getCurrencyManager().setDisplayMode(player.getUniqueId(), mode);
            
            String modeName = mode.equals("sidebar") ? "справа" : 
                            mode.equals("actionbar") ? "снизу" : "отключено";
            player.sendMessage("§aРежим отображения изменён: §e" + modeName);
            
            return true;
        }
        
        // Помощь
        sender.sendMessage("§b§l❄ КОМАНДЫ СНЕЖИНОК:");
        sender.sendMessage("§e/snowflakes §7- посмотреть свой баланс");
        sender.sendMessage("§e/snowflakes balance [игрок] §7- посмотреть баланс");
        sender.sendMessage("§e/snowflakes display <sidebar/actionbar/off> §7- настроить отображение");
        
        if (sender.hasPermission("newyear.admin.currency")) {
            sender.sendMessage("§c§lАДМИН:");
            sender.sendMessage("§e/snowflakes give <игрок> <кол-во> §7- выдать снежинки");
            sender.sendMessage("§e/snowflakes take <игрок> <кол-во> §7- забрать снежинки");
            sender.sendMessage("§e/snowflakes set <игрок> <кол-во> §7- установить баланс");
        }
        
        return true;
    }
}