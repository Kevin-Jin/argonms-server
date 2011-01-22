package argonms.login;

import argonms.ServerType;
import argonms.net.server.RemoteCenterInterface;

/**
 * Provides an interface for the login server between it and the center server.
 * 
 * @author GoldenKevin
 */
public class LoginCenterInterface extends RemoteCenterInterface {
	public LoginCenterInterface(String password, LoginServer ls) {
		super(ls, password, new CenterLoginPacketProcessor(ls));
	}

	protected byte getWorld() {
		return ServerType.LOGIN;
	}
}
