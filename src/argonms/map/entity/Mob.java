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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import argonms.character.Player;
import argonms.character.inventory.InventorySlot;
import argonms.loading.mob.MobDataLoader;
import argonms.loading.mob.MobStats;
import argonms.loading.mob.Skill;
import argonms.loading.skill.MobSkillEffectsData;
import argonms.map.MapEntity;
import argonms.map.MobSkills;
import argonms.map.MonsterStatusEffect;
import argonms.net.client.CommonPackets;
import argonms.tools.Timer;

import java.awt.Point;
import java.awt.Rectangle;
import java.lang.ref.WeakReference;

/**
 *
 * @author GoldenKevin
 */
public class Mob extends MapEntity {
	private final MobStats stats;
	private int remHp;
	private int remMp;
	private final List<MobDeathHook> hooks;
	private WeakReference<Player> highestDamageKiller;
	private WeakReference<Player> controller;
	private boolean aggroAware, hasAggro;
	private final Set<MonsterStatusEffect> buffs;
	private final Map<MonsterStatusEffect, ScheduledFuture<?>> skillCancels;
	private int spawnedSummons;
	private byte spawnEffect;

	public Mob(MobStats stats) {
		this.stats = stats;
		this.remHp = stats.getMaxHp();
		this.remMp = stats.getMaxMp();
		this.hooks = new ArrayList<MobDeathHook>();
		this.highestDamageKiller = new WeakReference<Player>(null);
		this.controller = new WeakReference<Player>(null);
		this.buffs = EnumSet.noneOf(MonsterStatusEffect.class);
		this.skillCancels = new EnumMap<MonsterStatusEffect, ScheduledFuture<?>>(MonsterStatusEffect.class);
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
		int dropMesos = stats.getMesosToDrop();
		if (dropMesos != 0)
			combined.add(new ItemDrop(dropMesos));
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
		for (ScheduledFuture<?> cancelTask : skillCancels.values())
			cancelTask.cancel(true);
		for (MobDeathHook hook : hooks)
			hook.monsterKilled(highestDamageKiller.get());
		if (stats.getBuffToGive() > 0) {
			//TODO, give buff and show message to map
		}
	}

