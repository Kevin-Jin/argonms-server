/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2012  GoldenKevin
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

package argonms.common.net.external;

import argonms.common.GlobalConstants;
import argonms.common.util.ByteTool;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Provides a class for encrypting MapleStory packets with AES OFB encryption
 * and MapleStory's own in-house encryption.
 *
 * Taken and merged with MapleCustomEncryption from an OdinMS-derived source
 * with some major modifications.
 *
 * @author Frz, GoldenKevin
 * @version 2.0
 */
public final class MapleAesOfb {
	private static final Logger LOG = Logger.getLogger(MapleAesOfb.class.getName());

	private static final byte[] aesKey = {
		(byte) 0x13, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		(byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xB4, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		(byte) 0x1B, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0F, (byte) 0x00, (byte) 0x00, (byte) 0x00,
		(byte) 0x33, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x52, (byte) 0x00, (byte) 0x00, (byte) 0x00
	};

	private static final SecretKeySpec sKeySpec;
	private static final CipherPool aesCiphers;

	private static class CipherThrowablePair {
		public Cipher cipher;
		public Exception throwable;
	}

	private static class CipherPool extends ThreadLocal<CipherThrowablePair> {
		@Override
		public CipherThrowablePair get() {
			CipherThrowablePair result = new CipherThrowablePair();
			try {
				result.cipher = Cipher.getInstance("AES/ECB/NoPadding");
				result.cipher.init(Cipher.ENCRYPT_MODE, sKeySpec);
			} catch (Exception ex) {
				result.throwable = ex;
			}
			return result;
		}

		public Cipher getCipher() throws Exception {
			CipherThrowablePair result = get();
			if (result.throwable != null) {
				remove();
				throw result.throwable;
			}
			return result.cipher;
		}
	}

	static {
		sKeySpec = new SecretKeySpec(aesKey, "AES");
		aesCiphers = new CipherPool();
	}

	public static void testCipher() throws Exception {
		aesCiphers.getCipher();
		aesCiphers.remove();
	}

	private static final byte[] ivKeys = {
		(byte) 0xEC, (byte) 0x3F, (byte) 0x77, (byte) 0xA4, (byte) 0x45, (byte) 0xD0, (byte) 0x71, (byte) 0xBF,
		(byte) 0xB7, (byte) 0x98, (byte) 0x20, (byte) 0xFC, (byte) 0x4B, (byte) 0xE9, (byte) 0xB3, (byte) 0xE1,
		(byte) 0x5C, (byte) 0x22, (byte) 0xF7, (byte) 0x0C, (byte) 0x44, (byte) 0x1B, (byte) 0x81, (byte) 0xBD,
		(byte) 0x63, (byte) 0x8D, (byte) 0xD4, (byte) 0xC3, (byte) 0xF2, (byte) 0x10, (byte) 0x19, (byte) 0xE0,
		(byte) 0xFB, (byte) 0xA1, (byte) 0x6E, (byte) 0x66, (byte) 0xEA, (byte) 0xAE, (byte) 0xD6, (byte) 0xCE,
		(byte) 0x06, (byte) 0x18, (byte) 0x4E, (byte) 0xEB, (byte) 0x78, (byte) 0x95, (byte) 0xDB, (byte) 0xBA,
		(byte) 0xB6, (byte) 0x42, (byte) 0x7A, (byte) 0x2A, (byte) 0x83, (byte) 0x0B, (byte) 0x54, (byte) 0x67,
		(byte) 0x6D, (byte) 0xE8, (byte) 0x65, (byte) 0xE7, (byte) 0x2F, (byte) 0x07, (byte) 0xF3, (byte) 0xAA,
		(byte) 0x27, (byte) 0x7B, (byte) 0x85, (byte) 0xB0, (byte) 0x26, (byte) 0xFD, (byte) 0x8B, (byte) 0xA9,
		(byte) 0xFA, (byte) 0xBE, (byte) 0xA8, (byte) 0xD7, (byte) 0xCB, (byte) 0xCC, (byte) 0x92, (byte) 0xDA,
		(byte) 0xF9, (byte) 0x93, (byte) 0x60, (byte) 0x2D, (byte) 0xDD, (byte) 0xD2, (byte) 0xA2, (byte) 0x9B,
		(byte) 0x39, (byte) 0x5F, (byte) 0x82, (byte) 0x21, (byte) 0x4C, (byte) 0x69, (byte) 0xF8, (byte) 0x31,
		(byte) 0x87, (byte) 0xEE, (byte) 0x8E, (byte) 0xAD, (byte) 0x8C, (byte) 0x6A, (byte) 0xBC, (byte) 0xB5,
		(byte) 0x6B, (byte) 0x59, (byte) 0x13, (byte) 0xF1, (byte) 0x04, (byte) 0x00, (byte) 0xF6, (byte) 0x5A,
		(byte) 0x35, (byte) 0x79, (byte) 0x48, (byte) 0x8F, (byte) 0x15, (byte) 0xCD, (byte) 0x97, (byte) 0x57,
		(byte) 0x12, (byte) 0x3E, (byte) 0x37, (byte) 0xFF, (byte) 0x9D, (byte) 0x4F, (byte) 0x51, (byte) 0xF5,
		(byte) 0xA3, (byte) 0x70, (byte) 0xBB, (byte) 0x14, (byte) 0x75, (byte) 0xC2, (byte) 0xB8, (byte) 0x72,
		(byte) 0xC0, (byte) 0xED, (byte) 0x7D, (byte) 0x68, (byte) 0xC9, (byte) 0x2E, (byte) 0x0D, (byte) 0x62,
		(byte) 0x46, (byte) 0x17, (byte) 0x11, (byte) 0x4D, (byte) 0x6C, (byte) 0xC4, (byte) 0x7E, (byte) 0x53,
		(byte) 0xC1, (byte) 0x25, (byte) 0xC7, (byte) 0x9A, (byte) 0x1C, (byte) 0x88, (byte) 0x58, (byte) 0x2C,
		(byte) 0x89, (byte) 0xDC, (byte) 0x02, (byte) 0x64, (byte) 0x40, (byte) 0x01, (byte) 0x5D, (byte) 0x38,
		(byte) 0xA5, (byte) 0xE2, (byte) 0xAF, (byte) 0x55, (byte) 0xD5, (byte) 0xEF, (byte) 0x1A, (byte) 0x7C,
		(byte) 0xA7, (byte) 0x5B, (byte) 0xA6, (byte) 0x6F, (byte) 0x86, (byte) 0x9F, (byte) 0x73, (byte) 0xE6,
		(byte) 0x0A, (byte) 0xDE, (byte) 0x2B, (byte) 0x99, (byte) 0x4A, (byte) 0x47, (byte) 0x9C, (byte) 0xDF,
		(byte) 0x09, (byte) 0x76, (byte) 0x9E, (byte) 0x30, (byte) 0x0E, (byte) 0xE4, (byte) 0xB2, (byte) 0x94,
		(byte) 0xA0, (byte) 0x3B, (byte) 0x34, (byte) 0x1D, (byte) 0x28, (byte) 0x0F, (byte) 0x36, (byte) 0xE3,
		(byte) 0x23, (byte) 0xB4, (byte) 0x03, (byte) 0xD8, (byte) 0x90, (byte) 0xC8, (byte) 0x3C, (byte) 0xFE,
		(byte) 0x5E, (byte) 0x32, (byte) 0x24, (byte) 0x50, (byte) 0x1F, (byte) 0x3A, (byte) 0x43, (byte) 0x8A,
		(byte) 0x96, (byte) 0x41, (byte) 0x74, (byte) 0xAC, (byte) 0x52, (byte) 0x33, (byte) 0xF0, (byte) 0xD9,
		(byte) 0x29, (byte) 0x80, (byte) 0xB1, (byte) 0x16, (byte) 0xD3, (byte) 0xAB, (byte) 0x91, (byte) 0xB9,
		(byte) 0x84, (byte) 0x7F, (byte) 0x61, (byte) 0x1E, (byte) 0xCF, (byte) 0xC5, (byte) 0xD1, (byte) 0x56,
		(byte) 0x3D, (byte) 0xCA, (byte) 0xF4, (byte) 0x05, (byte) 0xC6, (byte) 0xE5, (byte) 0x08, (byte) 0x49
	};

	public static void aesCrypt(byte[] data, byte[] iv) {
		int remaining = data.length;
		int llength = 0x5B0;
		int start = 0;
		Cipher ciph;
		try {
			ciph = aesCiphers.getCipher();
		} catch (Exception ex) {
			LOG.log(Level.WARNING, "Could not make AES cipher", ex);
			return;
		}
		while (remaining > 0) {
			byte[] myIv = ByteTool.multiplyBytes(iv, 4, 4);
			if (remaining < llength)
				llength = remaining;
			for (int x = 0; x < llength; x++) {
				int myIvIndex = x % myIv.length;
				if (myIvIndex == 0) {
					try {
						System.arraycopy(ciph.doFinal(myIv), 0, myIv, 0, myIv.length);
					} catch (Exception ex) {
						LOG.log(Level.WARNING, "Could not encrypt mvIv", ex);
					}
				}
				data[x + start] ^= myIv[myIvIndex];
			}
			start += llength;
			remaining -= llength;
			llength = 0x5B4;
		}
	}

	/**
	 * Generates a packet header for a packet that is <code>length</code>
	 * long.
	 *
	 * @param length How long the packet that this header is for is.
	 * @return The header.
	 */
	public static byte[] makePacketHeader(int length, byte[] iv) {
		int v = (((iv[3] & 0xFF) << 8) | (iv[2] & 0xFF)) ^ ~GlobalConstants.MAPLE_VERSION; //version
		int l = v ^ length; //length
		//write v and l as two 16-bit little-endian integers
		return new byte[] {
			(byte) (v & 0xFF), (byte) ((v >>> 8) & 0xFF),
			(byte) (l & 0xFF), (byte) ((l >>> 8) & 0xFF)
		};
	}

	/**
	 * Gets the packet length from a header.
	 *
	 * @param packetHeader The bytes of the header.
	 * @return The length of the packet.
	 */
	public static int getPacketLength(byte[] packetHeader) {
		//read two 16-bit little-endian integers and XOR them.
		return (((packetHeader[0] & 0xFF) | ((packetHeader[1] & 0xFF) << 8)) ^
				((packetHeader[2] & 0xFF) | ((packetHeader[3] & 0xFF) << 8)));
	}

	/**
	 * Check the packet to make sure it has a header and verify it is valid.
	 *
	 * @param packet The packet to check.
	 * @return <code>True</code> if the packet has a correct header,
	 *         <code>false</code> otherwise.
	 */
	public static boolean checkPacket(byte[] packetHeader, byte[] iv) {
		return ((((packetHeader[0] ^ iv[2]) & 0xFF) == (GlobalConstants.MAPLE_VERSION & 0xFF)) &&
				(((packetHeader[1] ^ iv[3]) & 0xFF) == ((GlobalConstants.MAPLE_VERSION & 0xFF) >> 8)));
	}

	//The below aren't actually AESOFB, they're just MapleStory's custom
	//encryption routines (that's why we named the class MapleAESOFB, not just
	//AESOFB!)
	public static byte[] nextIv(byte[] oldIv) {
		byte[] newIv = { (byte) 0xF2, (byte) 0x53, (byte) 0x50, (byte) 0xC6 };
		for (int x = 0; x < oldIv.length; x++) {
			byte temp1 = newIv[1];
			byte temp2 = oldIv[x];
			byte temp3 = ivKeys[temp1 & 0xFF];
			temp3 -= temp2;
			newIv[0] += temp3;
			temp3 = newIv[2];
			temp3 ^= ivKeys[temp2 & 0xFF];
			temp1 -= temp3;
			newIv[1] = temp1;
			temp1 = newIv[3];
			temp3 = temp1;
			temp1 -= newIv[0];
			temp3 = ivKeys[temp3 & 0xFF];
			temp3 += temp2;
			temp3 ^= newIv[2];
			newIv[2] = temp3;
			temp1 += ivKeys[temp2 & 0xFF];
			newIv[3] = temp1;

			//essentially reverses the byte order of newIv, rotates all bits
			//3 to the left, then reverses the byte order again
			temp1 = (byte) ((newIv[3] & 0xFF) >>> 5); //the "carry"
			newIv[3] = (byte) (newIv[3] << 3 | (newIv[2] & 0xFF) >>> 5);
			newIv[2] = (byte) (newIv[2] << 3 | (newIv[1] & 0xFF) >>> 5);
			newIv[1] = (byte) (newIv[1] << 3 | (newIv[0] & 0xFF) >>> 5);
			newIv[0] = (byte) (newIv[0] << 3 | temp1);
		}
		return newIv;
	}

	/**
	 * Encrypts <code>data</code> with Maple's encryption routines.
	 *
	 * @param data The data to encrypt.
	 * @return The encrypted data.
	 */
	public static byte[] mapleEncrypt(byte[] data) {
		for (int j = 0; j < 6; j++) {
			byte remember = 0;
			byte dataLength = (byte) (data.length & 0xFF);
			if (j % 2 == 0) {
				for (int i = 0; i < data.length; i++) {
					byte cur = data[i];
					cur = ByteTool.rollLeft(cur, 3);
					cur += dataLength;
					cur ^= remember;
					remember = cur;
					cur = ByteTool.rollRight(cur, dataLength & 0xFF);
					cur = ((byte) (~cur & 0xFF));
					cur += 0x48;
					dataLength--;
					data[i] = cur;
				}
			} else {
				for (int i = data.length - 1; i >= 0; i--) {
					byte cur = data[i];
					cur = ByteTool.rollLeft(cur, 4);
					cur += dataLength;
					cur ^= remember;
					remember = cur;
					cur ^= 0x13;
					cur = ByteTool.rollRight(cur, 3);
					dataLength--;
					data[i] = cur;
				}
			}
		}
		return data;
	}

	/**
	 * Decrypts <code>data</code> with Maple's encryption routines.
	 *
	 * @param data The data to decrypt.
	 * @return The decrypted data.
	 */
	public static byte[] mapleDecrypt(byte[] data) {
		for (int j = 1; j <= 6; j++) {
			byte remember = 0;
			byte dataLength = (byte) (data.length & 0xFF);
			byte nextRemember = 0;

			if (j % 2 == 0) {
				for (int i = 0; i < data.length; i++) {
					byte cur = data[i];
					cur -= 0x48;
					cur = ((byte) (~cur & 0xFF));
					cur = ByteTool.rollLeft(cur, dataLength & 0xFF);
					nextRemember = cur;
					cur ^= remember;
					remember = nextRemember;
					cur -= dataLength;
					cur = ByteTool.rollRight(cur, 3);
					data[i] = cur;
					dataLength--;
				}
			} else {
				for (int i = data.length - 1; i >= 0; i--) {
					byte cur = data[i];
					cur = ByteTool.rollLeft(cur, 3);
					cur ^= 0x13;
					nextRemember = cur;
					cur ^= remember;
					remember = nextRemember;
					cur -= dataLength;
					cur = ByteTool.rollRight(cur, 4);
					data[i] = cur;
					dataLength--;
				}
			}
		}
		return data;
	}

	private MapleAesOfb() {
		//uninstantiable...
	}
}
