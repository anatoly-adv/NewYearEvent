package com.yourname.newyearevent.managers;

import com.yourname.newyearevent.NewYearEventPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SnowManager {
    
    private final NewYearEventPlugin plugin;
    private final Set<String> snowPositions = ConcurrentHashMap.newKeySet();
    private File snowFile;
    
    private boolean isProcessing = false;
    private boolean isRemoving = false;
    private BukkitTask particleTask = null;
    private BukkitTask weatherTask = null;  // Таск для контроля погоды
    
    // Параметры размещения БАТЧАМИ (с адаптацией к TPS)
    private static final int ADD_BATCH_SIZE = 300;
    private static final long ADD_DELAY = 2L;
    
    // Параметры удаления БАТЧАМИ (с адаптацией к TPS)
    private static final int REMOVE_BATCH_SIZE = 300;
    private static final long REMOVE_DELAY = 2L;
    
    public SnowManager(NewYearEventPlugin plugin) {
        this.plugin = plugin;
        this.snowFile = new File(plugin.getDataFolder(), "snow_positions.txt");
        loadSnowPositions();
    }
    
    /**
     * Получает текущий TPS сервера
     */
    private double getCurrentTPS() {
        try {
            return Bukkit.getTPS()[0];
        } catch (Exception e) {
            return 20.0;
        }
    }
    
    /**
     * Вычисляет адаптивный размер батча на основе TPS
     */
    private int calculateBatchSize(int baseBatchSize) {
        double tps = getCurrentTPS();
        int batchSize = (int)(baseBatchSize * (tps / 20.0));
        
        int minBatch = baseBatchSize / 10;
        int maxBatch = baseBatchSize * 2;
        
        return Math.max(minBatch, Math.min(maxBatch, batchSize));
    }
    
    /**
     * НОВЫЙ МЕТОД: Запускает снегопад (вызывается из StartEventCommand)
     * Алиас для addSnow() + запуск частиц и погоды
     */
    public void startSnowfall() {
        plugin.getLogger().info("❄️ Запускаем снегопад...");
        addSnow();
    }
    
    /**
     * Начинает постепенное добавление снега БАТЧАМИ (с адаптацией к TPS)
     */
    public void addSnow() {
        if (isProcessing) {
            plugin.getLogger().warning("⚠ Процесс добавления снега уже идёт!");
            return;
        }
        
        isProcessing = true;
        plugin.getLogger().info("❄️ Начинаем покрывать мир снегом (БАТЧАМИ с TPS мониторингом)...");
        
        World world = Bukkit.getWorld("world");
        if (world == null) {
            plugin.getLogger().warning("⚠ Мир 'world' не найден!");
            isProcessing = false;
            return;
        }
        
        // Запускаем частицы снега
        startSnowParticles(world);
        
        // Запускаем контроль погоды (принудительно CLEAR)
        startWeatherControl(world);
        
        // Собираем все загруженные чанки
        Chunk[] chunks = world.getLoadedChunks();
        List<Block> blockList = new ArrayList<>();
        
        plugin.getLogger().info("📦 Собираем блоки из " + chunks.length + " загруженных чанков...");
        
        for (Chunk chunk : chunks) {
            if (shouldSkipChunk(chunk)) {
                continue;
            }
            
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int worldX = chunk.getX() * 16 + x;
                    int worldZ = chunk.getZ() * 16 + z;
                    int y = world.getHighestBlockYAt(worldX, worldZ);
                    
                    Block bottomBlock = world.getBlockAt(worldX, y, worldZ);
                    Block topBlock = bottomBlock.getRelative(BlockFace.UP);
                    
                    if (canPlaceSnow(bottomBlock, topBlock)) {
                        blockList.add(topBlock);
                    }
                }
            }
        }
        
        int totalBlocks = blockList.size();
        plugin.getLogger().info("❄️ Найдено блоков для снега: " + totalBlocks);
        
        if (totalBlocks == 0) {
            plugin.getLogger().info("✓ Нет блоков для добавления снега");
            isProcessing = false;
            return;
        }
        
        blockList.sort((b1, b2) -> Integer.compare(b1.getZ(), b2.getZ()));
        plugin.getLogger().info("🧭 Блоки отсортированы: СЕВЕР → ЮГ (по Z координате)");
        
        new BukkitRunnable() {
            private int index = 0;
            private int processed = 0;
            private long lastLogTime = System.currentTimeMillis();
            
            @Override
            public void run() {
                if (index >= blockList.size()) {
                    plugin.getLogger().info("✅ Снег успешно добавлен! Всего блоков: " + processed);
                    saveSnowPositionsAsync();
                    isProcessing = false;
                    this.cancel();
                    return;
                }
                
                int batchSize = calculateBatchSize(ADD_BATCH_SIZE);
                double tps = getCurrentTPS();
                
                for (int i = 0; i < batchSize && index < blockList.size(); i++, index++) {
                    Block block = blockList.get(index);
                    
                    if (block != null && block.getType() == Material.AIR) {
                        block.setType(Material.SNOW, false);
                        String pos = posToString(block.getLocation());
                        snowPositions.add(pos);
                        processed++;
                    }
                }
                
                long now = System.currentTimeMillis();
                if (now - lastLogTime > 3000) {
                    double percent = (index * 100.0) / totalBlocks;
                    
                    String direction = "";
                    if (index < blockList.size()) {
                        int z = blockList.get(index).getZ();
                        direction = " | Z: " + z + " (→ЮГ)";
                    }
                    
                    plugin.getLogger().info(String.format(
                        "❄️ Прогресс: %.1f%% (%d/%d) | TPS: %.1f | Батч: %d блоков%s",
                        percent, index, totalBlocks, tps, batchSize, direction
                    ));
                    lastLogTime = now;
                }
            }
        }.runTaskTimer(plugin, 0L, ADD_DELAY);
    }
    
    /**
     * Начинает удаление снега БАТЧАМИ (с адаптацией к TPS)
     */
    public void removeSnow() {
        if (isRemoving) {
            plugin.getLogger().warning("⚠ Процесс удаления снега уже идёт!");
            return;
        }
        
        if (snowPositions.isEmpty()) {
            plugin.getLogger().info("ℹ Нет снега для удаления");
            return;
        }
        
        isRemoving = true;
        stopSnowParticles();
        stopWeatherControl();
        
        plugin.getLogger().info("🧹 Начинаем удаление снега (БАТЧАМИ с TPS мониторингом)...");
        
        List<String> positions = new ArrayList<>(snowPositions);
        positions.sort((pos1, pos2) -> {
            try {
                int z1 = Integer.parseInt(pos1.split(":")[2]);
                int z2 = Integer.parseInt(pos2.split(":")[2]);
                return Integer.compare(z2, z1);
            } catch (Exception e) {
                return 0;
            }
        });
        
        int total = positions.size();
        plugin.getLogger().info("🧭 Удаление: ЮГ → СЕВЕР (обратный порядок)");
        
        new BukkitRunnable() {
            private int index = 0;
            private int removed = 0;
            private long lastLogTime = System.currentTimeMillis();
            
            @Override
            public void run() {
                if (index >= positions.size()) {
                    plugin.getLogger().info("✅ Снег успешно удалён! Всего блоков: " + removed);
                    snowPositions.clear();
                    saveSnowPositionsAsync();
                    isRemoving = false;
                    this.cancel();
                    return;
                }
                
                int batchSize = calculateBatchSize(REMOVE_BATCH_SIZE);
                double tps = getCurrentTPS();
                
                for (int i = 0; i < batchSize && index < positions.size(); i++, index++) {
                    String posStr = positions.get(index);
                    Location loc = stringToPos(posStr);
                    
                    if (loc != null) {
                        Block block = loc.getBlock();
                        if (block.getType() == Material.SNOW) {
                            block.setType(Material.AIR, false);
                            snowPositions.remove(posStr);
                            removed++;
                        }
                    }
                }
                
                long now = System.currentTimeMillis();
                if (now - lastLogTime > 3000) {
                    double percent = (index * 100.0) / total;
                    
                    String currentZ = "";
                    if (index < positions.size()) {
                        try {
                            int z = Integer.parseInt(positions.get(index).split(":")[2]);
                            currentZ = " | Z: " + z + " (→СЕВЕР)";
                        } catch (Exception ignored) {}
                    }
                    
                    plugin.getLogger().info(String.format(
                        "🧹 Прогресс: %.1f%% (%d/%d) | TPS: %.1f | Батч: %d блоков%s",
                        percent, index, total, tps, batchSize, currentZ
                    ));
                    lastLogTime = now;
                }
            }
        }.runTaskTimer(plugin, 0L, REMOVE_DELAY);
    }
    
    /**
     * Добавляет снег на новый загруженный чанк БАТЧАМИ
     */
    public void addSnowToChunk(Chunk chunk) {
        if (isProcessing || isRemoving) {
            return;
        }
        
        World world = chunk.getWorld();
        List<Block> blockList = new ArrayList<>();
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunk.getX() * 16 + x;
                int worldZ = chunk.getZ() * 16 + z;
                int y = world.getHighestBlockYAt(worldX, worldZ);
                
                Block bottomBlock = world.getBlockAt(worldX, y, worldZ);
                Block topBlock = bottomBlock.getRelative(BlockFace.UP);
                
                if (canPlaceSnow(bottomBlock, topBlock)) {
                    blockList.add(topBlock);
                }
            }
        }
        
        if (blockList.isEmpty()) {
            return;
        }
        
        blockList.sort((b1, b2) -> Integer.compare(b1.getZ(), b2.getZ()));
        
        new BukkitRunnable() {
            private int index = 0;
            
            @Override
            public void run() {
                if (index >= blockList.size()) {
                    this.cancel();
                    return;
                }
                
                int batchSize = calculateBatchSize(50);
                
                for (int i = 0; i < batchSize && index < blockList.size(); i++, index++) {
                    Block block = blockList.get(index);
                    
                    if (block != null && block.getType() == Material.AIR) {
                        block.setType(Material.SNOW, false);
                        String pos = posToString(block.getLocation());
                        snowPositions.add(pos);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, ADD_DELAY);
        
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, 
            this::saveSnowPositionsAsync, 100L);
    }
    
    /**
     * Запускает спавн частиц снега над всеми блоками снега (ПЛОТНЫЙ СНЕГОПАД)
     */
    private void startSnowParticles(World world) {
        if (particleTask != null) {
            particleTask.cancel();
        }
        
        plugin.getLogger().info("❄️ Запускаем плотный снегопад из частиц...");
        
        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (snowPositions.isEmpty()) {
                    return;
                }
                
                List<String> positions = new ArrayList<>(snowPositions);
                // УВЕЛИЧЕНО: 500 позиций за раз (было 200)
                int particlesPerTick = Math.min(500, positions.size());
                
                java.util.Collections.shuffle(positions);
                
                for (int i = 0; i < particlesPerTick && i < positions.size(); i++) {
                    String posStr = positions.get(i);
                    Location loc = stringToPos(posStr);
                    
                    if (loc != null) {
                        // Спавним НЕСКОЛЬКО частиц на разной высоте для плотности
                        for (int j = 0; j < 3; j++) {  // 3 частицы на позицию
                            double height = 10 + Math.random() * 40;  // От 10 до 50 блоков
                            Location particleLoc = loc.clone().add(
                                Math.random() - 0.5,  // Случайный X
                                height,
                                Math.random() - 0.5   // Случайный Z
                            );
                            
                            // УВЕЛИЧЕНО: 3 частицы за раз (было 1)
                            world.spawnParticle(
                                org.bukkit.Particle.SNOWFLAKE,
                                particleLoc,
                                3,       // Количество частиц
                                0.2,     // Разброс X
                                0.5,     // Разброс Y
                                0.2,     // Разброс Z
                                0.02     // Скорость падения
                            );
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);  // Каждые 2 тика
    }
    
    /**
     * Останавливает спавн частиц снега
     */
    private void stopSnowParticles() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
            plugin.getLogger().info("❄️ Частицы снега остановлены");
        }
    }
    
    /**
     * Запускает принудительный контроль погоды (всегда CLEAR)
     */
    private void startWeatherControl(World world) {
        if (weatherTask != null) {
            weatherTask.cancel();
        }
        
        plugin.getLogger().info("☀️ Принудительно устанавливаем CLEAR погоду (отключаем дождь/снег)...");
        
        weatherTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Устанавливаем ясную погоду
                world.setStorm(false);
                world.setThundering(false);
                world.setWeatherDuration(Integer.MAX_VALUE);
                world.setClearWeatherDuration(Integer.MAX_VALUE);
                
                // Сбрасываем персональную погоду для всех игроков
                world.getPlayers().forEach(player -> {
                    player.resetPlayerWeather();
                });
            }
        }.runTaskTimer(plugin, 0L, 200L);  // Каждые 10 секунд проверяем
    }
    
    /**
     * Останавливает контроль погоды
     */
    private void stopWeatherControl() {
        if (weatherTask != null) {
            weatherTask.cancel();
            weatherTask = null;
            plugin.getLogger().info("☀️ Контроль погоды остановлен");
        }
    }
    
    /**
     * Проверяет, нужно ли пропустить чанк
     */
    private boolean shouldSkipChunk(Chunk chunk) {
        World world = chunk.getWorld();
        int chunkX = chunk.getX() * 16;
        int chunkZ = chunk.getZ() * 16;
        
        for (int i = 0; i < 4; i++) {
            int x = chunkX + (i % 2) * 8 + 4;
            int z = chunkZ + (i / 2) * 8 + 4;
            int y = world.getHighestBlockYAt(x, z);
            
            Block block = world.getBlockAt(x, y, z);
            Material type = block.getType();
            
            if (type != Material.WATER && type != Material.LAVA) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Проверяет, можно ли разместить снег
     */
    private boolean canPlaceSnow(Block bottomBlock, Block topBlock) {
        if (topBlock.getType() != Material.AIR) {
            return false;
        }
        
        Material bottomType = bottomBlock.getType();
        String name = bottomType.name();
        
        if (bottomType == Material.WATER || bottomType == Material.LAVA) {
            return false;
        }
        
        if (name.contains("SLAB") && !name.contains("DOUBLE")) {
            return false;
        }
        
        if (name.contains("STAIRS")) {
            return false;
        }
        
        if (name.contains("TORCH") || 
            name.contains("LANTERN") ||
            name.contains("LAMP") ||
            name.contains("FIRE") ||
            name.contains("CAMPFIRE")) {
            return false;
        }
        
        if (name.contains("FLOWER") ||
            name.contains("SAPLING") ||
            name.contains("MUSHROOM") ||
            name.contains("BUSH") ||
            bottomType == Material.TALL_GRASS || 
            bottomType == Material.SHORT_GRASS ||
            bottomType == Material.FERN) {
            return false;
        }
        
        if (name.contains("CARPET") || 
            name.contains("PRESSURE_PLATE") || 
            name.contains("BUTTON")) {
            return false;
        }
        
        return true;
    }
    
    private String posToString(Location loc) {
        return loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }
    
    private Location stringToPos(String str) {
        try {
            String[] parts = str.split(":");
            World world = Bukkit.getWorld("world");
            if (world == null) return null;
            
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            
            return new Location(world, x, y, z);
        } catch (Exception e) {
            return null;
        }
    }
    
    private void saveSnowPositionsAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(snowFile))) {
                List<String> positions = new ArrayList<>(snowPositions);
                for (String pos : positions) {
                    writer.write(pos);
                    writer.newLine();
                }
                plugin.getLogger().info("💾 Сохранено позиций снега: " + positions.size());
            } catch (IOException e) {
                plugin.getLogger().severe("❌ Ошибка сохранения позиций снега: " + e.getMessage());
            }
        });
    }
    
    private void loadSnowPositions() {
        if (!snowFile.exists()) {
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(snowFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                snowPositions.add(line.trim());
            }
            
            if (!snowPositions.isEmpty()) {
                plugin.getLogger().info("✓ Загружено позиций снега: " + snowPositions.size());
            }
        } catch (IOException e) {
            plugin.getLogger().severe("❌ Ошибка загрузки позиций снега: " + e.getMessage());
        }
    }
    
    public void addSnowPosition(Location loc) {
        String pos = posToString(loc);
        snowPositions.add(pos);
    }
    
    public void removeSnowPosition(Location loc) {
        String pos = posToString(loc);
        snowPositions.remove(pos);
    }
    
    public boolean hasSnow() {
        return !snowPositions.isEmpty();
    }
    
    public int getSnowCount() {
        return snowPositions.size();
    }
    
    public boolean isProcessing() {
        return isProcessing;
    }
    
    public boolean isRemoving() {
        return isRemoving;
    }
    
    public void shutdown() {
        stopSnowParticles();
        stopWeatherControl();
        if (!snowPositions.isEmpty()) {
            saveSnowPositionsAsync();
        }
    }
}