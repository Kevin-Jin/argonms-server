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
import argonms.character.inventory.InventoryTools.UpdatedSlots;
import argonms.character.inventory.ItemTools;
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
import argonms.map.MapEntity.EntityType;
import argonms.map.movement.LifeMovementFragment;
import argonms.map.entity.ItemDrop;
import argonms.map.entity.Mist;
import argonms.map.entity.Mob;
import argonms.map.entity.Mob.MobDeathHook;
import argonms.map.entity.Npc;
import argonms.map.entity.PlayerNpc;
import argonms.map.entity.Reactor;
import argonms.net.external.ClientSendOps;
import argonms.net.external.CommonPackets;
import argonms.tools.Timer;
import argonms.tools.collections.LockableList;
import argonms.tools.collections.LockableMap;
import argonms.tools.collections.Pair;
import argonms.tools.output.LittleEndianByteArrayWriter;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
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
	 * The maximum distance that any character could see.
	 * Used to be 850 squared, but the introduction of the jump down movement
	 * made that constant insufficient, so the maximum distance that a ranged
	 * entity can be from the player before it is removed (and the minimum
	 * distance that a ranged entity needs to be from the player before it is
	 * sent to their client) was increased significantly.
	 *
	 * Hopefully should fix those disappearing ranged map entity bugs when
	 * jumping down. Perhaps this value is too big or too small, but only tests
	 * would tell.
	 *
	 * An inappropriate value for this constant can affect bandwidth usage in
	 * both ways. A too large value will result in ranged entities (items, mobs,
	 * etc) being sent to the client even if they never will be able to see it,
	 * so bandwidth will be wasted. A smaller value will result in ranged
	 * entities being resent frequently if the player walks around the map many
	 * times (and will result in "disappearing" glitches if too small).
	 *
	 * This value was chosen experimentally from the area I found with one of
	 * the highest jump down distances - The Field South of Ellinia (100050000)
	 * on the right side of the map around where the portal is.
	 */
	private static final double MAX_VIEW_RANGE_SQ = 2100 * 2100;
	/**
	 * Amount of time in milliseconds that a dropped item will remain on the map
	 */
	private static final int DROP_EXPIRE = 60000;
	private final MapStats stats;
	private final Map<EntityType, EntityPool> entPools;
	private final LockableList<MonsterSpawn> monsterSpawns;
	private final AtomicInteger monsters;
	private ConcurrentHashMap<Player, ScheduledFuture<?>> timeLimitTasks;
	private ConcurrentHashMap<Player, ScheduledFuture<?>> decHpTasks;

	protected GameMap(MapStats stats) {
		this.stats = stats;
		this.entPools = new EnumMap<EntityType, EntityPool>(EntityType.class);
		for (EntityType type : EntityType.values())
			entPools.put(type, new EntityPool());
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
			timeLimitTasks = new ConcurrentHashMap<Player, ScheduledFuture<?>>();
		if (stats.getDecHp() > 0)
			decHpTasks = new ConcurrentHashMap<Player, ScheduledFuture<?>>();
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

	public int getPlayerCount() {
		return entPools.get(EntityType.PLAYER).getSizeSafely();
	}

	private void updateMonsterController(Mob monster) {
		if (!monster.isAlive())
			return;
		Player controller = monster.getController();
		if (controller != null)
			if (controller.getMap() == this)
				return;
			else
				controller = null;
		int minControlled = Integer.MAX_VALUE;
		EntityPool players = entPools.get(EntityType.PLAYER);
		players.lockRead();
		try {
			for (MapEntity ent : players.allEnts()) {
				Player p = (Player) ent;
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
			controller.getClient().getSession().send(CommonPackets.writeShowAndControlMonster(monster, aggro));
		}
	}

	public final void spawnEntity(MapEntity ent) {
		EntityPool pool = entPools.get(ent.getEntityType());
		pool.lockWrite();
		try {
			ent.setId(pool.nextEntId());
			pool.add(ent);
		} finally {
			pool.unlockWrite();
		}
		if (ent.isVisible()) {
			if (ent.isNonRangedType()) {
				sendToAll(ent.getCreationMessage());
			} else {
				byte[] packet = ent.getCreationMessage();
				EntityPool players = entPools.get(EntityType.PLAYER);
				players.lockRead();
				try {
					for (MapEntity pEnt : players.allEnts()) {
						Player p = (Player) pEnt;
						if (entityInRange(ent, p)) {
							p.addToVisibleMapEntities(ent);
							p.getClient().getSession().send(packet);
						}
					}
				} finally {
					players.unlockRead();
				}
			}
		}
	}

	private void sendEntityData(MapEntity ent, Player p) {
		if (ent.isVisible()) {
			if (ent.isNonRangedType()) {
				p.getClient().getSession().send(ent.getShowEntityMessage());
				if (ent.getEntityType() == EntityType.NPC && ((Npc) ent).isPlayerNpc())
					p.getClient().getSession().send(CommonPackets.writePlayerNpcLook((PlayerNpc) ent));
			} else {
				if (ent.isAlive()) {
					if (ent.getEntityType() == EntityType.MONSTER)
						updateMonsterController((Mob) ent);
					if (entityInRange(ent, p) && !p.seesEntity(ent)) {
						p.getClient().getSession().send(ent.getShowEntityMessage());
						p.addToVisibleMapEntities(ent);
					}
				}
			}
		}
	}

	public void spawnPlayer(final Player p) {
		EntityPool players = entPools.get(EntityType.PLAYER);
		players.lockWrite();
		try { //write lock allows us to read in mutex, so no need for a readLock
			if (p.isVisible()) //show ourself to other clients if we are not hidden
				sendToAll(p.getCreationMessage());
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
		if (timeLimitTasks != null) {
			//TODO: I heard that ScheduledFutures still hold onto strong references
			//when canceled, so should we just use a WeakReference to player?
			ScheduledFuture<?> future = Timer.getInstance().runAfterDelay(new Runnable() {
				public void run() {
					p.changeMap(stats.getForcedReturn());
					p.getClient().getSession().send(writeShowTimeLimit(0));
				}
			}, stats.getTimeLimit() * 1000);
			p.getClient().getSession().send(writeShowTimeLimit(stats.getTimeLimit()));
			timeLimitTasks.put(p, future);
		}
		if (decHpTasks != null) {
			ScheduledFuture<?> future = Timer.getInstance().runRepeatedly(new Runnable() {
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
		Timer.getInstance().runAfterDelay(new Runnable() {
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

	public void drop(ItemDrop drop, Point dropperPos, Point dropPos, byte pickupAllow, int owner) {
		drop.init(owner, dropPos, dropperPos, pickupAllow);
		drop(drop);
	}

	public void drop(ItemDrop drop, Player dropper, byte pickupAllow, int owner, boolean undroppable) {
		Point pos = dropper.getPosition();
		drop.init(owner, calcDropPos(pos, pos), pos, pickupAllow);
		if (!undroppable)
			drop(drop);
		else //sendToAll or just dropper.getClient().getSession().send(...)?
			sendToAll(drop.getDisappearMessage());
	}

	//FIXME: temporary fix to the client freeze when expiring more than one item
	//drop from the same monster at the same time. I believe it is a thread
	//concurrency issue (although this is faster than having each drop have its
	//own expiration Runnable if we called drop(ItemDrop), the issue at hand is
	//the fact we have the concurrency issues, not the performance).
	public void drop(List<ItemDrop> drops, MapEntity ent, byte pickupAllow, int owner) {
		Point entPos = ent.getPosition(), dropPos = new Point(entPos);
		int entX = entPos.x;
		int width = pickupAllow != ItemDrop.PICKUP_EXPLOSION ? 25 : 40;
		int dropNum = 0, delta;
		final List<Integer> dropped = new ArrayList<Integer>(drops.size());
		for (ItemDrop drop : drops) {
			dropNum++;
			delta = width * (dropNum / 2);
			if (dropNum % 2 == 0) //drop even numbered drops right
				dropPos.x = entX + delta;
			else //drop odd numbered drops left
				dropPos.x = entX - delta;
			drop.init(owner, calcDropPos(dropPos, entPos), entPos, pickupAllow);
			spawnEntity(drop);
			if (drop.getDropType() == ItemDrop.ITEM)
				checkForItemTriggeredReactors(drop);
			dropped.add(Integer.valueOf(drop.getId()));
		}
		//expire every drop at once in the same Runnable rather than have a
		//separate Runnable job for each drop running in parallel.
		Timer.getInstance().runAfterDelay(new Runnable() {
			public void run() {
				for (Integer eId : dropped) {
					ItemDrop drop = (ItemDrop) entPools.get(EntityType.DROP).getByIdSafely(eId.intValue());
					if (drop != null) {
						drop.expire();
						destroyEntity(drop);
					}
				}
			}
		}, DROP_EXPIRE);
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
					List<MapEntity> affectedMonsters = getMapEntitiesInRect(mist.getBox(), EnumSet.of(EntityType.MONSTER));
					for (MapEntity mo : affectedMonsters) {
						if (mist.shouldHurt()) {
							Mob monster = (Mob) mo;
							//TODO: implement poison mist
							//MonsterStatusEffect poisonEffect = new MonsterStatusEffect(Collections.singletonMap(MonsterStatus.POISON, 1), mist.getSourceSkill(), false);
							//monster.applyStatus(mist.getOwner(), poisonEffect, true, duration, false);
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

	public void spawnPlayerNpc(PlayerNpc n) {
		spawnEntity(n);
		EntityPool players = entPools.get(EntityType.PLAYER);
		players.lockRead();
		try {
			for (MapEntity p : players.allEnts())
				((Player) p).getClient().getSession().send(CommonPackets.writePlayerNpcLook(n));
		} finally {
			players.unlockRead();
		}
	}

	public void destroyEntity(MapEntity ent) {
		entPools.get(ent.getEntityType()).removeByIdSafely(Integer.valueOf(ent.getId()));
		if (ent.isVisible()) {
			if (ent.isNonRangedType()) {
				sendToAll(ent.getDestructionMessage());
			} else {
				byte[] packet = ent.getDestructionMessage();
				EntityPool players = entPools.get(EntityType.PLAYER);
				players.lockRead();
				try {
					for (MapEntity pEnt : players.allEnts()) {
						Player p = (Player) pEnt;
						if (entityInRange(ent, p)) {
							p.removeVisibleMapEntity(ent);
							p.getClient().getSession().send(packet);
						}
					}
				} finally {
					players.unlockRead();
				}
			}
		}
	}

	public void removePlayer(Player p) {
		destroyEntity(p);
		for (Mob m : p.getControlledMobs()) {
			m.setController(null);
			m.setControllerHasAggro(false);
			m.setControllerKnowsAboutAggro(false);
			updateMonsterController(m);
		}
		p.clearControlledMobs();
		p.clearVisibleEntities();
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

	public void killMonster(Mob monster, Player killer) {
		monster.died(killer);
		destroyEntity(monster);
		monsters.decrementAndGet();
	}

	public void destroyReactor(final Reactor r) {
		destroyEntity(r);
		if (r.getDelay() > 0) {
			Timer.getInstance().runAfterDelay(new Runnable() {
				public void run() {
					respawnReactor(r);
				}
			}, r.getDelay() * 1000);
		}
	}

	public void pickUpDrop(ItemDrop d, Player p) {
		if (d.getDropType() == ItemDrop.MESOS) {
			if (p.gainMesos(d.getDataId(), false, true)) {
				d.pickUp(p.getId());
				destroyEntity(d);
			} else {
				p.getClient().getSession().send(CommonPackets.writeInventoryFull());
			}
		} else {
			InventorySlot pickedUp = d.getItem();
			int itemid = pickedUp.getDataId();
			short qty = pickedUp.getQuantity();
			InventoryType type = InventoryTools.getCategory(d.getItem().getDataId());
			Inventory inv = p.getInventory(type);
			if (InventoryTools.canFitEntirely(inv, itemid, qty)) {
				d.pickUp(p.getId());
				destroyEntity(d);
				if (!ItemDataLoader.getInstance().isConsumeOnPickup(itemid)) {
					UpdatedSlots changedSlots = InventoryTools.addToInventory(inv, pickedUp, qty);
					short pos;
					InventorySlot slot;
					for (Short s : changedSlots.modifiedSlots) {
						pos = s.shortValue();
						slot = inv.get(pos);
						p.getClient().getSession().send(CommonPackets.writeInventorySlotUpdate(type, pos, slot));
					}
					for (Short s : changedSlots.addedOrRemovedSlots) {
						pos = s.shortValue();
						slot = inv.get(pos);
						p.getClient().getSession().send(CommonPackets.writeInventoryAddSlot(type, pos, slot));
					}
					p.gainedItem(itemid);
				} else {
					ItemTools.useItem(p, itemid);
				}
				p.getClient().getSession().send(CommonPackets.writeShowItemGain(itemid, qty));
			} else {
				p.getClient().getSession().send(CommonPackets.writeInventoryFull());
				p.getClient().getSession().send(CommonPackets.writeShowInventoryFull());
			}
		}
	}

	public void mesoExplosion(ItemDrop d, Player p) {
		if (d.getDropType() == ItemDrop.MESOS) {
			p.gainMesos(d.getDataId(), false);
			d.explode();
			destroyEntity(d);
		}
	}

	public void respawnReactor(Reactor r) {
		r.reset();
		spawnEntity(r);
	}

	//FIXME: I think all mobs spawn only on the top spawnpoint. o.O
	public void respawnMobs() {
		if (entPools.get(EntityType.PLAYER).getSizeSafely() == 0)
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

	private void checkForItemTriggeredReactors(ItemDrop d) {
		Player p = (Player) getEntityById(EntityType.PLAYER, d.getOwner());
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

	public void unhidePlayer(Player p) {
		sendToAll(p.getShowEntityMessage());
		EntityPool mobs = entPools.get(EntityType.MONSTER);
		mobs.lockRead();
		try {
			for (MapEntity ent : mobs.allEnts())
				updateMonsterController((Mob) ent);
		} finally {
			mobs.unlockRead();
		}
	}

	public void hidePlayer(Player p) {
		sendToAll(p.getDestructionMessage(), p);
	}

	public void playerMoved(Player p, List<LifeMovementFragment> moves, Point startPos) {
		sendToAll(writePlayerMovement(p, moves, startPos), p);

		LockableList<MapEntity> visible = p.getVisibleMapEntities();
		visible.lockWrite();
		try {
			Iterator<MapEntity> iter = visible.iterator();
			while (iter.hasNext()) {
				MapEntity ent = iter.next();
				if (!entityInRange(ent, p)) {
					p.getClient().getSession().send(ent.getOutOfViewMessage());
					iter.remove();
				}
			}
			//TODO: once we implement all entity types and the ranged flags of each
			//are stable, make a constant EnumSet of EntityTypes that only contain
			//non ranged types and only iterate over the pools of those types
			for (EntityPool pool : entPools.values()) {
				pool.lockRead();
				try {
					for (MapEntity ent : pool.allEnts()) {
						if (!ent.isNonRangedType()) {
							if (entityInRange(ent, p) && !p.seesEntity(ent) && ent.isVisible() && ent.isAlive()) {
								p.getClient().getSession().send(ent.getShowEntityMessage());
								visible.add(ent);
							}
						}
					}
				} finally {
					pool.unlockRead();
				}
			}
		} finally {
			visible.unlockWrite();
		}
	}

	private void rangedEntityMoved(MapEntity ent) {
		EntityPool players = entPools.get(EntityType.PLAYER);
		byte[] showPacket = ent.getShowEntityMessage();
		byte[] removePacket = ent.getDestructionMessage();
		players.lockRead();
		try {
			for (MapEntity pEnt : players.allEnts()) {
				Player p = (Player) pEnt;
				if (entityInRange(ent, p)) {
					if (!p.seesEntity(p)) {
						p.addToVisibleMapEntities(ent);
						p.getClient().getSession().send(showPacket);
					}
				} else {
					if (p.seesEntity(p)) {
						p.removeVisibleMapEntity(ent);
						p.getClient().getSession().send(removePacket);
					}
				}
			}
		} finally {
			players.unlockRead();
		}
	}

	public void monsterMoved(Player p, Mob m, List<LifeMovementFragment> moves, boolean useSkill, byte skill, short skillId, byte s2, byte s3, byte s4, Point startPos) {
		rangedEntityMoved(m);
		sendToAll(writeMonsterMovement(useSkill, skill, skillId, s2, s3, s4, m.getId(), startPos, moves), m.getPosition(), p);
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
				return enterPortal(p, portal);
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
		EntityPool players = entPools.get(EntityType.PLAYER);
		players.lockRead();
		try {
			for (MapEntity p : players.allEnts())
				((Player) p).getClient().getSession().send(message);
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
		EntityPool players = entPools.get(EntityType.PLAYER);
		players.lockRead();
		try {
			for (MapEntity p : players.allEnts())
				if (!p.equals(source))
					((Player) p).getClient().getSession().send(message);
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
		EntityPool players = entPools.get(EntityType.PLAYER);
		players.lockRead();
		try {
			for (MapEntity p : players.allEnts())
				if (!p.equals(source))
					if (center.distanceSq(p.getPosition()) <= range)
						((Player) p).getClient().getSession().send(message);
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

	private static boolean entityInRange(MapEntity ent, Player p) {
		return (p.getPosition().distanceSq(ent.getPosition()) <= MAX_VIEW_RANGE_SQ);
	}

	private List<MapEntity> getMapEntitiesInRect(Rectangle box, Set<EntityType> types) {
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
				public void monsterKilled(Player highestAttacker, Player finalAttacker) {
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

	private static byte[] writeShowTimeLimit(int seconds) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		lew.writeShort(ClientSendOps.CLOCK);
		lew.writeByte((byte) 2);
		lew.writeInt(seconds);
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

		//TODO: reuse entity ids of the dead
		public int nextEntId() {
			//preincrement so we can reserve eId of 0 for a non-existant entity
			return ++nextEntId;
		}

		public MapEntity getById(int entityId) {
			return entities.get(Integer.valueOf(entityId));
		}

		public MapEntity getByIdSafely(int entityId) {
			return entities.getWhenSafe(Integer.valueOf(entityId));
		}

		public void add(MapEntity ent) {
			entities.put(Integer.valueOf(ent.getId()), ent);
		}

		public void addSafely(MapEntity ent) {
			entities.putWhenSafe(Integer.valueOf(ent.getId()), ent);
		}

		public void removeById(int entityId) {
			entities.remove(Integer.valueOf(entityId));
		}

		public void removeByIdSafely(int entityId) {
			entities.removeWhenSafe(Integer.valueOf(entityId));
		}

		public int getSize() {
			return entities.size();
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
