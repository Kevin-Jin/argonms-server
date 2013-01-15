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

package argonms.common.character;

/**
 *
 * @author GoldenKevin
 */
public class BuddyListEntry {
	public static final byte
		OFFLINE_CHANNEL = 0,
		CASH_SHOP_CHANNEL = 21
	;

	private final int id;
	private final String name;
	private byte channel;
	private byte status;

	public BuddyListEntry(int id, String name, byte status, byte channel) {
		this.id = id;
		this.name = name;
		this.status = status;
		this.channel = channel;
	}

	public BuddyListEntry(int id, String name, byte status) {
		this(id, name, status, (byte) 0);
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	/**
	 *
	 * @return the channel that this buddy is on. If the buddy is offline,
	 * {@link #OFFLINE_CHANNEL} will be returned. If the buddy is in the cash
	 * shop, {@link #CASH_SHOP_CHANNEL} will be returned.
	 */
	public byte getChannel() {
		return channel;
	}

	public void setChannel(byte channel) {
		this.channel = channel;
	}

	public byte getStatus() {
		return status;
	}

	public void setStatus(byte status) {
		this.status = status;
	}
}
