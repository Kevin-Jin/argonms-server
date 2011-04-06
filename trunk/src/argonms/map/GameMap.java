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

package argonms.map;

import argonms.GlobalConstants;
import argonms.character.Player;
import argonms.character.inventory.Inventory;
import argonms.character.inventory.Inventory.InventoryType;
import argonms.character.inventory.InventorySlot;
import argonms.character.inventory.InventoryTools;
import argonms.game.GameServer;
import argonms.game.script.PortalScriptManager;
import argonms.loading.item.ItemDataLoader;
import argonms.loading.map.Foothold;
import argonms.loading.map.SpawnData;
import argonms.loading.map.MapStats;
import argonms.loading.map.PortalData;
import argonms.loading.map.ReactorData;
import argonms.loading.mob.MobDataLoader;
import argonms.loading.mob.MobStats;
import argonms.loading.reactor.ReactorDataLoader;
import argonms.map.MapEntity.MapEntityType;
import argonms.map.movement.LifeMovementFragment;
import argonms.map.entity.ItemDrop;
import argonms.map.entity.Mist;
import argonms.map.entity.Mob;
import argonms.map.entity.Mob.MobDeathHook;
import argonms.map.entity.Npc;
import argonms.map.entity.Reactor;
import argonms.net.client.ClientSendOps;
import argonms.net.client.CommonPackets;
import argonms.tools.Pair;
import argonms.tools.Timer;
import argonms.tools.collections.LockableList;
import argonms.tools.collections.LockableMap;
import argonms.tools.collections.LockableSet;
import argonms.tools.output.LittleEndianByteArrayWriter;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author GoldenKevin
 */
public class GameMap {
	/**
	 * The maximum distance that any character could see.
	 */
	private static final double MAX_VIEW_RANGE_SQ = 850 * 850;
	/**
	 * Amount of time in milliseconds that a dropped item will remain on the map
	 */
	private static final int DROP_EXPIRE = 60000;
	private final MapStats stats;
	private final LockableMap<Integer, MapEntity> entities;
	private final LockableSet<Player> players;
	private final LockableList<MonsterSpawn> monsterSpawns;
	private final AtomicInteger monsters;
	//no need to be Atomic because it always is locked by entities...
	private int nextEntityId;

	protected GameMap(MapStats stats) {
		this.stats = stats;
		this.entities = new LockableMap<Integer, MapEntity>(new LinkedHashMap<Integer, MapEntity>());
		this.players = new LockableSet<Player>(new LinkedHashSet<Player>());
		this.monsterSpawns = new LockableList<MonsterSpawn>(new LinkedList<MonsterSpawn>());
		this.monsters = new AtomicInteger(0);
		for (SpawnData spawnData : stats.getLife().values()) {
			switch (spawnData.getType()) {
				case 'm': {
					addMonsterSpawn(MobDataLoader.getInstance().getMobStats(spawnData.getDataId()), new Point(spawnData.getX(), spawnData.getY()), spawnData.getFoothold(), spawnData.getMobTime());
					break;
				} case 'n': {
					Npc n = new Npc(spawnData.getDataId());
					n.setFoothold(spawnData.getFoothold());
					n.setPosition(new Point(spawnData.getX(), spawnData.getY()));
					n.setCy(spawnData.getCy());
					n.setRx(spawnData.getRx0(), spawnData.getRx1());
					n.setF(spawnData.isF());
					spawnNpc(n);
					break;
				}
			}
		}
		for (ReactorData r : stats.getReactors().values()) {
			Reactor reactor = new Reactor(ReactorDataLoader.getInstance().getReactorStats(r.getDataId()));
			reactor.setPosition(new Point(r.getX(), r.getY()));
			reactor.setName(r.getName());
			reactor.setDelay(r.getReactorTime());
			spawnEntity(reactor);
		}
	}

	public MapStats getStaticData() {
		return stats;
	}

	public int getMapId() {
		return stats.getMapId();
	}

	public int getReturnMap() {
		return stats.getReturnMap();
	}

	public int getForcedReturnMap() {
		return stats.getForcedReturn();
	}

