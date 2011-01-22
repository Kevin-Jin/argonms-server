package argonms.net.client;

import argonms.tools.input.LittleEndianReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class ClientShopPacketProcessor extends ClientPacketProcessor {
	private static final Logger LOG = Logger.getLogger(ClientPacketProcessor.class.getName());

	public void process(LittleEndianReader reader, RemoteClient s) {
		switch (reader.readShort()) {
			default:
				LOG.log(Level.FINE, "Received unhandled packet {0} bytes long:\n{1}", new Object[] { reader.available() + 2, reader });
				break;
		}
	}
}
