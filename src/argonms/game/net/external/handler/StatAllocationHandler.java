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

package argonms.game.net.external.handler;

import argonms.common.character.Skills;
import argonms.common.net.external.CheatTracker;
import argonms.common.util.input.LittleEndianReader;
import argonms.game.character.ClientUpdateKey;
import argonms.game.character.GameCharacter;
import argonms.game.character.SkillMacro;
import argonms.game.loading.skill.SkillDataLoader;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;
import java.util.EnumMap;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public final class StatAllocationHandler {
	public static void handleApAllocation(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		/*int time = */packet.readInt();
		int updateMask = packet.readInt();
		Map<ClientUpdateKey, Short> updatedStats = new EnumMap<ClientUpdateKey, Short>(ClientUpdateKey.class);
		p.writeLockStats();
		try {
			for (ClientUpdateKey key : ClientUpdateKey.valueOf(updateMask)) {
				if (p.getAp() <= 0)
					return;
				switch (key) {
					case STR:
						if (p.getStr() < Short.MAX_VALUE) {
							updatedStats.put(ClientUpdateKey.STR, Short.valueOf(p.incrementLocalStr()));
							updatedStats.put(ClientUpdateKey.AVAILABLEAP, Short.valueOf(p.decrementLocalAp()));
						}
						break;
					case DEX:
						if (p.getDex() < Short.MAX_VALUE) {
							updatedStats.put(ClientUpdateKey.DEX, Short.valueOf(p.incrementLocalDex()));
							updatedStats.put(ClientUpdateKey.AVAILABLEAP, Short.valueOf(p.decrementLocalAp()));
						}
						break;
					case INT:
						if (p.getInt() < Short.MAX_VALUE) {
							updatedStats.put(ClientUpdateKey.INT, Short.valueOf(p.incrementLocalInt()));
							updatedStats.put(ClientUpdateKey.AVAILABLEAP, Short.valueOf(p.decrementLocalAp()));
						}
						break;
					case LUK:
						if (p.getLuk() < Short.MAX_VALUE) {
							updatedStats.put(ClientUpdateKey.LUK, Short.valueOf(p.incrementLocalLuk()));
							updatedStats.put(ClientUpdateKey.AVAILABLEAP, Short.valueOf(p.decrementLocalAp()));
						}
						break;
					case MAXHP:
						if (p.getMaxHp() < 30000) {
							byte level;
							int increase = 1;
							if ((level = p.getSkillLevel(Skills.IMPROVE_MAXHP)) > 0)
								increase += SkillDataLoader.getInstance().getSkill(Skills.IMPROVE_MAXHP).getLevel(level).getY();
							if ((level = p.getSkillLevel(Skills.IMPROVED_MAXHP_INCREASE)) > 0)
								increase += SkillDataLoader.getInstance().getSkill(Skills.IMPROVED_MAXHP_INCREASE).getLevel(level).getY();
							updatedStats.put(ClientUpdateKey.MAXHP, Short.valueOf(p.incrementMaxHp(increase)));
							updatedStats.put(ClientUpdateKey.AVAILABLEAP, Short.valueOf(p.decrementLocalAp()));
						}
						break;
					case MAXMP:
						if (p.getMaxMp() < 30000) {
							byte level;
							int increase = 1;
							if ((level = p.getSkillLevel(Skills.IMPROVED_MAXMP_INCREASE)) > 0)
								increase += SkillDataLoader.getInstance().getSkill(Skills.IMPROVED_MAXMP_INCREASE).getLevel(level).getY();
							updatedStats.put(ClientUpdateKey.MAXMP, Short.valueOf(p.incrementMaxMp(increase)));
							updatedStats.put(ClientUpdateKey.AVAILABLEAP, Short.valueOf(p.decrementLocalAp()));
						}
						break;
				}
			}
		} finally {
			p.writeUnlockStats();
		}
		gc.getSession().send(GamePackets.writeUpdatePlayerStats(updatedStats, true));
	}

	public static void handleSpAllocation(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		/*int time = */packet.readInt();
		int skillId = packet.readInt();
		p.writeLockStats();
		try {
			byte newLevel = (byte) (p.getSkillLevel(skillId) + 1);
			if (newLevel <= p.getMasterSkillLevel(skillId)) {
				if (Skills.isBeginnerSkill(skillId)) {
					p.setSkillLevel(skillId, newLevel, (byte) -1, false);
				} else {
					if (p.getSp() > 0) {
						p.setSp((short) (p.getSp() - 1));
						p.setSkillLevel(skillId, newLevel, (byte) -1, false);
					}
				}
			} else {
				CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to allocate SP to a mastered skill");
			}
		} finally {
			p.writeUnlockStats();
		}
	}

	public static void handleSkillMacroAssign(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		byte count = packet.readByte();
		SkillMacro[] macros = new SkillMacro[count];
		for (byte i = 0; i < count; i++) {
			String name = packet.readLengthPrefixedString();
			boolean silent = packet.readBool();
			int skill1 = packet.readInt();
			int skill2 = packet.readInt();
			int skill3 = packet.readInt();

			macros[i] = new SkillMacro(name, silent, skill1, skill2, skill3);
		}
		p.setMacros(macros);
	}

	private StatAllocationHandler() {
		//uninstantiable...
	}
}
