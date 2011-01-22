package argonms.net.server;

import argonms.LocalServer;
import argonms.tools.input.LittleEndianReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public abstract class CenterRemotePacketProcessor {
	private static final Logger LOG = Logger.getLogger(CenterRemotePacketProcessor.class.getName());

	public abstract void process(LittleEndianReader packet, RemoteCenterInterface s);

	protected void processAuthResponse(LittleEndianReader packet, LocalServer ls) {
		String error = packet.readLengthPrefixedString();
		if (error.length() != 0)
			LOG.log(Level.SEVERE, "Unable to auth with center server: {0}", error);
		else
			ls.centerConnected();
	}
}
