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

package argonms.game.character;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 *
 * @author GoldenKevin
 */
public class QuestEntry {
	public static final byte
		STATE_NOT_STARTED = 0,
		STATE_STARTED = 1,
		STATE_COMPLETED = 2
	;

	//for use with CommonPackets.writeQuestError
	public static final byte
		QUEST_START_SUCCESS = 0x08,
		QUEST_START_ERROR_UNKNOWN = 0x09, //??? don't ask me, I copied it from Vana!
		QUEST_START_ERROR_INVENTORY_FULL = 0x0A,
		QUEST_START_ERROR_INSUFFICIENT_FUNDS = 0x0B,
		QUEST_START_ERROR_EQUIP_WORN = 0x0D,
		QUEST_START_ERROR_ONLY_ONE = 0x0E,
		QUEST_START_ERROR_EXPIRED = 0x0F
	;

	private byte state;
	private final Map<Integer, Count> mobCount;
	private long completionTime;

	/**
	 * Start quest with no mob kill progress.
	 * @param state
	 * @param mobsToWatch
	 */
	public QuestEntry(byte state, Set<Integer> mobsToWatch) {
		this.state = state;
		mobCount = new LinkedHashMap<Integer, Count>(mobsToWatch.size());
		for (Integer mobId : mobsToWatch)
			mobCount.put(mobId, new Count());
	}

	/**
	 * Start quest with the given mob kill progress.
	 * @param state
	 * @param mobKillsProgress
	 */
	public QuestEntry(byte state, Map<Integer, Short> mobKillsProgress) {
		this.state = state;
		mobCount = new LinkedHashMap<Integer, Count>(mobKillsProgress.size());
		for (Entry<Integer, Short> mob : mobKillsProgress.entrySet())
			mobCount.put(mob.getKey(), new Count(mob.getValue().shortValue()));
	}

	public void updateState(byte newState) {
		state = newState;
	}

	public String getData() {
		StringBuilder sb = new StringBuilder(mobCount.size() * 3);
		for (Count c : mobCount.values())
			sb.append(c.getEncodedString());
		return sb.toString();
	}

	public long getCompletionTime() {
		return completionTime;
	}

	public void setCompletionTime(long time) {
		completionTime = time;
	}

	public void killedMob(int mobId) {
		Integer oId = Integer.valueOf(mobId);
		mobCount.get(oId).amount++;
	}

	public byte getState() {
		return state;
	}

	public short getMobCount(int mobId) {
		Count count = mobCount.get(Integer.valueOf(mobId));
		return count != null ? count.amount : (short) 0;
	}

	public Map<Integer, Short> getAllMobCounts() {
		Map<Integer, Short> counts = new LinkedHashMap<Integer, Short>(mobCount.size());
		for (Entry<Integer, Count> entry : mobCount.entrySet())
			counts.put(entry.getKey(), Short.valueOf(entry.getValue().amount));
		return counts;
	}

	//A mutable short class basically.
	private static class Count {
		public short amount;

		public Count() {
			amount = 0;
		}

		public Count(short initial) {
			amount = initial;
		}

		public String getEncodedString() {
			return String.format("%03d", amount);
		}
	}
}
