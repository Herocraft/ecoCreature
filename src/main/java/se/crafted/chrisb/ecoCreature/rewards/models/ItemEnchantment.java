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
package se.crafted.chrisb.ecoCreature.rewards.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.enchantments.Enchantment;

public class ItemEnchantment
{
    private final Random random = new Random();

    private Enchantment enchantment;
    private int minLevel;
    private int maxLevel;

    public Enchantment getEnchantment()
    {
        return enchantment;
    }

    public void setEnchantment(Enchantment enchantment)
    {
        this.enchantment = enchantment;
    }

    public int getMinLevel()
    {
        return minLevel;
    }

    public void setMinLevel(int minLevel)
    {
        this.minLevel = minLevel;
    }

    public int getMaxLevel()
    {
        return maxLevel;
    }

    public void setMaxLevel(int maxLevel)
    {
        this.maxLevel = maxLevel;
    }

    public int getLevel()
    {
        return minLevel + random.nextInt(Math.abs(maxLevel - minLevel + 1));
    }

    public static Map<Enchantment, Integer> getOutcome(Set<ItemEnchantment> enchantments)
    {
        Map<Enchantment, Integer> enchantmentMap = new HashMap<Enchantment, Integer>();

        for (ItemEnchantment enchantment : enchantments) {
            int level = enchantment.getLevel();
            if (level > 0) {
                enchantmentMap.put(enchantment.getEnchantment(), Integer.valueOf(level));
            }
        }

        return enchantmentMap;
    }
}
