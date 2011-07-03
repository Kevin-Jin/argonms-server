/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011  GoldenKevin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package argonms.login.handler;

import argonms.common.net.external.ClientSendOps;
import argonms.common.tools.input.LittleEndianReader;
import argonms.common.tools.output.LittleEndianByteArrayWriter;
import argonms.login.LoginClient;
import argonms.login.LoginServer;

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

	public static void handleLogin(LittleEndianReader packet, LoginClient lc) {
		String login = packet.readLengthPrefixedString();
		String pwd = packet.readLengthPrefixedString();

		lc.setAccountName(login);

		byte result = lc.loginResult(pwd);

		LittleEndianByteArrayWriter writer = new LittleEndianByteArrayWriter(result == 0 ? 39 + login.length() : result == 2 ? 17 : 8);

		writer.writeShort(ClientSendOps.LOGIN_RESULT);
		writer.writeShort(result);
		writer.writeInt(0);

		if (result == 0) {
			writer.writeInt(lc.getAccountId());
			writer.writeByte(lc.getGender()); //0 = male, 1 = female, 0x0A = ask gender, 0x0B = ask pin
			writer.writeByte((byte) 0); //Admin byte, allows client to use "/". Used for logging commands I guess. Disables any player interactions though.
			writer.writeByte((byte) 0x4E);
			writer.writeLengthPrefixedString(login);
			writer.writeBytes(new byte[]{3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xDC, 0x3D, 0x0B, 0x28, 0x64, (byte) 0xC5, 1, 8, 0, 0, 0});
		} else if (result == 2) {
			writer.writeByte(lc.getBanReason());
			writer.writeInt(0);
			writer.writeInt(lc.getBanExpiration());
		}

		lc.getSession().send(writer.getBytes());
	}

	public static void handleGender(LittleEndianReader packet, LoginClient lc) {
		byte op1 = packet.readByte();
		byte op2;
		if (op1 == 1) {
			op2 = packet.readByte();
			lc.setGender(op2);
			lc.getSession().send(genderDone(op2));
		} else if (op1 == 0) {
			lc.updateState(LoginClient.STATUS_NOTLOGGEDIN);
		}
	}

	public static void handlePin(LittleEndianReader packet, LoginClient lc) {
		byte op1 = packet.readByte();
		byte op2 = packet.readByte();
		packet.readInt(); //always 1?
		if (op1 == 1 && op2 == 1) {
			if (LoginServer.getInstance().usePin()) {
				if (lc.getPin() == null) {
					lc.getSession().send(pinOperation(PIN_REGISTER));
				} else {
					lc.getSession().send(pinOperation(PIN_REQUEST));
				}
			} else {
				lc.getSession().send(pinOperation(PIN_ACCEPTED));
			}
		} else if (op1 == 1 && op2 == 0) {
			String pin = packet.readLengthPrefixedString();
			if (pin.equals(lc.getPin())) {
				lc.getSession().send(pinOperation(PIN_ACCEPTED));
			} else {
				lc.getSession().send(pinOperation(PIN_REJECTED));
			}
		} else if (op1 == 2 && op2 == 0) {
			String pin = packet.readLengthPrefixedString();
			if (pin.equals(lc.getPin())) {
				lc.getSession().send(pinOperation(PIN_REGISTER));
			} else {
				lc.getSession().send(pinOperation(PIN_REJECTED));
			}
		} else if (op1 == 0) {
			lc.updateState(LoginClient.STATUS_NOTLOGGEDIN);
		}
	}

	public static void handlePinRegister(LittleEndianReader packet, LoginClient lc) {
		byte c2 = packet.readByte();
		if (c2 != 0) {
			String pin = packet.readLengthPrefixedString();
			if (pin != null) {
				lc.setPin(pin);
				lc.getSession().send(pinRegistered());
			}
		}
		lc.updateState(LoginClient.STATUS_NOTLOGGEDIN);
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
