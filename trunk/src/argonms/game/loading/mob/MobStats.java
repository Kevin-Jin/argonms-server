/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2012  GoldenKevin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package argonms.game.loading.mob;

import argonms.common.character.inventory.Equip;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventorySlot.ItemType;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.util.Rng;
import argonms.game.GameServer;
import argonms.game.field.Element;
import argonms.game.field.entity.Mob;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

/**
 *
 * @author GoldenKevin
 */
public class MobStats {
	private final int mobid;
	private final Map<Element, Byte> elemAttr;
	private final Map<Integer, Byte> loseItems;
	private final List<Integer> summons;
	private final Map<Byte, Attack> attacks;
	private final List<Skill> skills;
	private final Map<String, Integer> delays;
	private final List<ItemDropEntry> itemDrops;
	private short level;
	private int maxHp;
	private int maxMp;
	private int pad;
	private int exp;
	private boolean undead;
	private int removeAfter;
	private byte deathAnimation;
	private boolean hideHp;
	private boolean hideName;
	private byte hpTagColor;
	private byte hpTagBgColor;
	private boolean boss;
	private int sd;
	private boolean invincible;
	private boolean firstAttack;
	private int buff;
	private MesoDropChance mesoDrop;
	private byte dropItemPeriod;

	protected MobStats(int mobid) {
		this.mobid = mobid;
		this.elemAttr = new EnumMap<Element, Byte>(Element.class);
		this.loseItems = new HashMap<Integer, Byte>();
		this.summons = new ArrayList<Integer>();
		this.attacks = new HashMap<Byte, Attack>();
		this.skills = new ArrayList<Skill>();
		this.delays = new HashMap<String, Integer>();
		this.itemDrops = new ArrayList<ItemDropEntry>();
		this.removeAfter = -1;
		this.deathAnimation = Mob.DESTROY_ANIMATION_NORMAL;
	}

	protected void setLevel(short level) {
		this.level = level;
	}

	protected void setMaxHp(int value) {
		this.maxHp = value;
	}

	protected void setMaxMp(int value) {
		this.maxMp = value;
	}

	protected void setPhysicalDamage(int pad) {
		this.pad = pad;
	}

	protected void setExp(int exp) {
		this.exp = exp;
	}

	protected void setUndead() {
		this.undead = true;
	}

	protected void setElementalAttribute(String attr) {
		for (int i = 0; i < attr.length(); i += 2) {
			Element e = Element.valueOf(attr.charAt(i));
			elemAttr.put(e, Byte.valueOf((byte) (attr.charAt(i + 1) - '0')));
		}
	}

	protected void setRemoveAfter(int time) {
		this.removeAfter = time;
	}

	protected void setDestroyAnimation(byte animation) {
		this.deathAnimation = animation;
	}

	protected void setHideHp() {
		this.hideHp = true;
	}

	protected void setHideName() {
		this.hideName = true;
	}

	protected void setHpTagColor(byte color) {
		this.hpTagColor = color;
	}

	protected void setHpTagBgColor(byte color) {
		this.hpTagBgColor = color;
	}

	protected void setBoss() {
		this.boss = true;
	}

	protected void setSelfDestructHp(int hp) {
		this.sd = hp;
	}

	protected void addLoseItem(int itemid, byte prob) {
		this.loseItems.put(Integer.valueOf(itemid), Byte.valueOf(prob));
	}

	protected void setInvincible() {
		this.invincible = true;
	}

	protected void addSummon(int mobid) {
		this.summons.add(Integer.valueOf(mobid));
	}

	protected void setFirstAttack() {
		this.firstAttack = true;
	}

	protected void addAttack(byte attackid, Attack attack) {
		this.attacks.put(Byte.valueOf(attackid), attack);
	}

	protected void addSkill(Skill skill) {
		this.skills.add(skill);
	}

	protected void setBuffToGive(int buffid) {
		this.buff = buffid;
	}

	protected void addDelay(String name, int delay) {
		this.delays.put(name, Integer.valueOf(delay));
	}

	protected void addItemDrop(int itemid, int chance, short min, short max) {
		this.itemDrops.add(new ItemDropEntry(itemid, chance, min, max));
	}

	protected void setMesoDrop(int chance, int min, int max) {
		this.mesoDrop = new MesoDropChance(chance, min, max);
	}

	protected void setDropItemPeriod(byte period) {
		dropItemPeriod = period;
	}

	public int getMobId() {
		return mobid;
	}

	public short getLevel() {
		return level;
	}

	public int getMaxHp() {
		return maxHp;
	}

	public int getMaxMp() {
		return maxMp;
	}

	public int getPhysicalDamage() {
		return pad;
	}

	public int getExp() {
		return exp;
	}

	public boolean isUndead() {
		return undead;
	}

