package argonms.game;

import argonms.net.client.RemoteClient;

/**
 *
 * @author GoldenKevin
 */
public class GameClient extends RemoteClient {
	private Player player;

	public GameClient(byte world, byte channel) {
		setWorld(world);
		setChannel(world);
		this.player = new Player();
		GameServer.getChannel(channel).increaseLoad();
	}

	public Player getPlayer() {
		return player;
	}

	public void disconnect() {
		updateState(STATUS_NOTLOGGEDIN);
		GameServer.getChannel(getChannel()).decreaseLoad();
	}
}
