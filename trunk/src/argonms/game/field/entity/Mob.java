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

package argonms.game.field.entity;

import argonms.common.GlobalConstants;
import argonms.common.character.PlayerStatusEffect;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.field.MonsterStatusEffect;
import argonms.common.loading.StatusEffectsData;
import argonms.common.net.external.ClientSendOps;
import argonms.common.util.Scheduler;
import argonms.common.util.collections.LockableMap;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.game.GameServer;
import argonms.game.character.GameCharacter;
import argonms.game.character.Party;
import argonms.game.character.StatusEffectTools;
import argonms.game.character.inventory.ItemTools;
import argonms.game.field.Element;
import argonms.game.field.GameMap;
import argonms.game.field.MapEntity;
import argonms.game.field.MonsterStatusEffectValues;
import argonms.game.loading.mob.MobStats;
import argonms.game.loading.mob.Skill;
import argonms.game.loading.skill.MobSkillEffectsData;
import argonms.game.net.external.GamePackets;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;

/**
 *
 * @author GoldenKevin
 */
public class Mob extends MapEntity {
	public static final byte
		DESTROY_ANIMATION_NONE = 0,
		DESTROY_ANIMATION_NORMAL = 1,
		DESTROY_ANIMATION_EXPLODE = 2
	;

	public static final byte
		CONTROL_STATUS_NORMAL = 1,
		CONTROL_STATUS_NONE = 5
	;

	private final MobStats stats;
	private final GameMap map;
	private int remHp;
	private int remMp;
	private final Queue<MobDeathHook> hooks;
	private final LockableMap<GameCharacter, PlayerDamage> damages;
	private WeakReference<GameCharacter> controller;
	private boolean aggroAware, hasAggro;
	private final Map<MonsterStatusEffect, MonsterStatusEffectValues> activeEffects;
	private final Map<Short, ScheduledFuture<?>> skillCancels;
	private final Map<Integer, ScheduledFuture<?>> diseaseCancels;
	private int spawnedSummons;
	private byte spawnEffect, deathEffect;
	private byte venomUseCount;

	public Mob(MobStats stats, GameMap map) {
		this.stats = stats;
		this.map = map;
		this.remHp = stats.getMaxHp();
		this.remMp = stats.getMaxMp();
		this.hooks = new ConcurrentLinkedQueue<MobDeathHook>();
		this.damages = new LockableMap<GameCharacter, PlayerDamage>(new WeakHashMap<GameCharacter, PlayerDamage>());
		this.controller = new WeakReference<GameCharacter>(null);
		this.activeEffects = new EnumMap<MonsterStatusEffect, MonsterStatusEffectValues>(MonsterStatusEffect.class);
		this.skillCancels = new HashMap<Short, ScheduledFuture<?>>();
		this.diseaseCancels = new HashMap<Integer, ScheduledFuture<?>>();
		this.deathEffect = DESTROY_ANIMATION_NORMAL;
		setStance((byte) 5);
	}

	public int getDataId() {
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

	private void makeDrops(final int owner, final byte pickupAllow) {
		Scheduler.getInstance().runAfterDelay(new Runnable() {
			@Override
			public void run() {
				map.drop(getDrops(), Mob.this, pickupAllow, owner);
			}
		}, getAnimationTime("die1")); //artificial drop lag...
	}

	private GameCharacter giveExp(GameCharacter killer) {
		GameCharacter highestDamageAttacker = null;
		long highestDamage = 0;

		Map<Party, PartyExp> parties = new HashMap<Party, PartyExp>();
		GameCharacter attacker;
		long damage;
		short attackerLevel;
		Party attackerParty;
		PartyExp partyExp;
		//damages shouldn't be accessed from here on out anyway since the mob is
		//already dead and cannot be attacked any more, but some lagger might
		//screw up our assertion...
		damages.lockRead();
		try {
			for (Entry<GameCharacter, PlayerDamage> pd : damages.entrySet()) {
				attacker = pd.getKey();
				damage = pd.getValue().getDamage();
				if (damage > highestDamage) {
					highestDamageAttacker = attacker;
					highestDamage = damage;
				}
				if (attacker == null || attacker.isClosed() || attacker.getMapId() != map.getDataId() || !attacker.isAlive())
					continue;

				attackerLevel = attacker.getLevel();
				attackerParty = attacker.getParty();

				long exp = (long) stats.getExp() * ((8 * damage / stats.getMaxHp()) + (attacker == killer ? 2 : 0)) / 10;
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
							attacker.getEffectValue(PlayerStatusEffect.HOLY_SYMBOL).getModifier() : 0;
					//exp = exp * getTauntEffect() / 100;
					exp *= GameServer.getVariables().getExpRate();
					exp += ((exp * hsRate) / 100);
					attacker.gainExp((int) Math.min(exp, Integer.MAX_VALUE), attacker == killer, false);
				}
			}
		} finally {
			damages.unlockRead();
		}
		if (!parties.isEmpty()) {
			List<GameCharacter> members;
			for (Entry<Party, PartyExp> entry : parties.entrySet()) {
				attackerParty = entry.getKey();
				partyExp = entry.getValue();
				members = attackerParty.getMembersInMap(map.getDataId());
				short totalLevel = 0;
				for (GameCharacter member : members) {
					attackerLevel = member.getLevel();
					if (attackerLevel >= (partyExp.getMinAttackerLevel() - 5) || attackerLevel >= (stats.getLevel() - 5))
						totalLevel += attackerLevel;
				}
				for (GameCharacter member : members) {
					attackerLevel = member.getLevel();
					if (attackerLevel >= (partyExp.getMinAttackerLevel() - 5) || attackerLevel >= (stats.getLevel() - 5)) {
						long exp = (long) stats.getExp() * ((8 * attackerLevel / totalLevel) + (member == partyExp.getHighestDamagePlayer() ? 2 : 0)) / 10;
						int hsRate = member.isEffectActive(PlayerStatusEffect.HOLY_SYMBOL) ?
								member.getEffectValue(PlayerStatusEffect.HOLY_SYMBOL).getModifier() : 0;
						//exp = exp * getTauntEffect() / 100;
						exp *= GameServer.getVariables().getExpRate();
						exp += ((exp * hsRate) / 100);
						member.gainExp((int) Math.min(exp, Integer.MAX_VALUE), member == killer, false);
					}
				}
			}
		}
		return highestDamageAttacker;
	}

