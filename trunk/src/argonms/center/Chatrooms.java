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

package argonms.center;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author GoldenKevin
 */
public class Chatrooms {
	private final AtomicInteger nextRoomId;
	private final Map<Integer, Chatroom> rooms;

	public Chatrooms() {
		nextRoomId = new AtomicInteger();
		rooms = new ConcurrentHashMap<Integer, Chatroom>();
	}

	/**
	 * 
	 * @param creator
	 * @return the unique roomId of the newly created chatroom
	 */
	public int makeNewRoom(Chatroom.Avatar creator) {
		//make sure we never return 0, because the client treats 0 specially
		int roomId = nextRoomId.incrementAndGet();
		rooms.put(Integer.valueOf(roomId), new Chatroom(creator));
		return roomId;
	}

	public Chatroom remove(int roomId) {
		return rooms.remove(Integer.valueOf(roomId));
	}

	public Chatroom get(int roomId) {
		return rooms.get(Integer.valueOf(roomId));
	}

	public void set(int roomid, Chatroom room) {
		rooms.put(Integer.valueOf(roomid), room);
	}
}
