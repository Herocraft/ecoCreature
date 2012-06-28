package se.crafted.chrisb.ecoCreature.managers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import se.crafted.chrisb.ecoCreature.ecoCreature;
import se.crafted.chrisb.ecoCreature.events.CreatureKilledByPlayerEvent;
import se.crafted.chrisb.ecoCreature.events.PlayerKilledByPlayerEvent;
import se.crafted.chrisb.ecoCreature.models.ecoReward;
import se.crafted.chrisb.ecoCreature.models.ecoReward.RewardType;
import se.crafted.chrisb.ecoCreature.utils.ecoEntityUtil;
import se.crafted.chrisb.ecoCreature.utils.ecoEntityUtil.TimePeriod;

public class ecoRewardManager
{
    public static Boolean warnGroupMultiplierSupport = true;

    public Boolean isIntegerCurrency;

    public Boolean canCampSpawner;
    public boolean campByDistance;
    public boolean campByEntity;
    public boolean shouldClearDefaultDrops;
    public Boolean shouldOverrideDrops;
    public Boolean isFixedDrops;
    public Boolean shouldClearCampDrops;
    public int campRadius;
    public Boolean hasBowRewards;
    public Boolean hasDeathPenalty;
    public Boolean hasPVPReward;
    public Boolean isPercentPenalty;
    public Boolean isPercentPvpReward;
    public Double penaltyAmount;
    public double pvpRewardAmount;
    public Boolean canHuntUnderSeaLevel;
    public Boolean isWolverineMode;
    public Boolean hasDTPRewards;
    public double dtpRewardAmount;
    public double dtpPenaltyAmount;
    public Boolean noFarm;
    public Boolean noFarmFire;
    public boolean hasMobArenaRewards;

    public Map<String, Double> groupMultiplier;
    public Map<TimePeriod, Double> timeMultiplier;
    public Map<Environment, Double> envMultiplier;
    public Map<RewardType, List<ecoReward>> rewards;
    public Map<String, ecoReward> rewardSet;

    private final ecoCreature plugin;

    public ecoRewardManager(ecoCreature plugin)
    {
        this.plugin = plugin;

        groupMultiplier = new HashMap<String, Double>();
        timeMultiplier = new HashMap<TimePeriod, Double>();
        envMultiplier = new HashMap<Environment, Double>();
        rewards = new HashMap<RewardType, List<ecoReward>>();
        rewardSet = new HashMap<String, ecoReward>();
    }

    public ecoReward getRewardFromEntity(Entity entity)
    {
        RewardType rewardType = RewardType.fromEntity(entity);
        ecoReward reward = null;

        if (rewardType != null) {
            reward = getRewardFromType(rewardType);
        }
        else {
            ecoCreature.getEcoLogger().warning("No reward found for entity " + entity.getClass());
        }

        return reward;
    }

    public ecoReward getRewardFromType(RewardType rewardType)
    {
        Random random = new Random();
        ecoReward reward = null;

        if (rewards.containsKey(rewardType)) {
            List<ecoReward> rewardList = rewards.get(rewardType);
            reward = rewardList.get(random.nextInt(rewardList.size()));
        }
        else {
            ecoCreature.getEcoLogger().warning("No reward defined for " + rewardType);
        }

        return reward;
    }

    public void registerPVPReward(PlayerKilledByPlayerEvent event)
    {
        if (!hasPVPReward || !hasPermission(event.getVictim(), "reward.player")) {
            return;
        }

        Double amount = 0.0D;

        if (rewards.containsKey(RewardType.PLAYER)) {
            ecoReward reward = getRewardFromType(RewardType.PLAYER);

            if (reward != null) {
                Integer exp = reward.getExpAmount();

                amount = computeReward(event.getVictim(), reward);
                if (!reward.getDrops().isEmpty() && shouldOverrideDrops) {
                    event.getDrops().clear();
                }
                event.getDrops().addAll(reward.computeDrops());
                if (exp != null) {
                    event.setDroppedExp(exp);
                }
            }
        }
        else if (plugin.hasEconomy()) {
            amount = isPercentPvpReward ? ecoCreature.economy.getBalance(event.getVictim().getName()) * (pvpRewardAmount / 100.0D) : pvpRewardAmount;
        }

        if (amount > 0.0D && plugin.hasEconomy()) {
            amount = Math.min(amount, ecoCreature.economy.getBalance(event.getVictim().getName()));
            ecoCreature.economy.withdrawPlayer(event.getVictim().getName(), amount);
            ecoCreature.getMessageManager(event.getVictim()).sendMessage(ecoCreature.getMessageManager(event.getVictim()).deathPenaltyMessage, event.getVictim(), amount);

            ecoCreature.economy.depositPlayer(event.getKiller().getName(), amount);
            ecoCreature.getMessageManager(event.getVictim()).sendMessage(ecoCreature.getMessageManager(event.getVictim()).pvpRewardMessage, event.getKiller(), amount, event.getVictim().getName(), "");
        }
    }

