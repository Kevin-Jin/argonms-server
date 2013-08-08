/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2013  GoldenKevin
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

package argonms.game.net;

import argonms.common.net.external.ClientListener;
import argonms.common.net.external.ClientListener.ClientFactory;
import argonms.common.net.external.CommonPackets;
import argonms.common.net.external.PlayerLog;
import argonms.common.net.internal.ChannelSynchronizationOps;
import argonms.common.net.internal.RemoteCenterOps;
import argonms.common.util.Scheduler;
import argonms.common.util.collections.Pair;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.game.GameServer;
import argonms.game.character.GameCharacter;
import argonms.game.character.PlayerContinuation;
import argonms.game.field.GameMap;
import argonms.game.field.MapFactory;
import argonms.game.net.external.ClientGamePacketProcessor;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;
import argonms.game.net.internal.CrossServerSynchronization;
import argonms.game.script.EventManager;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class WorldChannel {
	private static final Logger LOG = Logger.getLogger(WorldChannel.class.getName());
	private static final int CHANNEL_CHANGE_TIMEOUT = 2000;

	private final Map<Integer, PlayerContinuation> channelChangeData;
	private final Map<Integer, Pair<Byte, ScheduledFuture<?>>> queuedChannelChanges;
	private long startTime;
	private final ClientListener<GameClient> handler;
	private final byte world, channel;
	private int port;
	private final MapFactory mapFactory;
	private EventManager eventManager;
	private final PlayerLog<GameCharacter> storage;
	private CrossServerSynchronization worldComm;

	public WorldChannel(final byte world, final byte channel, int port) {
		channelChangeData = new ConcurrentHashMap<Integer, PlayerContinuation>();
		queuedChannelChanges = new ConcurrentHashMap<Integer, Pair<Byte, ScheduledFuture<?>>>();
		this.world = world;
		this.channel = channel;
		this.port = port;
		mapFactory = new MapFactory();
		storage = new PlayerLog<GameCharacter>();
		handler = new ClientListener<GameClient>(new ClientGamePacketProcessor(), new ClientFactory<GameClient>() {
			@Override
			public GameClient newInstance() {
				return new GameClient(world, channel);
			}
		});
	}

	public void listen(boolean useNio) {
		if (handler.bind(port)) {
			LOG.log(Level.INFO, "World {0} Channel {1} is online.", new Object[] { world, channel });
		} else {
			shutdown();
			return;
		}
		startTime = System.currentTimeMillis();
		Scheduler.getInstance().runRepeatedly(new Runnable() {
			@Override
			public void run() {
				for (GameMap map : mapFactory.getMaps())
					map.respawnMobs();
				for (GameMap map : mapFactory.getInstanceMaps())
					map.respawnMobs();
			}
		}, 0, 15000);
	}

	public byte getWorld() {
		return world;
	}

	public byte getChannelId() {
		return channel;
	}

	public long getTimeStarted() {
		return startTime;
	}

	public void initializeEventManager(String scriptPath, String[] persistentEvents) {
		eventManager = new EventManager(scriptPath, channel, persistentEvents);
	}

	public void addPlayer(GameCharacter p) {
		storage.addPlayer(p);
		sendNewLoad(storage.getConnectedCount());
	}

	public void removePlayer(GameCharacter p) {
		storage.deletePlayer(p);
		sendNewLoad(storage.getConnectedCount());
	}

	public GameCharacter getPlayerById(int characterid) {
		return storage.getPlayer(characterid);
	}

	public GameCharacter getPlayerByName(String name) {
		return storage.getPlayer(name);
	}

	public boolean isPlayerConnected(int characterid) {
		return storage.getPlayer(characterid) != null;
	}

	public Collection<GameCharacter> getConnectedPlayers() {
		return storage.getConnectedPlayers();
	}

	public void resetConnectedPlayers() {
		storage.clear();
		sendNewLoad((short) 0);
	}

	private void channelChangeError(GameCharacter p, byte msg) {
		queuedChannelChanges.remove(Integer.valueOf(p.getId()));
		p.getClient().getSession().send(GamePackets.writeServerMigrateFailed(msg));
		p.getClient().getSession().send(GamePackets.writeEnableActions());
	}

	public void requestChannelChange(final GameCharacter p, byte destCh) {
		p.channelChangeCancelSkills();
		queuedChannelChanges.put(Integer.valueOf(p.getId()), new Pair<Byte, ScheduledFuture<?>>(Byte.valueOf(destCh), Scheduler.getInstance().runAfterDelay(new Runnable() {
			@Override
			public void run() {
				channelChangeError(p, (byte) 1);
			}
		}, CHANNEL_CHANGE_TIMEOUT)));
		worldComm.sendChannelChangeRequest(destCh, p);
	}

	public void requestShopEntry(final GameCharacter p, final boolean cashShop) {
		if (!worldComm.shopServerConnected()) {
			p.getClient().getSession().send(GamePackets.writeServerMigrateFailed(cashShop ? (byte) 2 : (byte) 3));
			p.getClient().getSession().send(GamePackets.writeEnableActions());
			return;
		}

		p.channelChangeCancelSkills();
		queuedChannelChanges.put(Integer.valueOf(p.getId()), new Pair<Byte, ScheduledFuture<?>>(Byte.valueOf(ChannelSynchronizationOps.CHANNEL_CASH_SHOP), Scheduler.getInstance().runAfterDelay(new Runnable() {
			@Override
			public void run() {
				channelChangeError(p, cashShop ? (byte) 2 : (byte) 3);
			}
		}, CHANNEL_CHANGE_TIMEOUT)));
		worldComm.sendEnterShopRequest(p, cashShop);
	}

	public void performChannelChange(int playerId) {
		Pair<Byte, ScheduledFuture<?>> channelChangeState = queuedChannelChanges.remove(Integer.valueOf(playerId));
		channelChangeState.right.cancel(false);
		byte errorCode = 0;
		byte[] destHost;
		int destPort;
		try {
			Pair<byte[], Integer> hostAndPort;
			if (channelChangeState.left.byteValue() != ChannelSynchronizationOps.CHANNEL_CASH_SHOP) {
				errorCode = 1;
				hostAndPort = worldComm.getChannelHost(channelChangeState.left.byteValue());
			} else {
				errorCode = 2;
				hostAndPort = worldComm.getShopHost();
			}
			destHost = hostAndPort.left;
			destPort = hostAndPort.right.intValue();
		} catch (UnknownHostException e) {
			destHost = null;
			destPort = -1;
		}
		GameCharacter p = storage.getPlayer(playerId);
		if (destHost != null && destPort != -1) {
			p.prepareChannelChange();
			p.getClient().setMigratingHost();
			p.getClient().getSession().send(CommonPackets.writeNewGameHost(destHost, destPort));
		} else {
			channelChangeError(p, errorCode);
		}
	}

	public void storePlayerBuffs(int playerId, PlayerContinuation context) {
		channelChangeData.put(Integer.valueOf(playerId), context);
	}

	public byte applyBuffsFromLastChannel(GameCharacter p) {
		PlayerContinuation context = channelChangeData.remove(Integer.valueOf(p.getId()));
		if (context == null)
			return ChannelSynchronizationOps.CHANNEL_OFFLINE;
		context.applyTo(p);
		return context.getOriginChannel();
	}

	private void sendNewLoad(short now) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(3);
		lew.writeByte(RemoteCenterOps.POPULATION_CHANGED);
		lew.writeByte(channel);
		lew.writeShort(now);
		GameServer.getInstance().getCenterInterface().getSession().send(lew.getBytes());
	}

	public void startup(int port) {
		if (port == -1) {
			this.port = port;
			if (handler.bind(port)) {
				LOG.log(Level.INFO, "Channel {0} is online.", channel);
				sendNewPort();
			}
		}
	}

	public void shutdown() {
		handler.close("Shutdown", null);
		port = -1;
		sendNewPort();
	}

	private void sendNewPort() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);
		lew.writeByte(RemoteCenterOps.MODIFY_CHANNEL_PORT);
		lew.writeByte(world);
		lew.writeByte(channel);
		lew.writeInt(port);
		GameServer.getInstance().getCenterInterface().getSession().send(lew.getBytes());
	}

	public int getPort() {
		return port;
	}

	public MapFactory getMapFactory() {
		return mapFactory;
	}

	public EventManager getEventManager() {
		return eventManager;
	}

	public void createWorldComm() {
		worldComm = new CrossServerSynchronization(this);
	}

	public CrossServerSynchronization getCrossServerInterface() {
		return worldComm;
	}
}
