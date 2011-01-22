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

package argonms.net;

import argonms.tools.HexTool;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Taken from an OdinMS-derived source, originally named LoginCrypto.
 * @author Frz
 */
public class HashFunctions {
	private static Random rand = new Random();

	private HashFunctions() {
	}

	private static String toSimpleHexString(byte[] bytes) {
		return HexTool.toString(bytes).replace(" ", "").toLowerCase();
	}

	private static String hashWithDigest(String in, String digest) {
		try {
			MessageDigest Digester = MessageDigest.getInstance(digest);
			Digester.update(in.getBytes("UTF-8"), 0, in.length());
			byte[] sha1Hash = Digester.digest();
			return toSimpleHexString(sha1Hash);
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException("Hashing the password failed", ex);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Encoding the string failed", e);
		}

	}

	public static String hexSha1(String in) {
		return hashWithDigest(in, "SHA-1");
	}

	private static String hexSha512(String in) {
		return hashWithDigest(in, "SHA-512");
	}

	public static boolean checkSha1Hash(String hash, String password) {
		return hash.equals(hexSha1(password));
	}

	public static boolean checkSaltedSha512Hash(String hash, String password, String salt) {
		return hash.equals(makeSaltedSha512Hash(password, salt));
	}

	public static String makeSaltedSha512Hash(String password, String salt) {
		return hexSha512(password + salt);
	}

	public static String makeSalt() {
		byte[] salt = new byte[16];
		rand.nextBytes(salt);
		return toSimpleHexString(salt);
	}
}