/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011  GoldenKevin
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

package argonms.map.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import argonms.character.Player;
import argonms.character.inventory.InventorySlot;
import argonms.loading.mob.MobStats;
import argonms.loading.mob.Skill;
import argonms.loading.skill.MobSkillEffect;
import argonms.map.MapEntity;
import argonms.net.client.CommonPackets;
import java.lang.ref.WeakReference;

/**
 *
 * @author GoldenKevin
 */
public class Mob extends MapEntity {
	private MobStats stats;
	private int remHp;
	private int remMp;
	private List<MobDeathHook> hooks;
	private Player highestDamageKiller;
	private WeakReference<Player> controller;
	private boolean aggroAware, hasAggro;

	public Mob(MobStats stats) {
		this.stats = stats;
		this.remHp = stats.getMaxHp();
		this.remMp = stats.getMaxMp();
		this.hooks = new ArrayList<MobDeathHook>();
		this.controller = new WeakReference<Player>(null);
		setStance((byte) 5);
	}

	public int getMobId() {
		return stats.getMobId();
	}

	public List<ItemDrop> getDrops() {
		List<InventorySlot> items = stats.getItemsToDrop();
		List<ItemDrop> combined = new ArrayList<ItemDrop>(items.size() + 1);
		for (InventorySlot item : items)
			combined.add(new ItemDrop(item));
		combined.add(new ItemDrop(stats.getMesosToDrop()));
		Collections.shuffle(combined);
		return combined;
	}

	public boolean isMobile() {
		return stats.getDelays().containsKey("move") || stats.getDelays().containsKey("fly");
	}

	public void addDeathHook(MobDeathHook hook) {
		hooks.add(hook);
	}

	public void died() {
		for (MobDeathHook hook : hooks)
			hook.monsterKilled(highestDamageKiller);
	}

	public Player getController() {
		return controller.get();
	}

	public void setController(Player newController) {
		this.controller = new WeakReference<Player>(newController);
	}

	public boolean isFirstAttack() {
		return stats.isFirstAttack();
	}

	public int getHp() {
		return remHp;
	}

	public int getMp() {
		return remMp;
	}

	public List<Skill> getSkills() {
		return stats.getSkills();
	}

	public boolean canUseSkill(MobSkillEffect effect) {
		if (effect.getHp() < (remHp / stats.getMaxHp() * 100))
			return false;
		return true;
	}

	public boolean hasSkill(short skillId, byte skillLevel) {
		for (Skill s : stats.getSkills())
			if (s.getSkill() == skillId && s.getLevel() == skillLevel)
				return true;
		return false;
	}

	//TODO: implement
	public void applyEffect(MobSkillEffect playerSkillEffect, Player player, boolean b) {
		
	}

	//TODO: implement
	public boolean wasAttackedBy(Player player) {
		return false;
	}

	public boolean controllerHasAggro() {
		return hasAggro;
	}

	public void setControllerHasAggro(boolean controllerHasAggro) {
		this.hasAggro = controllerHasAggro;
	}

	public boolean controllerKnowsAboutAggro() {
		return aggroAware;
	}

	public void setControllerKnowsAboutAggro(boolean aware) {
		this.aggroAware = aware;
	}

	public MapEntityType getEntityType() {
		return MapEntityType.MONSTER;
	}

	public boolean isAlive() {
		return remHp > 0;
	}

	public boolean isVisible() {
		return true;
	}

	public byte[] getCreationMessage() {
		return CommonPackets.writeShowMonster(this, true, (byte) 0);
	}

	public byte[] getShowEntityMessage() {
		return CommonPackets.writeShowMonster(this, false, (byte) 0);
	}

	public byte[] getOutOfViewMessage() {
		return CommonPackets.writeRemoveMonster(this, (byte) 0);
	}

	public byte[] getDestructionMessage() {
		return CommonPackets.writeRemoveMonster(this, (byte) 1);
	}

	public boolean isNonRangedType() {
		return false;
	}

	public interface MobDeathHook {
		public void monsterKilled(Player highestDamageChar);
	}
}
