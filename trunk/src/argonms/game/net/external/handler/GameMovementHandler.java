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

import argonms.common.net.external.ClientSendOps;
import argonms.common.util.Rng;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.game.character.GameCharacter;
import argonms.game.field.MapEntity;
import argonms.game.field.MapEntity.EntityType;
import argonms.game.field.MonsterStatusEffectTools;
import argonms.game.field.entity.Mob;
import argonms.game.field.entity.PlayerSkillSummon;
import argonms.game.field.movement.AbsoluteLifeMovement;
import argonms.game.field.movement.ChairMovement;
import argonms.game.field.movement.ChangeEquipMovement;
import argonms.game.field.movement.FootholdChangedMovementFragment;
import argonms.game.field.movement.JumpDownMovement;
import argonms.game.field.movement.LifeMovementFragment;
import argonms.game.field.movement.LifeMovementFragment.UpdatedEntityInfo;
import argonms.game.field.movement.PositionChangedMovementFragment;
import argonms.game.field.movement.RelativeLifeMovement;
import argonms.game.field.movement.StanceChangedMovementFragment;
import argonms.game.field.movement.TeleportMovement;
import argonms.game.loading.mob.Skill;
import argonms.game.loading.skill.MobSkillEffectsData;
import argonms.game.loading.skill.SkillDataLoader;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author GoldenKevin
 */
public class GameMovementHandler {
	public static final byte
		NORMAL_MOVE = 0,
		JUMP = 1,
		JUMP_AND_KNOCKBACK = 2,
		UNK_SKILL = 3,
		TELEPORT = 4,
		NORMAL_MOVE_2 = 5,
		FLASH_JUMP = 6,
		ASSAULTER = 7,
		ASSASSINATE = 8,
		RUSH = 9,
		EQUIP = 10,
		CHAIR = 11,
		HORNTAIL_KNOCKBACK = 12,
		RECOIL_SHOT = 13,
		UNK = 14,
		JUMP_DOWN = 15,
		WINGS = 16,
		WINGS_FALL = 17
	;

	/**
	 * Finds the ceiling of (x / y)
	 * @param x must be non-negative
	 * @param y must be positive
	 * @return
	 */
	private static int ceil(int x, int y) {
		if (x == 0)
			return 0;
		return ((x - 1) / y) + 1;
	}

	public static void handleMovePlayer(LittleEndianReader packet, GameClient gc) {
		/*byte portalCount = */packet.readByte();
		Point startPos = packet.readPos();
		List<LifeMovementFragment> res = parseMovement(packet);
		//looks like there are exactly ceil(n / 2) bytes after the first byte,
		//where n is the value of the first byte. Probably the amount of hex
		//digits (2 hex digits per byte)
		int count = ceil(packet.readByte(), 2);
		//Lot of 1's when holding down, 2's when holding up, 4's when holding
		//right, 8 when holding left, and adding any two of them on diagonals
		//(5 on down right, 6 on up right, 9 on down left, A on up left). Looks
		//like some kind of bitmask. Client is probably telling us which keys
		//were being pressed during the movement.
		packet.skip(count);
		/*Point initialPos = */packet.readPos();
		/*Point finalPos = */packet.readPos();

		GameCharacter player = gc.getPlayer();
		updatePosition(res, player, 0);
		player.getMap().playerMoved(player, res, startPos);
	}

	public static void handleMovePet(LittleEndianReader packet, GameClient gc) {
		//TODO: implement
	}

	public static void handleMoveSummon(LittleEndianReader packet, GameClient gc) {
		int entId = packet.readInt();

		GameCharacter player = gc.getPlayer();
		//PlayerSkillSummon summon = p.getSummonBySkill(p.getEffectValue(PlayerStatusEffect.SUMMON).getSource());
		PlayerSkillSummon summon = (PlayerSkillSummon) player.getMap().getEntityById(EntityType.SUMMON, entId);
		if (summon == null)
			return;

		Point startPos = packet.readPos();
		List<LifeMovementFragment> res = parseMovement(packet);
		int count = ceil(packet.readByte(), 2);
		packet.skip(count);
		/*Point initialPos = */packet.readPos();
		/*Point finalPos = */packet.readPos();
		updatePosition(res, summon, 0);
		player.getMap().summonMoved(player, summon, res, startPos);
	}

