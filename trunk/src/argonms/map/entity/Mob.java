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

import argonms.character.Party;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import argonms.character.Player;
import argonms.character.inventory.InventorySlot;
import argonms.character.skill.PlayerStatusEffectValues.PlayerStatusEffect;
import argonms.game.GameServer;
import argonms.loading.mob.MobDataLoader;
import argonms.loading.mob.MobStats;
import argonms.loading.mob.Skill;
import argonms.loading.skill.MobSkillEffectsData;
import argonms.map.GameMap;
import argonms.map.MapEntity;
import argonms.map.MobSkills;
import argonms.map.MonsterStatusEffect;
import argonms.net.client.CommonPackets;
import argonms.tools.Rng;
import argonms.tools.Timer;
import argonms.tools.collections.LockableMap;

import java.awt.Point;
import java.awt.Rectangle;
import java.lang.ref.WeakReference;
import java.util.Random;
import java.util.WeakHashMap;

/**
 *
 * @author GoldenKevin
 */
public class Mob extends MapEntity {
	private final MobStats stats;
	private final GameMap map;
	private int remHp;
	private int remMp;
	private final List<MobDeathHook> hooks;
	private final LockableMap<Player, PlayerDamage> damages;
	private WeakReference<Player> controller;
	private boolean aggroAware, hasAggro;
	private final Set<MonsterStatusEffect> buffs;
	private final Map<MonsterStatusEffect, ScheduledFuture<?>> skillCancels;
	private int spawnedSummons;
	private byte spawnEffect;

	public Mob(MobStats stats, GameMap map) {
		this.stats = stats;
		this.map = map;
		this.remHp = stats.getMaxHp();
		this.remMp = stats.getMaxMp();
		this.hooks = new ArrayList<MobDeathHook>();
		this.damages = new LockableMap<Player, PlayerDamage>(new WeakHashMap<Player, PlayerDamage>());
		this.controller = new WeakReference<Player>(null);
		this.buffs = EnumSet.noneOf(MonsterStatusEffect.class);
		this.skillCancels = new EnumMap<MonsterStatusEffect, ScheduledFuture<?>>(MonsterStatusEffect.class);
		setStance((byte) 5);
	}

	public int getMobId() {
		return stats.getMobId();
	}

	public boolean isMobile() {
		//TODO: fix for MCDB
		return stats.getDelays().containsKey("move") || stats.getDelays().containsKey("fly");
	}

