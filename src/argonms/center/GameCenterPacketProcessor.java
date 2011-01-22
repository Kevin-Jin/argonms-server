package argonms.center;

import argonms.net.server.CenterRemoteOps;
import argonms.net.server.RemoteCenterOps;
import argonms.net.server.RemoteCenterPacketProcessor;
import argonms.net.server.CenterRemoteInterface;
import argonms.tools.input.LittleEndianReader;
import argonms.tools.output.LittleEndianByteArrayWriter;

/**
 * Processes packet sent from the game server and received at the center
 * server.
 * @author GoldenKevin
 */
public class GameCenterPacketProcessor extends RemoteCenterPacketProcessor {
	public GameCenterPacketProcessor(CenterRemoteInterface r, byte world) {
		super(r, world);
	}

	public void process(LittleEndianReader packet) {
		switch (packet.readByte()) {
			case RemoteCenterOps.ONLINE:
				serverOnline(packet);
				break;
			case RemoteCenterOps.POPULATION_CHANGED:
				processPopulationChange(packet);
				break;
			case RemoteCenterOps.MODIFY_CHANNEL_PORT:
				processChannelPortChange(packet);
				break;
		}
	}

	private void processPopulationChange(LittleEndianReader packet) {
		byte channel = packet.readByte();
		boolean increase = packet.readBool();
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);
		lew.writeByte(CenterRemoteOps.CHANGE_POPULATION);
		lew.writeByte(world);
		lew.writeByte(channel);
		lew.writeBool(increase);
		CenterServer.getInstance().getLoginServer().send(lew.getBytes());
	}

	private void processChannelPortChange(LittleEndianReader packet) {
		byte channel = packet.readByte();
		int newPort = packet.readInt();
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);
		lew.writeByte(CenterRemoteOps.CHANNEL_PORT_CHANGE);
		lew.writeByte(world);
		lew.writeByte(channel);
		lew.writeInt(newPort);
		CenterServer.getInstance().getLoginServer().send(lew.getBytes());
	}
}