	public static void handleMoveMob(LittleEndianReader packet, GameClient gc) {
		int entId = packet.readInt();
		short moveid = packet.readShort();

		GameCharacter player = gc.getPlayer();
		//TODO: Synchronize on the mob (for the canUseSkill, which gets Hp, and
		//the aggro things)
		Mob monster = (Mob) player.getMap().getEntityById(EntityType.MONSTER, entId);
		if (monster == null)
			return;

		List<LifeMovementFragment> res = null;
		boolean useSkill = packet.readBool();
		byte skill = packet.readByte();
		short skillId = (short) (packet.readByte() & 0xFF);
		byte skillLevel = packet.readByte();
		byte skill3 = packet.readByte();
		byte skill4 = packet.readByte();

		Skill skillToUse = null;
		MobSkillEffectsData skillToUseEffect = null;

		//TODO: I don't think we should apply a debuff again if it is already active to the player
		//this useSkill and skillId/skillLevel part is probably all wrong...
		//probably will "borrow" a working algorithm from Vana
		List<Skill> skills = monster.getSkills();
		int skillsCount = skills.size();
		if (useSkill && skillsCount > 0) {
			skillToUse = skills.get(Rng.getGenerator().nextInt(skillsCount));
			skillToUseEffect = SkillDataLoader.getInstance().getMobSkill(skillToUse.getSkill()).getLevel(skillToUse.getLevel());
			if (skillToUseEffect.makeChanceResult() && monster.canUseSkill(skillToUseEffect))
				MonsterStatusEffectTools.applyEffectsAndShowVisuals(monster, player, skillToUseEffect);
			else
				skillToUse = null;
		}

		if ((skillId >= 100 && skillId <= 200) && monster.hasSkill(skillId, skillLevel)) {
			skillToUseEffect = SkillDataLoader.getInstance().getMobSkill(skillId).getLevel(skillLevel);
			if (skillToUseEffect.makeChanceResult() && monster.canUseSkill(skillToUseEffect))
				MonsterStatusEffectTools.applyEffectsAndShowVisuals(monster, player, skillToUseEffect);
		}

		packet.readByte();
		packet.readInt();

		GameCharacter controller = monster.getController();
		if (controller != player) {
			if (monster.wasAttackedBy(player)) { //aggro and controller change
				if (controller != null) {
					controller.uncontrolMonster(monster);
					controller.getClient().getSession().send(GamePackets.writeStopControlMonster(monster));
				}
				monster.setController(player);
				player.controlMonster(monster);
				player.getClient().getSession().send(GamePackets.writeShowAndControlMonster(monster, true));
				monster.setControllerHasAggro(true);
				monster.setControllerKnowsAboutAggro(false);
			} else {
				//TODO: is this a hacking/packet-editing attempt?
				return;
			}
		} else if (skill == -1 && monster.controllerKnowsAboutAggro() && !monster.isMobile() && !monster.isFirstAttack()) {
			monster.setControllerHasAggro(false);
			monster.setControllerKnowsAboutAggro(false);
		}
		boolean aggro = monster.controllerHasAggro();

		if (skillToUse != null)
			gc.getSession().send(moveMonsterResponse(entId, moveid, monster.getMp(), aggro, skillToUse.getSkill(), skillToUse.getLevel()));
		else
			gc.getSession().send(moveMonsterResponse(entId, moveid, monster.getMp(), aggro, (short) 0, (byte) 0));

		if (aggro)
			monster.setControllerKnowsAboutAggro(true);

		Point startPos = packet.readPos();
		res = parseMovement(packet);
		int count = ceil(packet.readByte(), 2);
		packet.skip(count);
		/*Point initialPos = */packet.readPos();
		/*Point finalPos = */packet.readPos();
		updatePosition(res, monster, -1);
		player.getMap().monsterMoved(player, monster, res, useSkill, skill, skillId, skillLevel, skill3, skill4, startPos);
	}