	public byte nearestSpawnPoint(Point from) {
		byte closest = 0;
		double shortestDistance = Double.POSITIVE_INFINITY;
		for (Entry<Byte, PortalData> entry : stats.getPortals().entrySet()) {
			PortalData portal = entry.getValue();
			double distance = portal.getPosition().distanceSq(from);
			if (portal.getPortalType() >= 0 && portal.getPortalType() <= 2 && distance < shortestDistance) {
				closest = entry.getKey().byteValue();
				shortestDistance = distance;
			}
		}
		return closest;
	}

	public Point getPortalPosition(byte portal) {
		PortalData p = stats.getPortals().get(Byte.valueOf(portal));
		if (p != null)
			return p.getPosition();
		return null;
	}

	/**
	 * Retrieve the entity on this map with the given entity id.
	 *
	 * Make sure to synchronize the returned map entity if necessary (i.e. if
	 * you modify state data inside it)!
	 * @param eId the entity id
	 * @return the map entity with the given id, or null if there is no such
	 * entity.
	 */
	public MapEntity getEntityById(int eId) {
		entities.lockRead();
		try {
			return entities.get(Integer.valueOf(eId));
		} finally {
			entities.unlockRead();
		}
	}

	public int getPlayerCount() {
		return players.getSizeWhenSafe();
	}

	private void updateMonsterController(Mob monster) {
		if (!monster.isAlive())
			return;
		Player controller = monster.getController();
		if (controller != null)
			if (monster.getController().getMap() == this)
				return;
			else
				controller = null;
		int minControlled = Integer.MAX_VALUE;
		players.lockRead();
		try {
			for (Player p : players) {
				int count = p.getControlledMobs().size();
				if (p.isVisible() && count < minControlled) {
					minControlled = count;
					controller = p;
					break;
				}
			}
		} finally {
			players.unlockRead();
		}
		if (controller != null) {
			monster.setController(controller);
			controller.controlMonster(monster);
			boolean aggro = monster.isFirstAttack();
			if (aggro) {
				monster.setControllerHasAggro(true);
				monster.setControllerKnowsAboutAggro(true);
			}
			controller.getClient().getSession().send(CommonPackets.writeControlMonster(monster, aggro));
		}
	}

	public final void spawnEntity(MapEntity ent) {
		entities.lockWrite();
		try {
			ent.setId(nextEntityId++);
			entities.put(Integer.valueOf(ent.getId()), ent);
		} finally {
			entities.unlockWrite();
		}
		if (ent.isNonRangedType())
			sendToAll(ent.getCreationMessage());
		else
			sendToAll(ent.getCreationMessage(), ent.getPosition(), null);
	}

	private void sendEntityData(MapEntity ent, Player p) {
		if (ent.isNonRangedType()) {
			if (ent.isVisible()) {
				p.getClient().getSession().send(ent.getShowEntityMessage());
				switch (ent.getEntityType()) { //maybe it's just NPC?
					case NPC:
					case HIRED_MERCHANT:
					case PLAYER_NPC:
						p.getClient().getSession().send(CommonPackets.writeControlNpc((Npc) ent));
						break;
					/*case PLAYER:
						Pet[] equippedPets = ((Player) ent).getPets();
						for (byte i = 0; i < 3; i++) {
							if (equippedPets[i] != null)
								p.getClient().getSession().send(CommonPackets.writeShowPet(this, i, equippedPets[i], true, false));
						break;*/
				}
			}
		} else {
			if (ent.getEntityType() == MapEntityType.MONSTER)
				updateMonsterController((Mob) ent);
			if (entityVisible(ent, p) && !p.canSeeObject(ent) && ent.isAlive()) {
				p.getClient().getSession().send(ent.getShowEntityMessage());
				p.addToVisibleMapObjects(ent);
			}
		}
	}

	public void spawnPlayer(Player p) {
		entities.lockWrite();
		try { //write lock allows us to read in mutex, so no need for a readLock
			if (p.isVisible()) { //show ourself to other clients if we are not hidden
				sendToAll(p.getCreationMessage());
				/*Pet[] equippedPets = p.getPets();
					for (byte i = 0; i < 3; i++) {
						if (equippedPets[i] != null)
							p.getClient().getSession().send(CommonPackets.writeShowPet(this, i, equippedPets[i], true, false));*/
			}
			players.addWhenSafe(p);
			for (MapEntity ent : entities.values())
				sendEntityData(ent, p);
			entities.put(Integer.valueOf(p.getId()), p);
		} finally {
			entities.unlockWrite();
		}
	}

