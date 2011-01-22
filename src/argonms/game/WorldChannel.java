package argonms.game;

import argonms.net.client.ClientListener;
import argonms.net.server.RemoteCenterOps;
import argonms.tools.output.LittleEndianByteArrayWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class WorldChannel {
	private static final Logger LOG = Logger.getLogger(WorldChannel.class.getName());

	private ClientListener handler;
	private byte world, channel;
	private int port;

	protected WorldChannel(byte world, byte channel, int port) {
		this.world = world;
		this.channel = channel;
		this.port = port;
	}

	public void listen(boolean useNio) {
		handler = new ClientListener(world, channel, useNio);
		if (handler.bind(port))
			LOG.log(Level.INFO, "Channel {0} is online.", channel);
		else
			shutdown();
	}

	public void increaseLoad() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);
		lew.writeByte(RemoteCenterOps.POPULATION_CHANGED);
		lew.writeByte(channel);
		lew.writeBool(true);
		GameServer.getInstance().getCenterInterface().send(lew.getBytes());
	}

	public void decreaseLoad() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);
		lew.writeByte(RemoteCenterOps.POPULATION_CHANGED);
		lew.writeByte(channel);
		lew.writeBool(false);
		GameServer.getInstance().getCenterInterface().send(lew.getBytes());
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
		port = -1;
		sendNewPort();
	}

	private void sendNewPort() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);
		lew.writeByte(RemoteCenterOps.MODIFY_CHANNEL_PORT);
		lew.writeByte(channel);
		lew.writeInt(port);
		GameServer.getInstance().getCenterInterface().send(lew.getBytes());
	}

	public int getPort() {
		return port;
	}
}
