package argonms.game;

import argonms.net.server.CenterRemoteOps;
import argonms.net.server.CenterRemotePacketProcessor;
import argonms.net.server.RemoteCenterInterface;
import argonms.tools.input.LittleEndianReader;

/**
 * Processes packet sent from the center server and received at the game server.
 * @author GoldenKevin
 */
public class CenterGamePacketProcessor extends CenterRemotePacketProcessor {
	private GameServer local;

	public CenterGamePacketProcessor(GameServer gs) {
		local = gs;
	}

	public void process(LittleEndianReader packet, RemoteCenterInterface r) {
		switch (packet.readByte()) {
			case CenterRemoteOps.AUTH_RESPONSE:
				processAuthResponse(packet, r.getLocalServer());
				break;
			case CenterRemoteOps.SHOP_CONNECTED:
				processShopConnected(packet);
				break;
			case CenterRemoteOps.SHOP_DISCONNECTED:
				processShopDisconnected(packet);
				break;
		}
	}

	private void processShopConnected(LittleEndianReader packet) {
		String host = packet.readLengthPrefixedString();
		int port = packet.readInt();
		local.shopConnected(host, port);
	}

	private void processShopDisconnected(LittleEndianReader packet) {
		local.shopDisconnected();
	}
}
