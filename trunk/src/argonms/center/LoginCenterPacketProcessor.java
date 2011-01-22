package argonms.center;

import argonms.net.server.RemoteCenterOps;
import argonms.net.server.RemoteCenterPacketProcessor;
import argonms.net.server.CenterRemoteInterface;
import argonms.tools.input.LittleEndianReader;

/**
 * Processes packet sent from the login server and received at the center
 * server.
 * @author GoldenKevin
 */
public class LoginCenterPacketProcessor extends RemoteCenterPacketProcessor {
	public LoginCenterPacketProcessor(CenterRemoteInterface r, byte world) {
		super(r, world);
	}

	public void process(LittleEndianReader packet) {
		switch (packet.readByte()) {
			case RemoteCenterOps.ONLINE:
				serverOnline(packet);
				break;
		}
	}
}
