package argonms.shop;

import argonms.game.Player;
import argonms.net.client.RemoteClient;

/**
 *
 * @author GoldenKevin
 */
public class ShopClient extends RemoteClient {
	private Player player;

	//world and channel needed to redirect player to game server when they exit
	public ShopClient(int world, int channel) {
		this.player = new Player();
	}

	public Player getPlayer() {
		return player;
	}

	public void disconnect() {
		updateState(STATUS_NOTLOGGEDIN);
	}
}