	public final void spawnMonster(Mob monster) {
		spawnEntity(monster);
		updateMonsterController(monster);
		monsters.incrementAndGet();
	}

	/**
	 * Spawns an item drop entity on this map.
	 * Make sure you call ItemDrop.init with the correct data on the passed
	 * ItemDrop object before calling this method.
	 * @param d the ItemDrop entity to spawn.
	 */
	public void drop(ItemDrop d) {
		spawnEntity(d);
		final Integer eid = Integer.valueOf(d.getId());
		Timer.getInstance().runAfterDelay(new Runnable() {
			public void run() {
				ItemDrop d = (ItemDrop) entities.getWhenSafe(eid);
				if (d != null) {
					d.expire();
					destroyEntity(d);
				}
			}
		}, DROP_EXPIRE); //expire after 1 minute
	}

	public final void addMonsterSpawn(MobStats stats, Point pos, short fh, int mobTime) {
		pos = calcPointBelow(pos);
		pos.y -= 1;
		if (mobTime == -1) {
			Mob mob = new Mob(stats, this);
			mob.setFoothold(fh);
			mob.setPosition(pos);
			spawnMonster(mob);
		} else {
			MonsterSpawn sp = new MonsterSpawn(stats, pos, fh, mobTime);
			monsterSpawns.addWhenSafe(sp);
			if (sp.shouldSpawn())
				spawnMonster(sp.getNewSpawn());
		}
	}

	public void spawnMist(final Mist mist, final int duration) {
		spawnEntity(mist);
		Timer tMan = Timer.getInstance();
		final ScheduledFuture<?> poisonSchedule;
		if (mist.getMistType() == Mist.POISON_MIST) {
			Runnable poisonTask = new Runnable() {
				public void run() {
					List<MapEntity> affectedMonsters = getMapObjectsInRect(mist.getBox());
					for (MapEntity mo : affectedMonsters) {
						if (mo.getEntityType() == MapEntityType.MONSTER && mist.shouldHurt()) {
							Mob monster = (Mob) mo;
							//TODO: implement poison mist
							//MonsterStatusEffect poisonEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), mist.getSourceSkill(), false);
							//((MapleMonster) mo).applyStatus(mist.getOwner(), poisonEffect, true, duration, false);
						}
					}
				}
			};
			poisonSchedule = tMan.runRepeatedly(poisonTask, 2000, 2500);
		} else {
			poisonSchedule = null;
		}
		tMan.runAfterDelay(new Runnable() {
			@Override
			public void run() {
				destroyEntity(mist);
				if (poisonSchedule != null)
					poisonSchedule.cancel(false);
			}
		}, duration);
	}

	public final void spawnNpc(Npc n) {
		spawnEntity(n);
		//let every client animate their own NPCs.
		//controlling mobs is complicated enough as it is, we
		//don't need to keep track of more than necessary things
		players.lockRead();
		try {
			for (Player p : players)
				p.getClient().getSession().send(CommonPackets.writeControlNpc(n));
		} finally {
			players.unlockRead();
		}
	}

	public void destroyEntity(MapEntity ent) {
		players.lockRead();
		try {
			if (!ent.isNonRangedType()) {
				for (Player p : players)
					p.removeVisibleMapObject(ent);
				sendToAll(ent.getDestructionMessage(), ent.getPosition(), null);
			} else {
				sendToAll(ent.getDestructionMessage());
			}
		} finally {
			players.unlockRead();
		}
		entities.lockWrite();
		try {
			entities.remove(Integer.valueOf(ent.getId()));
		} finally {
			entities.unlockWrite();
		}
	}

	public void removePlayer(Player p) {
		players.removeWhenSafe(p);
		for (Mob m : p.getControlledMobs()) {
			m.setController(null);
			m.setControllerHasAggro(false);
			m.setControllerKnowsAboutAggro(false);
			updateMonsterController(m);
		}
		destroyEntity(p);
	}

	public void killMonster(Mob monster, Player killer) {
		monster.died(killer);
		destroyEntity(monster);
		monsters.decrementAndGet();
	}

