package com.yourname.newyearevent.listeners;

import com.yourname.newyearevent.NewYearEventPlugin;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;

public class SnowListener implements Listener {
    
    private final NewYearEventPlugin plugin;
    
    public SnowListener(NewYearEventPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Автоматически покрывает новые загруженные чанки снегом во время ивента
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        // Проверяем активен ли ивент
        if (!plugin.isEventActive()) {
            return;
        }
        
        // Пропускаем если идёт глобальный процесс
        if (plugin.getSnowManager().isProcessing() || plugin.getSnowManager().isRemoving()) {
            return;
        }
        
        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();
        
        // Только для overworld
        if (!world.getName().equals("world")) {
            return;
        }
        
        // Покрываем чанк снегом постепенно
        plugin.getSnowManager().addSnowToChunk(chunk);
    }
    
    /**
     * Отслеживаем натуральное образование снега (от погоды) и записываем его
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSnowForm(BlockFormEvent event) {
        // Проверяем активен ли ивент
        if (!plugin.isEventActive()) {
            return;
        }
        
        Block block = event.getBlock();
        
        // Проверяем что это именно снег
        if (event.getNewState().getType() == Material.SNOW) {
            // Записываем позицию снега, чтобы удалить его при окончании ивента
            plugin.getSnowManager().addSnowPosition(block.getLocation());
        }
    }
    
    /**
     * Сбрасываем персональную погоду для игроков при входе (чтобы видели CLEAR)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Проверяем активен ли ивент
        if (!plugin.isEventActive()) {
            return;
        }
        
        // Сбрасываем персональную погоду чтобы игрок видел мировую (CLEAR)
        event.getPlayer().resetPlayerWeather();
    }
}