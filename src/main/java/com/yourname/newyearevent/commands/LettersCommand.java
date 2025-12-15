package com.yourname.newyearevent.commands;

import com.yourname.newyearevent.NewYearEventPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class LettersCommand implements CommandExecutor {
    
    private final NewYearEventPlugin plugin;
    
    public LettersCommand(NewYearEventPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Проверка прав
        if (!sender.hasPermission("newyear.admin.letters")) {
            sender.sendMessage("§c❌ У вас нет прав на использование этой команды!");
            return true;
        }
        
        // /newyear_letters list
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            listAllLetters(sender);
            return true;
        }
        
        // /newyear_letters view <игрок>
        if (args[0].equalsIgnoreCase("view")) {
            if (args.length < 2) {
                sender.sendMessage("§c❌ Использование: /newyear_letters view <игрок>");
                return true;
            }
            
            String playerName = args[1];
            viewPlayerLetter(sender, playerName);
            return true;
        }
        
        sender.sendMessage("§c❌ Использование:");
        sender.sendMessage("§e/newyear_letters list §7- список всех писем");
        sender.sendMessage("§e/newyear_letters view <игрок> §7- просмотр письма игрока");
        return true;
    }
    
    /**
     * Показывает список всех писем
     */
    private void listAllLetters(CommandSender sender) {
        File lettersFolder = plugin.getLettersFolder();
        File blockFolder = new File(lettersFolder, "block");
        File freeFolder = new File(lettersFolder, "free");
        
        List<String> blockLetters = getLetterFiles(blockFolder);
        List<String> freeLetters = getLetterFiles(freeFolder);
        
        sender.sendMessage("§6╔══════════════════════════════════════╗");
        sender.sendMessage("§6║  §e§l📬 СПИСОК ПИСЕМ ДЕДУ МОРОЗУ §6    ║");
        sender.sendMessage("§6╚══════════════════════════════════════╝");
        sender.sendMessage("");
        
        if (blockLetters.isEmpty() && freeLetters.isEmpty()) {
            sender.sendMessage("§7  Писем пока нет...");
            sender.sendMessage("");
            return;
        }
        
        if (!blockLetters.isEmpty()) {
            sender.sendMessage("§a§l📦 Письма с предметами:");
            for (String letter : blockLetters) {
                String playerName = extractPlayerName(letter);
                sender.sendMessage("§a  • §f" + playerName + " §7(" + letter + ")");
            }
            sender.sendMessage("");
        }
        
        if (!freeLetters.isEmpty()) {
            sender.sendMessage("§b§l📝 Текстовые письма:");
            for (String letter : freeLetters) {
                String playerName = extractPlayerName(letter);
                sender.sendMessage("§b  • §f" + playerName + " §7(" + letter + ")");
            }
            sender.sendMessage("");
        }
        
        sender.sendMessage("§7Всего писем: §e" + (blockLetters.size() + freeLetters.size()));
        sender.sendMessage("§7Используйте: §e/newyear_letters view <игрок>");
    }
    
    /**
     * Показывает письмо конкретного игрока
     */
    private void viewPlayerLetter(CommandSender sender, String playerName) {
        File lettersFolder = plugin.getLettersFolder();
        
        // Ищем в блочных письмах
        File blockFile = findLetterFile(new File(lettersFolder, "block"), playerName);
        if (blockFile != null) {
            displayBlockLetter(sender, blockFile, playerName);
            return;
        }
        
        // Ищем в текстовых письмах
        File freeFile = findLetterFile(new File(lettersFolder, "free"), playerName);
        if (freeFile != null) {
            displayFreeLetter(sender, freeFile, playerName);
            return;
        }
        
        sender.sendMessage("§c❌ Письмо от игрока §e" + playerName + " §cне найдено!");
    }
    
    /**
     * Отображает блочное письмо
     */
    private void displayBlockLetter(CommandSender sender, File file, String playerName) {
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            
            sender.sendMessage("§6╔══════════════════════════════════════╗");
            sender.sendMessage("§6║  §e§l📦 ПИСЬМО С ПРЕДМЕТАМИ §6         ║");
            sender.sendMessage("§6╚══════════════════════════════════════╝");
            sender.sendMessage("");
            sender.sendMessage("§7От: §f" + playerName);
            sender.sendMessage("§7Файл: §f" + file.getName());
            sender.sendMessage("");
            sender.sendMessage("§a§lЖелаемые предметы:");
            
            for (String line : lines) {
                if (line.contains("Материал:") || line.contains("Количество:")) {
                    sender.sendMessage("§a  • §f" + line);
                }
            }
            
            sender.sendMessage("");
            sender.sendMessage("§7Выдать подарки: §e/newyear_gift " + playerName + " fulfil");
            sender.sendMessage("§7Кастомный подарок: §e/newyear_gift " + playerName + " custom <материал> <кол-во>");
            
        } catch (IOException e) {
            sender.sendMessage("§c❌ Ошибка чтения письма: " + e.getMessage());
        }
    }
    
    /**
     * Отображает текстовое письмо
     */
    private void displayFreeLetter(CommandSender sender, File file, String playerName) {
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            
            sender.sendMessage("§6╔══════════════════════════════════════╗");
            sender.sendMessage("§6║  §e§l📝 ТЕКСТОВОЕ ПИСЬМО §6            ║");
            sender.sendMessage("§6╚══════════════════════════════════════╝");
            sender.sendMessage("");
            sender.sendMessage("§7От: §f" + playerName);
            sender.sendMessage("§7Файл: §f" + file.getName());
            sender.sendMessage("");
            sender.sendMessage("§b§lТекст письма:");
            
            // Находим строку "ТЕКСТ ПИСЬМА:"
            boolean foundMarker = false;
            boolean skipNextDashes = false;
            StringBuilder letterText = new StringBuilder();
            
            for (String line : lines) {
                // Нашли маркер "ТЕКСТ ПИСЬМА:"
                if (line.contains("ТЕКСТ ПИСЬМА:")) {
                    foundMarker = true;
                    skipNextDashes = true;
                    continue;
                }
                
                // Пропускаем разделитель после маркера
                if (skipNextDashes && line.contains("----")) {
                    skipNextDashes = false;
                    continue;
                }
                
                // Останавливаемся на финальном разделителе
                if (foundMarker && line.contains("====")) {
                    break;
                }
                
                // Читаем текст письма
                if (foundMarker && !skipNextDashes && !line.trim().isEmpty()) {
                    if (letterText.length() > 0) {
                        letterText.append(" ");
                    }
                    letterText.append(line.trim());
                }
            }
            
            if (letterText.length() > 0) {
                sender.sendMessage("§f\"" + letterText.toString() + "\"");
            } else {
                sender.sendMessage("§c(Текст письма не найден)");
                // Отладка - покажем все строки файла
                sender.sendMessage("§7Содержимое файла:");
                for (int i = 0; i < Math.min(lines.size(), 20); i++) {
                    sender.sendMessage("§7[" + i + "] §f" + lines.get(i));
                }
            }
            
            sender.sendMessage("");
            sender.sendMessage("§7Выдать кастомный подарок: §e/newyear_gift " + playerName + " custom <материал> <кол-во>");
            
        } catch (IOException e) {
            sender.sendMessage("§c❌ Ошибка чтения письма: " + e.getMessage());
        }
    }
    
    /**
     * Получает список файлов писем в папке
     */
    private List<String> getLetterFiles(File folder) {
        List<String> files = new ArrayList<>();
        if (!folder.exists()) {
            return files;
        }
        
        File[] letterFiles = folder.listFiles((dir, name) -> name.endsWith(".txt"));
        if (letterFiles != null) {
            for (File file : letterFiles) {
                files.add(file.getName());
            }
        }
        
        return files;
    }
    
    /**
     * Находит файл письма по имени игрока
     */
    private File findLetterFile(File folder, String playerName) {
        if (!folder.exists()) {
            return null;
        }
        
        File[] files = folder.listFiles((dir, name) -> 
            name.toLowerCase().startsWith(playerName.toLowerCase() + "_") && name.endsWith(".txt")
        );
        
        return (files != null && files.length > 0) ? files[0] : null;
    }
    
    /**
     * Извлекает имя игрока из имени файла
     */
    private String extractPlayerName(String fileName) {
        // Формат: Name_UUID_datetime.txt
        int underscoreIndex = fileName.indexOf('_');
        if (underscoreIndex > 0) {
            return fileName.substring(0, underscoreIndex);
        }
        return fileName;
    }
}