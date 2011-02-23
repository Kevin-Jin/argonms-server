/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011  GoldenKevin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package argonms.game;

import argonms.character.Player;
import argonms.net.client.CommonPackets;
import argonms.net.server.RemoteCenterOps;
import argonms.tools.Pair;
import argonms.tools.input.LittleEndianReader;
import argonms.tools.output.LittleEndianByteArrayWriter;
import argonms.tools.output.LittleEndianWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author GoldenKevin
 */
public class InterChannelCommunication {
	private static final byte //network opcodes (for remote server communications)
		MULTI_CHAT = 1,
		SPOUSE_CHAT = 2
	;

	private byte[] localChannels;
	private Map<Byte, Integer> remoteChannelPorts;
	private Map<Byte, byte[]> remoteChannelAddresses;
	private WorldChannel self;

	public InterChannelCommunication(byte[] local, WorldChannel channel) {
		this.localChannels = local;
		this.remoteChannelPorts = new HashMap<Byte, Integer>();
		this.remoteChannelAddresses = new HashMap<Byte, byte[]>();
		this.self = channel;
	}

	public void addRemoteChannels(byte[] host, Map<Byte, Integer> ports) {
		remoteChannelPorts.putAll(ports);
		for (Byte ch : ports.keySet())
			remoteChannelAddresses.put(ch, host);
	}

	public void removeRemoteChannels(Set<Byte> channels) {
		for (Byte ch : channels) {
			remoteChannelPorts.remove(ch);
			remoteChannelAddresses.remove(ch);
		}
	}

	private GameCenterInterface getCenterComm() {
		return GameServer.getInstance().getCenterInterface();
	}

	public Pair<byte[], Integer> getChannelHost(byte ch) throws UnknownHostException {
		for (int i = 0; i < localChannels.length; i++) {
			if (ch == localChannels[i]) {
				byte[] address = InetAddress.getByName(GameServer.getInstance().getExternalIp()).getAddress();
				return new Pair<byte[], Integer>(address, Integer.valueOf(GameServer.getChannel(ch).getPort()));
			}
		}
		Integer port = remoteChannelPorts.get(Byte.valueOf(ch));
		return new Pair<byte[], Integer>(remoteChannelAddresses.get(Byte.valueOf(ch)), port != null ? port : Integer.valueOf(-1));
	}

	public void sendPrivateChat(byte type, int[] recipients, Player p, String message) {
		String name = p.getName();
		boolean valid = false;

		switch (type) {
			case 0:
				valid = true;
				break;
			case 1:
				valid = (p.getParty() != null);
				break;
			case 2:
				valid = (p.getGuildId() != 0);
				break;
		}
		if (valid) {
			receivedPrivateChat(type, recipients, name, message); //recipients could be on same channel
			for (byte ch : localChannels) //recipients on same process
				GameServer.getChannel(ch).getInterChannelInterface().receivedPrivateChat(type, recipients, name, message);
			for (Byte ch : remoteChannelPorts.keySet()) //recipients on another process
				getCenterComm().send(writePrivateChatMessage(getCenterComm().getWorld(), ch.byteValue(), type, recipients, name, message));
		} else {
			//most likely hacking
		}
	}

	public void sendSpouseChat(String spouse, Player p, String message) {
		//perhaps we should compare spouse's name with p.getSpouseId()?
		String name = p.getName();
		int recipient = p.getSpouseId();
		if (recipient != 0) {
			receivedSpouseChat(recipient, name, message); //recipient could be on same channel
			for (byte ch : localChannels) //recipient on same process
				GameServer.getChannel(ch).getInterChannelInterface().receivedSpouseChat(recipient, name, message);
			for (Byte ch : remoteChannelPorts.keySet()) //recipient on another process
				getCenterComm().send(writeSpouseChatMessage(getCenterComm().getWorld(), ch.byteValue(), recipient, name, message));
		} else {
			//most likely hacking
		}
	}

	public void receivedPrivateChat(byte type, int[] recipients, String name, String message) {
		Player p;
		for (int i = 0; i < recipients.length; i++) {
			p = self.getPlayerById(recipients[i]);
			if (p != null)
				p.getClient().getSession().send(CommonPackets.writePrivateChatMessage(type, name, message));
		}
	}

	public void receivedSpouseChat(int recipient, String name, String message) {
		Player p = self.getPlayerById(recipient);
		if (p != null)
			p.getClient().getSession().send(CommonPackets.writeSpouseChatMessage(name, message));
	}

	private static void writeHeader(LittleEndianWriter lew, byte world, byte channel) {
		lew.writeByte(RemoteCenterOps.INTER_CHANNEL);
		lew.writeByte(world);
		lew.writeByte(channel);
	}

	private static byte[] writePrivateChatMessage(byte world, byte channel, byte type, int[] recipients, String name, String message) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		writeHeader(lew, world, channel);
		lew.writeByte(MULTI_CHAT);
		lew.writeByte(type);
		lew.writeByte((byte) recipients.length);
		for (byte i = 0; i < recipients.length; i++)
			lew.writeInt(recipients[i]);
		lew.writeLengthPrefixedString(name);
		lew.writeLengthPrefixedString(message);
		return lew.getBytes();
	}

	private static byte[] writeSpouseChatMessage(byte world, byte channel, int recipient, String name, String message) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();
		writeHeader(lew, world, channel);
		lew.writeByte(SPOUSE_CHAT);
		lew.writeInt(recipient);
		lew.writeLengthPrefixedString(name);
		lew.writeLengthPrefixedString(message);
		return lew.getBytes();
	}

	public void receivedPacket(LittleEndianReader packet) {
		switch (packet.readByte()) {
			case MULTI_CHAT: {
				byte type = packet.readByte();
				byte amount = packet.readByte();
				int[] recipients = new int[amount];
				for (byte i = 0; i < amount; i++)
					recipients[i] = packet.readInt();
				String name = packet.readLengthPrefixedString();
				String message = packet.readLengthPrefixedString();
				receivedPrivateChat(type, recipients, name, message);
				break;
			} case SPOUSE_CHAT: {
				int recipient = packet.readInt();
				String name = packet.readLengthPrefixedString();
				String message = packet.readLengthPrefixedString();
				receivedSpouseChat(recipient, name, message);
				break;
			}
		}
	}
}
