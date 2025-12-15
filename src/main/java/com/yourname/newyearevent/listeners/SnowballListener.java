package com.yourname.newyearevent.listeners;

import com.yourname.newyearevent.NewYearEventPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SnowballListener implements Listener {
    
    private final NewYearEventPlugin plugin;
    
    public SnowballListener(NewYearEventPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onSnowballHit(ProjectileHitEvent event) {
        // Проверяем, активно ли событие
        if (!plugin.isEventActive()) {
            return;
        }
        
        // Проверяем, что это снежок
        if (!(event.getEntity() instanceof Snowball)) {
            return;
        }
        
        Snowball snowball = (Snowball) event.getEntity();
        
        // Проверяем, что бросил игрок
        if (!(snowball.getShooter() instanceof Player)) {
            return;
        }
        
        Player shooter = (Player) snowball.getShooter();
        
        // Проверяем, что попал в живое существо (игрок, моб, животное и т.д.)
        if (!(event.getHitEntity() instanceof LivingEntity)) {
            return;
        }
        
        LivingEntity victim = (LivingEntity) event.getHitEntity();
        
        // Не обрабатываем попадание в самого себя
        if (victim.equals(shooter)) {
            return;
        }
        
        // Наносим урон
        victim.damage(1.0);
        
        // Откидывание
        victim.setVelocity(victim.getVelocity().add(snowball.getVelocity().normalize().multiply(2.0)));
        
        // Эффект заморозки (замедление)
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 2)); // 1 секунда, уровень III
        
        // Регенерация жертве
        victim.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20, 0)); // 1 секунда, уровень I
        
        // Частицы снега
        Location hitLoc = victim.getLocation().clone().add(0, 1, 0);
        victim.getWorld().spawnParticle(Particle.SNOWFLAKE, hitLoc, 20, 0.5, 0.5, 0.5, 0.1);
        
        // Звуки
        victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
        shooter.playSound(shooter.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);
        
        // ИСПРАВЛЕНО: Используем Adventure API вместо устаревшего String API
        // БЛИНК ЭФФЕКТ в ActionBar - показываем 0.2 сек, потом агрессивно убираем
        shooter.sendActionBar(Component.text("❄ Попадание!", NamedTextColor.AQUA));
        
        // Через 4 тика (0.2 сек) начинаем агрессивно стирать
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Отправляем пустой Component вместо пробела - более надежная очистка
            shooter.sendActionBar(Component.empty());
        }, 4L);
        
        // Дублируем очистку еще раз для гарантии
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            shooter.sendActionBar(Component.empty());
        }, 5L);
        
        // И еще раз
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            shooter.sendActionBar(Component.empty());
        }, 6L);
    }
}