	public int getHighestDamageAttacker() {
		return highestDamageKiller.get() != null ? highestDamageKiller.get().getId() : 0;
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

	public int getMaxHp() {
		return stats.getMaxHp();
	}

	public int getMp() {
		return remMp;
	}

	public int getMaxMp() {
		return stats.getMaxMp();
	}

	public void hurt(Player p, int damage) {
		this.remHp -= damage;
		//TODO: calculate highest damage attacker
		this.highestDamageKiller = new WeakReference<Player>(p);
	}

	public List<Skill> getSkills() {
		return stats.getSkills();
	}

	public boolean canUseSkill(MobSkillEffectsData effect) {
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

	private void applyBuff(final MonsterStatusEffect b, int duration) {
		//TODO: IMPLEMENT
		synchronized (buffs) {
			buffs.add(b);
		}
		synchronized (skillCancels) {
			skillCancels.put(b, Timer.getInstance().runAfterDelay(new Runnable() {
				public void run() {
					dispelBuff(b);
				}
			}, duration));
		}
	}

	private void dispelBuff(MonsterStatusEffect b) {
		synchronized (buffs) {
			buffs.remove(b);
		}
		synchronized (skillCancels) {
			skillCancels.remove(b).cancel(true);
		}
	}

	public boolean isBuffActive(MonsterStatusEffect b) {
		return buffs.contains(b);
	}

	public void setSpawnEffect(byte effect) {
		this.spawnEffect = effect;
	}

	private Rectangle calculateBoundingBox(Point posFrom, Point lt, Point rb, boolean facingLeft) {
		int ltx, lty, rbx, rby;
		if (facingLeft) {
			ltx = lt.x + posFrom.x;
			lty = lt.y + posFrom.y;
			rbx = rb.x + posFrom.x;
			rby = rb.y + posFrom.y;
		} else {
			ltx = rb.x * -1 + posFrom.x;
			lty = lt.y + posFrom.y;
			rbx = lt.x * -1 + posFrom.x;
			rby = rb.y + posFrom.y;
		}
		return new Rectangle(ltx, lty, rbx - ltx, rby - lty);
	}

	public void applyEffect(MobSkillEffectsData playerSkillEffect, Player player, boolean b) {
		switch (playerSkillEffect.getDataId()) {
			//TODO: IMPLEMENT HEAL, DISPEL, SEDUCE, BANISH
			/*case MobSkills.HEAL:
				heal = true;
				break;
			case MobSkills.DISPEL:
				dispel = true;
				break;
			case MobSkills.SEDUCE: // Seduce
				seduce = true;
				break;
			case MobSkills.BANISH: // Banish
				banish = true;
				break;*/
			case MobSkills.MIST: // Mist
				Rectangle bounds = calculateBoundingBox(getPosition(), playerSkillEffect.getLt(), playerSkillEffect.getRb(), true);
				Mist mist = new Mist(bounds, this, playerSkillEffect);
				player.getMap().spawnMist(mist, playerSkillEffect.getX() * 10);
				break;
			case MobSkills.PHYSICAL_IMMUNITY:
				if (playerSkillEffect.shouldPerform() && !isBuffActive(MonsterStatusEffect.MAGIC_IMMUNITY))
					applyBuff(MonsterStatusEffect.WEAPON_IMMUNITY, playerSkillEffect.getDuration());
				break;
			case MobSkills.MAGIC_IMMUNITY:
				if (playerSkillEffect.shouldPerform() && !isBuffActive(MonsterStatusEffect.WEAPON_IMMUNITY))
					applyBuff(MonsterStatusEffect.MAGIC_IMMUNITY, playerSkillEffect.getDuration());
				break;
			case MobSkills.SUMMON: //TODO: use playerSkillEffect.getSummonLimit()?
				short limit = playerSkillEffect.getSummonLimit();
				if (limit == 5000)
					limit = (short) (30 + player.getMap().getPlayerCount() * 2);
				if (spawnedSummons < limit) {
					for (Integer oMobId : playerSkillEffect.getSummons().values()) {
						int mobId = oMobId.intValue();
						Mob summon = new Mob(MobDataLoader.getInstance().getMobStats(mobId));
						int ypos, xpos;
						xpos = getPosition().x;
						ypos = getPosition().y;
						switch (mobId) {
							case 8500003: // Pap bomb high
								summon.setFoothold((short)Math.ceil(Math.random() * 19.0));
								ypos = -590; //no break?
							case 8500004: // Pap bomb
								//Spawn between -500 and 500 from the monsters X position
								xpos = (int)(getPosition().x + Math.ceil(Math.random() * 1000.0) - 500);
								if (ypos != -590)
									ypos = getPosition().y;
								break;
							case 8510100: //Pianus bomb
								if (Math.ceil(Math.random() * 5) == 1) {
									ypos = 78;
									xpos = (int)(0 + Math.ceil(Math.random() * 5)) + ((Math.ceil(Math.random() * 2) == 1) ? 180 : 0);
								} else
									xpos = (int)(getPosition().x + Math.ceil(Math.random() * 1000.0) - 500);
								break;
						}
						// Get spawn coordinates (This fixes monster lock)
						// TODO get map left and right wall. Any suggestions?
						switch (player.getMapId()) {
							case 220080001: //Pap map
								if (xpos < -890)
									xpos = (int)(-890 + Math.ceil(Math.random() * 150));
								else if (xpos > 230)
									xpos = (int)(230 - Math.ceil(Math.random() * 150));
								break;
							case 230040420: // Pianus map
								if (xpos < -239)
									xpos = (int)(-239 + Math.ceil(Math.random() * 150));
								else if (xpos > 371)
									xpos = (int)(371 - Math.ceil(Math.random() * 150));
								break;
						}
						summon.setPosition(new Point(xpos, ypos));
						summon.setSpawnEffect(playerSkillEffect.getSummonEffect());
						player.getMap().spawnMonster(summon);
					}
				}
				break;
			default:
				if (playerSkillEffect.getDisease() != null)
					player.applyDisease(playerSkillEffect);
				if (playerSkillEffect.getBuff() != null)
					applyBuff(playerSkillEffect.getBuff(), playerSkillEffect.getDuration());
				break;
		}
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

	public int getAnimationTime(String name) {
		Integer time = stats.getDelays().get(name);
		return time != null ? time.intValue() : 0;
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
		return CommonPackets.writeShowMonster(this, true, spawnEffect);
	}

	public byte[] getShowEntityMessage() {
		return CommonPackets.writeShowMonster(this, false, spawnEffect);
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