	public static void handleMoveNpc(LittleEndianReader packet, GameClient gc) {
		//too complicated to add one NPC animator per map (mobs were bad enough)
		//so we'll just let all clients animate their own NPCs and echo back
		//what they send to us.
		LittleEndianByteArrayWriter lew;
		int remaining = packet.available();
		if (remaining == 6) { //speech bubble
			lew = new LittleEndianByteArrayWriter(8);
			lew.writeShort(ClientSendOps.MOVE_NPC);
			//int - entityid, short - messageid
			lew.writeBytes(packet.readBytes(remaining));
		} else { //actual movement
			lew = new LittleEndianByteArrayWriter(remaining - 7);
			lew.writeShort(ClientSendOps.MOVE_NPC);
			lew.writeBytes(packet.readBytes(remaining - 9));
		}
		gc.getSession().send(lew.getBytes());
	}

	private static List<LifeMovementFragment> parseMovement(LittleEndianReader packet) {
		List<LifeMovementFragment> res = new ArrayList<LifeMovementFragment>();
		int numCommands = packet.readByte();
		for (int i = 0; i < numCommands; i++) {
			byte command = packet.readByte();
			switch (command) {
				case NORMAL_MOVE:
				case NORMAL_MOVE_2:
				case WINGS_FALL: {
					Point pos = packet.readPos();
					Point wobble = packet.readPos();
					short foothold = packet.readShort();
					byte stance = packet.readByte();
					short duration = packet.readShort();
					res.add(new AbsoluteLifeMovement(command, pos, wobble, foothold, stance, duration));
					break;
				} case JUMP:
				case JUMP_AND_KNOCKBACK:
				case FLASH_JUMP:
				case HORNTAIL_KNOCKBACK:
				case RECOIL_SHOT:
				case WINGS: {
					Point pos = packet.readPos();
					byte stance = packet.readByte();
					short foothold = packet.readShort();
					res.add(new RelativeLifeMovement(command, pos, stance, foothold));
					break;
				} case UNK_SKILL:
				case TELEPORT:
				case ASSAULTER:
				case ASSASSINATE:
				case RUSH:
				case UNK: {
					Point pos = packet.readPos();
					Point wobble = packet.readPos();
					byte stance = packet.readByte();
					res.add(new TeleportMovement(command, pos, wobble, stance));
					break;
				} case EQUIP: {
					byte count = packet.readByte();
					res.add(new ChangeEquipMovement(count));
					break;
				} case CHAIR: {
					Point pos = packet.readPos();
					short foothold = packet.readShort();
					byte stance = packet.readByte();
					short duration = packet.readShort();
					res.add(new ChairMovement(pos, foothold, stance, duration));
					break;
				} case JUMP_DOWN: {
					Point pos = packet.readPos();
					Point wobble = packet.readPos();
					short unk = packet.readShort();
					short foothold = packet.readShort();
					byte stance = packet.readByte();
					short duration = packet.readShort();
					res.add(new JumpDownMovement(pos, wobble, unk, foothold, stance, duration));
					break;
				} default: {
					return null;
				}
			}
		}
		return res;
	}

	private static void updatePosition(List<LifeMovementFragment> movement, MapEntity target, int yoffset) {
		for (LifeMovementFragment move : movement) {
			for (UpdatedEntityInfo stat : move.updatedStats()) {
				switch (stat) {
					case POSITION:
						Point pos = ((PositionChangedMovementFragment) move).getPosition();
						target.setPosition(new Point(pos.x, pos.y + yoffset));
						break;
					case FOOTHOLD:
						target.setFoothold(((FootholdChangedMovementFragment) move).getFoothold());
						break;
					case STANCE:
						target.setStance(((StanceChangedMovementFragment) move).getStance());
						break;
				}
			}
		}
	}

	private static byte[] moveMonsterResponse(int entityid, short moveid, int currentMp, boolean useSkills, short skillId, byte skillLevel) {
		LittleEndianByteArrayWriter mplew = new LittleEndianByteArrayWriter(13);

		mplew.writeShort(ClientSendOps.MOVE_MONSTER_RESPONSE);
		mplew.writeInt(entityid);
		mplew.writeShort(moveid);
		mplew.writeBool(useSkills);
		mplew.writeShort((short) currentMp);
		mplew.writeByte((byte) skillId);
		mplew.writeByte(skillLevel);

		return mplew.getBytes();
	}
}
