package argonms.net.client;

/**
 *
 * @author GoldenKevin
 */
public final class ClientSendOps {
	public static final short
		LOGIN_RESULT = 0x00,
		SERVERLOAD_MSG = 0x03,
		GENDER_DONE = 0x04,
		PIN_RESPONSE = 0x06,
		PIN_ASSIGNED = 0x07,
		ALL_CHARLIST = 0x08,
		WORLD_ENTRY = 0x0A,
		CHARLIST = 0x0B,
		CHECK_NAME_RESP = 0x0D
	;
	
	private ClientSendOps() {
		//uninstantiable...
	}
}
