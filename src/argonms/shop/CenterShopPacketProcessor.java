package argonms.shop;

import argonms.net.server.CenterRemoteOps;
import argonms.net.server.CenterRemotePacketProcessor;
import argonms.net.server.RemoteCenterInterface;
import argonms.tools.input.LittleEndianReader;

/**
 * Processes packet sent from the center server and received at the shop
 * server.
 * @author GoldenKevin
 */
public class CenterShopPacketProcessor extends CenterRemotePacketProcessor {
	private ShopServer local;

	public CenterShopPacketProcessor(ShopServer ls) {
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
}
