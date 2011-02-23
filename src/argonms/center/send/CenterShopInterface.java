package argonms.center.send;

import argonms.center.send.CenterRemoteInterface;
import argonms.ServerType;
import argonms.center.CenterServer;
import argonms.center.recv.ShopCenterPacketProcessor;
import argonms.center.recv.RemoteCenterPacketProcessor;
import argonms.center.CenterRemoteSession;

public class CenterShopInterface extends CenterRemoteInterface {
	private String host;
	private int port;

	public CenterShopInterface(CenterRemoteSession session) {
		super(session);
	}

	public RemoteCenterPacketProcessor createPacketProcessor() {
		return new ShopCenterPacketProcessor(this);
	}

	public byte getServerId() {
		return ServerType.SHOP;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setClientPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public int getClientPort() {
		return port;
	}

	public void disconnect() {
		CenterServer.getInstance().shopDisconnected();
	}
}
