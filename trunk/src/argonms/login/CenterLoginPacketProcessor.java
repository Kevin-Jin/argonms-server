package argonms.login;

import argonms.net.server.CenterRemoteOps;
import argonms.net.server.CenterRemotePacketProcessor;
import argonms.net.server.RemoteCenterInterface;
import argonms.tools.input.LittleEndianReader;

/**
 * Processes packet sent from the center server and received at the login
 * server.
 * @author GoldenKevin
 */
public class CenterLoginPacketProcessor extends CenterRemotePacketProcessor {
	private LoginServer local;

	public CenterLoginPacketProcessor(LoginServer ls) {
		local = ls;
	}

	public void process(LittleEndianReader packet, RemoteCenterInterface r) {
		switch (packet.readByte()) {
			case CenterRemoteOps.AUTH_RESPONSE:
				processAuthResponse(packet, r.getLocalServer());
				break;
			case CenterRemoteOps.GAME_CONNECTED:
				processGameConnected(packet);
				break;
			case CenterRemoteOps.GAME_DISCONNECTED:
				processGameDisconnected(packet);
				break;
			case CenterRemoteOps.CHANGE_POPULATION:
				processPopulationChange(packet);
				break;
			case CenterRemoteOps.CHANNEL_PORT_CHANGE:
				processChannelPortChange(packet);
				break;
		}
	}

	private void processGameConnected(LittleEndianReader packet) {
		byte world = packet.readByte();
		String host = packet.readLengthPrefixedString();
		int[] ports = new int[packet.readByte()];
		for (int i = 0; i < ports.length; i++)
			ports[i] = packet.readInt();
		local.gameConnected(world, host, ports);
	}

	private void processGameDisconnected(LittleEndianReader packet) {
		byte world = packet.readByte();
		local.gameDisconnected(world);
	}

	private void processPopulationChange(LittleEndianReader packet) {
		byte world = packet.readByte();
		byte channel = packet.readByte();
		boolean increase = packet.readBool();
		local.changePopulation(world, channel, increase);
	}

	private void processChannelPortChange(LittleEndianReader packet) {
		byte world = packet.readByte();
		byte channel = packet.readByte();
		int newPort = packet.readInt();
		World w = local.getWorlds().get(Byte.valueOf(world));
		if (w != null)
			w.getPorts()[channel] = newPort;
	}
}
