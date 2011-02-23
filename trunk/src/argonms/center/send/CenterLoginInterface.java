package argonms.center.send;

import argonms.ServerType;
import argonms.center.CenterServer;
import argonms.center.recv.LoginCenterPacketProcessor;
import argonms.center.recv.RemoteCenterPacketProcessor;
import argonms.center.CenterRemoteSession;

public class CenterLoginInterface extends CenterRemoteInterface {
	private String host;
	private int port;

	public CenterLoginInterface(CenterRemoteSession session) {
		super(session);
	}

	public RemoteCenterPacketProcessor createPacketProcessor() {
		return new LoginCenterPacketProcessor(this);
	}

	public byte getServerId() {
		return ServerType.LOGIN;
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
		CenterServer.getInstance().loginDisconnected();
	}
}