    public void registerDeathPenalty(Player player)
    {
        if (!hasDeathPenalty || !hasPermission(player, "reward.deathpenalty") || !plugin.hasEconomy()) {
            return;
        }

        Double amount = isPercentPenalty ? ecoCreature.economy.getBalance(player.getName()) * (penaltyAmount / 100.0D) : penaltyAmount;
        if (amount > 0.0D) {
            ecoCreature.economy.withdrawPlayer(player.getName(), amount);
            ecoCreature.getMessageManager(player).sendMessage(ecoCreature.getMessageManager(player).deathPenaltyMessage, player, amount);
        }
    }

    public void registerCreatureDeath(CreatureKilledByPlayerEvent event)
    {
        if (shouldClearDefaultDrops) {
            event.getDrops().clear();
        }

        if (event.getKiller().getItemInHand().getType().equals(Material.BOW) && !hasBowRewards) {
            ecoCreature.getMessageManager(event.getKiller()).sendMessage(ecoCreature.getMessageManager(event.getKiller()).noBowRewardMessage, event.getKiller());
            return;
        }
        else if (ecoEntityUtil.isUnderSeaLevel(event.getKiller()) && !canHuntUnderSeaLevel) {
            ecoCreature.getMessageManager(event.getKiller()).sendMessage(ecoCreature.getMessageManager(event.getKiller()).noBowRewardMessage, event.getKiller());
            return;
        }
        else if (ecoEntityUtil.isOwner(event.getKiller(), event.getKilledCreature())) {
            // TODO: message no killing your own pets?
            return;
        }
        else if (ecoCreature.mobArenaHandler != null && ecoCreature.mobArenaHandler.isPlayerInArena(event.getKiller()) && !hasMobArenaRewards) {
            // TODO: message no arena awards?
            return;
        }
        else if (!canCampSpawner && (campByDistance || campByEntity)) {
            // Reordered the conditional slightly, to make it more efficient, since
            // java will stop evaluating when it knows the outcome.
            if ((campByEntity && ecoEntityUtil.isSpawnerMob(event.getKilledCreature())) || (campByDistance && (ecoEntityUtil.isNearSpawner(event.getKiller()) || ecoEntityUtil.isNearSpawner(event.getKilledCreature())))) {
                if (shouldClearCampDrops) {
                    event.getDrops().clear();
                    event.setDroppedExp(0);
                }
                ecoCreature.getMessageManager(event.getKiller()).sendMessage(ecoCreature.getMessageManager(event.getKiller()).noCampMessage, event.getKiller());
                return;
            }
        }
        else if (!hasPermission(event.getKiller(), "reward." + RewardType.fromEntity(event.getKilledCreature()).getName())) {
            return;
        }

        ecoReward reward = getRewardFromEntity(event.getKilledCreature());

        if (reward != null) {
            Integer exp = reward.getExpAmount();
            if (exp != null) {
                event.setDroppedExp(exp);
            }
            String weaponName = event.getTamedCreature() != null ? RewardType.fromEntity(event.getTamedCreature()).getName() : Material.getMaterial(event.getKiller().getItemInHand().getTypeId()).name();
            registerReward(event.getKiller(), reward, weaponName);
            try {
                List<ItemStack> rewardDrops = reward.computeDrops();
                if (!rewardDrops.isEmpty()) {
                    if (!event.getDrops().isEmpty() && shouldOverrideDrops) {
                        event.getDrops().clear();
                    }
                    event.getDrops().addAll(rewardDrops);
                }
            }
            catch (IllegalArgumentException e) {
                ecoCreature.getEcoLogger().warning(e.getMessage());
            }
        }
    }

    public void registerSpawnerBreak(Player player, Block block)
    {
        if (player == null || block == null) {
            return;
        }

        if (!block.getType().equals(Material.MOB_SPAWNER)) {
            return;
        }

        if (hasPermission(player, "reward.spawner") && rewards.containsKey(RewardType.SPAWNER)) {

            ecoReward reward = getRewardFromType(RewardType.SPAWNER);
            
            if (reward != null) {
                registerReward(player, reward, Material.getMaterial(player.getItemInHand().getTypeId()).name());
            
                for (ItemStack itemStack : reward.computeDrops()) {
                    block.getWorld().dropItemNaturally(block.getLocation(), itemStack);
                }
            }
        }
    }

