package com.yourname.newyearevent.commands;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.yourname.newyearevent.NewYearEventPlugin;
import com.yourname.newyearevent.managers.CurrencyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Команда /shop - открывает магазин с предметами за снежинки
 * Содержит обычные предметы и кастомные новогодние предметы
 */
public class ShopCommand implements CommandExecutor, Listener {
    
    private final NewYearEventPlugin plugin;
    private final CurrencyManager currencyManager;
    private final Logger logger;
    
    private static final String SHOP_TITLE = "§6§l🛒 Новогодний Магазин";
    
    // CustomModelData ID для предметов
    private static final int SANTA_HAT_ID = 2;
    private static final int WINTER_HAT_ID = 3;
    private static final int WINTER_LEGGINGS_ID = 4;
    
    public ShopCommand(NewYearEventPlugin plugin, CurrencyManager currencyManager) {
        this.plugin = plugin;
        this.currencyManager = currencyManager;
        this.logger = plugin.getLogger();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Команда доступна только игрокам!", NamedTextColor.RED));
            return true;
        }
        
        Player player = (Player) sender;
        openShop(player);
        return true;
    }
    
    /**
     * Открывает магазин игроку
     */
    private void openShop(Player player) {
        Inventory shop = Bukkit.createInventory(null, 27, Component.text(SHOP_TITLE));
        
        int balance = currencyManager.getCurrency(player);
        
        // Слот 0: Книга с балансом
        shop.setItem(0, createBalanceBook(balance));
        
        // Слот 8: Голова помощи (новогодняя текстура)
        shop.setItem(8, createHelpHead());
        
        // Центральные слоты с товарами
        // Ряд 2 (слоты 11-15): Обычные предметы
        shop.setItem(11, createShopItem(Material.SNOW_BLOCK, 64, 10, "§b§lСнежные блоки", "Для строительства"));
        shop.setItem(12, createShopItem(Material.ICE, 32, 15, "§b§lЛёд", "Скользкий и холодный"));
        shop.setItem(13, createShopItem(Material.FIREWORK_ROCKET, 1, 20, "§c§lФейерверк", "Праздничные залпы"));
        shop.setItem(14, createEnchantedBook(50));
        shop.setItem(15, createShopItem(Material.DIAMOND, 4, 40, "§b§lАлмазы", "Драгоценные камни"));
        
        // Ряд 3 (слоты 20-24): Ценные предметы и новогодние
        shop.setItem(20, createShopItem(Material.EMERALD, 4, 35, "§a§lИзумруды", "Торговая валюта"));
        shop.setItem(21, createShopItem(Material.GOLDEN_APPLE, 2, 25, "§6§lЗолотые яблоки", "Для восстановления"));
        shop.setItem(22, createShopItem(Material.EXPERIENCE_BOTTLE, 16, 30, "§d§lПузырьки опыта", "Для зачарования"));
        shop.setItem(23, createCustomItem(Material.DIAMOND_HELMET, SANTA_HAT_ID, 100, "§c§lШапка Деда Мороза", "Новогодний головной убор"));
        shop.setItem(24, createCustomItem(Material.DIAMOND_HELMET, WINTER_HAT_ID, 80, "§b§lЗимняя шапка", "С помпоном"));
        
        // Ряд 4 (слоты 29-30): Дополнительные новогодние предметы
        shop.setItem(29, createCustomItem(Material.DIAMOND_LEGGINGS, WINTER_LEGGINGS_ID, 90, "§b§lНовогодние штаны", "Праздничные леггинсы"));
        
        player.openInventory(shop);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
        
        logger.info("🛒 " + player.getName() + " открыл магазин (баланс: " + balance + "❄)");
    }
    
    /**
     * Создает книгу с балансом
     */
    private ItemStack createBalanceBook(int balance) {
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        
        meta.displayName(Component.text("💰 Ваш баланс", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("Снежинок: " + balance + " ❄", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Зарабатывайте снежинки:", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("• Убивая мобов (10%)", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("• Находя в сундуках", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        book.setItemMeta(meta);
        return book;
    }
    
    /**
     * Создает голову с новогодней текстурой (помощь)
     */
    private ItemStack createHelpHead() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        // Создаем профиль с кастомной текстурой
        PlayerProfile profile = Bukkit.createProfile(UUID.fromString("c8050621-83db-4b05-af96-b5dcb4dce12c"));
        
        // Добавляем текстуру
        profile.setProperty(new ProfileProperty(
            "textures",
            "e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWU3OTQ0NWI0ZmJiMWU4MTkwYTMwNmZlYWEwMjJkOWM1MThjNTY1ZGQwMDEzYTU2Nzc3Y2YxYThlMDMxNWZmNiJ9fX0="
        ));
        
        meta.setPlayerProfile(profile);
        
        meta.displayName(Component.text("❓ Помощь", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("Нажмите на предмет для покупки", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Новогодние предметы требуют", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("установки ресурспака!", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("https://namemc.com/skin/7ce6031e8ec358cf", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        head.setItemMeta(meta);
        return head;
    }
    
    /**
     * Создает обычный предмет магазина
     */
    private ItemStack createShopItem(Material material, int amount, int price, String name, String description) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(Component.text(name)
                .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text(description, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Цена: " + price + " ❄", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        lore.add(Component.text(""));
        lore.add(Component.text("▶ Нажмите для покупки", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Создает кастомный предмет с CustomModelData
     */
    private ItemStack createCustomItem(Material baseMaterial, int modelData, int price, String name, String description) {
        ItemStack item = new ItemStack(baseMaterial, 1);
        ItemMeta meta = item.getItemMeta();
        
        // Устанавливаем CustomModelData
        meta.setCustomModelData(modelData);
        
        meta.displayName(Component.text(name)
                .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text(description, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("⚠ Требуется ресурспак!", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Цена: " + price + " ❄", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        lore.add(Component.text(""));
        lore.add(Component.text("▶ Нажмите для покупки", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        
        // Добавляем сияние для кастомных предметов
        meta.setEnchantmentGlintOverride(true);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Создает зачарованную книгу
     */
    private ItemStack createEnchantedBook(int price) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        
        meta.displayName(Component.text("§5§lКнига зачарований")
                .decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("Случайное зачарование", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text("Цена: " + price + " ❄", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true));
        lore.add(Component.text(""));
        lore.add(Component.text("▶ Нажмите для покупки", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        book.setItemMeta(meta);
        return book;
    }
    
    /**
     * Обработка кликов в магазине
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Component title = event.getView().title();
        // Преобразуем Component в строку для сравнения
        String titleString = ((net.kyori.adventure.text.TextComponent) title).content();
        if (!titleString.equals(SHOP_TITLE)) {
            return;
        }
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        
        // Проверяем что это не информационные предметы (слоты 0 и 8)
        int slot = event.getSlot();
        if (slot == 0 || slot == 8) {
            return;
        }
        
        // Получаем цену из лора
        int price = getPriceFromLore(clicked);
        if (price <= 0) {
            return;
        }
        
        // Проверяем баланс
        int balance = currencyManager.getCurrency(player);
        if (balance < price) {
            player.sendMessage(Component.text("❌ Недостаточно снежинок! Нужно: " + price + " ❄, у вас: " + balance + " ❄", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        
        // Снимаем снежинки
        currencyManager.takeCurrency(player, price);
        
        // Создаем копию предмета без лора с ценой
        ItemStack reward = createRewardItem(clicked);
        
        // Выдаем предмет
        player.getInventory().addItem(reward);
        
        // Звук и сообщения
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        player.sendMessage(Component.text("✅ Покупка успешна! -" + price + " ❄", NamedTextColor.GREEN));
        
        logger.info("🛒 " + player.getName() + " купил предмет за " + price + "❄ (остаток: " + currencyManager.getCurrency(player) + "❄)");
        
        // Обновляем магазин
        player.closeInventory();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> openShop(player), 1L);
    }
    
    /**
     * Получает цену из лора предмета
     */
    private int getPriceFromLore(ItemStack item) {
        if (!item.hasItemMeta()) {
            return 0;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) {
            return 0;
        }
        
        List<Component> lore = meta.lore();
        if (lore == null) {
            return 0;
        }
        
        for (Component line : lore) {
            String text = ((net.kyori.adventure.text.TextComponent) line).content();
            
            if (text.contains("Цена:")) {
                // Убираем цветовые коды
                text = text.replaceAll("§.", "");
                
                // Ищем число
                String[] parts = text.split("\\s+");
                for (String part : parts) {
                    try {
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
     * Создает награду без служебного лора
     */
    private ItemStack createRewardItem(ItemStack shopItem) {
        ItemStack reward = shopItem.clone();
        ItemMeta meta = reward.getItemMeta();
        
        if (meta == null) {
            return reward;
        }
        
        // Убираем служебный лор с ценой и кнопкой покупки
        if (meta.hasLore()) {
            List<Component> oldLore = meta.lore();
            List<Component> newLore = new ArrayList<>();
            
            if (oldLore != null) {
                for (Component line : oldLore) {
                    String text = ((net.kyori.adventure.text.TextComponent) line).content();
                    
                    // Пропускаем строки с ценой и кнопкой
                    if (!text.contains("Цена:") && !text.contains("Нажмите для покупки") && !text.contains("Требуется ресурспак")) {
                        newLore.add(line);
                    }
                }
            }
            
            // Убираем пустые строки в конце
            while (!newLore.isEmpty()) {
                Component last = newLore.get(newLore.size() - 1);
                String text = ((net.kyori.adventure.text.TextComponent) last).content();
                if (text.trim().isEmpty()) {
                    newLore.remove(newLore.size() - 1);
                } else {
                    break;
                }
            }
            
            meta.lore(newLore.isEmpty() ? null : newLore);
        }
        
        reward.setItemMeta(meta);
        return reward;
    }
}