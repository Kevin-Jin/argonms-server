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

import java.awt.Point;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public class ShopPlayerContinuation extends AbstractPlayerContinuation {
	private final Map<Integer, BuffState.PlayerSummonState> activeSummons;

	private boolean cashShop;

	public ShopPlayerContinuation(Map<Integer, BuffState.ItemState> activeItems,
			Map<Integer, BuffState.SkillState> activeSkills,
			Map<Short, BuffState.MobSkillState> activeDebuffs,
			byte channel, short energyCharge, int chatroom, boolean isEnteringCashShop) {
		super(activeItems, activeSkills, activeDebuffs, channel, energyCharge);
		setChatroomId(chatroom);
		activeSummons = new HashMap<Integer, BuffState.PlayerSummonState>();
		cashShop = isEnteringCashShop;
	}

	public ShopPlayerContinuation() {
		super();
		activeSummons = new HashMap<Integer, BuffState.PlayerSummonState>();
	}

	public Map<Integer, BuffState.PlayerSummonState> getActiveSummons() {
		return activeSummons;
	}

	/**
	 * Returns false if the player is entering the MTS.
	 * @return 
	 */
	public boolean isEnteringCashShop() {
		return cashShop;
	}

	public void addActiveSummon(int skillId, Point pos, byte stance) {
		activeSummons.put(Integer.valueOf(skillId), new BuffState.PlayerSummonState(pos, stance));
	}

	public void setEnteringCashShop(boolean cashShop) {
		this.cashShop = cashShop;
	}

	public void compactForReturn() {
		long now = System.currentTimeMillis();
		for (Iterator<BuffState.ItemState> iter = getActiveItems().values().iterator(); iter.hasNext(); ) {
			BuffState.ItemState buff = iter.next();
			if (buff.endTime <= now)
				iter.remove();
		}
		for (Iterator<Map.Entry<Integer, BuffState.SkillState>> iter = getActiveSkills().entrySet().iterator(); iter.hasNext(); ) {
			Map.Entry<Integer, BuffState.SkillState> buff = iter.next();
			if (buff.getValue().endTime <= now) {
				iter.remove();
				activeSummons.remove(buff.getKey());
			}
		}
		for (Iterator<BuffState.MobSkillState> iter = getActiveDebuffs().values().iterator(); iter.hasNext(); ) {
			BuffState.MobSkillState buff = iter.next();
			if (buff.endTime <= now)
				iter.remove();
		}
		setChatroomId(0);
	}
}
