/*
 * This file is part of ecoCreature.
 *
 * Copyright (c) 2011-2012, R. Ramos <http://github.com/mung3r/>
 * ecoCreature is licensed under the GNU Lesser General Public License.
 *
 * ecoCreature is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ecoCreature is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.crafted.chrisb.ecoCreature.rewards.gain;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import se.crafted.chrisb.ecoCreature.commons.DependencyUtils;
import se.crafted.chrisb.ecoCreature.commons.ECLogger;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.struct.Relation;

public class FactionsGain extends AbstractPlayerGain
{
    private Map<Relation, Double> multipliers;

    public FactionsGain(Map<Relation, Double> multipliers)
    {
        this.multipliers = multipliers;
    }

    @Override
    public double getMultiplier(Player player)
    {
        double multiplier = 1.0;

        if (DependencyUtils.hasPermission(player, "gain.factions") && DependencyUtils.hasFactions()) {
            FPlayer fPlayer = FPlayers.i.get(player);
            if (fPlayer != null && multipliers.containsKey(fPlayer.getRelationToLocation())) {
                multiplier = multipliers.get(fPlayer.getRelationToLocation());
                ECLogger.getInstance().debug(this.getClass(), "Factions multiplier: " + multiplier);
            }
        }

        return multiplier;
    }

    public static Set<PlayerGain> parseConfig(ConfigurationSection config)
    {
        Set<PlayerGain> gain = Collections.emptySet();

        if (config != null && DependencyUtils.hasFactions()) {
            Map<Relation, Double> multipliers = new HashMap<Relation, Double>();
            for (String relation : config.getKeys(false)) {
                try {
                    multipliers.put(Relation.valueOf(relation), Double.valueOf(config.getConfigurationSection(relation).getDouble("Amount", 1.0D)));
                }
                catch (IllegalArgumentException e) {
                    ECLogger.getInstance().warning("Unrecognized Factions relation: " + relation);
                }
            }
            gain = new HashSet<PlayerGain>();
            gain.add(new FactionsGain(multipliers));
        }

        return gain;
    }
}
