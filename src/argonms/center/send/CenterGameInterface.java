package argonms.center.send;

import java.util.Map;
import java.util.Set;

import argonms.center.CenterServer;
import argonms.center.recv.GameCenterPacketProcessor;
import argonms.center.recv.RemoteCenterPacketProcessor;
import argonms.center.CenterRemoteSession;

public class CenterGameInterface extends CenterRemoteInterface {
	private String host;
	private byte world;
	private Map<Byte, Integer> clientPorts;
	private byte serverId;

	public CenterGameInterface(CenterRemoteSession session, byte serverId) {
		super(session);
		this.serverId = serverId;
	}

	public RemoteCenterPacketProcessor createPacketProcessor() {
		return new GameCenterPacketProcessor(this);
	}

	public byte getServerId() {
		return serverId;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setWorld(byte world) {
		this.world = world;
	}

	public byte getWorld() {
		return world;
	}

	public void setClientPorts(Map<Byte, Integer> clientPorts) {
		this.clientPorts = clientPorts;
	}

	public Set<Byte> getChannels() {
		return clientPorts.keySet();
	}

	public String getHost() {
		return host;
	}

	public Map<Byte, Integer> getClientPorts() {
		return clientPorts;
	}

	public void disconnect() {
		CenterServer.getInstance().gameDisconnected(this);
	}
}
