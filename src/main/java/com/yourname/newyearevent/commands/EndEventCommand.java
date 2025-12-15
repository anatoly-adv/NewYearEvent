package com.yourname.newyearevent.commands;

import com.yourname.newyearevent.NewYearEventPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class EndEventCommand implements CommandExecutor {
    
    private final NewYearEventPlugin plugin;
    
    public EndEventCommand(NewYearEventPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("newyear.admin.end")) {
            sender.sendMessage("§c❌ У вас нет прав на использование этой команды!");
            return true;
        }
        
        if (!plugin.isEventActive()) {
            sender.sendMessage("§e⚠ Новогодний ивент не активен!");
            return true;
        }
        
        sender.sendMessage("§e⏳ Завершаем новогодний ивент...");
        
        // Останавливаем автоматические фейерверки
        plugin.getFireworkManager().stopAutoFireworks();
        sender.sendMessage("§a✓ Автоматические фейерверки остановлены!");
        
        // Отключаем отображение снежинок у всех игроков
        plugin.getCurrencyManager().disableDisplayForAll();
        sender.sendMessage("§a✓ Отображение снежинок отключено!");
        
        // Удаляем снег
        sender.sendMessage("§e⏳ Убираем снег...");
        plugin.getSnowManager().removeSnow();
        
        // Деактивируем ивент
        plugin.setEventActive(false);
        
        // Объявление всем игрокам
        String[] announcement = {
            "§c╔═══════════════════════════════════════╗",
            "§c║  §f🎄 §lНОВОГОДНИЙ ИВЕНТ ЗАВЕРШЁН! §f🎄 §c  ║",
            "§c╚═══════════════════════════════════════╝",
            "",
            "§7Спасибо всем за участие!",
            "§7До встречи в следующем году! 🎁",
            ""
        };
        
        for (String line : announcement) {
            Bukkit.broadcastMessage(line);
        }
        
        sender.sendMessage("§a✓ Новогодний ивент успешно завершён!");
        
        return true;
    }
}