	public void died(GameCharacter killer) {
		for (ScheduledFuture<?> cancelTask : skillCancels.values())
			cancelTask.cancel(true);
		for (ScheduledFuture<?> cancelTask : diseaseCancels.values())
			cancelTask.cancel(true);
		int deathBuff = stats.getBuffToGive();
		if (deathBuff > 0) {
			ItemTools.useItem(killer, deathBuff);
			killer.getClient().getSession().send(GamePackets.writeSelfVisualEffect(StatusEffectTools.MOB_BUFF, deathBuff, (byte) 1, (byte) -1));
			map.sendToAll(GamePackets.writeBuffMapVisualEffect(killer, StatusEffectTools.MOB_BUFF, deathBuff, (byte) 1, (byte) -1), killer);
		}
		GameCharacter highestDamage = giveExp(killer);
		for (MobDeathHook hook : hooks)
			hook.monsterKilled(highestDamage, killer);
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
		hooks.offer(hook);
	}

	public void executeDeathHooksNoRewards() {
		for (MobDeathHook hook : hooks)
			hook.monsterKilled(null, null);
	}

	public byte getControlStatus() {
		return getController() != null ? CONTROL_STATUS_NORMAL : CONTROL_STATUS_NONE;
	}

	public GameCharacter getController() {
		//for this WeakReference, there is no need to check for
		//GameCharacter.isClosed() since setController(null) is always called
		//when GameCharacter is closing (before GameCharacter.isClosed() can
		//become true).
		return controller.get();
	}

