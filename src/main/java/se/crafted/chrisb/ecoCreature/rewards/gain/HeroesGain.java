package se.crafted.chrisb.ecoCreature.rewards.gain;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import se.crafted.chrisb.ecoCreature.commons.DependencyUtils;
import se.crafted.chrisb.ecoCreature.commons.ECLogger;

public class HeroesGain extends BasicGain
{
    private double multiplier;

    public HeroesGain(double multiplier, boolean isShared)
    {
        this.multiplier = multiplier;
        this.isShared = isShared;
    }

    @Override
    public double getMultiplier(Player player)
    {
        if (DependencyUtils.hasPermission(player, "gain.heroes") && DependencyUtils.hasHeroes() && DependencyUtils.getHeroes().getCharacterManager().getHero(player).hasParty()) {
            ECLogger.getInstance().debug("Heroes multiplier: " + multiplier);
            return multiplier;
        }

        return super.getMultiplier(player);
    }

    public static Gain parseConfig(ConfigurationSection config)
    {
        Gain gain = null;

        if (config != null) {
            gain = new HeroesGain(config.getDouble("Amount", 1.0D), config.getBoolean("Share", true));
        }

        return gain;
    }
}
