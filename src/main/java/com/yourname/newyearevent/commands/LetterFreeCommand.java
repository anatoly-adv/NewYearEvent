package com.yourname.newyearevent.commands;

import com.yourname.newyearevent.NewYearEventPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LetterFreeCommand implements CommandExecutor {
    
    private final NewYearEventPlugin plugin;
    private static final int MAX_TEXT_LENGTH = 500;
    
    public LetterFreeCommand(NewYearEventPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c❌ Эту команду могут использовать только игроки!");
            return true;
        }
        
        // Проверка активности ивента
        if (!plugin.isEventActive()) {
            player.sendMessage("§c❌ Ивент ещё не начался! Письма можно отправлять только во время ивента.");
            return true;
        }
        
        // Проверка на уже отправленное письмо
        if (plugin.hasPlayerSentLetter(player.getUniqueId())) {
            player.sendMessage("§c❌ Ты уже отправил письмо Деду Морозу! Одно письмо на игрока.");
            return true;
        }
        
        // Проверка аргументов
        if (args.length == 0) {
            player.sendMessage("§e📝 Использование: §f/send_new_year_letter_free <текст письма>");
            player.sendMessage("§7Пример: §f/send_new_year_letter_free Дорогой Дед Мороз! Хочу алмазную броню!");
            return true;
        }
        
        // Собираем текст
        String letterText = String.join(" ", args);
        
        // Проверка длины
        if (letterText.length() > MAX_TEXT_LENGTH) {
            player.sendMessage("§c❌ Письмо слишком длинное!");
            player.sendMessage("§eМаксимум: §f" + MAX_TEXT_LENGTH + " символов");
            player.sendMessage("§eТвоё письмо: §f" + letterText.length() + " символов");
            return true;
        }
        
        // Сохраняем письмо
        boolean success = saveLetter(player, letterText);
        
        if (success) {
            plugin.addPlayerLetter(player.getUniqueId());
            
            player.sendMessage("§a╔═══════════════════════════════════════╗");
            player.sendMessage("§a║  §f🎅 §lПИСЬМО ОТПРАВЛЕНО! §f📜          §a║");
            player.sendMessage("§a╚═══════════════════════════════════════╝");
            player.sendMessage("");
            player.sendMessage("§eТвоё пожелание:");
            player.sendMessage("§7\"" + letterText + "\"");
            player.sendMessage("");
            player.sendMessage("§7Дед Мороз получил твоё письмо! 📬");
            player.sendMessage("§7Он обязательно его прочитает! 🎄✨");
            
            plugin.getLogger().info("📬 Игрок " + player.getName() + " отправил текстовое письмо");
        } else {
            player.sendMessage("§c❌ Ошибка при сохранении письма! Попробуй ещё раз.");
        }
        
        return true;
    }
    
    private boolean saveLetter(Player player, String letterText) {
        try {
            File freeFolder = new File(plugin.getLettersFolder(), "free");
            
            // Формат имени файла
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String filename = player.getName() + "_" + player.getUniqueId() + "_" + timestamp + ".txt";
            File letterFile = new File(freeFolder, filename);
            
            // ВАЖНО: Пишем письмо в UTF-8 для поддержки русского текста
            try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(letterFile), StandardCharsets.UTF_8))) {
                
                writer.write("============================================================\n");
                writer.write("         🎅 ПИСЬМО ДЕДУ МОРОЗУ (ТЕКСТОВОЕ) 📜\n");
                writer.write("============================================================\n\n");
                
                writer.write("Игрок: " + player.getName() + "\n");
                writer.write("UUID: " + player.getUniqueId() + "\n");
                writer.write("Дата отправки: " + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")) + "\n");
                writer.write("Мир: " + player.getWorld().getName() + "\n");
                writer.write("Координаты игрока: X=" + player.getLocation().getBlockX() + 
                            ", Y=" + player.getLocation().getBlockY() + 
                            ", Z=" + player.getLocation().getBlockZ() + "\n\n");
                
                writer.write("------------------------------------------------------------\n");
                writer.write("ТЕКСТ ПИСЬМА:\n");
                writer.write("------------------------------------------------------------\n");
                writer.write(letterText + "\n");
                
                writer.write("\n============================================================\n");
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка сохранения письма: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}