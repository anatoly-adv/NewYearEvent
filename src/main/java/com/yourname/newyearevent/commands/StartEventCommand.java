package com.yourname.newyearevent.commands;

import com.yourname.newyearevent.NewYearEventPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class StartEventCommand implements CommandExecutor {
    
    private final NewYearEventPlugin plugin;
    
    public StartEventCommand(NewYearEventPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("newyear.admin.start")) {
            sender.sendMessage("§c❌ У вас нет прав на использование этой команды!");
            return true;
        }
        
        if (plugin.isEventActive()) {
            sender.sendMessage("§e⚠ Новогодний ивент уже активен!");
            return true;
        }
        
        sender.sendMessage("§e⏳ Запускаем новогодний ивент...");
        
        // Активируем ивент
        plugin.setEventActive(true);
        
        // Запускаем снег
        sender.sendMessage("§e⏳ Добавляем снег по всему миру...");
        plugin.getSnowManager().addSnow();
        
        // Запускаем автоматические фейерверки
        plugin.getFireworkManager().startAutoFireworks();
        sender.sendMessage("§a✓ Автоматические фейерверки запущены!");
        
        // Включаем отображение снежинок всем игрокам
        plugin.getCurrencyManager().enableDisplayForAll();
        sender.sendMessage("§a✓ Отображение снежинок включено!");
        
        // Объявление всем игрокам
        String[] announcement = {
            "§a╔═══════════════════════════════════════╗",
            "§a║  §f🎄 §lНОВОГОДНИЙ ИВЕНТ НАЧАЛСЯ! §f🎄 §a  ║",
            "§a╚═══════════════════════════════════════╝",
            "",
            "§f  • Снег покрывает мир",
            "§f  • Боевые снежки активны",
            "§f  • Фейерверки запущены",
            "§f  • Собирайте снежинки ❄ с мобов и сундуков",
            "§f  • Отправьте письмо Деду Морозу!",
            "",
            "§7Команды:",
            "§e  /letterblock §7- письмо с предметами",
            "§e  /letterfree <текст> §7- текстовое письмо",
            "§e  /firework §7- запустить фейерверк",
            "§e  /snowflakes §7- посмотреть снежинки",
            "§e  /shop §7- новогодний магазин",
            ""
        };
        
        for (String line : announcement) {
            Bukkit.broadcastMessage(line);
        }
        
        sender.sendMessage("§a✓ Новогодний ивент успешно запущен!");
        
        return true;
    }
}