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

package argonms.game.field;

import argonms.common.GlobalConstants;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.character.inventory.InventoryTools.UpdatedSlots;
import argonms.common.loading.item.ItemDataLoader;
import argonms.common.net.external.ClientSendOps;
import argonms.common.net.external.ClientSession;
import argonms.common.util.Scheduler;
import argonms.common.util.collections.LockableList;
import argonms.common.util.collections.LockableMap;
import argonms.common.util.collections.Pair;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.game.GameServer;
import argonms.game.character.GameCharacter;
import argonms.game.character.inventory.ItemTools;
import argonms.game.field.MapEntity.EntityType;
import argonms.game.field.entity.ItemDrop;
import argonms.game.field.entity.Mist;
import argonms.game.field.entity.Mob;
import argonms.game.field.entity.Mob.MobDeathListener;
import argonms.game.field.entity.Npc;
import argonms.game.field.entity.PlayerNpc;
import argonms.game.field.entity.PlayerSkillSummon;
import argonms.game.field.entity.Reactor;
import argonms.game.field.movement.LifeMovementFragment;
import argonms.game.loading.map.Foothold;
import argonms.game.loading.map.MapStats;
import argonms.game.loading.map.PortalData;
import argonms.game.loading.map.ReactorData;
import argonms.game.loading.map.SpawnData;
import argonms.game.loading.mob.MobDataLoader;
import argonms.game.loading.mob.MobStats;
import argonms.game.loading.reactor.ReactorDataLoader;
import argonms.game.net.external.GamePackets;
import argonms.game.script.PortalScriptManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author GoldenKevin
 */
public class GameMap {
	/**
	 * Amount of time in milliseconds that a dropped item will remain on the map
	 */
	private static final int DROP_EXPIRE = 60000;
	private final MapStats stats;
	private final Map<EntityType, EntityPool> entPools;
	private final LockableList<MonsterSpawn> monsterSpawns;
	private final AtomicInteger monsters;
	private final Map<String, String> portalOverrides;
	private final Set<Short> occupiedChairs;
	private Map<GameCharacter, ScheduledFuture<?>> timeLimitTasks;
	private Map<GameCharacter, ScheduledFuture<?>> decHpTasks;

