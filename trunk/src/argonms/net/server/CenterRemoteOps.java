package argonms.net.server;

/**
 * Opcodes for packets sent from the center server and received on a remote
 * server.
 * @author GoldenKevin
 */
public class CenterRemoteOps {
	public static final byte
		AUTH_RESPONSE = 0x00,
		GAME_CONNECTED = 0x01,
		SHOP_CONNECTED = 0x02,
		GAME_DISCONNECTED = 0x03,
		SHOP_DISCONNECTED = 0x04,
		CHANGE_POPULATION = 0x05,
		CHANNEL_PORT_CHANGE = 0x06
	;

	private CenterRemoteOps() {
		//uninstantiable...
	}
}
