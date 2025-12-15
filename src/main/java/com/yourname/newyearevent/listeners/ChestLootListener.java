package com.yourname.newyearevent.listeners;

import com.yourname.newyearevent.NewYearEventPlugin;
import com.yourname.newyearevent.managers.CurrencyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Слушатель для добавления снежинок в сундуки
 * Добавляет кастомные снежинки в натуральные сундуки/вагонетки
 * Автоматически конвертирует снежинки в валюту или книгу починки
 */
public class ChestLootListener implements Listener {
    
    private final NewYearEventPlugin plugin;
    private final CurrencyManager currencyManager;
    private final Logger logger;
    private final Random random;
    
    // Маркер для идентификации снежинок
    private static final String SNOWFLAKE_MARKER = "§8SNOWFLAKE_CURRENCY_ITEM";
    
    // CustomModelData ID для снежинки
    private static final int SNOWFLAKE_MODEL_ID = 1;
    
    // Таймер для проверки инвентарей
    private BukkitTask scanTask;
    
    public ChestLootListener(NewYearEventPlugin plugin, CurrencyManager currencyManager) {
        this.plugin = plugin;
        this.currencyManager = currencyManager;
        this.logger = plugin.getLogger();
        this.random = new Random();
        
        // Запускаем таймер сканирования инвентарей каждые 5 тиков (0.25 сек)
        startInventoryScan();
        
        logger.info("📦 ChestLootListener инициализирован (кастомные снежинки)");
    }
    
    /**
     * Добавляет снежинки в лут натуральных сундуков
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onLootGenerate(LootGenerateEvent event) {
        // Проверяем что событие активно
        if (!plugin.isEventActive()) {
            return;
        }
        
        // Добавляем снежинки только в натуральные сундуки/вагонетки
        if (event.getLootContext().getLootedEntity() == null) {
            // Генерируем случайное количество снежинок (10-25)
            int amount = 10 + random.nextInt(16);
            
            // Создаем снежинку
            ItemStack snowflake = createSnowflakeItem(amount);
            
            // Добавляем в лут
            event.getLoot().add(snowflake);
            
            logger.info("📦 Добавлена снежинка в лут: " + amount + "❄");
        }
    }
    
    /**
     * Создает предмет снежинки с кастомной текстурой
     */
    private ItemStack createSnowflakeItem(int amount) {
        ItemStack item = new ItemStack(Material.SNOWBALL, 1);
        ItemMeta meta = item.getItemMeta();
        
        // Устанавливаем CustomModelData для кастомной текстуры
        meta.setCustomModelData(SNOWFLAKE_MODEL_ID);
        
        // Название
        meta.displayName(
            Component.text("❄ Снежинки", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true)
        );
        
        // Лор с количеством и маркером
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Количество: " + amount + " ❄", NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Новогодняя валюта", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("§8SNOWFLAKE_CURRENCY_ITEM")
                .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        
        // Добавляем сияние
        meta.setEnchantmentGlintOverride(true);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Запускает таймер сканирования инвентарей игроков
     */
    private void startInventoryScan() {
        scanTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            // Проходимся по всем онлайн игрокам
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                scanPlayerInventory(player);
            }
        }, 5L, 5L); // Каждые 5 тиков (0.25 сек)
    }
    
    /**
     * Сканирует инвентарь игрока на наличие снежинок
     */
    private void scanPlayerInventory(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            
            if (item != null && isSnowflakeItem(item)) {
                // Получаем количество снежинок
                int amount = getSnowflakeAmount(item);
                
                if (amount > 0) {
                    // Проверяем активно ли событие
                    if (plugin.isEventActive()) {
                        // Конвертируем в валюту
                        currencyManager.addCurrency(player, amount);
                        
                        // Удаляем предмет
                        player.getInventory().setItem(i, null);
                        
                        // Звук и сообщение
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
                        player.sendActionBar(Component.text("§b+ " + amount + " ❄", NamedTextColor.AQUA));
                        
                        logger.info("💰 " + player.getName() + " получил " + amount + " снежинок из предмета");
                        
                    } else {
                        // Событие закончилось - заменяем на книгу починки
                        ItemStack mendingBook = createMendingBook();
                        player.getInventory().setItem(i, mendingBook);
                        
                        player.sendMessage(Component.text("❄ Событие закончилось! Снежинка превратилась в книгу починки.", NamedTextColor.YELLOW));
                        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
                        
                        logger.info("📚 " + player.getName() + " получил книгу починки (событие закончилось)");
                    }
                }
            }
        }
    }
    
    /**
     * Проверяет является ли предмет снежинкой
     */
    private boolean isSnowflakeItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // Проверяем маркер в лоре
        if (meta.hasLore()) {
            List<Component> lore = meta.lore();
            if (lore != null) {
                for (Component line : lore) {
                    String plainText = ((net.kyori.adventure.text.TextComponent) line).content();
                    if (plainText.contains("SNOWFLAKE_CURRENCY_ITEM")) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Получает количество снежинок из предмета
     */
    private int getSnowflakeAmount(ItemStack item) {
        if (!item.hasItemMeta()) {
            return 0;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) {
            return 0;
        }
        
        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) {
            return 0;
        }
        
        // Ищем строку с количеством: "Количество: X ❄"
        for (Component line : lore) {
            String plainText = ((net.kyori.adventure.text.TextComponent) line).content();
            
            // Убираем цветовые коды
            plainText = plainText.replaceAll("§.", "");
            
            if (plainText.contains("Количество:")) {
                // Разбиваем по пробелам
                String[] parts = plainText.split("\\s+");
                
                // Ищем число
                for (String part : parts) {
                    try {
                        // Убираем всё кроме цифр
                        String digits = part.replaceAll("[^0-9]", "");
                        if (!digits.isEmpty()) {
                            return Integer.parseInt(digits);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        
        return 0;
    }
    
    /**
     * Создает книгу починки
     */
    private ItemStack createMendingBook() {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        
        // Добавляем зачарование
        meta.addStoredEnchant(Enchantment.MENDING, 1, true);
        
        book.setItemMeta(meta);
        return book;
    }
    
    /**
     * Дополнительная проверка при клике в инвентаре
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        
        if (clickedItem != null && isSnowflakeItem(clickedItem)) {
            // Проверяем через 2 тика после клика
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                scanPlayerInventory(player);
            }, 2L);
        }
    }
    
    /**
     * Проверка при поднятии предмета
     */
    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        ItemStack item = event.getItem().getItemStack();
        
        if (isSnowflakeItem(item)) {
            // Проверяем через 2 тика после поднятия
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                scanPlayerInventory(player);
            }, 2L);
        }
    }
    
    /**
     * Остановка таймера при выгрузке плагина
     */
    public void shutdown() {
        if (scanTask != null) {
            scanTask.cancel();
            logger.info("📦 ChestLootListener остановлен");
        }
    }
}