	public void destroyReactor(final Reactor r) {
		r.setAlive(false);
		destroyEntity(r);
		if (r.getDelay() > 0) {
			Timer.getInstance().runAfterDelay(new Runnable() {
				public void run() {
					respawnReactor(r);
				}
			}, r.getDelay());
		}
	}

	public void pickUpDrop(ItemDrop d, Player p) {
		if (d.getDropType() == ItemDrop.MESOS) {
			p.gainMesos(d.getMesoValue(), false);
			d.pickUp(p.getId());
			destroyEntity(d);
		} else {
			InventorySlot pickedUp = d.getItem();
			int itemid = pickedUp.getItemId();
			short qty = pickedUp.getQuantity();
			InventoryType type = InventoryTools.getCategory(d.getItem().getItemId());
			Inventory inv = p.getInventory(type);
			if (InventoryTools.canFitEntirely(inv, itemid, qty)) {
				d.pickUp(p.getId());
				destroyEntity(d);
				if (!ItemDataLoader.getInstance().isConsumeOnPickup(itemid)) {
					Pair<List<Short>, List<Short>> changedSlots = InventoryTools.addToInventory(inv, itemid, qty);
					short pos;
					InventorySlot slot;
					for (Short s : changedSlots.getLeft()) { //modified
						pos = s.shortValue();
						slot = inv.get(pos);
						qty = slot.getQuantity();
						p.getClient().getSession().send(CommonPackets.writeInventorySlotUpdate(type, pos, slot, true, false));
					}
					for (Short s : changedSlots.getRight()) { //added
						pos = s.shortValue();
						slot = inv.get(pos);
						qty = slot.getQuantity();
						p.getClient().getSession().send(CommonPackets.writeInventorySlotUpdate(type, pos, slot, true, true));
					}
				} else {
					//TODO: apply item effect
				}
				p.getClient().getSession().send(CommonPackets.writeShowItemGain(itemid, pickedUp.getQuantity()));
			} else {
				p.getClient().getSession().send(CommonPackets.writeInventoryFull());
				p.getClient().getSession().send(CommonPackets.writeShowInventoryFull());
			}
		}
	}

	public void mesoExplosion(ItemDrop d, Player p) {
		if (d.getDropType() == ItemDrop.MESOS) {
			p.gainMesos(d.getMesoValue(), false);
			d.explode();
			destroyEntity(d);
		}
	}

	public void respawnReactor(Reactor r) {
		r.setState((byte) 0);
		r.setAlive(true);
		spawnEntity(r);
	}

	//FIXME: I think all mobs spawn only on the top spawnpoint. o.O
	public void respawnMobs() {
		if (players.getSizeWhenSafe() == 0)
			return;
		monsterSpawns.lockRead();
		try {
			if (monsterSpawns.isEmpty())
				return;
			Collections.sort(monsterSpawns);
			int numShouldSpawn = (monsterSpawns.size() - monsters.get()) * Math.round(stats.getMobRate());
			if (numShouldSpawn > 0) {
				int spawned = 0;
				for (MonsterSpawn spawnPoint : monsterSpawns) {
					if (spawnPoint.shouldSpawn()) {
						spawnMonster(spawnPoint.getNewSpawn());
						spawned++;
					}
					if (spawned >= numShouldSpawn)
						break;
				}
			}
		} finally {
			monsterSpawns.unlockRead();
		}
	}

	public void playerMoved(Player p, List<LifeMovementFragment> moves, Point startPos) {
		sendToAll(writePlayerMovement(p, moves, startPos), p);

		Iterator<MapEntity> iter = p.getVisibleMapObjects().iterator();
		while (iter.hasNext()) {
			MapEntity ent = iter.next();
			if (!entityVisible(ent, p)) {
				p.getClient().getSession().send(ent.getOutOfViewMessage());
				iter.remove();
			}
		}
		entities.lockRead();
		try {
			for (MapEntity ent : entities.values()) {
				if (!ent.isNonRangedType()) {
					if (entityVisible(ent, p) && !p.canSeeObject(ent) && ent.isAlive()) {
						p.getClient().getSession().send(ent.getShowEntityMessage());
						p.addToVisibleMapObjects(ent);
					}
				}
			}
		} finally {
			entities.unlockRead();
		}
	}

