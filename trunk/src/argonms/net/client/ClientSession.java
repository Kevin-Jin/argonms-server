package argonms.net.client;

import argonms.ServerType;
import argonms.game.GameClient;
import argonms.login.LoginClient;
import argonms.shop.ShopClient;
import org.jboss.netty.channel.Channel;

/**
 *
 * @author GoldenKevin
 */
public class ClientSession {
	private Channel ch;
	private MapleAESOFB sendCypher, recvCypher;
	private RemoteClient client;

	public ClientSession(Channel ch, byte world, byte channel) {
		this.ch = ch;

		byte ivRecv[] = { 70, 114, 122, -1 };
		byte ivSend[] = { 82, 48, 120, -1 };

		ivRecv[3] = (byte) (Math.random() * 255);
		ivSend[3] = (byte) (Math.random() * 255);
		this.recvCypher = new MapleAESOFB(ivRecv, (byte) 62);
		this.sendCypher = new MapleAESOFB(ivSend, (short) (0xFFFF - (byte) 62));

		if (world == ServerType.LOGIN)
			this.client = new LoginClient();
		else if (world == ServerType.SHOP)
			this.client = new ShopClient(world, channel);
		else
			this.client = new GameClient(world, channel);
	}

	public MapleAESOFB getRecvCypher() {
		return recvCypher;
	}

	public MapleAESOFB getSendCypher() {
		return sendCypher;
	}

	public RemoteClient getClient() {
		return client;
	}

	public void send(byte[] input) {
		ch.write(input);
	}

	public void close() {
		ch.disconnect();
	}
}