	protected GameMap(MapStats stats) {
		this.stats = stats;
		this.entPools = new EnumMap<EntityType, EntityPool>(EntityType.class);
		for (EntityType type : EntityType.values())
			entPools.put(type, new EntityPool());
		this.monsterSpawns = new LockableList<MonsterSpawn>(new LinkedList<MonsterSpawn>());
		this.monsters = new AtomicInteger(0);
		this.portalOverrides = new ConcurrentHashMap<String, String>();
		this.occupiedChairs = Collections.newSetFromMap(new ConcurrentHashMap<Short, Boolean>());
		for (SpawnData spawnData : stats.getLife().values()) {
			switch (spawnData.getType()) {
				case 'm':
					addMonsterSpawn(MobDataLoader.getInstance().getMobStats(spawnData.getDataId()), new Point(spawnData.getX(), spawnData.getY()), spawnData.getFoothold(), spawnData.getMobTime());
					break;
				case 'n': {
					Npc n = new Npc(spawnData.getDataId());
					n.setFoothold(spawnData.getFoothold());
					n.setPosition(new Point(spawnData.getX(), spawnData.getY()));
					n.setCy(spawnData.getCy());
					n.setRx(spawnData.getRx0(), spawnData.getRx1());
					n.setStance((byte) (spawnData.isF() ? 0 : 1));
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
		if (stats.getTimeLimit() > 0 && stats.getForcedReturn() != GlobalConstants.NULL_MAP)
			timeLimitTasks = new ConcurrentHashMap<GameCharacter, ScheduledFuture<?>>();
		if (stats.getDecHp() > 0)
			decHpTasks = new ConcurrentHashMap<GameCharacter, ScheduledFuture<?>>();
	}

	public MapStats getStaticData() {
		return stats;
	}

	public int getDataId() {
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
	public MapEntity getEntityById(EntityType type, int eId) {
		return entPools.get(type).getByIdSafely(eId);
	}

	/**
	 * Returns a copy of all the entities in the specified pool type on this
	 * map at the time of the method call. Future changes to this map's entity
	 * pool will not alter the Collection that is returned from this method, and
	 * changes made to the returned Collection will not be reflected in this
	 * map's entity pool. Thus, the returned Collection is also thread-safe.
	 * @param type
	 * @return 
	 */
	public Collection<MapEntity> getAllEntities(EntityType type) {
		EntityPool pool = entPools.get(type);
		pool.lockRead();
		try {
			return new ArrayList<MapEntity>(entPools.get(type).allEnts());
		} finally {
			pool.unlockRead();
		}
	}

	public int getPlayerCount() {
		return entPools.get(EntityType.PLAYER).getSizeSafely();
	}

	private void updateMonsterController(Mob monster) {
		if (!monster.isAlive())
			return;
		GameCharacter controller = monster.getController();
		if (controller != null)
			if (controller.getMap() == this)
				return;
			else
				controller = null;
		int minControlled = Integer.MAX_VALUE, count;
		EntityPool players = entPools.get(EntityType.PLAYER);
		players.lockRead();
		try {
			for (MapEntity ent : players.allEnts()) {
				GameCharacter p = (GameCharacter) ent;
				if (p.isVisible() && (count = p.getControlledMobs().getSizeWhenSafe()) < minControlled) {
					minControlled = count;
					controller = p;
				}
			}
		} finally {
			players.unlockRead();
		}
		if (controller != null) {
			monster.setController(controller);
			controller.controlMonster(monster);
			boolean aggro = monster.isFirstAttack();
			monster.setControllerHasAggro(aggro);
			monster.setControllerKnowsAboutAggro(aggro);
			controller.getClient().getSession().send(GamePackets.writeShowAndControlMonster(monster, aggro));
		}
	}

	private void spawnEntityInternal(MapEntity ent, boolean newSpawn) {
		EntityPool pool = entPools.get(ent.getEntityType());
		pool.lockWrite();
		try {
			ent.setId(pool.nextEntId());
			pool.add(ent);
		} finally {
			pool.unlockWrite();
		}
		if (ent.isVisible())
			sendToAll(newSpawn ? ent.getShowNewSpawnMessage() : ent.getShowExistingSpawnMessage());
	}

	public final void spawnEntity(MapEntity ent) {
		spawnEntityInternal(ent, true);
	}

	public void spawnExistingEntity(MapEntity ent) {
		spawnEntityInternal(ent, false);
	}

	private void sendEntityData(MapEntity ent, GameCharacter p) {
		if (ent.isVisible()) {
			p.getClient().getSession().send(ent.getShowExistingSpawnMessage());
			switch (ent.getEntityType()) {
				case NPC:
					if (((Npc) ent).isPlayerNpc())
						p.getClient().getSession().send(GamePackets.writePlayerNpcLook((PlayerNpc) ent));
					break;
				case MONSTER:
					updateMonsterController((Mob) ent);
					break;
			}
		}
	}

	public void spawnPlayer(final GameCharacter p) {
		EntityPool players = entPools.get(EntityType.PLAYER);
		players.lockWrite();
		try { //write lock allows us to read in mutex, so no need for a readLock
			if (p.isVisible()) //show ourself to other clients if we are not hidden
				sendToAll(p.getShowNewSpawnMessage());
			players.add(p);
			for (EntityPool pool : entPools.values()) {
				pool.lockRead();
				try {
					for (MapEntity ent : pool.allEnts())
						if (p != ent)
							sendEntityData(ent, p);
				} finally {
					pool.unlockRead();
				}
			}
		} finally {
			players.unlockWrite();
		}
		for (PlayerSkillSummon summon : p.getAllSummons().values())
			spawnExistingEntity(summon);
		if (stats.hasClock())
			p.getClient().getSession().send(GamePackets.writeClock());
		if (timeLimitTasks != null) {
			//TODO: I heard that ScheduledFutures still hold onto strong references
			//when canceled, so should we just use a WeakReference to player?
			ScheduledFuture<?> future = Scheduler.getInstance().runAfterDelay(new Runnable() {
				@Override
				public void run() {
					p.changeMap(stats.getForcedReturn());
					p.getClient().getSession().send(GamePackets.writeTimer(0));
				}
			}, stats.getTimeLimit() * 1000);
			p.getClient().getSession().send(GamePackets.writeTimer(stats.getTimeLimit()));
			timeLimitTasks.put(p, future);
		}
		if (decHpTasks != null) {
			ScheduledFuture<?> future = Scheduler.getInstance().runRepeatedly(new Runnable() {
				@Override
				public void run() {
					p.doDecHp(stats.getProtectItem(), stats.getDecHp());
				}
			}, 10000, 10000);
			decHpTasks.put(p, future);
		}
		//it would be too wasteful to save the "fieldType" properties from the
		//XMLs just to check if it's equal to 81 or 82 here, so check manually
		switch (stats.getMapId()) {
			case 1:
			case 2:
			case 809000101:
			case 809000201:
				p.getClient().getSession().send(writeForceMapEquip());
				break;
		}
		p.pullPartyHp();
		p.pushHpToParty();
	}

	public final void spawnMonster(Mob monster) {
		spawnEntity(monster);
		updateMonsterController(monster);
		monsters.incrementAndGet();
	}

	/**
	 * Spawns an item drop entity on this map.
	 * Make sure you call ItemDrop.init with the correct data on the passed
	 * ItemDrop entity before calling this method.
	 * @param d the ItemDrop entity to spawn.
	 */
	private void drop(ItemDrop d) {
		spawnEntity(d);
		if (d.getDropType() == ItemDrop.ITEM)
			checkForItemTriggeredReactors(d);
		final Integer eid = Integer.valueOf(d.getId());
		Scheduler.getInstance().runAfterDelay(new Runnable() {
			@Override
			public void run() {
				ItemDrop d = (ItemDrop) entPools.get(EntityType.DROP).getByIdSafely(eid);
				if (d != null) {
					d.expire();
					destroyEntity(d);
				}
			}
		}, DROP_EXPIRE); //expire after 1 minute
	}

	private Point calcDropPos(Point initial, Point fallback) {
		Point ret = calcPointBelow(new Point(initial.x, initial.y - 99));
		return ret != null ? ret : fallback;
	}

	public void drop(ItemDrop drop, int dropperMobId, Point dropperPos, Point dropPos, byte pickupAllow, int owner) {
		drop.init(dropperMobId, owner, dropPos, dropperPos, pickupAllow);
		drop(drop);
	}

	public void drop(ItemDrop drop, int dropperMobId, GameCharacter dropper, byte pickupAllow, int owner, boolean undroppable) {
		Point pos = dropper.getPosition();
		drop.init(dropperMobId, owner, calcDropPos(pos, pos), pos, pickupAllow);
		if (!undroppable)
			drop(drop);
		else //sendToAll or just dropper.getClient().getSession().send(...)?
			sendToAll(drop.getDisappearMessage());
	}

	public void drop(List<ItemDrop> drops, MapEntity ent, byte pickupAllow, int owner) {
		Point entPos = ent.getPosition(), dropPos = new Point(entPos);
		int entX = entPos.x;
		int width = pickupAllow != ItemDrop.PICKUP_EXPLOSION ? 25 : 40;
		int dropNum = 0, delta;
		for (ItemDrop drop : drops) {
			dropNum++;
			delta = width * (dropNum / 2);
			if (dropNum % 2 == 0) //drop even numbered drops right
				dropPos.x = entX + delta;
			else //drop odd numbered drops left
				dropPos.x = entX - delta;
			drop.init((ent instanceof Mob) ? ent.getId() : 0, owner, calcDropPos(dropPos, entPos), entPos, pickupAllow);
			drop(drop);
		}
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

	public void spawnMist(final Mist mist, final int duration, final ScheduledFuture<?> periodicTask) {
		spawnEntity(mist);
		Scheduler.getInstance().runAfterDelay(new Runnable() {
			@Override
			public void run() {
				destroyEntity(mist);
				if (periodicTask != null)
					periodicTask.cancel(false);
			}
		}, duration);
	}

	public void spawnPlayerNpc(PlayerNpc n) {
		spawnEntity(n);
		sendToAll(GamePackets.writePlayerNpcLook(n));
	}

	public void destroyEntity(MapEntity ent) {
		entPools.get(ent.getEntityType()).removeByIdSafely(Integer.valueOf(ent.getId()));
		if (ent.isVisible())
			sendToAll(ent.getDestructionMessage());
	}

	public void removePlayer(GameCharacter p) {
		destroyEntity(p);
		for (PlayerSkillSummon summon : p.getAllSummons().values())
			destroyEntity(summon);
		LockableList<Mob> controlledMobs = p.getControlledMobs();
		controlledMobs.lockWrite();
		try {
			for (Mob m : controlledMobs) {
				m.setController(null);
				m.setControllerHasAggro(false);
				m.setControllerKnowsAboutAggro(false);
				updateMonsterController(m);
			}
			controlledMobs.clear();
		} finally {
			controlledMobs.unlockWrite();
		}
		if (timeLimitTasks != null) {
			ScheduledFuture<?> future = timeLimitTasks.remove(p);
			if (future != null)
				future.cancel(false);
		}
		if (decHpTasks != null) {
			ScheduledFuture<?> future = decHpTasks.remove(p);
			if (future != null)
				future.cancel(false);
		}
	}

	public void killMonster(Mob monster, GameCharacter killer) {
		GameCharacter controller = monster.getController();
		if (controller != null)
			controller.uncontrolMonster(monster);
		monster.died(killer);
		destroyEntity(monster);
		monsters.decrementAndGet();
	}

	public void removeMonster(Mob monster) {
		GameCharacter controller = monster.getController();
		if (controller != null)
			controller.uncontrolMonster(monster);
		monster.fireDeathEventNoRewards();
		destroyEntity(monster);
		monsters.decrementAndGet();
	}

	public void destroyReactor(final Reactor r) {
		destroyEntity(r);
		if (r.getDelay() > 0) {
			Scheduler.getInstance().runAfterDelay(new Runnable() {
				@Override
				public void run() {
					respawnReactor(r);
				}
			}, r.getDelay() * 1000);
		}
	}

	public void pickUpDrop(ItemDrop d, GameCharacter p) {
		if (d.getDropType() == ItemDrop.MESOS) {
			if (p.gainMesos(d.getDataId(), false, true)) {
				d.pickUp(p.getId());
				destroyEntity(d);
			} else {
				p.getClient().getSession().send(GamePackets.writeInventoryNoChange());
				p.getClient().getSession().send(GamePackets.writeShowInventoryFull());
			}
		} else {
			InventorySlot pickedUp = d.getItem();
			int itemid = pickedUp.getDataId();
			short qty = pickedUp.getQuantity();
			InventoryType type = InventoryTools.getCategory(d.getItem().getDataId());
			Inventory inv = p.getInventory(type);
			if (InventoryTools.canFitEntirely(inv, itemid, qty, false)) {
				d.pickUp(p.getId());
				destroyEntity(d);
				if (!ItemDataLoader.getInstance().isConsumeOnPickup(itemid)) {
					UpdatedSlots changedSlots = InventoryTools.addToInventory(inv, pickedUp, qty, false);
					ClientSession<?> ses = p.getClient().getSession();
					short pos;
					for (Short s : changedSlots.modifiedSlots) {
						pos = s.shortValue();
						ses.send(GamePackets.writeInventoryUpdateSlotQuantity(type, pos, inv.get(pos)));
					}
					for (Short s : changedSlots.addedOrRemovedSlots) {
						pos = s.shortValue();
						ses.send(GamePackets.writeInventoryAddSlot(type, pos, inv.get(pos)));
					}
					p.itemCountChanged(itemid);
				} else {
					ItemTools.useItem(p, itemid);
				}
				p.getClient().getSession().send(GamePackets.writeShowItemGain(itemid, qty));
			} else {
				p.getClient().getSession().send(GamePackets.writeInventoryNoChange());
				p.getClient().getSession().send(GamePackets.writeShowInventoryFull());
			}
		}
	}

	public void mesoExplosion(ItemDrop d, GameCharacter p) {
		if (d.getDropType() == ItemDrop.MESOS) {
			d.explode();
			destroyEntity(d);
		}
	}

	public void respawnReactor(Reactor r) {
		r.reset();
		spawnEntity(r);
	}

	public void respawnReactors() {
		for (MapEntity ent : getAllEntities(EntityType.REACTOR)) {
			Reactor r = (Reactor) ent;
			if (!r.isAlive())
				spawnEntity(r);
			r.reset();
		}
	}

	public void respawnMobs() {
		if (entPools.get(EntityType.PLAYER).getSizeSafely() == 0)
			return;
		monsterSpawns.lockRead();
		try {
			if (monsterSpawns.isEmpty())
				return;
			Collections.sort(monsterSpawns);
			int numShouldSpawn = Math.round((monsterSpawns.size() - monsters.get()) * stats.getMobRate());
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

	private void checkForItemTriggeredReactors(ItemDrop d) {
		GameCharacter p = (GameCharacter) getEntityById(EntityType.PLAYER, d.getOwner());
		int itemId = d.getDataId();
		short quantity = d.getItem().getQuantity();
		Point pos = d.getPosition();
		EntityPool reactors = entPools.get(EntityType.REACTOR);
		reactors.lockRead();
		try {
			for (MapEntity ent : reactors.allEnts()) {
				Reactor r = (Reactor) ent;
				Pair<Integer, Short> itemTrigger = r.getItemTrigger();
				if (itemTrigger != null
						&& itemTrigger.left.intValue() == itemId
						&& itemTrigger.right.shortValue() == quantity
						&& r.getItemTriggerZone().contains(pos))
					r.hit(p, (byte) 0);
			}
		} finally {
			reactors.unlockRead();
		}
	}

	public void unhidePlayer(GameCharacter p) {
		sendToAll(p.getShowExistingSpawnMessage(), p);
		EntityPool mobs = entPools.get(EntityType.MONSTER);
		mobs.lockRead();
		try {
			for (MapEntity ent : mobs.allEnts())
				updateMonsterController((Mob) ent);
		} finally {
			mobs.unlockRead();
		}
		for (PlayerSkillSummon summon : p.getAllSummons().values())
			sendToAll(summon.getShowExistingSpawnMessage(), p);
		p.pushHpToParty();
	}

	public void hidePlayer(GameCharacter p) {
		sendToAll(p.getDestructionMessage(), p);
	}

	public void playerMoved(GameCharacter p, List<LifeMovementFragment> moves, Point startPos) {
		sendToAll(writePlayerMovement(p, moves, startPos), p);
	}

	public void summonMoved(GameCharacter p, PlayerSkillSummon s, List<LifeMovementFragment> moves, Point startPos) {
		sendToAll(writeSummonMovement(p, s, moves, startPos), p);
	}

	public void monsterMoved(GameCharacter p, Mob m, List<LifeMovementFragment> moves, boolean useSkill, byte skill, short skillId, byte s2, byte s3, byte s4, Point startPos) {
		sendToAll(writeMonsterMovement(m, useSkill, skill, skillId, s2, s3, s4, startPos, moves), p);
	}

	public void damageMonster(GameCharacter p, Mob m, int damage) {
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

	public boolean enterPortal(GameCharacter p, String portalName) {
		for (PortalData portal : stats.getPortals().values())
			if (portal.getPortalName().equals(portalName))
				return enterPortal(p, portal);
		return false;
	}

	public boolean enterPortal(GameCharacter p, byte portalId) {
		PortalData portal = stats.getPortals().get(Byte.valueOf(portalId));
		return (portal != null ? enterPortal(p, portal) : false);
	}

	private boolean enterPortal(GameCharacter p, PortalData portal) {
		String scriptName = portalOverrides.get(portal.getPortalName());
		if (scriptName == null || scriptName.isEmpty())
			scriptName = portal.getScript();
		if (scriptName != null && !scriptName.isEmpty()) {
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

	public void overridePortal(String portalName, String script) {
		portalOverrides.put(portalName, script);
	}

	public void revertPortal(String portalName) {
		portalOverrides.remove(portalName);
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
		EntityPool players = entPools.get(EntityType.PLAYER);
		players.lockRead();
		try {
			for (MapEntity p : players.allEnts())
				((GameCharacter) p).getClient().getSession().send(message);
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
	public void sendToAll(byte[] message, GameCharacter source) {
		EntityPool players = entPools.get(EntityType.PLAYER);
		players.lockRead();
		try {
			for (MapEntity p : players.allEnts())
				if (!p.equals(source))
					((GameCharacter) p).getClient().getSession().send(message);
		} finally {
			players.unlockRead();
		}
	}

	public List<MapEntity> getMapEntitiesInRect(Rectangle box, Set<EntityType> types) {
		List<MapEntity> ret = new LinkedList<MapEntity>();
		for (EntityType type : types) {
			EntityPool pool = entPools.get(type);
			pool.lockRead();
			try {
				for (MapEntity ent : pool.allEnts())
					if (box.contains(ent.getPosition()))
						ret.add(ent);
			} finally {
				pool.unlockRead();
			}
		}
		return ret;
	}

	public List<MapEntity> getMapEntitiesInRect(Rectangle box) {
		return getMapEntitiesInRect(box, EnumSet.allOf(EntityType.class));
	}

	public boolean isChairOccupied(short chairId) {
		return occupiedChairs.contains(Short.valueOf(chairId));
	}

	public void occupyChair(short chairId) {
		occupiedChairs.add(Short.valueOf(chairId));
	}

	public void leaveChair(short chairId) {
		occupiedChairs.remove(Short.valueOf(chairId));
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
			mob.addListener(new MobDeathListener() {
				@Override
				public void monsterKilled(GameCharacter highestAttacker, GameCharacter finalAttacker) {
					//this has to be atomic, so I had to do away with assigning
					//nextPossibleSpawn more than once.
					if (mobTime > 0) {
						nextPossibleSpawn = System.currentTimeMillis() + mobTime * 1000;
					} else {
						Integer deathTime = mobStats.getDelays().get("die1");
						nextPossibleSpawn = System.currentTimeMillis()
								+ (deathTime != null ? deathTime.intValue() : 0);
					}
					spawnedMonsters.decrementAndGet();
				}
			});
			if (mobTime == 0)
				nextPossibleSpawn = System.currentTimeMillis() + 5000;
			return mob;
		}

		@Override
		public int compareTo(MonsterSpawn m) {
			int aliveDelta = spawnedMonsters.get() - m.spawnedMonsters.get();
			if (aliveDelta == 0)
				return (int) (nextPossibleSpawn - m.nextPossibleSpawn);
			return aliveDelta;
		}
	}

	private static byte[] writePlayerMovement(GameCharacter p, List<LifeMovementFragment> moves, Point startPos) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.MOVE_PLAYER);
		lew.writeInt(p.getId());
		lew.writePos(startPos);
		GamePackets.writeSerializedMovements(lew, moves);
		return lew.getBytes();
	}

	private static byte[] writeSummonMovement(GameCharacter p, PlayerSkillSummon s, List<LifeMovementFragment> moves, Point startPos) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.MOVE_SUMMON);
		lew.writeInt(p.getId());
		lew.writeInt(s.getId());
		lew.writePos(startPos);
		GamePackets.writeSerializedMovements(lew, moves);
		return lew.getBytes();
	}

	private static byte[] writeMonsterMovement(Mob m, boolean useSkill, byte skill, short skillId, byte skillLevel, byte s3, byte s4, Point startPos, List<LifeMovementFragment> moves) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.MOVE_MONSTER);
		lew.writeInt(m.getId());
		lew.writeBool(useSkill);
		lew.writeByte(skill);
		lew.writeByte((byte) skillId);
		lew.writeByte(skillLevel);
		lew.writeByte(s3);
		lew.writeByte(s4); //or is this just 0?
		lew.writePos(startPos);
		GamePackets.writeSerializedMovements(lew, moves);
		return lew.getBytes();
	}

	private static byte[] writeForceMapEquip() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.SHOW_EQUIP_EFFECT);
		return lew.getBytes();
	}

	private static class EntityPool {
		private final LockableMap<Integer, MapEntity> entities;
		//no need to be Atomic because it always is locked when accessed...
		private int nextEntId;

		public EntityPool() {
			this.entities = new LockableMap<Integer, MapEntity>(new LinkedHashMap<Integer, MapEntity>());
			this.nextEntId = 0;
		}

		public int nextEntId() {
			//reserve eId of 0 = non-existant entity
			while (entities.containsKey(Integer.valueOf(++nextEntId)) || nextEntId == 0); //avoid collisions/entId=0 when the entity id overflows
			return nextEntId;
		}

		public MapEntity getByIdSafely(int entityId) {
			return entities.getWhenSafe(Integer.valueOf(entityId));
		}

		public void add(MapEntity ent) {
			entities.put(Integer.valueOf(ent.getId()), ent);
		}

		public void removeByIdSafely(int entityId) {
			entities.removeWhenSafe(Integer.valueOf(entityId));
		}

		public int getSizeSafely() {
			return entities.getSizeWhenSafe();
		}

		public Collection<MapEntity> allEnts() {
			return entities.values();
		}

		public void lockRead() {
			entities.lockRead();
		}

		public void unlockRead() {
			entities.unlockRead();
		}

		public void lockWrite() {
			entities.lockWrite();
		}

		public void unlockWrite() {
			entities.unlockWrite();
		}
	}
}