	public void monsterMoved(Player p, Mob m, List<LifeMovementFragment> moves, boolean useSkill, byte skill, short skillId, byte s2, byte s3, byte s4, Point startPos) {
		sendToAll(writeMonsterMovement(useSkill, skill, skillId, s2, s3, s4, m.getId(), startPos, moves), m.getPosition(), MAX_VIEW_RANGE_SQ, p);
	}

	public void damageMonster(Player p, Mob m, int damage) {
		m.hurt(p, damage);
		if (!m.isAlive())
			killMonster(m, p);
	}

	public byte getPortalIdByName(String portalName) {
		for (Entry<Byte, PortalData> portal : stats.getPortals().entrySet())
			if (portal.getValue().getPortalName().equals(portalName))
				return portal.getKey().byteValue();
		return -1;
	}

	public boolean enterPortal(Player p, String portalName) {
		for (PortalData portal : stats.getPortals().values())
			if (portal.getPortalName().equals(portalName))
				enterPortal(p, portal);
		return false;
	}

	public boolean enterPortal(Player p, byte portalId) {
		PortalData portal = stats.getPortals().get(Byte.valueOf(portalId));
		return (portal != null ? enterPortal(p, portal) : false);
	}

	private boolean enterPortal(Player p, PortalData portal) {
		String scriptName = portal.getScript();
		if (scriptName != null && scriptName.length () != 0) {
			return PortalScriptManager.getInstance().runScript(scriptName, p);
		} else {
			int tm = portal.getTargetMapId();
			GameMap toMap = GameServer.getChannel(p.getClient().getChannel()).getMapFactory().getMap(tm);
			if (tm != GlobalConstants.NULL_MAP && toMap != null) {
				byte portalId = toMap.getPortalIdByName(portal.getTargetName());
				if (portalId != -1)
					return p.changeMap(tm, portalId);
			}
		}
		return false;
	}

	public Point calcPointBelow(Point initial) {
		Foothold fh = stats.getFootholds().findBelow(initial);
		if (fh == null)
			return null;
		int dropY = fh.getY1();
		if (!fh.isWall() && fh.getY1() != fh.getY2()) {
			double s1 = Math.abs(fh.getY2() - fh.getY1());
			double s2 = Math.abs(fh.getX2() - fh.getX1());
			double s4 = Math.abs(initial.x - fh.getX1());
			double alpha = Math.atan(s2 / s1);
			double beta = Math.atan(s1 / s2);
			double s5 = Math.cos(alpha) * (s4 / Math.cos(beta));
			if (fh.getY2() < fh.getY1())
				dropY = fh.getY1() - (int) s5;
			else
				dropY = fh.getY1() + (int) s5;
		}
		return new Point(initial.x, dropY);
	}

	/**
	 * Send a packet to all players in this map, regardless of their distance
	 * from a specific point and whether they were the source of the message.
	 * @param message the packet to send to all players
	 */
	public void sendToAll(byte[] message) {
		players.lockRead();
		try {
			for (Player p : players)
				p.getClient().getSession().send(message);
		} finally {
			players.unlockRead();
		}
	}

	/**
	 * Send a packet to all players in this map, except the one Player that is
	 * given as source, regardless of their distance from a specific point.
	 * @param message the packet to send to the players
	 * @param source the player that will not receive this message. If all
	 * players should receive this message, pass null for this parameter.
	 */
	public void sendToAll(byte[] message, Player source) {
		players.lockRead();
		try {
			for (Player p : players)
				if (!p.equals(source))
					p.getClient().getSession().send(message);
		} finally {
			players.unlockRead();
		}
	}

	/**
	 * Send a packet to all players in this map that are within the given
	 * distance from the specified center point.
	 * @param message the packet to send to the players
	 * @param center the point that the distance is centered around
	 * @param range the maximum distance from the given center point that a
	 * player will receive this message from
	 * @param source the player that will not receive this message. If all
	 * players should receive this message, pass null for this parameter.
	 */
	public void sendToAll(byte[] message, Point center, double range, Player source) {
		players.lockRead();
		try {
			for (Player p : players)
				if (!p.equals(source))
					if (center.distanceSq(p.getPosition()) <= range)
						p.getClient().getSession().send(message);
		} finally {
			players.unlockRead();
		}
	}

