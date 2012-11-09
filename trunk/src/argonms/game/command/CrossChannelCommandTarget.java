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

package argonms.game.command;

import java.util.List;

/**
 *
 * @author GoldenKevin
 */
public class CrossChannelCommandTarget implements CommandTarget {
	private final byte targetChannel;
	private final String targetName;

	public CrossChannelCommandTarget(byte targetChannel, String targetName) {
		this.targetChannel = targetChannel;
		this.targetName = targetName;
	}

	private int getInt(CharacterManipulation update) {
		return ((Integer) update.getValue()).intValue();
	}

	private short getShort(CharacterManipulation update) {
		return ((Short) update.getValue()).shortValue();
	}

	//TODO: receiving end will construct a "List<CharacterManipulation> updates"
	//from the packet and construct a LocalChannelCommandTarget that parses
	//updates
	@Override
	public void mutate(List<CharacterManipulation> updates) {
		for (CharacterManipulation update : updates) {
			switch (update.getKey()) {
				case CHANGE_MAP:
					//writeInt
					//writeByte
					break;
				case CHANGE_CHANNEL:
					//writeByte
					break;
				case ADD_LEVEL:
				case SET_LEVEL:
				case SET_JOB:
				case ADD_STR:
				case SET_STR:
				case ADD_DEX:
				case SET_DEX:
				case ADD_INT:
				case SET_INT:
				case ADD_LUK:
				case SET_LUK:
				case ADD_AP:
				case SET_AP:
				case ADD_SP:
				case SET_SP:
				case ADD_MAX_HP:
				case SET_MAX_HP:
				case ADD_MAX_MP:
				case SET_MAX_MP:
				case ADD_HP:
				case SET_HP:
				case ADD_MP:
				case SET_MP:
				case ADD_FAME:
				case SET_FAME:
					//writeShort
					break;
				case ADD_EXP:
				case SET_EXP:
				case ADD_MESO:
				case SET_MESO:
					//writeInt
					break;
				case SET_SKILL_LEVEL:
					//writeInt
					//writeByte
					//writeByte
					break;
				case ADD_ITEM:
					//writeInt
					//writeInt
					break;
				case CANCEL_DEBUFFS:
					//writeLong of masks
					break;
				case MAX_ALL_EQUIP_STATS:
				case MAX_INVENTORY_SLOTS:
				case MAX_BUDDY_LIST_SLOTS:
					//do not write any additional bytes
					break;
			}
		}
	}

	//TODO: use response id interface in InterChannelCommunication for blocking retrievals
	@Override
	public Object access(CharacterProperty key) {
		switch (key) {
			case MAP:
				return null;
			case CHANNEL:
				return Byte.valueOf(targetChannel);
			case POSITION:
				return null;
			case PLAYER_ID:
				return null;
			default:
				return null;
		}
	}
}
