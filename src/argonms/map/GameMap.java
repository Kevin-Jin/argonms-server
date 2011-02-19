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
import argonms.loading.map.Foothold;
import argonms.loading.map.SpawnData;
import argonms.loading.map.MapStats;
import argonms.loading.map.PortalData;
import argonms.loading.map.ReactorData;
import argonms.loading.mob.MobDataLoader;
import argonms.loading.mob.MobStats;
import argonms.loading.reactor.ReactorDataLoader;
import argonms.map.movement.LifeMovementFragment;
import argonms.map.object.ItemDrop;
import argonms.map.object.Mob;
import argonms.map.object.Mob.MobDeathHook;
import argonms.map.object.Npc;
import argonms.map.object.Reactor;
import argonms.net.client.ClientSendOps;
import argonms.net.client.CommonPackets;
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
	private final MapStats stats;
	private final Map<Integer, MapObject> objects;
	private final Set<Player> players;
	private final AtomicInteger monsters;
	private final List<MonsterSpawn> monsterSpawns;
	private int nextObjectId;

	protected GameMap(MapStats stats) {
		this.stats = stats;
		this.objects = new LinkedHashMap<Integer, MapObject>();
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
					spawnObject(n);
					break;
				}
			}
		}
		for (ReactorData r : stats.getReactors().values()) {
			Reactor reactor = new Reactor(ReactorDataLoader.getInstance().getReactorStats(r.getDataId()));
			reactor.setPosition(new Point(r.getX(), r.getY()));
			reactor.setName(r.getName());
			reactor.setDelay(r.getReactorTime());
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

	public MapObject getObjectById(int oid) {
		return objects.get(Integer.valueOf(oid));
	}

	public void spawnObject(MapObject obj) {
		synchronized (objects) {
			obj.setId(nextObjectId++);
			if (obj.isNonRangedType())
				sendToAll(obj.getCreationMessage(), obj.getPosition(), null);
			else
				sendToAll(obj.getCreationMessage());
			objects.put(Integer.valueOf(obj.getId()), obj);
		}
	}

	public void spawnPlayer(Player p) {
		synchronized (objects) {
			sendToAll(p.getCreationMessage());
			synchronized (players) {
				players.add(p);
			}
			for (MapObject mo : objects.values()) {
				if (mo.isNonRangedType()) {
					p.getClient().getSession().send(mo.getCreationMessage());
				} else if (objectVisible(mo, p) && !p.canSeeObject(mo)) {
					p.getClient().getSession().send(mo.getShowObjectMessage());
					p.addToVisibleMapObjects(mo);
				}
			}
			objects.put(Integer.valueOf(p.getId()), p);
		}
	}

	public void spawnMonster(Mob monster) {
		spawnObject(monster);
		monsters.incrementAndGet();
	}

	public void addMonsterSpawn(MobStats stats, Point pos, short fh, int mobTime) {
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

	public void destroyObject(MapObject obj) {
		synchronized (objects) {
			if (!obj.isNonRangedType()) {
				synchronized (players) {
					for (Player p : players)
						p.removeVisibleMapObject(obj);
				}
				sendToAll(obj.getDestructionMessage(), obj.getPosition(), null);
			} else {
				sendToAll(obj.getDestructionMessage());
			}
			objects.remove(Integer.valueOf(obj.getId()));
		}
	}

	public void removePlayer(Player p) {
		synchronized (players) {
			players.remove(p);
		}
		destroyObject(p);
	}

	public void killMonster(Mob monster, int killer) {
		monster.died();
		destroyObject(monster);
		monsterDrop(monster, killer);
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

	public void playerMoved(Player p, List<LifeMovementFragment> moves) {
		synchronized (objects) {
			sendToAll(writePlayerMovement(p, moves), p);

			Iterator<MapObject> iter = p.getVisibleMapObjects().iterator();
			while (iter.hasNext()) {
				MapObject mo = iter.next();
				if (!objectVisible(mo, p)) {
					p.getClient().getSession().send(mo.getOutOfViewMessage());
					iter.remove();
				}
			}
			for (MapObject mo : objects.values()) {
				if (!mo.isNonRangedType() && objectVisible(mo, p) && !p.canSeeObject(mo)) {
					p.getClient().getSession().send(mo.getShowObjectMessage());
					p.addToVisibleMapObjects(mo);
				}
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
			drop.init((byte) 1, killer, calcDropPos(pos, monster.getPosition()), monster.getPosition(), monster.getId());

			spawnObject(drop);
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

	public static boolean objectVisible(MapObject mapobj, Player p) {
		return (p.getPosition().distanceSq(mapobj.getPosition()) <= MAX_VIEW_RANGE_SQ);
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

	private static byte[] writePlayerMovement(Player p, List<LifeMovementFragment> moves) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.MOVE_PLAYER);
		lew.writeInt(p.getId());
		lew.writeInt(0);
		CommonPackets.writeSerializedMovements(lew, moves);
		return lew.getBytes();
	}
}