	private List<ItemDrop> getDrops() {
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

	private Point calcDropPos(Point initial, Point fallback) {
		Point ret = map.calcPointBelow(new Point(initial.x, initial.y - 99));
		return ret != null ? ret : fallback;
	}

	private void makeDrops(final int owner, final byte pickupAllow) {
		Timer.getInstance().runAfterDelay(new Runnable() {
			public void run() {
				Point pos = getPosition();
				int mobX = pos.x;
				int dropNum = 1;
				for (ItemDrop drop : getDrops()) {
					if (pickupAllow == ItemDrop.PICKUP_EXPLOSION)
						pos.x = mobX + (dropNum % 2 == 0 ? (40 * (dropNum / 2)) : -(40 * (dropNum / 2)));
					else
						pos.x = mobX + (dropNum % 2 == 0 ? (25 * (dropNum / 2)) : -(25 * (dropNum / 2)));
					drop.init(owner, calcDropPos(pos, getPosition()), getPosition(), getId(), pickupAllow);

					map.drop(drop);
					dropNum++;
				}
			}
		}, getAnimationTime("die1")); //artificial drop lag...
	}

	private Player giveExp(Player killer) {
		Player highestDamageAttacker = null;
		long highestDamage = 0;

		Map<Party, PartyExp> parties = new HashMap<Party, PartyExp>();
		Player attacker;
		long damage;
		short attackerLevel;
		Party attackerParty;
		PartyExp partyExp;
		for (Entry<Player, PlayerDamage> pd : damages.entrySet()) {
			attacker = pd.getKey();
			damage = pd.getValue().getDamage();
			if (damage > highestDamage) {
				highestDamageAttacker = attacker;
				highestDamage = damage;
			}
			if (attacker == null || attacker.getMapId() != map.getMapId() || !attacker.isAlive())
				continue;

			attackerLevel = attacker.getLevel();
			attackerParty = attacker.getParty();

			int exp = (int) (stats.getExp() * ((8 * damage / stats.getMaxHp()) + (attacker == killer ? 2 : 0)) / 10);
			if (attackerParty != null) {
				partyExp = parties.get(attackerParty);
				if (partyExp == null) {
					partyExp = new PartyExp();
					parties.put(attackerParty, partyExp);
				}
				partyExp.compareAndSetMinAttackerLevel(attackerLevel);
				partyExp.compareAndSetMaxDamage(damage, attacker);
			} else {
				int hsRate = attacker.isEffectActive(PlayerStatusEffect.HOLY_SYMBOL) ?
						attacker.getEffectValue(PlayerStatusEffect.HOLY_SYMBOL).getX() : 0;
				//exp = exp * getTauntEffect() / 100;
				exp *= GameServer.getVariables().getExpRate();
				exp += ((exp * hsRate) / 100);
				attacker.gainExp(exp, attacker == killer, false);
			}
		}
		if (!parties.isEmpty()) {
			List<Player> members;
			for (Entry<Party, PartyExp> entry : parties.entrySet()) {
				attackerParty = entry.getKey();
				partyExp = entry.getValue();
				members = attackerParty.getMembersInMap(map.getMapId());
				short totalLevel = 0;
				for (Player member : members) {
					attackerLevel = member.getLevel();
					if (attackerLevel >= (partyExp.getMinAttackerLevel() - 5) || attackerLevel >= (stats.getLevel() - 5))
						totalLevel += attackerLevel;
				}
				for (Player member : members) {
					attackerLevel = member.getLevel();
					if (attackerLevel >= (partyExp.getMinAttackerLevel() - 5) || attackerLevel >= (stats.getLevel() - 5)) {
						int exp = (int) (stats.getExp() * ((8 * attackerLevel / totalLevel) + (member == partyExp.getHighestDamagePlayer() ? 2 : 0)) / 10);
						int hsRate = member.isEffectActive(PlayerStatusEffect.HOLY_SYMBOL) ?
								member.getEffectValue(PlayerStatusEffect.HOLY_SYMBOL).getX() : 0;
						//exp = exp * getTauntEffect() / 100;
						exp *= GameServer.getVariables().getExpRate();
						exp += ((exp * hsRate) / 100);
						member.gainExp(exp, member == killer, false);
					}
				}
			}
		}
		return highestDamageAttacker;
	}

	public void died(Player killer) {
		for (ScheduledFuture<?> cancelTask : skillCancels.values())
			cancelTask.cancel(true);
		for (MobDeathHook hook : hooks)
			hook.monsterKilled(killer);
		if (stats.getBuffToGive() > 0) {
			//TODO, give buff and show message to map
		}
		Player highestDamage = giveExp(killer);
		int id;
		byte pickupAllow;
		if (highestDamage.getParty() != null) {
			id = highestDamage.getParty().getId();
			pickupAllow = ItemDrop.PICKUP_ALLOW_PARTY;
		} else {
			id = highestDamage.getId();
			pickupAllow = ItemDrop.PICKUP_ALLOW_OWNER;
		}
		makeDrops(id, pickupAllow);
	}

	public void addDeathHook(MobDeathHook hook) {
		hooks.add(hook);
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
		if (damage > remHp)
			damage = remHp;
		this.remHp -= damage;
		PlayerDamage pd = damages.getWhenSafe(p);
		if (pd != null)
			pd.addDamage(damage);
		else
			damages.putWhenSafe(p, new PlayerDamage(damage));
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

	private void applyEffect(final MonsterStatusEffect b, int duration) {
		//TODO: IMPLEMENT
		synchronized (buffs) {
			buffs.add(b);
		}
		synchronized (skillCancels) {
			skillCancels.put(b, Timer.getInstance().runAfterDelay(new Runnable() {
				public void run() {
					dispelEffect(b);
				}
			}, duration));
		}
	}

	private void dispelEffect(MonsterStatusEffect b) {
		synchronized (buffs) {
			buffs.remove(b);
		}
		synchronized (skillCancels) {
			skillCancels.remove(b).cancel(true);
		}
	}

	public boolean isEffectActive(MonsterStatusEffect b) {
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
				if (playerSkillEffect.shouldPerform() && !isEffectActive(MonsterStatusEffect.MAGIC_IMMUNITY))
					applyEffect(MonsterStatusEffect.WEAPON_IMMUNITY, playerSkillEffect.getDuration());
				break;
			case MobSkills.MAGIC_IMMUNITY:
				if (playerSkillEffect.shouldPerform() && !isEffectActive(MonsterStatusEffect.WEAPON_IMMUNITY))
					applyEffect(MonsterStatusEffect.MAGIC_IMMUNITY, playerSkillEffect.getDuration());
				break;
			case MobSkills.SUMMON:
				short limit = playerSkillEffect.getSummonLimit();
				if (limit == 5000)
					limit = (short) (30 + player.getMap().getPlayerCount() * 2);
				if (spawnedSummons < limit) {
					Random generator = Rng.getGenerator();
					for (Integer oMobId : playerSkillEffect.getSummons().values()) {
						int mobId = oMobId.intValue();
						Mob summon = new Mob(MobDataLoader.getInstance().getMobStats(mobId), map);
						int ypos, xpos;
						xpos = getPosition().x;
						ypos = getPosition().y;
						switch (mobId) {
							case 8500003: // Pap bomb high
								summon.setFoothold((short)Math.ceil(generator.nextDouble() * 19.0));
								ypos = -590; //no break?
							case 8500004: // Pap bomb
								//Spawn between -500 and 500 from the monsters X position
								xpos = (int)(getPosition().x + Math.ceil(generator.nextDouble() * 1000.0) - 500);
								if (ypos != -590)
									ypos = getPosition().y;
								break;
							case 8510100: //Pianus bomb
								if (Math.ceil(generator.nextDouble() * 5) == 1) {
									ypos = 78;
									xpos = (int)(0 + Math.ceil(generator.nextDouble() * 5)) + ((Math.ceil(generator.nextDouble() * 2) == 1) ? 180 : 0);
								} else
									xpos = (int)(getPosition().x + Math.ceil(generator.nextDouble() * 1000.0) - 500);
								break;
						}
						// Get spawn coordinates (This fixes monster lock)
						// TODO get map left and right wall. Any suggestions?
						switch (map.getMapId()) {
							case 220080001: //Pap map
								if (xpos < -890)
									xpos = (int)(-890 + Math.ceil(generator.nextDouble() * 150));
								else if (xpos > 230)
									xpos = (int)(230 - Math.ceil(generator.nextDouble() * 150));
								break;
							case 230040420: // Pianus map
								if (xpos < -239)
									xpos = (int)(-239 + Math.ceil(generator.nextDouble() * 150));
								else if (xpos > 371)
									xpos = (int)(371 - Math.ceil(generator.nextDouble() * 150));
								break;
						}
						summon.setPosition(new Point(xpos, ypos));
						summon.setSpawnEffect(playerSkillEffect.getSummonEffect());
						player.getMap().spawnMonster(summon);
						spawnedSummons++;
					}
				}
				break;
			default:
				if (!playerSkillEffect.getEffects().isEmpty())
					player.applyEffect(playerSkillEffect);
				if (playerSkillEffect.getBuff() != null)
					applyEffect(playerSkillEffect.getBuff(), playerSkillEffect.getDuration());
				break;
		}
	}

	public boolean wasAttackedBy(Player player) {
		return damages.containsKey(player);
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
		public void monsterKilled(Player killer);
	}

	private static class PlayerDamage {
		private long damage;

		public PlayerDamage(int initialDamage) {
			this.damage = initialDamage;
		}

		public void addDamage(int gain) {
			damage += gain;
		}

		public long getDamage() {
			return damage;
		}
	}

	private static class PartyExp {
		private short minAttackerLevel;
		private Player highestDamagePlayer;
		private long highestDamage;

		public PartyExp() {
			minAttackerLevel = 200;
		}

		public void compareAndSetMinAttackerLevel(short damagerlevel) {
			if (damagerlevel < minAttackerLevel)
				minAttackerLevel = damagerlevel;
		}

		public void compareAndSetMaxDamage(long damage, Player attacker) {
			if (damage > highestDamage) {
				highestDamagePlayer = attacker;
				highestDamage = damage;
			}
		}

		public short getMinAttackerLevel() {
			return minAttackerLevel;
		}

		public Player getHighestDamagePlayer() {
			return highestDamagePlayer;
		}
	}
}
