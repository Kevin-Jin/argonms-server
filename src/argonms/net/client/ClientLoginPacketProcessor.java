package argonms.net.client;

import argonms.net.client.handler.AuthHandler;
import argonms.net.client.handler.WorldlistHandler;
import argonms.tools.input.LittleEndianReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class ClientLoginPacketProcessor extends ClientPacketProcessor {
	private static final Logger LOG = Logger.getLogger(ClientPacketProcessor.class.getName());

	public void process(LittleEndianReader reader, RemoteClient s) {
		switch (reader.readShort()) {
			case ClientRecvOps.LOGIN_PASSWORD:
				AuthHandler.handleLogin(reader, s);
				break;
			case ClientRecvOps.CHARLIST_REQ:
				WorldlistHandler.handleCharlist(reader, s);
				break;
			case ClientRecvOps.REQ_SERVERLOAD:
				WorldlistHandler.sendServerStatus(reader, s);
				break;
			case ClientRecvOps.SET_GENDER:
				AuthHandler.handleGender(reader, s);
				break;
			case ClientRecvOps.PIN_OPERATION:
				AuthHandler.handlePin(reader, s);
				break;
			case ClientRecvOps.REGISTER_PIN:
				AuthHandler.handlePinRegister(reader, s);
				break;
			case ClientRecvOps.SERVERLIST_REQUEST:
				WorldlistHandler.handleWorldListRequest(reader, s);
				break;
			case ClientRecvOps.VIEW_ALL_CHARS:
				WorldlistHandler.sendAllChars(reader, s);
				break;
			case ClientRecvOps.CHECK_CHAR_NAME:
				WorldlistHandler.checkName(reader, s);
				break;
			case ClientRecvOps.CREATE_CHAR:
				WorldlistHandler.createCharacter(reader, s);
				break;
			case ClientRecvOps.AES_IV_UPDATE_REQUEST:
				//no-op
				break;
			default:
				LOG.log(Level.FINE, "Received unhandled packet {0} bytes long:\n{1}", new Object[] { reader.available() + 2, reader });
				break;
		}
	}
}
