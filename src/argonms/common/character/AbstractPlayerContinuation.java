/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2013  GoldenKevin
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

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public abstract class AbstractPlayerContinuation {
	private final Map<Integer, BuffState.ItemState> activeItems;
	private final Map<Integer, BuffState.SkillState> activeSkills;
	private final Map<Short, BuffState.MobSkillState> activeDebuffs;
	private short energyCharge;
	private int chatroomId;

	public AbstractPlayerContinuation(Map<Integer, BuffState.ItemState> activeItems,
			Map<Integer, BuffState.SkillState> activeSkills,
			Map<Short, BuffState.MobSkillState> activeDebuffs,
			short energyCharge) {
		this.activeItems = activeItems;
		this.activeSkills = activeSkills;
		this.activeDebuffs = activeDebuffs;
	}

	public AbstractPlayerContinuation() {
		activeItems = new HashMap<Integer, BuffState.ItemState>();
		activeSkills = new HashMap<Integer, BuffState.SkillState>();
		activeDebuffs = new HashMap<Short, BuffState.MobSkillState>();
	}

	public Map<Integer, BuffState.ItemState> getActiveItems() {
		return activeItems;
	}

	public Map<Integer, BuffState.SkillState> getActiveSkills() {
		return activeSkills;
	}

	public Map<Short, BuffState.MobSkillState> getActiveDebuffs() {
		return activeDebuffs;
	}

	public short getEnergyCharge() {
		return energyCharge;
	}

	public int getChatroomId() {
		return chatroomId;
	}

	public void addItemBuff(int itemId, long endTime) {
		activeItems.put(Integer.valueOf(itemId), new BuffState.ItemState(endTime));
	}

	public void addSkillBuff(int skillId, byte level, long endTime) {
		activeSkills.put(Integer.valueOf(skillId), new BuffState.SkillState(level, endTime));
	}

	public void addMonsterDebuff(short skillId, byte level, long endTime) {
		activeDebuffs.put(Short.valueOf(skillId), new BuffState.MobSkillState(level, endTime));
	}

	public void setEnergyCharge(short val) {
		energyCharge = val;
	}

	public void setChatroomId(int roomId) {
		chatroomId = roomId;
	}
}
