package com.yourname.newyearevent.commands;

import com.yourname.newyearevent.NewYearEventPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class GiftCommand implements CommandExecutor {
    
    private final NewYearEventPlugin plugin;
    
    public GiftCommand(NewYearEventPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Проверка прав
        if (!sender.hasPermission("newyear.admin.gift")) {
            sender.sendMessage("§c❌ У вас нет прав на использование этой команды!");
            return true;
        }
        
        // /newyear_gift <игрок> fulfil
        // /newyear_gift <игрок> custom <материал> <количество>
        
        if (args.length < 2) {
            sender.sendMessage("§c❌ Использование:");
            sender.sendMessage("§e/newyear_gift <игрок> fulfil §7- выдать подарки из письма");
            sender.sendMessage("§e/newyear_gift <игрок> custom <материал> <кол-во> §7- кастомный подарок");
            return true;
        }
        
        String playerName = args[0];
        String action = args[1];
        
        // Находим игрока
        Player target = Bukkit.getPlayer(playerName);
        if (target == null || !target.isOnline()) {
            sender.sendMessage("§c❌ Игрок §e" + playerName + " §cне в сети!");
            return true;
        }
        
        if (action.equalsIgnoreCase("fulfil")) {
            fulfilLetter(sender, target);
            return true;
        }
        
        if (action.equalsIgnoreCase("custom")) {
            if (args.length < 4) {
                sender.sendMessage("§c❌ Использование: /newyear_gift <игрок> custom <материал> <кол-во>");
                return true;
            }
            
            String materialName = args[2].toUpperCase();
            int amount;
            
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§c❌ Неверное количество: " + args[3]);
                return true;
            }
            
            giveCustomGift(sender, target, materialName, amount);
            return true;
        }
        
        sender.sendMessage("§c❌ Неизвестное действие: " + action);
        sender.sendMessage("§7Используйте: §efulfil §7или §ecustom");
        return true;
    }
    
    /**
     * Выдаёт подарки из письма с предметами
     */
    private void fulfilLetter(CommandSender sender, Player target) {
        File lettersFolder = plugin.getLettersFolder();
        File blockFolder = new File(lettersFolder, "block");
        
        // Находим файл письма
        File letterFile = findLetterFile(blockFolder, target.getName());
        
        if (letterFile == null) {
            sender.sendMessage("§c❌ Блочное письмо от §e" + target.getName() + " §cне найдено!");
            sender.sendMessage("§7Возможно игрок отправил текстовое письмо. Используйте §ecustom §7для выдачи подарка.");
            return;
        }
        
        // Читаем письмо и выдаём предметы
        List<ItemStack> items = new ArrayList<>();
        
        try {
            List<String> lines = Files.readAllLines(letterFile.toPath(), StandardCharsets.UTF_8);
            
            for (String line : lines) {
                // Ищем строку с материалом
                if (!line.contains("Материал:")) continue;
                
                // Формат: "Материал: diamond_block"
                String materialName = line.split(":")[1].trim().toUpperCase();
                
                // Ищем следующую строку с количеством
                int lineIndex = lines.indexOf(line);
                if (lineIndex + 1 < lines.size()) {
                    String quantityLine = lines.get(lineIndex + 1);
                    if (quantityLine.contains("Количество:")) {
                        // Формат: "Количество: 3 штуки"
                        String[] parts = quantityLine.split(":");
                        if (parts.length > 1) {
                            String countStr = parts[1].trim().split(" ")[0];
                            int count = Integer.parseInt(countStr);
                            
                            Material material = Material.getMaterial(materialName);
                            if (material != null) {
                                items.add(new ItemStack(material, count));
                            } else {
                                sender.sendMessage("§c⚠ Пропущен неизвестный материал: " + materialName);
                            }
                        }
                    }
                }
            }
            
        } catch (IOException e) {
            sender.sendMessage("§c❌ Ошибка чтения письма: " + e.getMessage());
            return;
        } catch (Exception e) {
            sender.sendMessage("§c❌ Ошибка парсинга письма: " + e.getMessage());
            return;
        }
        
        if (items.isEmpty()) {
            sender.sendMessage("§c❌ В письме не найдено предметов для выдачи!");
            return;
        }
        
        // Выдаём предметы игроку
        for (ItemStack item : items) {
            target.getInventory().addItem(item);
        }
        
        // Помечаем письмо как выполненное (переименовываем)
        File fulfilledFile = new File(letterFile.getParent(), "FULFILLED_" + letterFile.getName());
        letterFile.renameTo(fulfilledFile);
        
        // Сообщения
        sender.sendMessage("§a✓ Выдано §e" + items.size() + " §aтипов предметов игроку §e" + target.getName());
        target.sendMessage("§6╔═══════════════════════════════════════╗");
        target.sendMessage("§6║  §e§l🎁 ПОДАРОК ОТ ДЕДА МОРОЗА! §6    ║");
        target.sendMessage("§6╚═══════════════════════════════════════╝");
        target.sendMessage("");
        target.sendMessage("§aВаше письмо было прочитано!");
        target.sendMessage("§aПодарки добавлены в инвентарь! §6🎁");
        target.sendMessage("");
        
        // Звук
        target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        // Фейерверк
        plugin.getFireworkManager().launchFireworkAbovePlayer(target);
    }
    
    /**
     * Выдаёт кастомный подарок
     */
    private void giveCustomGift(CommandSender sender, Player target, String materialName, int amount) {
        Material material = Material.getMaterial(materialName);
        
        if (material == null) {
            sender.sendMessage("§c❌ Неизвестный материал: " + materialName);
            sender.sendMessage("§7Примеры: DIAMOND, EMERALD, NETHERITE_INGOT");
            return;
        }
        
        if (amount <= 0 || amount > 6400) {
            sender.sendMessage("§c❌ Количество должно быть от 1 до 6400");
            return;
        }
        
        // Выдаём предмет
        ItemStack item = new ItemStack(material, amount);
        target.getInventory().addItem(item);
        
        // Сообщения
        sender.sendMessage("§a✓ Выдан подарок игроку §e" + target.getName() + "§a: §f" + materialName + " x" + amount);
        target.sendMessage("§6╔═══════════════════════════════════════╗");
        target.sendMessage("§6║  §e§l🎁 ПОДАРОК ОТ ДЕДА МОРОЗА! §6    ║");
        target.sendMessage("§6╚═══════════════════════════════════════╝");
        target.sendMessage("");
        target.sendMessage("§aВы получили специальный подарок:");
        target.sendMessage("§f  " + materialName + " §7x§e" + amount);
        target.sendMessage("");
        
        // Звук
        target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        // Фейерверк
        plugin.getFireworkManager().launchFireworkAbovePlayer(target);
    }
    
    /**
     * Находит файл письма по имени игрока
     */
    private File findLetterFile(File folder, String playerName) {
        if (!folder.exists()) {
            return null;
        }
        
        File[] files = folder.listFiles((dir, name) -> 
            name.toLowerCase().startsWith(playerName.toLowerCase() + "_") && 
            name.endsWith(".txt") &&
            !name.startsWith("FULFILLED_")  // Игнорируем выполненные
        );
        
        return (files != null && files.length > 0) ? files[0] : null;
    }
}