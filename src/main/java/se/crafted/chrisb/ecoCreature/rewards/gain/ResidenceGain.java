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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import se.crafted.chrisb.ecoCreature.commons.DependencyUtils;
import se.crafted.chrisb.ecoCreature.commons.LoggerUtil;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;

public class ResidenceGain extends AbstractPlayerGain<String>
{
    public ResidenceGain(Map<String, Double> multipliers)
    {
        super(multipliers);
    }

    @Override
    public boolean hasPermission(Player player)
    {
        return DependencyUtils.hasPermission(player, "gain.residence") && DependencyUtils.hasResidence();
    }

    @Override
    public double getMultiplier(Player player)
    {
        double multiplier = NO_GAIN;

        ClaimedResidence residence = Residence.getResidenceManager().getByLoc(player.getLocation());
        if (residence != null && getMultipliers().containsKey(residence.getName())) {
            multiplier = getMultipliers().get(residence.getName());
            LoggerUtil.getInstance().debug(this.getClass(), "Residence multiplier: " + multiplier);
        }

        return multiplier;
    }

    public static Set<PlayerGain> parseConfig(ConfigurationSection config)
    {
        Set<PlayerGain> gain = Collections.emptySet();

        if (config != null) {
            gain = new HashSet<PlayerGain>();
            gain.add(new ResidenceGain(parseMultipliers(config)));
        }

        return gain;
    }
}
