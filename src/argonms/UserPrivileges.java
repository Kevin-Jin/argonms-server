package argonms;

/**
 *
 * @author GoldenKevin
 */
public final class UserPrivileges {
	public static final byte
		USER = 0,
		JUNIOR_GM = 25,
		GM = 50,
		SUPER_GM = 75,
		ADMIN = 100
	;

	private UserPrivileges() {
		//uninstantiable...
	}
}
