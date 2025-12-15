package com.yourname.newyearevent.managers;

import com.yourname.newyearevent.NewYearEventPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;

public class FireworkManager {
    private final NewYearEventPlugin plugin;
    private final Random random;
    private BukkitTask autoTask;
    private boolean autoEnabled;

    private static final Color[] COLORS = {
        Color.RED, Color.WHITE, Color.BLUE, Color.YELLOW, 
        Color.LIME, Color.AQUA, Color.ORANGE, Color.FUCHSIA
    };

    private static final FireworkEffect.Type[] TYPES = {
        FireworkEffect.Type.BALL,
        FireworkEffect.Type.BALL_LARGE,
        FireworkEffect.Type.BURST,
        FireworkEffect.Type.CREEPER,
        FireworkEffect.Type.STAR
    };

    public FireworkManager(NewYearEventPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.autoEnabled = false;
    }

    /**
     * Запускает фейерверк над конкретным игроком на 5 блоков выше
     */
    public void launchFireworkAbovePlayer(Player player) {
        Location loc = player.getLocation().clone().add(0, 5, 0);
        launchFirework(loc);
    }

    /**
     * Запускает фейерверк в указанной локации
     */
    public void launchFirework(Location location) {
        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        // 1-3 эффекта
        int effectCount = random.nextInt(3) + 1;
        for (int i = 0; i < effectCount; i++) {
            FireworkEffect effect = FireworkEffect.builder()
                .with(TYPES[random.nextInt(TYPES.length)])
                .withColor(COLORS[random.nextInt(COLORS.length)])
                .withFade(COLORS[random.nextInt(COLORS.length)])
                .flicker(random.nextBoolean())
                .trail(random.nextBoolean())
                .build();
            meta.addEffect(effect);
        }

        meta.setPower(random.nextInt(2) + 1); // 1-2 power
        firework.setFireworkMeta(meta);
    }

    /**
     * Включает/выключает автоматические фейерверки
     */
    public void setAutoFireworks(boolean enabled) {
        this.autoEnabled = enabled;

        if (enabled) {
            startAutoFireworks();
        } else {
            stopAutoFireworks();
        }
    }

    /**
     * Запускает автоматические фейерверки каждые 2 минуты
     */
    public void startAutoFireworks() {
        if (autoTask != null) {
            autoTask.cancel();
        }

        // 2 минуты = 2400 тиков (120 секунд * 20 тиков)
        autoTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (autoEnabled) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    launchFireworkAbovePlayer(player);
                }
                Bukkit.broadcastMessage("§6§l✨ §eАвтоматические фейерверки! §6§l✨");
            }
        }, 2400L, 2400L); // Первый запуск через 2 мин, потом каждые 2 мин
        
        this.autoEnabled = true;
        plugin.getLogger().info("🎆 Автоматические фейерверки запущены!");
    }

    /**
     * Останавливает автоматические фейерверки
     */
    public void stopAutoFireworks() {
        if (autoTask != null) {
            autoTask.cancel();
            autoTask = null;
        }
        this.autoEnabled = false;
        plugin.getLogger().info("🎆 Автоматические фейерверки остановлены!");
    }

    public boolean isAutoEnabled() {
        return autoEnabled;
    }

    /**
     * Очистка при выключении плагина
     */
    public void shutdown() {
        stopAutoFireworks();
    }
}