	public void setController(GameCharacter newController) {
		this.controller = new WeakReference<GameCharacter>(newController);
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

	public void hurt(GameCharacter p, int damage) {
		if (damage > remHp)
			damage = remHp;
		this.remHp -= damage;
		damages.lockWrite();
		try {
			PlayerDamage pd = damages.get(p);
			if (pd != null) {
				pd.addDamage(damage);
			} else {
				damages.put(p, new PlayerDamage(damage));
				for (MobDeathHook hook : p.getMobDeathHooks(getDataId()))
					hooks.offer(hook);
			}
		} finally {
			damages.unlockWrite();
		}

		//TODO: add friendly mob damage stuffs too (after stats.isBoss check)
		if (stats.getHpTagColor() > 0) //boss
			map.sendToAll(writeShowBossHp(stats, remHp));
		else if (stats.isBoss()) //minibosses
			map.sendToAll(writeShowMobHp(getId(), (byte) (remHp * 100 / stats.getMaxHp())));
		else
			p.getClient().getSession().send(writeShowMobHp(getId(), (byte) (remHp * 100 / stats.getMaxHp())));

		if (remHp > 0 && remHp <= stats.getSelfDestructHp()) {
			remHp = 0;
			deathEffect = DESTROY_ANIMATION_EXPLODE;
		}
	}

	public void loseMp(int loss) {
		this.remMp -= loss;
	}

	public List<Skill> getSkills() {
		return stats.getSkills();
	}

	public boolean canUseSkill(MobSkillEffectsData effect) {
		return ((remHp / stats.getMaxHp() * 100) <= effect.getMaxPercentHp());
	}

	public boolean hasSkill(short skillId, byte skillLevel) {
		for (Skill s : stats.getSkills())
			if (s.getSkill() == skillId && s.getLevel() == skillLevel)
				return true;
		return false;
	}

	public byte getElementalResistance(Element elem) {
		return stats.getElementalResistance(elem);
	}

	public byte getVenomCount() {
		return venomUseCount;
	}

	public void addToVenomCount() {
		venomUseCount++;
	}

	public void addToActiveEffects(MonsterStatusEffect buff, MonsterStatusEffectValues value) {
		synchronized (activeEffects) {
			activeEffects.put(buff, value);
		}
	}

	public void addCancelEffectTask(StatusEffectsData e, ScheduledFuture<?> cancelTask) {
		switch (e.getSourceType()) {
			case MOB_SKILL:
				synchronized (skillCancels) {
					skillCancels.put(Short.valueOf((short) e.getDataId()), cancelTask);
				}
				break;
			case PLAYER_SKILL:
				synchronized (diseaseCancels) {
					diseaseCancels.put(Integer.valueOf(e.getDataId()), cancelTask);
				}
				break;
		}
	}

	public Map<MonsterStatusEffect, MonsterStatusEffectValues> getAllEffects() {
		return activeEffects;
	}

	public MonsterStatusEffectValues getEffectValue(MonsterStatusEffect buff) {
		return activeEffects.get(buff);
	}

	public MonsterStatusEffectValues removeFromActiveEffects(MonsterStatusEffect e) {
		synchronized (activeEffects) {
			return activeEffects.remove(e);
		}
	}

	public void removeCancelEffectTask(StatusEffectsData e) {
		ScheduledFuture<?> cancelTask;
		switch (e.getSourceType()) {
			case MOB_SKILL:
				synchronized (skillCancels) {
					cancelTask = skillCancels.remove(Short.valueOf((short) e.getDataId()));
				}
				break;
			case PLAYER_SKILL:
				synchronized (diseaseCancels) {
					cancelTask = diseaseCancels.remove(Integer.valueOf(e.getDataId()));
				}
				break;
			default:
				cancelTask = null;
				break;
		}
		if (cancelTask != null)
			cancelTask.cancel(false);
	}

	public boolean isEffectActive(MonsterStatusEffect b) {
		return activeEffects.containsKey(b);
	}

	public boolean isSkillActive(short skillid) {
		return skillCancels.containsKey(Short.valueOf(skillid));
	}

	public boolean isDebuffActive(int mobSkillId) {
		return diseaseCancels.containsKey(Integer.valueOf(mobSkillId));
	}

	public int getSpawnedSummons() {
		return spawnedSummons;
	}

	public void addToSpawnedSummons() {
		spawnedSummons++;
	}

	public void setSpawnEffect(byte effect) {
		this.spawnEffect = effect;
	}

	public boolean wasAttackedBy(GameCharacter player) {
		return damages.getWhenSafe(player) != null;
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

	@Override
	public EntityType getEntityType() {
		return EntityType.MONSTER;
	}

	@Override
	public boolean isAlive() {
		return remHp > 0;
	}

	@Override
	public boolean isVisible() {
		return true;
	}

	@Override
	public byte[] getShowNewSpawnMessage() {
		return GamePackets.writeShowMonster(this, true, spawnEffect);
	}

	@Override
	public byte[] getShowExistingSpawnMessage() {
		return GamePackets.writeShowMonster(this, false, spawnEffect);
	}

	@Override
	public byte[] getDestructionMessage() {
		return GamePackets.writeRemoveMonster(this, deathEffect);
	}

	public interface MobDeathHook {
		public void monsterKilled(GameCharacter highestDamage, GameCharacter last);
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
		private GameCharacter highestDamagePlayer;
		private long highestDamage;

		public PartyExp() {
			minAttackerLevel = GlobalConstants.MAX_LEVEL;
		}

		public void compareAndSetMinAttackerLevel(short damagerlevel) {
			if (damagerlevel < minAttackerLevel)
				minAttackerLevel = damagerlevel;
		}

		public void compareAndSetMaxDamage(long damage, GameCharacter attacker) {
			if (damage > highestDamage) {
				highestDamagePlayer = attacker;
				highestDamage = damage;
			}
		}

		public short getMinAttackerLevel() {
			return minAttackerLevel;
		}

		public GameCharacter getHighestDamagePlayer() {
			return highestDamagePlayer;
		}
	}

	private static byte[] writeShowMobHp(int mobid, byte percent) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.SHOW_MONSTER_HP);
		lew.writeInt(mobid);
		lew.writeByte(percent);

		return lew.getBytes();
	}

	private static byte[] writeShowBossHp(MobStats stats, int remHp) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.MAP_EFFECT);
		lew.writeByte((byte) 0x05);
		lew.writeInt(stats.getMobId());
		lew.writeInt(remHp);
		lew.writeInt(stats.getMaxHp());
		lew.writeByte(stats.getHpTagColor());
		lew.writeByte(stats.getHpTagBgColor());

		return lew.getBytes();
	}
}
