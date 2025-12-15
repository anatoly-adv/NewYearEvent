package com.yourname.newyearevent;

import com.yourname.newyearevent.commands.*;
import com.yourname.newyearevent.listeners.*;
import com.yourname.newyearevent.managers.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Главный класс плагина NewYearEvent
 * Новогоднее событие для сервера Minecraft
 */
public class NewYearEventPlugin extends JavaPlugin {
    
    private FileConfiguration config;
    private Logger logger;
    
    // Менеджеры
    private SnowManager snowManager;
    private FireworkManager fireworkManager;
    private CurrencyManager currencyManager;
    
    // Слушатели
    private ChestLootListener chestLootListener;
    
    @Override
    public void onEnable() {
        logger = getLogger();
        logger.info("========================================");
        logger.info("  🎄 NewYearEvent Plugin загружается...");
        logger.info("========================================");
        
        // Создаем папки для данных
        createDataFolders();
        
        // Загружаем конфигурацию
        saveDefaultConfig();
        config = getConfig();
        
        // Инициализируем менеджеры
        snowManager = new SnowManager(this);
        fireworkManager = new FireworkManager(this);
        currencyManager = new CurrencyManager(this);
        
        // Регистрируем слушатели
        registerListeners();
        
        // Регистрируем команды
        registerCommands();
        
        // Если событие активно - запускаем менеджеры
        if (isEventActive()) {
            snowManager.startSnowfall();
            fireworkManager.startAutoFireworks();
            currencyManager.enableDisplayForAll();
            logger.info("✅ Событие активно - менеджеры запущены");
        }
        
        logger.info("========================================");
        logger.info("  ✅ NewYearEvent Plugin загружен!");
        logger.info("========================================");
    }
    
    @Override
    public void onDisable() {
        logger.info("========================================");
        logger.info("  🎄 NewYearEvent Plugin выгружается...");
        logger.info("========================================");
        
        // Останавливаем менеджеры
        if (snowManager != null) {
            snowManager.shutdown();
        }
        
        if (fireworkManager != null) {
            fireworkManager.shutdown();
        }
        
        if (currencyManager != null) {
            currencyManager.shutdown();
        }
        
        if (chestLootListener != null) {
            chestLootListener.shutdown();
        }
        
        // Сохраняем данные
        saveConfig();
        
        logger.info("========================================");
        logger.info("  ✅ NewYearEvent Plugin выгружен!");
        logger.info("========================================");
    }
    
    /**
     * Создает необходимые папки для данных
     */
    private void createDataFolders() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        // Папки для писем
        File lettersFolder = new File(dataFolder, "letters");
        if (!lettersFolder.exists()) {
            lettersFolder.mkdirs();
        }
        
        File blockLettersFolder = new File(lettersFolder, "block");
        if (!blockLettersFolder.exists()) {
            blockLettersFolder.mkdirs();
        }
        
        File freeLettersFolder = new File(lettersFolder, "free");
        if (!freeLettersFolder.exists()) {
            freeLettersFolder.mkdirs();
        }
        
        logger.info("📁 Папки данных созданы");
    }
    
    /**
     * Регистрирует все слушатели событий
     */
    private void registerListeners() {
        SnowListener snowListener = new SnowListener(this);
        getServer().getPluginManager().registerEvents(snowListener, this);
        
        SnowballListener snowballListener = new SnowballListener(this);
        getServer().getPluginManager().registerEvents(snowballListener, this);
        
        MobKillListener mobKillListener = new MobKillListener(this);
        getServer().getPluginManager().registerEvents(mobKillListener, this);
        
        chestLootListener = new ChestLootListener(this, currencyManager);
        getServer().getPluginManager().registerEvents(chestLootListener, this);
        
        logger.info("📋 Слушатели зарегистрированы");
    }
    
    /**
     * Регистрирует все команды
     */
    private void registerCommands() {
        // Команды писем
        getCommand("send_new_year_letter_block").setExecutor(new LetterBlockCommand(this));
        getCommand("send_new_year_letter_free").setExecutor(new LetterFreeCommand(this));
        
        // Команды управления событием
        getCommand("newyear_start").setExecutor(new StartEventCommand(this));
        getCommand("newyear_end").setExecutor(new EndEventCommand(this));
        
        // Команды фейерверков
        getCommand("newyear_firework").setExecutor(new FireworkCommand(this));
        
        // Команды просмотра писем и подарков
        getCommand("newyear_letters").setExecutor(new LettersCommand(this));
        getCommand("newyear_gift").setExecutor(new GiftCommand(this));
        
        // Команды валюты
        SnowflakesCommand snowflakesCommand = new SnowflakesCommand(this);
        getCommand("snowflakes").setExecutor(snowflakesCommand);
        
        // Команды магазина
        ShopCommand shopCommand = new ShopCommand(this, currencyManager);
        getCommand("newyear_shop").setExecutor(shopCommand);
        getServer().getPluginManager().registerEvents(shopCommand, this);
        
        logger.info("📋 Команды зарегистрированы");
    }
    
    /**
     * Проверяет активно ли событие
     */
    public boolean isEventActive() {
        return config.getBoolean("event_active", false);
    }
    
    /**
     * Устанавливает статус события
     */
    public void setEventActive(boolean active) {
        config.set("event_active", active);
        saveConfig();
    }
    
    /**
     * Получает менеджер снега
     */
    public SnowManager getSnowManager() {
        return snowManager;
    }
    
    /**
     * Получает менеджер фейерверков
     */
    public FireworkManager getFireworkManager() {
        return fireworkManager;
    }
    
    /**
     * Получает менеджер валюты
     */
    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }
    
    // ========================================
    // НОВЫЕ МЕТОДЫ ДЛЯ РАБОТЫ С ПИСЬМАМИ
    // ========================================
    
    /**
     * НОВЫЙ МЕТОД: Проверяет отправил ли игрок письмо
     */
    public boolean hasPlayerSentLetter(UUID uuid) {
        List<String> players = config.getStringList("players_with_letters");
        return players.contains(uuid.toString());
    }
    
    /**
     * НОВЫЙ МЕТОД: Добавляет игрока в список отправивших письма
     */
    public void addPlayerLetter(UUID uuid) {
        List<String> players = config.getStringList("players_with_letters");
        if (!players.contains(uuid.toString())) {
            players.add(uuid.toString());
            config.set("players_with_letters", players);
            saveConfig();
        }
    }
    
    /**
     * НОВЫЙ МЕТОД: Получает папку с письмами
     */
    public File getLettersFolder() {
        return new File(getDataFolder(), "letters");
    }
}