    public void registerDeathStreak(Player player, Integer deaths)
    {
        if (hasPermission(player, "reward.deathstreak") && rewards.containsKey(RewardType.DEATH_STREAK)) {

            ecoReward reward = getRewardFromType(RewardType.DEATH_STREAK);

            if (reward != null) {
                reward.setCoinMin(reward.getCoinMin() * deaths);
                reward.setCoinMax(reward.getCoinMax() * deaths);
                registerReward(player, reward, "");

                for (ItemStack itemStack : reward.computeDrops()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
                }
            }
        }
    }

    public void registerKillStreak(Player player, Integer kills)
    {
        if (hasPermission(player, "reward.killstreak") && rewards.containsKey(RewardType.KILL_STREAK)) {

            ecoReward reward = getRewardFromType(RewardType.KILL_STREAK);

            if (reward != null) {
                reward.setCoinMin(reward.getCoinMin() * kills);
                reward.setCoinMax(reward.getCoinMax() * kills);
                registerReward(player, reward, "");

                for (ItemStack itemStack : reward.computeDrops()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
                }
            }
        }
    }

    public void handleNoFarm(EntityDeathEvent event)
    {
        EntityDamageEvent damageEvent = event.getEntity().getLastDamageCause();

        if (damageEvent != null) {
            if (damageEvent instanceof EntityDamageByBlockEvent && damageEvent.getCause().equals(DamageCause.CONTACT)) {
                event.getDrops().clear();
            }
            else if (damageEvent.getCause() != null) {
                if (damageEvent.getCause().equals(DamageCause.FALL) || damageEvent.getCause().equals(DamageCause.DROWNING) || damageEvent.getCause().equals(DamageCause.SUFFOCATION)) {
                    event.getDrops().clear();
                }
                else if (noFarmFire && (damageEvent.getCause().equals(DamageCause.FIRE) || damageEvent.getCause().equals(DamageCause.FIRE_TICK))) {
                    event.getDrops().clear();
                }
            }
        }
    }

    private void registerReward(Player player, ecoReward reward, String weaponName)
    {
        Double amount = computeReward(player, reward);

        if (amount > 0.0D && plugin.hasEconomy()) {
            ecoCreature.economy.depositPlayer(player.getName(), amount);
            ecoCreature.getMessageManager(player).sendMessage(reward.getRewardMessage(), player, amount, reward.getRewardName(), weaponName);
        }
        else if (amount < 0.0D && plugin.hasEconomy()) {
            ecoCreature.economy.withdrawPlayer(player.getName(), Math.abs(amount));
            ecoCreature.getMessageManager(player).sendMessage(reward.getPenaltyMessage(), player, Math.abs(amount), reward.getRewardName(), weaponName);
        }
        else {
            ecoCreature.getMessageManager(player).sendMessage(reward.getNoRewardMessage(), player, reward.getRewardName(), weaponName);
        }

        plugin.getMetrics().addCount(reward.getRewardType());
    }

    private Double computeReward(Player player, ecoReward reward)
    {
        Double amount = reward.getRewardAmount();

        try {
            if (ecoCreature.permission.getPrimaryGroup(player.getWorld().getName(), player.getName()) != null) {
                String group = ecoCreature.permission.getPrimaryGroup(player.getWorld().getName(), player.getName()).toLowerCase();
                if (hasPermission(player, "gain.group") && groupMultiplier.containsKey(group)) {
                    amount *= groupMultiplier.get(group);
                }
            }
        }
        catch (UnsupportedOperationException e) {
            if (warnGroupMultiplierSupport) {
                ecoCreature.getEcoLogger().warning(e.getMessage());
                warnGroupMultiplierSupport = false;
            }
        }

        if (hasPermission(player, "gain.time") && timeMultiplier.containsKey(ecoEntityUtil.getTimePeriod(player))) {
            amount *= timeMultiplier.get(ecoEntityUtil.getTimePeriod(player));
        }

        if (hasPermission(player, "gain.environment") && envMultiplier.containsKey(player.getWorld().getEnvironment())) {
            amount *= envMultiplier.get(player.getWorld().getEnvironment());
        }

        return isIntegerCurrency ? (double) Math.round(amount) : amount;
    }

    private Boolean hasPermission(Player player, String perm)
    {
        return ecoCreature.permission.has(player.getWorld(), player.getName(), "ecoCreature." + perm) || ecoCreature.permission.has(player.getWorld(), player.getName(), "ecocreature." + perm.toLowerCase());
    }
}