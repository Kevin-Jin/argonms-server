package argonms.net.client.handler;

import argonms.net.client.RemoteClient;
import argonms.login.LoginClient;
import argonms.login.LoginServer;
import argonms.net.client.ClientSendOps;
import argonms.tools.output.LittleEndianByteArrayWriter;
import argonms.tools.input.LittleEndianReader;

/**
 *
 * @author GoldenKevin
 */
public class AuthHandler {
	private static final byte
		PIN_ACCEPTED = 0x00,
		PIN_REGISTER = 0x01,
		PIN_REJECTED = 0x02,
		PIN_REQUEST = 0x04
	;

	public static void handleLogin(LittleEndianReader packet, RemoteClient rc) {
		LoginClient client = (LoginClient) rc;

		String login = packet.readLengthPrefixedString();
		String pwd = packet.readLengthPrefixedString();

		client.setAccountName(login);

		byte result = client.loginResult(pwd);

		LittleEndianByteArrayWriter writer = new LittleEndianByteArrayWriter(result == 0 ? 39 + login.length() : result == 2 ? 17 : 8);

		writer.writeShort(ClientSendOps.LOGIN_RESULT);
		writer.writeShort(result);
		writer.writeInt(0);

		if (result == 0) {
			writer.writeInt(client.getAccountId());
			writer.writeByte(client.getGender()); //0 = male, 1 = female, 0x0A = ask gender, 0x0B = ask pin
			writer.writeByte((byte) 0); //Admin byte, allows client to use "/". Used for logging commands I guess. Disables any player interactions though.
			writer.writeByte((byte) 0x4E);
			writer.writeLengthPrefixedString(login);
			writer.writeBytes(new byte[]{3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xDC, 0x3D, 0x0B, 0x28, 0x64, (byte) 0xC5, 1, 8, 0, 0, 0});
		} else if (result == 2) {
			writer.writeByte(client.getBanReason());
			writer.writeInt(0);
			writer.writeInt(client.getBanExpiration());
		}

		client.getSession().send(writer.getBytes());
	}

	public static void handleGender(LittleEndianReader packet, RemoteClient rc) {
		LoginClient client = (LoginClient) rc;

		byte op1 = packet.readByte();
		byte op2;
		if (op1 == 1) {
			op2 = packet.readByte();
			client.setGender(op2);
			client.getSession().send(genderDone(op2));
		} else if (op1 == 0) {
			client.updateState(RemoteClient.STATUS_NOTLOGGEDIN);
		}
	}

	public static void handlePin(LittleEndianReader packet, RemoteClient rc) {
		LoginClient client = (LoginClient) rc;

		byte op1 = packet.readByte();
		byte op2 = packet.readByte();
		packet.readInt(); //always 1?
		if (op1 == 1 && op2 == 1) {
			if (LoginServer.getInstance().usePin()) {
				if (client.getPin() == null) {
					client.getSession().send(pinOperation(PIN_REGISTER));
				} else {
					client.getSession().send(pinOperation(PIN_REQUEST));
				}
			} else {
				client.getSession().send(pinOperation(PIN_ACCEPTED));
			}
		} else if (op1 == 1 && op2 == 0) {
			String pin = packet.readLengthPrefixedString();
			if (pin.equals(client.getPin())) {
				client.getSession().send(pinOperation(PIN_ACCEPTED));
			} else {
				client.getSession().send(pinOperation(PIN_REJECTED));
			}
		} else if (op1 == 2 && op2 == 0) {
			String pin = packet.readLengthPrefixedString();
			if (pin.equals(client.getPin())) {
				client.getSession().send(pinOperation(PIN_REGISTER));
			} else {
				client.getSession().send(pinOperation(PIN_REJECTED));
			}
		} else if (op1 == 0) {
			client.updateState(RemoteClient.STATUS_NOTLOGGEDIN);
		}
	}

	public static void handlePinRegister(LittleEndianReader packet, RemoteClient rc) {
		LoginClient client = (LoginClient) rc;
		
		byte c2 = packet.readByte();
		if (c2 != 0) {
			String pin = packet.readLengthPrefixedString();
			if (pin != null) {
				client.setPin(pin);
				client.getSession().send(pinRegistered());
			}
		}
		client.updateState(RemoteClient.STATUS_NOTLOGGEDIN);
	}

	private static byte[] genderDone(byte gender) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4);

		lew.writeShort(ClientSendOps.GENDER_DONE);
		lew.writeByte(gender);
		lew.writeByte((byte) 1);

		return lew.getBytes();
	}

	private static byte[] pinOperation(byte mode) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(3);

		lew.writeShort(ClientSendOps.PIN_RESPONSE);
		lew.writeByte(mode);

		return lew.getBytes();
	}

	private static byte[] pinRegistered() {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(3);

		lew.writeShort(ClientSendOps.PIN_ASSIGNED);
		lew.writeByte((byte) 0);

		return lew.getBytes();
	}
}
