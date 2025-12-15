package com.yourname.newyearevent.commands;

import com.yourname.newyearevent.NewYearEventPlugin;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LetterBlockCommand implements CommandExecutor, TabCompleter {
    
    private final NewYearEventPlugin plugin;
    private final Map<Material, Integer> materialLimits = new HashMap<>();
    private final Set<Material> bannedMaterials = new HashSet<>();
    
    public LetterBlockCommand(NewYearEventPlugin plugin) {
        this.plugin = plugin;
        initMaterialLimits();
        initBannedMaterials();
    }
    
    private void initMaterialLimits() {
        // ОБЫЧНЫЕ (64 шт)
        addMaterialsToLimit(64, 
            Material.DIRT, Material.GRASS_BLOCK, Material.STONE, Material.COBBLESTONE,
            Material.SAND, Material.GRAVEL, Material.OAK_LOG, Material.BIRCH_LOG,
            Material.SPRUCE_LOG, Material.JUNGLE_LOG, Material.ACACIA_LOG, Material.DARK_OAK_LOG,
            Material.OAK_PLANKS, Material.BIRCH_PLANKS, Material.SPRUCE_PLANKS,
            Material.WHEAT, Material.POTATO, Material.CARROT, Material.BEETROOT,
            Material.APPLE, Material.MELON, Material.PUMPKIN
        );
        
        // СТРОИТЕЛЬНЫЕ (64 шт)
        addMaterialsToLimit(64,
            Material.GLASS, Material.WHITE_WOOL, Material.BRICKS, Material.STONE_BRICKS,
            Material.QUARTZ_BLOCK, Material.SMOOTH_STONE, Material.TERRACOTTA
        );
        
        // Все цвета шерсти, стекла, бетона
        for (Material mat : Material.values()) {
            if (mat.name().contains("_WOOL") || mat.name().contains("_CONCRETE") ||
                mat.name().contains("STAINED_GLASS") || mat.name().contains("_TERRACOTTA")) {
                materialLimits.put(mat, 64);
            }
        }
        
        // ПОЛЕЗНЫЕ РУДЫ (48 шт)
        addMaterialsToLimit(48,
            Material.COAL_ORE, Material.COAL, Material.DEEPSLATE_COAL_ORE,
            Material.COPPER_ORE, Material.COPPER_INGOT, Material.RAW_COPPER
        );
        
        // СРЕДНИЕ (32 шт)
        addMaterialsToLimit(32,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE, Material.RAW_IRON, Material.IRON_INGOT,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.RAW_GOLD,
            Material.REDSTONE, Material.LAPIS_LAZULI, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE
        );
        
        // ЦЕННЫЕ (16 шт)
        addMaterialsToLimit(16,
            Material.GOLD_INGOT, Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD, Material.ANCIENT_DEBRIS, Material.QUARTZ
        );
        
        // РЕДКИЕ (8 шт)
        addMaterialsToLimit(8,
            Material.DIAMOND, Material.NETHERITE_SCRAP, Material.ENDER_PEARL,
            Material.BLAZE_ROD, Material.ENCHANTED_BOOK, Material.SADDLE,
            Material.NAME_TAG, Material.TOTEM_OF_UNDYING
        );
        
        // ОЧЕНЬ РЕДКИЕ (4 шт)
        addMaterialsToLimit(4,
            Material.DIAMOND_BLOCK, Material.GOLD_BLOCK, Material.EMERALD_BLOCK,
            Material.NETHERITE_INGOT, Material.BEACON, Material.ELYTRA, Material.TRIDENT
        );
        
        // СВЕРХРЕДКИЕ (1 шт)
        addMaterialsToLimit(1,
            Material.NETHERITE_BLOCK, Material.DRAGON_EGG, Material.NETHER_STAR,
            Material.ENCHANTED_GOLDEN_APPLE
        );
    }
    
    private void addMaterialsToLimit(int limit, Material... materials) {
        for (Material mat : materials) {
            materialLimits.put(mat, limit);
        }
    }
    
    private void initBannedMaterials() {
        // Креативные блоки
        bannedMaterials.add(Material.BARRIER);
        bannedMaterials.add(Material.COMMAND_BLOCK);
        bannedMaterials.add(Material.CHAIN_COMMAND_BLOCK);
        bannedMaterials.add(Material.REPEATING_COMMAND_BLOCK);
        bannedMaterials.add(Material.COMMAND_BLOCK_MINECART);
        bannedMaterials.add(Material.STRUCTURE_BLOCK);
        bannedMaterials.add(Material.STRUCTURE_VOID);
        bannedMaterials.add(Material.JIGSAW);
        bannedMaterials.add(Material.DEBUG_STICK);
        bannedMaterials.add(Material.KNOWLEDGE_BOOK);
        bannedMaterials.add(Material.LIGHT);
        
        // Spawn eggs
        for (Material mat : Material.values()) {
            if (mat.name().endsWith("_SPAWN_EGG")) {
                bannedMaterials.add(mat);
            }
        }
    }
    
    private int getMaterialLimit(Material material) {
        // Проверяем точное совпадение
        if (materialLimits.containsKey(material)) {
            return materialLimits.get(material);
        }
        
        // Если не указан лимит - по умолчанию 64
        return 64;
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
        if (args.length < 2) {
            player.sendMessage("§e📝 Использование: §f/send_new_year_letter_block <материал> <количество>");
            player.sendMessage("§7Пример: §f/send_new_year_letter_block diamond_block 3");
            return true;
        }
        
        // Парсим материал
        String materialName = args[0].toUpperCase();
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            player.sendMessage("§c❌ Неизвестный материал: §f" + args[0]);
            player.sendMessage("§7Используй Tab для автодополнения!");
            return true;
        }
        
        // Проверка на запрещённый материал
        if (bannedMaterials.contains(material)) {
            player.sendMessage("§c❌ Этот предмет запрещён! Дед Мороз не дарит креативные блоки.");
            return true;
        }
        
        // Проверка что это предмет (не воздух)
        if (material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR) {
            player.sendMessage("§c❌ Нельзя запросить воздух!");
            return true;
        }
        
        // Парсим количество
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c❌ Неверное количество: §f" + args[1]);
            player.sendMessage("§7Укажи число от 1 до 64");
            return true;
        }
        
        // Проверка диапазона
        if (amount < 1) {
            player.sendMessage("§c❌ Количество должно быть больше 0!");
            return true;
        }
        
        if (amount > 64) {
            player.sendMessage("§c❌ Максимальное количество для любого блока: §f64 штуки");
            return true;
        }
        
        // Проверка лимита для конкретного материала
        int limit = getMaterialLimit(material);
        if (amount > limit) {
            player.sendMessage("§c❌ Эй эй, полегче! Много хочешь!");
            player.sendMessage("§e⚠ Максимум для §f" + formatMaterialName(material) + "§e: §f" + limit + " " + getPluralForm(limit));
            player.sendMessage("§e⚠ Твой запрос: §f" + amount + " " + getPluralForm(amount));
            return true;
        }
        
        // Сохраняем письмо
        boolean success = saveLetter(player, material, amount);
        
        if (success) {
            plugin.addPlayerLetter(player.getUniqueId());
            
            player.sendMessage("§a╔═══════════════════════════════════════╗");
            player.sendMessage("§a║  §f🎅 §lПИСЬМО ОТПРАВЛЕНО! §f🎁          §a║");
            player.sendMessage("§a╚═══════════════════════════════════════╝");
            player.sendMessage("");
            player.sendMessage("§eПредмет: §f" + formatMaterialName(material));
            player.sendMessage("§eКоличество: §f" + amount + " " + getPluralForm(amount));
            player.sendMessage("");
            player.sendMessage("§7Дед Мороз получил твоё письмо! 📬");
            player.sendMessage("§7Жди подарок под ёлкой! 🎄✨");
            
            plugin.getLogger().info("📬 Игрок " + player.getName() + " отправил письмо: " + 
                                    material.name() + " x" + amount);
        } else {
            player.sendMessage("§c❌ Ошибка при сохранении письма! Попробуй ещё раз.");
        }
        
        return true;
    }
    
    private boolean saveLetter(Player player, Material material, int amount) {
        try {
            File blockFolder = new File(plugin.getLettersFolder(), "block");
            
            // Формат имени файла
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String filename = player.getName() + "_" + player.getUniqueId() + "_" + timestamp + ".txt";
            File letterFile = new File(blockFolder, filename);
            
            // ВАЖНО: Пишем письмо в UTF-8 для поддержки русского текста
            try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(letterFile), StandardCharsets.UTF_8))) {
                
                writer.write("============================================================\n");
                writer.write("         🎅 ПИСЬМО ДЕДУ МОРОЗУ (БЛОЧНОЕ) 🎁\n");
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
                writer.write("ЗАПРОС:\n");
                writer.write("------------------------------------------------------------\n");
                writer.write("Материал: " + material.name().toLowerCase() + "\n");
                writer.write("Количество: " + amount + " " + getPluralForm(amount) + "\n");
                writer.write("Лимит для этого предмета: " + getMaterialLimit(material) + " шт.\n");
                
                writer.write("\n============================================================\n");
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка сохранения письма: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private String formatMaterialName(Material material) {
        // Преобразуем DIAMOND_BLOCK -> Diamond Block
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (result.length() > 0) result.append(" ");
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }
    
    private String getPluralForm(int amount) {
        if (amount == 1) return "штука";
        if (amount >= 2 && amount <= 4) return "штуки";
        return "штук";
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Автодополнение материалов
            String input = args[0].toUpperCase();
            for (Material mat : Material.values()) {
                if (mat.isItem() && !bannedMaterials.contains(mat)) {
                    if (mat.name().startsWith(input)) {
                        completions.add(mat.name().toLowerCase());
                    }
                }
            }
        } else if (args.length == 2) {
            // Подсказки количества
            try {
                Material mat = Material.valueOf(args[0].toUpperCase());
                int limit = getMaterialLimit(mat);
                completions.add("1");
                completions.add("8");
                completions.add("16");
                completions.add("32");
                completions.add(String.valueOf(limit));
            } catch (IllegalArgumentException ignored) {
            }
        }
        
        return completions;
    }
}