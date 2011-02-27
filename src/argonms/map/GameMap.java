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
import argonms.map.entity.Mob;
import argonms.map.entity.Mob.MobDeathHook;
import argonms.map.entity.Npc;
import argonms.map.entity.Reactor;
import argonms.net.client.ClientSendOps;
import argonms.net.client.CommonPackets;
import argonms.tools.Pair;
import argonms.tools.Timer;
import argonms.tools.output.LittleEndianByteArrayWriter;
import java.awt.Point;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
	private final Map<Integer, MapEntity> entities;
	private final Set<Player> players;
	private final AtomicInteger monsters;
	private final List<MonsterSpawn> monsterSpawns;
	private int nextEntityId;

	protected GameMap(MapStats stats) {
		this.stats = stats;
		this.entities = new LinkedHashMap<Integer, MapEntity>();
		this.players = new LinkedHashSet<Player>();
		this.monsters = new AtomicInteger(0);
		this.monsterSpawns = new LinkedList<MonsterSpawn>();
		for (SpawnData spawnData : stats.getLife().values()) {
			switch (spawnData.getType()) {
				case 'm': {
					Mob m = new Mob(MobDataLoader.getInstance().getMobStats(spawnData.getDataId()));
					m.setFoothold(spawnData.getFoothold());
					m.setPosition(new Point(spawnData.getX(), spawnData.getY()));
					addMonsterSpawn(MobDataLoader.getInstance().getMobStats(spawnData.getDataId()), new Point(spawnData.getX(), spawnData.getY()), spawnData.getFoothold(), spawnData.getMobTime());
					spawnMonster(m);
					break;
				} case 'n': {
					Npc n = new Npc(spawnData.getDataId());
					n.setFoothold(spawnData.getFoothold());
					n.setPosition(new Point(spawnData.getX(), spawnData.getY()));
					n.setCy(spawnData.getCy());
					n.setRx(spawnData.getRx0(), spawnData.getRx1());
					n.setF(spawnData.isF());
					spawnEntity(n);
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

	public MapEntity getEntityById(int eId) {
		return entities.get(Integer.valueOf(eId));
	}

	private void updateMonsterController(Mob monster) {
		if (!monster.isAlive())
			return;
		if (monster.getController() != null)
			if (monster.getController().getMap() == this)
				return;
		int minControlled = Integer.MAX_VALUE;
		Player newController = null;
		synchronized (players) {
			for (Player p : players) {
				int count = p.getControlledMobs().size();
				if (p.isVisible() && count < minControlled) {
					minControlled = count;
					newController = p;
					break;
				}
			}
		}
		if (newController != null) {
			monster.setController(newController);
			newController.controlMonster(monster);
			boolean aggro = monster.isFirstAttack();
			if (aggro) {
				monster.setControllerHasAggro(true);
				monster.setControllerKnowsAboutAggro(true);
			}
			newController.getClient().getSession().send(CommonPackets.writeControlMonster(monster, aggro));
		}
	}

	public final void spawnEntity(MapEntity ent) {
		synchronized (entities) {
			ent.setId(nextEntityId++);
			if (ent.isNonRangedType())
				sendToAll(ent.getCreationMessage(), ent.getPosition(), null);
			else
				sendToAll(ent.getCreationMessage());
			entities.put(Integer.valueOf(ent.getId()), ent);
		}
	}

	public void spawnPlayer(Player p) {
		synchronized (entities) {
			sendToAll(p.getCreationMessage());
			synchronized (players) {
				players.add(p);
			}
			for (MapEntity ent : entities.values()) {
				if (ent.isNonRangedType()) {
					p.getClient().getSession().send(ent.getShowObjectMessage());
				} else {
					if (ent.getEntityType() == MapEntityType.MONSTER)
						updateMonsterController((Mob) ent);
					if (entityVisible(ent, p) && !p.canSeeObject(ent) && ent.isAlive()) {
						p.getClient().getSession().send(ent.getShowObjectMessage());
						p.addToVisibleMapObjects(ent);
					}
				}
			}
			entities.put(Integer.valueOf(p.getId()), p);
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
				ItemDrop d = (ItemDrop) entities.get(eid);
				if (d.isAlive()) {
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
			Mob mob = new Mob(stats);
			mob.setFoothold(fh);
			mob.setPosition(pos);
			spawnMonster(mob);
		} else {
			MonsterSpawn sp = new MonsterSpawn(stats, pos, fh, mobTime);
			monsterSpawns.add(sp);
			if (sp.shouldSpawn())
				sp.spawnMonster();
		}
	}

	public void destroyEntity(MapEntity ent) {
		synchronized (entities) {
			if (!ent.isNonRangedType()) {
				synchronized (players) {
					for (Player p : players)
						p.removeVisibleMapObject(ent);
				}
				sendToAll(ent.getDestructionMessage(), ent.getPosition(), null);
			} else {
				sendToAll(ent.getDestructionMessage());
			}
			entities.remove(Integer.valueOf(ent.getId()));
		}
	}

	public void removePlayer(Player p) {
		synchronized (players) {
			players.remove(p);
		}
		for (Mob m : p.getControlledMobs()) {
			m.setController(null);
			m.setControllerHasAggro(false);
			m.setControllerKnowsAboutAggro(false);
			updateMonsterController(m);
		}
		destroyEntity(p);
	}

	public void killMonster(Mob monster, int killer) {
		monster.died();
		destroyEntity(monster);
		monsterDrop(monster, killer);
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
			p.gainMesos(d.getMesoValue());
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
					for (Short s : changedSlots.getLeft()) { //modified
						pos = s.shortValue();
						qty = inv.get(pos).getQuantity();
						p.getClient().getSession().send(CommonPackets.writeInventorySlotUpdate(type, pos, inv.get(pos), true, false));
					}
					for (Short s : changedSlots.getRight()) { //added
						pos = s.shortValue();
						qty = inv.get(pos).getQuantity();
						p.getClient().getSession().send(CommonPackets.writeInventorySlotUpdate(type, pos, inv.get(pos), true, true));
					}
				} else {
					//TODO: apply item effect
				}
			} else {
				p.getClient().getSession().send(CommonPackets.writeInventoryFull());
			}
		}
	}

	public void respawnReactor(Reactor r) {
		r.setState((byte) 0);
		r.setAlive(true);
		spawnEntity(r);
	}

	public void respawn() {
		if (players.isEmpty())
			return;
		synchronized (monsterSpawns) {
			int numShouldSpawn = (monsterSpawns.size() - monsters.get()) * Math.round(stats.getMobRate());
			if (numShouldSpawn > 0) {
				int spawned = 0;
				for (MonsterSpawn spawnPoint : monsterSpawns) {
					if (spawnPoint.shouldSpawn()) {
						spawnPoint.spawnMonster();
						spawned++;
					}
					if (spawned >= numShouldSpawn)
						break;
				}
			}
		}
	}

	public void playerMoved(Player p, List<LifeMovementFragment> moves, Point startPos) {
		synchronized (entities) {
			sendToAll(writePlayerMovement(p, moves, startPos), p);

			Iterator<MapEntity> iter = p.getVisibleMapObjects().iterator();
			while (iter.hasNext()) {
				MapEntity ent = iter.next();
				if (!entityVisible(ent, p)) {
					p.getClient().getSession().send(ent.getOutOfViewMessage());
					iter.remove();
				}
			}
			for (MapEntity ent : entities.values()) {
				if (!ent.isNonRangedType()) {
					if (entityVisible(ent, p) && !p.canSeeObject(ent) && ent.isAlive()) {
						p.getClient().getSession().send(ent.getShowObjectMessage());
						p.addToVisibleMapObjects(ent);
					}
				}
			}
		}
	}

	public void monsterMoved(Player p, Mob m, List<LifeMovementFragment> moves, boolean useSkill, byte skill, short skillId, byte s2, byte s3, byte s4, Point startPos) {
		synchronized (entities) {
			sendToAll(writeMonsterMovement(useSkill, skill, skillId, s2, s3, s4, m.getId(), startPos, moves), m.getPosition(), MAX_VIEW_RANGE_SQ, p);
		}
	}

	public byte getPortalIdByName(String portalName) {
		for (Entry<Byte, PortalData> portal : stats.getPortals().entrySet())
			if (portal.getValue().getPortalName().equals(portalName))
				return portal.getKey().byteValue();
		return -1;
	}

	public void enterPortal(Player p, String portalName) {
		for (PortalData portal : stats.getPortals().values())
			if (portal.getPortalName().equals(portalName))
				enterPortal(p, portal);
	}

	public void enterPortal(Player p, byte portalId) {
		enterPortal(p, stats.getPortals().get(Byte.valueOf(portalId)));
	}

	private void enterPortal(Player p, PortalData portal) {
		String scriptName = portal.getScript();
		if (scriptName != null && scriptName.length () != 0) {
			PortalScriptManager.runScript(scriptName, p);
		} else {
			int tm = portal.getTargetMapId();
			GameMap map = GameServer.getChannel(p.getClient().getChannel()).getMapFactory().getMap(tm);
			if (tm != 999999999 && map != null) {
				byte portalId = map.getPortalIdByName(portal.getTargetName());
				if (portalId != -1)
					p.changeMap(tm, portalId);
			}
		}
	}

	private Point calcPointBelow(Point initial) {
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

	private Point calcDropPos(Point initial, Point fallback) {
		Point ret = calcPointBelow(new Point(initial.x, initial.y - 99));
		return ret != null ? ret : fallback;
	}

	private void monsterDrop(Mob monster, int killer) {
		Point pos = monster.getPosition();
		int mobX = pos.x;
		int dropNum = 1;
		for (ItemDrop drop : monster.getDrops()) {
			if (drop.getDropType() == 3)
				pos.x = (int) (mobX + (dropNum % 2 == 0 ? (40 * (dropNum + 1) / 2) : -(40 * (dropNum / 2))));
			else
				pos.x = (int) (mobX + (dropNum % 2 == 0 ? (25 * (dropNum + 1) / 2) : -(25 * (dropNum / 2))));
			drop.init(killer, calcDropPos(pos, monster.getPosition()), monster.getPosition(), monster.getId());

			drop(drop);
			dropNum++;
		}
	}

	/**
	 * Send a packet to all players in this map, regardless of their distance
	 * from a specific point and whether they were the source of the message.
	 * @param message the packet to send to all players
	 */
	public void sendToAll(byte[] message) {
		synchronized (players) {
			for (Player p : players)
				p.getClient().getSession().send(message);
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
		synchronized (players) {
			for (Player p : players)
				if (!p.equals(source))
					p.getClient().getSession().send(message);
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
		synchronized (players) {
			for (Player p : players)
				if (!p.equals(source))
					if (center.distanceSq(p.getPosition()) <= range)
						p.getClient().getSession().send(message);
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

	private class MonsterSpawn {
		private MobStats mobStats;
		private Point pos;
		private short foothold;
		private long nextPossibleSpawn;
		private int mobTime;
		private AtomicInteger spawnedMonsters;
		private boolean immobile;
		
		public MonsterSpawn(MobStats stats, Point pos, short fh, int mobTime) {
			this.mobStats = stats;
			this.pos = pos;
			this.foothold = fh;
			this.mobTime = mobTime;
			this.immobile = !stats.getDelays().containsKey("move") && !stats.getDelays().containsKey("fly");
			this.nextPossibleSpawn = System.currentTimeMillis();
			spawnedMonsters = new AtomicInteger(0);
		}

		public boolean shouldSpawn() {
			return shouldSpawn(System.currentTimeMillis());
		}

		private boolean shouldSpawn(long now) {
			if (mobTime < 0 || ((mobTime != 0 || immobile) && spawnedMonsters.get() > 0) || spawnedMonsters.get() > 2)
				return false;
			return nextPossibleSpawn <= now;
		}

		public Mob spawnMonster() {
			Mob mob = new Mob(mobStats);
			mob.setFoothold(foothold);
			mob.setPosition(new Point(pos));
			spawnedMonsters.incrementAndGet();
			mob.addDeathHook(new MobDeathHook() {
				public void monsterKilled(Player highestDamageChar) {
					nextPossibleSpawn = System.currentTimeMillis();
					if (mobTime > 0)
						nextPossibleSpawn += mobTime * 1000;
					else
						nextPossibleSpawn += mobStats.getDelays().get("die1").intValue();
					spawnedMonsters.decrementAndGet();
				}
			});
			GameMap.this.spawnMonster(mob);
			if (mobTime == 0)
				nextPossibleSpawn = System.currentTimeMillis() + 5000;
			return mob;
		}
	}

	private static byte[] writePlayerMovement(Player p, List<LifeMovementFragment> moves, Point startPos) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.MOVE_PLAYER);
		lew.writeInt(p.getId());
		lew.writeShort((short) startPos.x);
		lew.writeShort((short) startPos.y);
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
		lew.writeShort((short) startPos.x);
		lew.writeShort((short) startPos.y);
		CommonPackets.writeSerializedMovements(lew, moves);
		return lew.getBytes();
	}
}
