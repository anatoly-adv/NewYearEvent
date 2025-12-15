package com.yourname.newyearevent.commands;

import com.yourname.newyearevent.NewYearEventPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FireworkCommand implements CommandExecutor {
    
    private final NewYearEventPlugin plugin;
    
    public FireworkCommand(NewYearEventPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cУ вас нет прав на использование этой команды!");
            return true;
        }
        
        // /fireworks auto on/off
        if (args.length == 2 && args[0].equalsIgnoreCase("auto")) {
            String action = args[1].toLowerCase();
            
            if (action.equals("on")) {
                plugin.getFireworkManager().setAutoFireworks(true);
                Bukkit.broadcastMessage("§a§l[Новый Год] §eАвтоматические фейерверки §aвключены§e! Запуск каждые 2 минуты.");
                return true;
            } else if (action.equals("off")) {
                plugin.getFireworkManager().setAutoFireworks(false);
                Bukkit.broadcastMessage("§c§l[Новый Год] §eАвтоматические фейерверки §cотключены§e!");
                return true;
            } else {
                sender.sendMessage("§cИспользование: /fireworks auto <on/off>");
                return true;
            }
        }
        
        // /fireworks - запуск для всех игроков
        if (args.length == 0) {
            int count = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                plugin.getFireworkManager().launchFireworkAbovePlayer(player);
                count++;
            }
            
            Bukkit.broadcastMessage("§6§l✨ §eФейерверки запущены над всеми игроками! §6§l✨");
            sender.sendMessage("§aЗапущено фейерверков: " + count);
            return true;
        }
        
        sender.sendMessage("§eИспользование:");
        sender.sendMessage("§a/fireworks §7- запустить фейерверки над всеми игроками");
        sender.sendMessage("§a/fireworks auto <on/off> §7- переключить автозапуск");
        return true;
    }
}