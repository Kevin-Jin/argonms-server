package argonms.shop;

import argonms.ServerType;
import argonms.net.server.RemoteCenterInterface;

/**
 *
 * @author GoldenKevin
 */
public class ShopCenterInterface extends RemoteCenterInterface {
	public ShopCenterInterface(String password, ShopServer ss) {
		super(ss, password, new CenterShopPacketProcessor(ss));
	}

	protected byte getWorld() {
		return ServerType.SHOP;
	}
}
