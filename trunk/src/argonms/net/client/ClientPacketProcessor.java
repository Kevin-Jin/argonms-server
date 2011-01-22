package argonms.net.client;

import argonms.ServerType;
import argonms.tools.input.LittleEndianReader;

/**
 *
 * @author GoldenKevin
 */
public abstract class ClientPacketProcessor {
	public static ClientPacketProcessor getProcessor(byte serverType) {
		switch (serverType) {
			case ServerType.LOGIN:
				return new ClientLoginPacketProcessor();
			case ServerType.SHOP:
				return new ClientShopPacketProcessor();
			default:
				if (serverType >= 0)
					return new ClientGamePacketProcessor();
		}
		return null;
	}

	public abstract void process(LittleEndianReader reader, RemoteClient s);
}
