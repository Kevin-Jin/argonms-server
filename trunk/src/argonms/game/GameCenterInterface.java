package argonms.game;

import argonms.net.server.RemoteCenterInterface;

/**
 *
 * @author GoldenKevin
 */
public class GameCenterInterface extends RemoteCenterInterface {
	private byte world;

	public GameCenterInterface(byte world, String password, GameServer gs) {
		super(gs, password, new CenterGamePacketProcessor(gs));
		this.world = world;
	}

	protected byte getWorld() {
		return world;
	}
}
