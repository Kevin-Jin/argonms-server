package argonms.center.send;

import argonms.ServerType;
import argonms.center.recv.RemoteCenterPacketProcessor;
import argonms.center.CenterRemoteSession;

public abstract class CenterRemoteInterface {
	private CenterRemoteSession session;

	public CenterRemoteInterface(CenterRemoteSession session) {
		this.session = session;
	}

	public CenterRemoteSession getSession() {
		return session;
	}

	public abstract RemoteCenterPacketProcessor createPacketProcessor();

	public abstract byte getServerId();

	public abstract void disconnect();

	public static CenterRemoteInterface getByServerId(byte serverId, CenterRemoteSession session) {
		if (ServerType.isGame(serverId))
			return new CenterGameInterface(session, serverId);
		if (ServerType.isLogin(serverId))
			return new CenterLoginInterface(session);
		if (ServerType.isShop(serverId))
			return new CenterShopInterface(session);
		return null;
	}
}
