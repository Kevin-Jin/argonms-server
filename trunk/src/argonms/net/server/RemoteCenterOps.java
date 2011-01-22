package argonms.net.server;

/**
 * Opcodes for packets sent from a remote server and received on the center
 * server.
 * @author GoldenKevin
 */
public class RemoteCenterOps {
	public static final byte
		AUTH = 0x00,
		ONLINE = 0x01,
		POPULATION_CHANGED = 0x02,
		MODIFY_CHANNEL_PORT = 0x03
	;

	private RemoteCenterOps() {
		//uninstantiable...
	}
}
