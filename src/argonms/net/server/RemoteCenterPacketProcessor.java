package argonms.net.server;

import argonms.ServerType;
import argonms.center.CenterServer;
import argonms.center.GameCenterPacketProcessor;
import argonms.center.LoginCenterPacketProcessor;
import argonms.center.ShopCenterPacketProcessor;
import argonms.tools.input.LittleEndianReader;

/**
 *
 * @author GoldenKevin
 */
public abstract class RemoteCenterPacketProcessor {
	protected CenterRemoteInterface r;
	protected byte world;

	public RemoteCenterPacketProcessor(CenterRemoteInterface r, byte world) {
		this.r = r;
		this.world = world;
	}

	public static RemoteCenterPacketProcessor getProcessor(CenterRemoteInterface r, byte world) {
		switch (world) {
			case ServerType.LOGIN:
				return new LoginCenterPacketProcessor(r, world);
			case ServerType.SHOP:
				return new ShopCenterPacketProcessor(r, world);
			default:
				if (world >= 0)
					return new GameCenterPacketProcessor(r, world);
		}
		return null;
	}

	public abstract void process(LittleEndianReader packet);

	protected void serverOnline(LittleEndianReader packet) {
		r.setHost(packet.readLengthPrefixedString());
		int[] clientPorts = new int[packet.readByte()];
		for (int i = 0; i < clientPorts.length; i++)
			clientPorts[i] = packet.readInt();
		r.setClientPorts(clientPorts);
		CenterServer.getInstance().serverConnected(world, r);
	}
}
