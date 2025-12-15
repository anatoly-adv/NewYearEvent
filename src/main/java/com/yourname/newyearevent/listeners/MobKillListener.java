package com.yourname.newyearevent.listeners;

import com.yourname.newyearevent.NewYearEventPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Random;

public class MobKillListener implements Listener {
    
    private final NewYearEventPlugin plugin;
    private final Random random;
    
    // 10% шанс дропа снежинок (было 1%)
    private static final double DROP_CHANCE = 0.10;
    
    public MobKillListener(NewYearEventPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }
    
    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        // Проверяем активен ли ивент
        if (!plugin.isEventActive()) {
            return;
        }
        
        // Проверяем что убил игрок
        if (!(event.getEntity().getKiller() instanceof Player)) {
            return;
        }
        
        Player killer = event.getEntity().getKiller();
        
        // 10% шанс на дроп снежинок
        if (random.nextDouble() < DROP_CHANCE) {
            // Выдаём от 1 до 3 снежинок
            int amount = random.nextInt(3) + 1;
            
            // ИСПРАВЛЕНО: Получаем менеджер через plugin
            plugin.getCurrencyManager().addBalance(killer.getUniqueId(), amount);
            
            killer.sendMessage("§b§l✨ §7Вам выпало §b" + amount + " ❄ §7снежинок!");
            
            plugin.getLogger().info("💰 Игрок " + killer.getName() + " получил " + amount + " снежинок за убийство моба");
        }
    }
}