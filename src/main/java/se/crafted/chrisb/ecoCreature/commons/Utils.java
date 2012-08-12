package se.crafted.chrisb.ecoCreature.commons;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class Utils
{
    public static boolean isUnderSeaLevel(Entity entity)
    {
        return entity != null && (entity.getLocation().getBlockY() < entity.getWorld().getSeaLevel());
    }

    public static Player getKillerFromDeathEvent(EntityDeathEvent event)
    {
        if (event != null && event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent) {

            Entity damager = ((EntityDamageByEntityEvent) event.getEntity().getLastDamageCause()).getDamager();

            if (damager instanceof Player) {
                return (Player) damager;
            }
            else if (damager instanceof Tameable) {
                Tameable tameable = (Tameable) damager;
                if (tameable.isTamed() && tameable.getOwner() instanceof Player) {
                    return (Player) tameable.getOwner();
                }
            }
            else if (damager instanceof Projectile) {
                Projectile projectile = (Projectile) damager;
                if (projectile.getShooter() instanceof Player) {
                    return (Player) projectile.getShooter();
                }
            }
        }

        return null;
    }

    public static LivingEntity getTamedKillerFromDeathEvent(EntityDeathEvent event)
    {
        if (event != null && event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent) {

            Entity damager = ((EntityDamageByEntityEvent) event.getEntity().getLastDamageCause()).getDamager();

            if (damager instanceof Tameable) {
                Tameable tameable = (Tameable) damager;
                if (tameable.isTamed() && tameable.getOwner() instanceof Player) {
                    return (LivingEntity) damager;
                }
            }
        }

        return null;
    }

    public static boolean isPVPDeath(PlayerDeathEvent event)
    {
        return event != null && event.getEntity().getKiller() != null;
    }

    public static boolean isOwner(Player player, Entity entity)
    {
        if (entity instanceof Tameable) {
            Tameable tameable = (Tameable) entity;
            if (tameable.isTamed() && tameable.getOwner() instanceof Player) {
                Player owner = (Player) tameable.getOwner();
                if (owner.getName().equals(player.getName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
