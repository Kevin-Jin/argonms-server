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

import argonms.GlobalConstants;
import argonms.character.Party;
import argonms.character.Player;
import argonms.character.StatusEffectTools;
import argonms.character.inventory.InventorySlot;
import argonms.character.inventory.ItemTools;
import argonms.character.skill.PlayerStatusEffectValues.PlayerStatusEffect;
import argonms.game.GameServer;
import argonms.loading.StatusEffectsData;
import argonms.loading.mob.MobStats;
import argonms.loading.mob.Skill;
import argonms.loading.skill.MobSkillEffectsData;
import argonms.map.Element;
import argonms.map.GameMap;
import argonms.map.MapEntity;
import argonms.map.MapEntity.EntityType;
import argonms.map.MonsterStatusEffectValues;
import argonms.map.MonsterStatusEffectValues.MonsterStatusEffect;
import argonms.net.external.ClientSendOps;
import argonms.net.external.CommonPackets;
import argonms.tools.Timer;
import argonms.tools.collections.LockableMap;
import argonms.tools.output.LittleEndianByteArrayWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
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

	private final MobStats stats;
	private final GameMap map;
	private int remHp;
	private int remMp;
	private final List<MobDeathHook> hooks;
	private final LockableMap<Player, PlayerDamage> damages;
	private WeakReference<Player> controller;
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
		this.hooks = new ArrayList<MobDeathHook>();
		this.damages = new LockableMap<Player, PlayerDamage>(new WeakHashMap<Player, PlayerDamage>());
		this.controller = new WeakReference<Player>(null);
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
		Timer.getInstance().runAfterDelay(new Runnable() {
			public void run() {
				map.drop(getDrops(), Mob.this, pickupAllow, owner);
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
			if (attacker == null || attacker.getMapId() != map.getDataId() || !attacker.isAlive())
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
						attacker.getEffectValue(PlayerStatusEffect.HOLY_SYMBOL).getModifier() : 0;
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
				members = attackerParty.getMembersInMap(map.getDataId());
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
								member.getEffectValue(PlayerStatusEffect.HOLY_SYMBOL).getModifier() : 0;
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
		for (ScheduledFuture<?> cancelTask : diseaseCancels.values())
			cancelTask.cancel(true);
		int deathBuff = stats.getBuffToGive();
		if (deathBuff > 0) {
			ItemTools.useItem(killer, deathBuff);
			killer.getClient().getSession().send(CommonPackets.writeSelfVisualEffect(StatusEffectTools.MOB_BUFF, deathBuff, (byte) 1, (byte) -1));
			map.sendToAll(CommonPackets.writeBuffMapVisualEffect(killer, StatusEffectTools.MOB_BUFF, deathBuff, (byte) 1, (byte) -1), killer);
		}
		Player highestDamage = giveExp(killer);
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
		if (pd != null) {
			pd.addDamage(damage);
		} else {
			damages.putWhenSafe(p, new PlayerDamage(damage));
			for (MobDeathHook hook : p.getMobDeathHooks(getDataId()))
				hooks.add(hook);
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

	public EntityType getEntityType() {
		return EntityType.MONSTER;
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
		return CommonPackets.writeRemoveMonster(this, DESTROY_ANIMATION_NONE);
	}

	public byte[] getDestructionMessage() {
		return CommonPackets.writeRemoveMonster(this, deathEffect);
	}

	public boolean isNonRangedType() {
		return false;
	}

	public interface MobDeathHook {
		public void monsterKilled(Player highestDamage, Player last);
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
			minAttackerLevel = GlobalConstants.MAX_LEVEL;
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
