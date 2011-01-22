package argonms;

/**
 *
 * @author GoldenKevin
 */
public final class ServerType {
	public static final byte
		UNDEFINED = -4,
		CENTER = -3,
		SHOP = -2,
		LOGIN = -1,
		GAME = 0
	;

	private ServerType() {
		//uninstantiable...
	}

	public static boolean isCenter(byte type) {
		return type == CENTER;
	}

	public static boolean isShop(byte type) {
		return type == SHOP;
	}

	public static boolean isLogin(byte type) {
		return type == LOGIN;
	}

	public static boolean isGame(byte type) {
		return type >= 0;
	}

	public static String getName(byte type) {
		switch (type) {
			case CENTER:
				return "Center";
			case SHOP:
				return "Shop";
			case LOGIN:
				return "Login";
			default:
				if (type >= 0)
					return "Game" + type;
				return null;
		}
	}
}
