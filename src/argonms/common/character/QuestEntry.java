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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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

	private volatile byte state;
	private final Map<Integer, AtomicInteger> mobCount;
	private volatile long completionTime;

	/**
	 * Start quest with no mob kill progress.
	 * @param state
	 * @param mobsToWatch
	 */
	public QuestEntry(byte state, Set<Integer> mobsToWatch) {
		this.state = state;
		Map<Integer, AtomicInteger> tempMobCount = new LinkedHashMap<Integer, AtomicInteger>(mobsToWatch.size());
		for (Integer mobId : mobsToWatch)
			tempMobCount.put(mobId, new AtomicInteger(0));
		mobCount = Collections.unmodifiableMap(tempMobCount);
	}

	/**
	 * Start quest with the given mob kill progress.
	 * @param state
	 * @param mobKillsProgress
	 */
	public QuestEntry(byte state, Map<Integer, Short> mobKillsProgress) {
		this.state = state;
		Map<Integer, AtomicInteger> tempMobCount = new LinkedHashMap<Integer, AtomicInteger>(mobKillsProgress.size());
		for (Entry<Integer, Short> mob : mobKillsProgress.entrySet())
			tempMobCount.put(mob.getKey(), new AtomicInteger(mob.getValue().shortValue()));
		mobCount = Collections.unmodifiableMap(tempMobCount);
	}

	public void updateState(byte newState) {
		state = newState;
	}

	public String getData() {
		StringBuilder sb = new StringBuilder(mobCount.size() * 3);
		for (AtomicInteger c : mobCount.values())
			sb.append(String.format("%03d", c.get()));
		return sb.toString();
	}

	public long getCompletionTime() {
		return completionTime;
	}

	public void setCompletionTime(long time) {
		completionTime = time;
	}

	/**
	 * Atomically adds a value to an AtomicInteger and clip the resulting value
	 * to within a certain range. This is equivalent to
	 * <pre>
	 *   int newValue = i.addAndGet(delta);
	 *   if (i.get() &lt; min)
	 *       i.set(min);
	 *   if (i.get() &gt; max)
	 *       i.set(max);
	 *   return newValue;</pre>
	 * except that the action is performed atomically.
	 * @param i the AtomicInteger instance
	 * @param delta the value to add
	 * @param min inclusive
	 * @param max inclusive
	 * @return the value of <tt>i</tt> after the clamped add.
	 */
	private int clampedAdd(AtomicInteger i, int delta, int min, int max) {
		//copied from AtomicInteger.addAndGet(int). only difference is that we
		//set the value to the clamped next and return the clamped next.
		while (true) {
			int current = i.get();
			int next = Math.min(Math.max(current + delta, min), max);
			if (i.compareAndSet(current, next))
				return next;
		}
	}

	public int mobKilled(Integer mobId, short max) {
		return clampedAdd(mobCount.get(mobId), 1, 0, max);
	}

	public byte getState() {
		return state;
	}

	public short getMobCount(int mobId) {
		AtomicInteger count = mobCount.get(Integer.valueOf(mobId));
		return (short) (count != null ? count.get() : 0);
	}

	public Map<Integer, Short> getAllMobCounts() {
		Map<Integer, Short> counts = new LinkedHashMap<Integer, Short>(mobCount.size());
		for (Entry<Integer, AtomicInteger> entry : mobCount.entrySet())
			counts.put(entry.getKey(), Short.valueOf((short) entry.getValue().get()));
		return counts;
	}
}