	/**
	 * Send a packet to all players in this map that are within viewable
	 * distance from the specified center point.
	 * @param message the packet to send to the players
	 * @param center the point that the distance is centered around
	 * @param source the player that will not receive this message. If all
	 * players should receive this message, pass null for this parameter.
	 */
	public void sendToAll(byte[] message, Point center, Player source) {
		sendToAll(message, center, MAX_VIEW_RANGE_SQ, source);
	}

	private static boolean entityVisible(MapEntity ent, Player p) {
		return (p.getPosition().distanceSq(ent.getPosition()) <= MAX_VIEW_RANGE_SQ);
	}

	private List<MapEntity> getMapObjectsInRect(Rectangle box) {
		List<MapEntity> ret = new LinkedList<MapEntity>();
		entities.lockRead();
		try {
			for (MapEntity ent : entities.values())
				if (box.contains(ent.getPosition()))
					ret.add(ent);
		} finally {
			entities.unlockRead();
		}
		return ret;
	}

	private class MonsterSpawn implements Comparable<MonsterSpawn> {
		private final MobStats mobStats;
		private final Point pos;
		private final short foothold;
		private long nextPossibleSpawn;
		private final int mobTime;
		private final AtomicInteger spawnedMonsters;
		private final boolean immobile;
		
		public MonsterSpawn(MobStats stats, Point pos, short fh, int mobTime) {
			this.mobStats = stats;
			this.pos = pos;
			this.foothold = fh;
			this.mobTime = mobTime;
			this.immobile = !stats.getDelays().containsKey("move") && !stats.getDelays().containsKey("fly");
			this.nextPossibleSpawn = System.currentTimeMillis();
			this.spawnedMonsters = new AtomicInteger(0);
		}

		public boolean shouldSpawn() {
			if (mobTime < 0 || ((mobTime != 0 || immobile) && spawnedMonsters.get() > 0) || spawnedMonsters.get() > 2)
				return false;
			return nextPossibleSpawn <= System.currentTimeMillis();
		}

		public Mob getNewSpawn() {
			Mob mob = new Mob(mobStats, GameMap.this);
			mob.setFoothold(foothold);
			mob.setPosition(new Point(pos));
			spawnedMonsters.incrementAndGet();
			mob.addDeathHook(new MobDeathHook() {
				public void monsterKilled(Player finalAttacker) {
					//this has to be atomic, so I had to do away with assigning
					//nextPossibleSpawn more than once.
					if (mobTime > 0)
						nextPossibleSpawn = System.currentTimeMillis() + mobTime * 1000;
					else
						nextPossibleSpawn = System.currentTimeMillis() + mobStats.getDelays().get("die1").intValue();
					spawnedMonsters.decrementAndGet();
				}
			});
			if (mobTime == 0)
				nextPossibleSpawn = System.currentTimeMillis() + 5000;
			return mob;
		}

		public int compareTo(MonsterSpawn m) {
			int aliveDelta = spawnedMonsters.get() - m.spawnedMonsters.get();
			if (aliveDelta == 0)
				return (int) (nextPossibleSpawn - m.nextPossibleSpawn);
			return aliveDelta;
		}
	}

	private static byte[] writePlayerMovement(Player p, List<LifeMovementFragment> moves, Point startPos) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.MOVE_PLAYER);
		lew.writeInt(p.getId());
		lew.writePos(startPos);
		CommonPackets.writeSerializedMovements(lew, moves);
		return lew.getBytes();
	}

	private static byte[] writeMonsterMovement(boolean useSkill, byte skill, short skillId, byte skillLevel, byte s3, byte s4, int eid, Point startPos, List<LifeMovementFragment> moves) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.MOVE_MONSTER);
		lew.writeInt(eid);
		lew.writeBool(useSkill);
		lew.writeByte(skill);
		lew.writeByte((byte) skillId);
		lew.writeByte(skillLevel);
		lew.writeByte(s3);
		lew.writeByte(s4); //or is this just 0?
		lew.writePos(startPos);
		CommonPackets.writeSerializedMovements(lew, moves);
		return lew.getBytes();
	}
}
