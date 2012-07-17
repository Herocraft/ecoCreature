package se.crafted.chrisb.ecoCreature.managers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import se.crafted.chrisb.ecoCreature.ecoCreature;
import se.crafted.chrisb.ecoCreature.models.ecoDrop;
import se.crafted.chrisb.ecoCreature.models.ecoMessage;
import se.crafted.chrisb.ecoCreature.models.ecoReward;
import se.crafted.chrisb.ecoCreature.models.ecoReward.RewardType;
import se.crafted.chrisb.ecoCreature.utils.ecoEntityUtil.TimePeriod;

public class ecoConfigManager
{
    public static final String DEFAULT_WORLD = "__DEFAULT_WORLD__";

    private static final String OLD_CONFIG_FILE = "ecoCreature.yml";
    private static final String DEFAULT_CONFIG_FILE = "default.yml";

    private final ecoCreature plugin;
    private final File dataWorldsFolder;

    private File defaultConfigFile;
    private FileConfiguration defaultConfig;
    private Map<String, FileConfiguration> worldConfigs;

    public ecoConfigManager(ecoCreature plugin)
    {
        this.plugin = plugin;
        dataWorldsFolder = new File(plugin.getDataFolder(), "worlds");
        dataWorldsFolder.mkdirs();

        try {
            load();
        }
        catch (Exception e) {
            ecoCreature.getEcoLogger().severe("Failed to load config: " + e.toString());
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }

    private void load() throws FileNotFoundException, IOException, InvalidConfigurationException
    {
        defaultConfig = new YamlConfiguration();
        defaultConfigFile = new File(plugin.getDataFolder(), DEFAULT_CONFIG_FILE);

        File oldConfigFile = new File(plugin.getDataFolder(), OLD_CONFIG_FILE);

        if (defaultConfigFile.exists()) {
            defaultConfig.load(defaultConfigFile);
        }
        else if (oldConfigFile.exists()) {
            ecoCreature.getEcoLogger().info("Converting old config file.");
            defaultConfig = getConfig(oldConfigFile);
            if (oldConfigFile.delete()) {
                ecoCreature.getEcoLogger().info("Old config file converted.");
            }
        }
        else {
            defaultConfig = getConfig(defaultConfigFile);
        }

        ecoCreature.getEcoLogger().info("Loaded config defaults.");
        ecoMessageManager defaultMessageManager = loadMessageConfig(defaultConfig);
        ecoRewardManager defaultRewardManager = loadRewardConfig(defaultConfig);
        plugin.getGlobalMessageManager().put(DEFAULT_WORLD, defaultMessageManager);
        plugin.getGlobalRewardManager().put(DEFAULT_WORLD, defaultRewardManager);

        worldConfigs = new HashMap<String, FileConfiguration>();

        for (World world : plugin.getServer().getWorlds()) {

            File worldConfigFile = new File(dataWorldsFolder, world.getName() + ".yml");

            if (worldConfigFile.exists()) {
                FileConfiguration worldConfig = getConfig(worldConfigFile);
                ecoCreature.getEcoLogger().info("Loaded config for " + world.getName() + " world.");
                plugin.getGlobalMessageManager().put(world.getName(), loadMessageConfig(worldConfig));
                plugin.getGlobalRewardManager().put(world.getName(), loadRewardConfig(worldConfig));
                worldConfigs.put(world.getName(), worldConfig);
            }
            else {
                plugin.getGlobalMessageManager().put(world.getName(), defaultMessageManager);
                plugin.getGlobalRewardManager().put(world.getName(), defaultRewardManager);
            }

        }
    }

    public void save()
    {
        try {
            defaultConfig.save(defaultConfigFile);

            for (String worldName : worldConfigs.keySet()) {
                FileConfiguration config = worldConfigs.get(worldName);
                File configFile = new File(dataWorldsFolder, worldName + ".yml");
                config.save(configFile);
            }
        }
        catch (Exception e) {
            ecoCreature.getEcoLogger().severe(e.getMessage());
        }
    }

    private ecoMessageManager loadMessageConfig(FileConfiguration config)
    {
        ecoMessageManager messageManager = new ecoMessageManager(plugin);

        messageManager.shouldOutputMessages = config.getBoolean("System.Messages.Output", true);
        messageManager.shouldLogCoinRewards = config.getBoolean("System.Messages.LogCoinRewards", true);
        messageManager.noBowRewardMessage = new ecoMessage(convertMessage(config.getString("System.Messages.NoBowMessage", ecoMessageManager.NO_BOW_REWARD_MESSAGE)), true);
        messageManager.noCampMessage = new ecoMessage(convertMessage(config.getString("System.Messages.NoCampMessage", ecoMessageManager.NO_CAMP_MESSAGE)), config.getBoolean("System.Messages.Spawner", false));
        messageManager.deathPenaltyMessage = new ecoMessage(convertMessage(config.getString("System.Messages.DeathPenaltyMessage", ecoMessageManager.DEATH_PENALTY_MESSAGE)), true);
        messageManager.pvpRewardMessage = new ecoMessage(convertMessage(config.getString("System.Messages.PVPRewardMessage", ecoMessageManager.PVP_REWARD_MESSAGE)), true);

        return messageManager;
    }

    private ecoRewardManager loadRewardConfig(FileConfiguration config)
    {
        ecoRewardManager rewardManager = new ecoRewardManager(plugin);

        rewardManager.isIntegerCurrency = config.getBoolean("System.Economy.IntegerCurrency", false);

        rewardManager.canCampSpawner = config.getBoolean("System.Hunting.AllowCamping", false);
        rewardManager.shouldClearCampDrops = config.getBoolean("System.Hunting.ClearCampDrops", true);
        rewardManager.campByDistance = config.getBoolean("System.Hunting.CampingByDistance", true);
        rewardManager.campByEntity = config.getBoolean("System.Hunting.CampingByEntity", false);
        rewardManager.shouldClearDefaultDrops = config.getBoolean("System.Hunting.ClearDefaultDrops", true);
        rewardManager.shouldOverrideDrops = config.getBoolean("System.Hunting.OverrideDrops", true);
        rewardManager.isFixedDrops = config.getBoolean("System.Hunting.FixedDrops", false);
        rewardManager.campRadius = config.getInt("System.Hunting.CampRadius", 7);
        rewardManager.hasBowRewards = config.getBoolean("System.Hunting.BowRewards", true);
        rewardManager.hasDeathPenalty = config.getBoolean("System.Hunting.PenalizeDeath", false);
        rewardManager.hasPVPReward = config.getBoolean("System.Hunting.PVPReward", true);
        rewardManager.isPercentPenalty = config.getBoolean("System.Hunting.PenalizeType", true);
        rewardManager.isPercentPvpReward = config.getBoolean("System.Hunting.PVPRewardType", true);
        rewardManager.penaltyAmount = config.getDouble("System.Hunting.PenalizeAmount", 0.05D);
        rewardManager.pvpRewardAmount = config.getDouble("System.Hunting.PenalizeAmount", 0.05D);
        rewardManager.canHuntUnderSeaLevel = config.getBoolean("System.Hunting.AllowUnderSeaLVL", true);
        rewardManager.isWolverineMode = config.getBoolean("System.Hunting.WolverineMode", true);
        rewardManager.noFarm = config.getBoolean("System.Hunting.NoFarm", false);
        rewardManager.noFarmFire = config.getBoolean("System.Hunting.NoFarmFire", false);
        rewardManager.hasMobArenaRewards = config.getBoolean("System.Hunting.MobArenaRewards", false);
        rewardManager.hasCreativeModeRewards = config.getBoolean("System.Hunting.CreativeModeRewards", false);

        ConfigurationSection groupGainConfig = config.getConfigurationSection("Gain.Groups");
        if (groupGainConfig != null) {
            for (String group : groupGainConfig.getKeys(false)) {
                rewardManager.groupMultiplier.put(group.toLowerCase(), Double.valueOf(groupGainConfig.getConfigurationSection(group).getDouble("Amount", 0.0D)));
            }
        }

        ConfigurationSection timeGainConfig = config.getConfigurationSection("Gain.Time");
        if (timeGainConfig != null) {
            for (String period : timeGainConfig.getKeys(false)) {
                rewardManager.timeMultiplier.put(TimePeriod.fromName(period), Double.valueOf(timeGainConfig.getConfigurationSection(period).getDouble("Amount", 1.0D)));
            }
        }

        ConfigurationSection envGainConfig = config.getConfigurationSection("Gain.Environment");
        if (envGainConfig != null) {
            for (String environment : envGainConfig.getKeys(false)) {
                try {
                    rewardManager.envMultiplier.put(Environment.valueOf(environment.toUpperCase()), Double.valueOf(envGainConfig.getConfigurationSection(environment).getDouble("Amount", 1.0D)));
                }
                catch (Exception e) {
                    ecoCreature.getEcoLogger().warning("Skipping unknown environment name: " + environment);
                }
            }
        }

        ConfigurationSection worldGuardGainConfig = config.getConfigurationSection("Gain.WorldGuard");
        if (worldGuardGainConfig != null) {
            for (String regionName : worldGuardGainConfig.getKeys(false)) {
                rewardManager.worldGuardRegionMultiplier.put(regionName, Double.valueOf(worldGuardGainConfig.getConfigurationSection(regionName).getDouble("Amount", 1.0D)));
            }
        }

        ConfigurationSection mobArenaGainConfig = config.getConfigurationSection("Gain.MobArena.InArena");
        if (mobArenaGainConfig != null) {
            rewardManager.mobArenaMultiplier = mobArenaGainConfig.getDouble("Amount", 1.0D);
            rewardManager.isMobArenaShare = mobArenaGainConfig.getBoolean("Share", true);
        }
        else {
            rewardManager.mobArenaMultiplier = 1.0D;
        }

        ConfigurationSection heroesGainConfig = config.getConfigurationSection("Gain.Heroes.InParty");
        if (heroesGainConfig != null) {
            rewardManager.heroesPartyMultiplier = heroesGainConfig.getDouble("Amount", 1.0D);
            rewardManager.isHeroesPartyShare = heroesGainConfig.getBoolean("Share", true);
        }
        else {
            rewardManager.heroesPartyMultiplier = 1.0D;
        }

        Map<String, ecoReward> rewardSet = new HashMap<String, ecoReward>();
        ConfigurationSection rewardSetsConfig = config.getConfigurationSection("RewardSets");
        if (rewardSetsConfig != null) {
            for (String setName : rewardSetsConfig.getKeys(false)) {
                rewardSet.put(setName, createReward(RewardType.CUSTOM, rewardSetsConfig.getConfigurationSection(setName), rewardManager, config.getBoolean("System.Messages.NoReward", false)));
            }
        }

        ConfigurationSection rewardTableConfig = config.getConfigurationSection("RewardTable");
        if (rewardTableConfig != null) {
            for (String rewardName : rewardTableConfig.getKeys(false)) {
                ecoReward reward = createReward(RewardType.fromName(rewardName), rewardTableConfig.getConfigurationSection(rewardName), rewardManager, config.getBoolean("System.Messages.NoReward", false));

                if (!rewardManager.rewards.containsKey(reward.getRewardType())) {
                    rewardManager.rewards.put(reward.getRewardType(), new ArrayList<ecoReward>());
                }

                if (rewardTableConfig.getConfigurationSection(rewardName).getList("Sets") != null) {
                    List<String> setList = rewardTableConfig.getConfigurationSection(rewardName).getStringList("Sets");
                    for (String setName : setList) {
                        if (rewardSet.containsKey(setName)) {
                            rewardManager.rewards.get(reward.getRewardType()).add(mergeReward(reward, rewardSet.get(setName)));
                        }
                    }
                }
                else {
                    rewardManager.rewards.get(reward.getRewardType()).add(reward);
                }
            }
        }

        return rewardManager;
    }

    private static ecoReward mergeReward(ecoReward from, ecoReward to)
    {
        ecoReward reward = new ecoReward();

        reward.setRewardName(to.getRewardName());
        reward.setRewardType(to.getRewardType());

        reward.setDrops(!from.getDrops().isEmpty() ? from.getDrops() : to.getDrops());

        reward.setCoinMin(from.getCoinMin() != null ? from.getCoinMin() : to.getCoinMin());
        reward.setCoinMax(from.getCoinMax() != null ? from.getCoinMax() : to.getCoinMax());
        reward.setCoinPercentage(from.getCoinPercentage() != null ? from.getCoinPercentage() : to.getCoinPercentage());

        reward.setExpMin(from.getExpMin() != null ? from.getExpMin() : to.getExpMin());
        reward.setExpMax(from.getExpMax() != null ? from.getExpMax() : to.getExpMax());
        reward.setExpPercentage(from.getExpPercentage() != null ? from.getExpPercentage() : to.getExpPercentage());

        reward.setNoRewardMessage(!from.getNoRewardMessage().equals(to.getNoRewardMessage()) ? from.getNoRewardMessage() : to.getNoRewardMessage());
        reward.setRewardMessage(!from.getRewardMessage().equals(to.getRewardMessage()) ? from.getRewardMessage() : to.getRewardMessage());
        reward.setPenaltyMessage(!from.getPenaltyMessage().equals(to.getPenaltyMessage()) ? from.getPenaltyMessage() : to.getPenaltyMessage());

        return reward;
    }

    private FileConfiguration getConfig(File file) throws FileNotFoundException, IOException, InvalidConfigurationException
    {
        FileConfiguration config = new YamlConfiguration();

        try {
            if (!file.exists()) {
                file.getParentFile().mkdir();
                file.createNewFile();
                InputStream inputStream = plugin.getResource(file.getName());
                FileOutputStream outputStream = new FileOutputStream(file);

                byte[] buffer = new byte[8192];
                int length = 0;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                inputStream.close();
                outputStream.close();

                ecoCreature.getEcoLogger().info("Default config written to " + file.getName());
            }

            config.load(file);
            config.setDefaults(YamlConfiguration.loadConfiguration(plugin.getResource(file.getName())));
            config.options().copyDefaults(true);
        }
        catch (Exception e) {
            ecoCreature.getEcoLogger().warning("Default config could not be created!");
        }

        return config;
    }

    private static ecoReward createReward(RewardType rewardType, ConfigurationSection rewardConfig, ecoRewardManager rewardManager, boolean isNoRewardMessage)
    {
        ecoReward reward = new ecoReward();
        reward.setRewardName(rewardConfig.getName());
        reward.setRewardType(rewardType);

        if (rewardConfig.getList("Drops") != null) {
            List<String> dropsList = rewardConfig.getStringList("Drops");
            reward.setDrops(ecoDrop.parseDrops(dropsList, rewardManager.isFixedDrops));
        }
        else {
            reward.setDrops(ecoDrop.parseDrops(rewardConfig.getString("Drops"), rewardManager.isFixedDrops));
        }
        reward.setCoinMax(rewardConfig.getDouble("Coin_Maximum", 0));
        reward.setCoinMin(rewardConfig.getDouble("Coin_Minimum", 0));
        reward.setCoinPercentage(rewardConfig.getDouble("Coin_Percent", 0));
        String expMin = rewardConfig.getString("ExpMin");
        String expMax = rewardConfig.getString("ExpMax");
        String expPercentage = rewardConfig.getString("ExpPercent");
        if (expMin != null && expMax != null && expPercentage != null) {
            try {
                reward.setExpMin(Integer.parseInt(expMin));
                reward.setExpMax(Integer.parseInt(expMax));
                reward.setExpPercentage(Double.parseDouble(expPercentage));
            }
            catch (NumberFormatException e) {
                ecoCreature.getEcoLogger().warning("Could not parse exp for " + rewardConfig.getName());
            }
        }

        reward.setNoRewardMessage(new ecoMessage(convertMessage(rewardConfig.getString("NoReward_Message", ecoMessageManager.NO_REWARD_MESSAGE)), isNoRewardMessage));
        reward.setRewardMessage(new ecoMessage(convertMessage(rewardConfig.getString("Reward_Message", ecoMessageManager.REWARD_MESSAGE)), true));
        reward.setPenaltyMessage(new ecoMessage(convertMessage(rewardConfig.getString("Penalty_Message", ecoMessageManager.PENALTY_MESSAGE)), true));

        return reward;
    }

    private static String convertMessage(String message)
    {
        if (message != null) {
            return message.replaceAll("&&", "\b").replaceAll("&", "§").replaceAll("\b", "&");
        }

        return null;
    }

}