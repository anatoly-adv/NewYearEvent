package com.yourname.newyearevent.managers;

import com.yourname.newyearevent.NewYearEventPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CurrencyManager {
    
    private final NewYearEventPlugin plugin;
    private final File currencyFile;
    private FileConfiguration currencyConfig;
    
    // Кэш балансов в памяти для быстрого доступа
    private final Map<UUID, Integer> balances = new HashMap<>();
    
    // Таск для обновления скорбордов
    private BukkitTask updateTask;
    
    public CurrencyManager(NewYearEventPlugin plugin) {
        this.plugin = plugin;
        this.currencyFile = new File(plugin.getDataFolder(), "currency.yml");
        loadCurrency();
        startScoreboardUpdater();
    }
    
    /**
     * Загружает данные о валюте из файла
     */
    private void loadCurrency() {
        if (!currencyFile.exists()) {
            try {
                currencyFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("❌ Не удалось создать currency.yml: " + e.getMessage());
            }
        }
        
        currencyConfig = YamlConfiguration.loadConfiguration(currencyFile);
        
        // Загружаем балансы в кэш
        if (currencyConfig.contains("balances")) {
            for (String uuidStr : currencyConfig.getConfigurationSection("balances").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                int balance = currencyConfig.getInt("balances." + uuidStr, 0);
                balances.put(uuid, balance);
            }
        }
        
        plugin.getLogger().info("💰 Загружено балансов: " + balances.size());
    }
    
    /**
     * Сохраняет данные о валюте в файл
     */
    public void saveCurrency() {
        // Сохраняем балансы из кэша в конфиг
        for (Map.Entry<UUID, Integer> entry : balances.entrySet()) {
            currencyConfig.set("balances." + entry.getKey().toString(), entry.getValue());
        }
        
        try {
            currencyConfig.save(currencyFile);
        } catch (IOException e) {
            plugin.getLogger().severe("❌ Не удалось сохранить currency.yml: " + e.getMessage());
        }
    }
    
    /**
     * Получает баланс игрока (по UUID)
     */
    public int getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, 0);
    }
    
    /**
     * Устанавливает баланс игрока (по UUID)
     */
    public void setBalance(UUID uuid, int amount) {
        balances.put(uuid, Math.max(0, amount));
        updateScoreboard(Bukkit.getPlayer(uuid));
    }
    
    /**
     * Добавляет снежинки игроку (по UUID)
     */
    public void addBalance(UUID uuid, int amount) {
        int current = getBalance(uuid);
        setBalance(uuid, current + amount);
    }
    
    /**
     * Забирает снежинки у игрока (по UUID)
     * @return true если хватило денег, false если нет
     */
    public boolean takeBalance(UUID uuid, int amount) {
        int current = getBalance(uuid);
        if (current >= amount) {
            setBalance(uuid, current - amount);
            return true;
        }
        return false;
    }
    
    /**
     * Проверяет хватает ли денег (по UUID)
     */
    public boolean hasBalance(UUID uuid, int amount) {
        return getBalance(uuid) >= amount;
    }
    
    // ========================================
    // НОВЫЕ МЕТОДЫ - ОБЁРТКИ ДЛЯ PLAYER
    // ========================================
    
    /**
     * НОВЫЙ МЕТОД: Получает баланс игрока (по Player)
     */
    public int getCurrency(Player player) {
        return getBalance(player.getUniqueId());
    }
    
    /**
     * НОВЫЙ МЕТОД: Добавляет снежинки игроку (по Player)
     */
    public void addCurrency(Player player, int amount) {
        addBalance(player.getUniqueId(), amount);
    }
    
    /**
     * НОВЫЙ МЕТОД: Забирает снежинки у игрока (по Player)
     */
    public boolean takeCurrency(Player player, int amount) {
        return takeBalance(player.getUniqueId(), amount);
    }
    
    /**
     * НОВЫЙ МЕТОД: Проверяет хватает ли денег (по Player)
     */
    public boolean hasCurrency(Player player, int amount) {
        return hasBalance(player.getUniqueId(), amount);
    }
    
    // ========================================
    // МЕТОДЫ ОТОБРАЖЕНИЯ
    // ========================================
    
    /**
     * Получает отображение валюты для игрока (sidebar/actionbar/off)
     */
    public String getDisplayMode(UUID uuid) {
        return currencyConfig.getString("display." + uuid.toString(), "sidebar");
    }
    
    /**
     * Устанавливает режим отображения для игрока
     */
    public void setDisplayMode(UUID uuid, String mode) {
        currencyConfig.set("display." + uuid.toString(), mode);
        saveCurrency();
        
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            // Сначала очищаем ВСЁ
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            
            // Потом устанавливаем новый режим
            updateScoreboard(player);
        }
    }
    
    /**
     * Обновляет скорборд для конкретного игрока
     */
    public void updateScoreboard(Player player) {
        if (player == null || !player.isOnline()) return;
        
        // КРИТИЧНО: Проверяем активен ли ивент
        if (!plugin.isEventActive()) {
            // Если ивент неактивен - убираем скорборд
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            return;
        }
        
        String mode = getDisplayMode(player.getUniqueId());
        
        if (mode.equals("off")) {
            // Убираем скорборд
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            return;
        }
        
        if (mode.equals("sidebar")) {
            updateSidebarScoreboard(player);
        }
        // actionbar обрабатывается в updateTask
    }
    
    /**
     * Обновляет sidebar скорборд
     */
    private void updateSidebarScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();
        
        Objective objective = scoreboard.registerNewObjective("snowflakes", "dummy", "§b§l❄ СНЕЖИНКИ");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        int balance = getBalance(player.getUniqueId());
        
        // Создаём красивый скорборд
        Score line5 = objective.getScore("§7═══════════");
        line5.setScore(5);
        
        Score line4 = objective.getScore("§f");
        line4.setScore(4);
        
        Score line3 = objective.getScore("§eБаланс:");
        line3.setScore(3);
        
        Score line2 = objective.getScore("§b" + balance + " ❄");
        line2.setScore(2);
        
        Score line1 = objective.getScore("§r");
        line1.setScore(1);
        
        Score line0 = objective.getScore("§7═══════════");
        line0.setScore(0);
        
        player.setScoreboard(scoreboard);
    }
    
    /**
     * Запускает периодическое обновление скорбордов
     */
    private void startScoreboardUpdater() {
        // Обновляем каждые 10 тиков (0.5 секунды) - без мигания!
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // КРИТИЧНО: Проверяем активен ли ивент
            if (!plugin.isEventActive()) {
                return;
            }
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                String mode = getDisplayMode(player.getUniqueId());
                
                if (mode.equals("sidebar")) {
                    updateSidebarScoreboard(player);
                } else if (mode.equals("actionbar")) {
                    int balance = getBalance(player.getUniqueId());
                    // ИСПРАВЛЕНО: Используем Adventure API вместо устаревшего String API
                    player.sendActionBar(Component.text("❄ " + balance + " снежинок", NamedTextColor.AQUA));
                }
            }
        }, 0L, 10L); // Каждые 10 тиков = 0.5 секунды (БЕЗ МИГАНИЯ!)
    }
    
    /**
     * Включает отображение снежинок всем игрокам при запуске ивента
     */
    public void enableDisplayForAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateScoreboard(player);
        }
        plugin.getLogger().info("💰 Отображение снежинок включено для всех игроков");
    }
    
    /**
     * Отключает отображение снежинок всем игрокам при остановке ивента
     */
    public void disableDisplayForAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Убираем скорборд
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
        plugin.getLogger().info("💰 Отображение снежинок отключено для всех игроков");
    }
    
    /**
     * Останавливает обновление скорбордов
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        saveCurrency();
    }
    
    /**
     * Получает конфигурацию валюты (для доступа из листенеров)
     */
    public FileConfiguration getCurrencyConfig() {
        return currencyConfig;
    }
}