	public byte getElementalResistance(Element elem) {
		Byte res = elemAttr.get(elem);
		return res != null ? res.byteValue() : 0;
	}

	public int getRemoveAfter() {
		return removeAfter;
	}

	public byte getDeathAnimation() {
		return deathAnimation;
	}

	public boolean isHpHidden() {
		return hideHp;
	}

	public boolean isNameHidden() {
		return hideName;
	}

	public byte getHpTagColor() {
		return hpTagColor;
	}

	public byte getHpTagBgColor() {
		return hpTagBgColor;
	}

	public boolean isBoss() {
		return boss;
	}

	public int getSelfDestructHp() {
		return sd;
	}

	public List<Integer> getItemsToTake() {
		List<Integer> list = new ArrayList<Integer>();
		Random r = Rng.getGenerator();
		for (Entry<Integer, Byte> entry : loseItems.entrySet())
			if (r.nextInt(100) < entry.getValue().byteValue())
				list.add(entry.getKey());
		return list;
	}

	public boolean isInvincible() {
		return invincible;
	}

	public List<Integer> getSummons() {
		return Collections.unmodifiableList(summons);
	}

	public boolean isFirstAttack() {
		return firstAttack;
	}

	public Map<Byte, Attack> getAttacks() {
		return attacks;
	}

	public List<Skill> getSkills() {
		return Collections.unmodifiableList(skills);
	}

	public int getBuffToGive() {
		return buff;
	}

	public Map<String, Integer> getDelays() {
		return Collections.unmodifiableMap(delays);
	}

	/**
	 * Randomly select an amount of mesos for this monster to drop when killed,
	 * using the chances that have been given in setMesoDrop.
	 * @return the amount of mesos that this monster will drop. If this monster
	 * should not drop any mesos, 0 will be returned.
	 */
	public int getMesosToDrop() {
		Random generator = Rng.getGenerator();
		int multiplier = GameServer.getVariables().getMesoRate();
		if (mesoDrop == null) {
			//TODO: formula for meso drops when not using mcdb is way off
			//(almost every mob drops mesos, most are usually only +1)
			//perhaps we need a nextGaussian and cut off any remaining outliers
			//because this method can return +1 no matter what, unless the exp
			//is a HUGE number (Random.nextDouble() can be very very small)
			double factor = Math.pow(0.93, getExp() / 300.0);
			if (factor > 1.0)
				factor = 1.0;
			else if (factor < 0.001)
				factor = 0.005;
			return (int) Math.min((long) Math.min(30000, (int) (factor * getExp() * generator.nextDouble() * 2.1)) * multiplier, Integer.MAX_VALUE);
		} else {
			//TODO: should we multiply this by drop rate?
			if (generator.nextInt(1000000) < mesoDrop.getDropChance()) {
				int min = mesoDrop.getMinMesoDrop();
				int max = mesoDrop.getMaxMesoDrop();
				return (int) Math.min((long) (generator.nextInt(max - min + 1) + min) * multiplier, Integer.MAX_VALUE);
			}
			return 0;
		}
	}

	public List<InventorySlot> getItemsToDrop() {
		Random generator = Rng.getGenerator();
		List<InventorySlot> items = new ArrayList<InventorySlot>();
		int multiplier = GameServer.getVariables().getDropRate();
		for (ItemDropEntry entry : itemDrops) {
			if (generator.nextInt(1000000) < ((long) entry.getDropChance() * multiplier)) {
				InventorySlot item = InventoryTools.makeItemWithId(entry.getItemId());
				if (item.getType() == ItemType.EQUIP)
					InventoryTools.randomizeStats((Equip) item);
				if (entry.getMaxQuantity() != 1)
					item.setQuantity((short) (generator.nextInt(entry.getMaxQuantity() - entry.getMinQuantity() + 1) + entry.getMinQuantity()));
				items.add(item);
			}
		}
		return items;
	}

	public byte getDropItemPeriod() {
		return dropItemPeriod;
	}

	private static class MesoDropChance {
		private int chance;
		private int min;
		private int max;

		public MesoDropChance(int chance, int min, int max) {
			this.chance = chance;
			this.min = min;
			this.max = max;
		}

		public int getDropChance() {
			return chance;
		}

		public int getMinMesoDrop() {
			return min;
		}

		public int getMaxMesoDrop() {
			return max;
		}
	}

	private static class ItemDropEntry {
		private int itemId;
		private int chance;
		private short min;
		private short max;

		public ItemDropEntry(int itemId, int chance, short min, short max) {
			this.itemId = itemId;
			this.chance = chance;
			this.min = min;
			this.max = max;
		}

		public int getItemId() {
			return itemId;
		}

		public int getDropChance() {
			return chance;
		}

		public short getMinQuantity() {
			return min;
		}

		public short getMaxQuantity() {
			return max;
		}
	}
}
