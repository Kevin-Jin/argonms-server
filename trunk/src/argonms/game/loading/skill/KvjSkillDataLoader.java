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

package argonms.game.loading.skill;

import argonms.common.loading.KvjEffects;
import argonms.common.tools.input.LittleEndianByteArrayReader;
import argonms.common.tools.input.LittleEndianReader;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class KvjSkillDataLoader extends SkillDataLoader {
	private static final Logger LOG = Logger.getLogger(KvjSkillDataLoader.class.getName());

	private static final byte //skill props
		NEXT_SKILL = 1,
		ELEM_ATTR = 2,
		DELAY = 3,
		SUMMON = 4,
		PREPARED = 5,
		KEY_DOWN = 6,
		KEY_DOWN_END = 7,
		NEXT_LEVEL = 8
	;

	private String dataPath;

	protected KvjSkillDataLoader(String wzPath) {
		this.dataPath = wzPath;
	}

	protected void loadPlayerSkill(int skillid) {
		String id = String.format("%07d", skillid);

		try {
			File f = new File(new StringBuilder(dataPath).append("Skill.wz").append(File.separator).append(id.substring(0, 3)).append(".img.kvj").toString());
			if (f.exists())
				doWork(new LittleEndianByteArrayReader(f));
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Could not read KVJ data file for skill " + skillid, e);
		}
	}

	protected void loadMobSkill(short skillid) {
		try {
			File f = new File(new StringBuilder(dataPath).append("Skill.wz").append(File.separator).append("MobSkill.img.kvj").toString());
			if (f.exists())
				doWork(new LittleEndianByteArrayReader(f));
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Could not read KVJ data file for mob skill " + skillid, e);
		}
	}

	public boolean loadAll() {
		try {
			File root = new File(dataPath + "Skill.wz");
			for (String kvj : root.list()) {
				if (kvj.equals("MobSkill.img.kvj")) {
					doMobWork(new LittleEndianByteArrayReader(new File(root.getAbsolutePath() + File.separatorChar + kvj)));
					//InputStream is = new BufferedInputStream(new FileInputStream(root.getAbsolutePath() + File.separatorChar + kvj));
					//doMobWork(new LittleEndianStreamReader(is));
					//is.close();
				} else {
					doWork(new LittleEndianByteArrayReader(new File(root.getAbsolutePath() + File.separatorChar + kvj)));
					//InputStream is = new BufferedInputStream(new FileInputStream(root.getAbsolutePath() + File.separatorChar + kvj));
					//doWork(new LittleEndianStreamReader(is));
					//is.close();
				}
			}
			return true;
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Could not load all skill data from KVJ files.", ex);
			return false;
		}
	}

	//TODO: Actually do real work to see if the skill exists in the file so we
	//can name this method exists() instead of loadable?
	public boolean canLoadPlayerSkill(int skillid) {
		if (skillStats.containsKey(Integer.valueOf(skillid)))
			return true;
		String id = String.format("%07d", skillid);
		File f = new File(new StringBuilder(dataPath).append("Skill.wz").append(File.separator).append(id.substring(0, 3)).append(".img.kvj").toString());
		return f.exists();
	}

	//TODO: Actually do real work to see if the skill exists in the file so we
	//can name this method exists() instead of loadable?
	public boolean canLoadMobSkill(short skillid) {
		if (mobSkillStats.containsKey(Short.valueOf(skillid)))
			return true;
		File f = new File(new StringBuilder(dataPath).append("Skill.wz").append(File.separator).append("MobSkill.img.kvj").toString());
		return f.exists();
	}

	private void doWork(LittleEndianReader reader) {
		SkillStats stats = null;
		byte level;
		int skillid = -1;
		for (byte now = reader.readByte(); now != -1; now = reader.readByte()) {
			switch (now) {
				case NEXT_SKILL:
					stats = new SkillStats();
					skillid = reader.readInt();
					skillStats.put(Integer.valueOf(skillid), stats);
					break;
				case ELEM_ATTR:
					stats.setElementalAttribute(reader.readNullTerminatedString());
					break;
				case DELAY:
					stats.setDelay(reader.readInt());
					break;
				case SUMMON:
					stats.setSummonType(reader.readByte());
					break;
				case PREPARED:
					stats.setPrepared();
					break;
				case KEY_DOWN:
					stats.setKeydown();
					break;
				case KEY_DOWN_END:
					stats.setKeydownEnd();
					break;
				case NEXT_LEVEL:
					level = reader.readByte();
					stats.addLevel(level, processEffect(skillid, level, reader));
					break;
			}
		}
	}

	private PlayerSkillEffectsData processEffect(int skillid, byte level, LittleEndianReader reader) {
		PlayerSkillEffectsData effect = new PlayerSkillEffectsData(skillid, level);
		loop:
		for (byte now = reader.readByte(); now != -1; now = reader.readByte()) {
			switch (now) {
				case KvjEffects.MP_CONSUME:
					effect.setMpConsume(reader.readShort());
					break;
				case KvjEffects.HP_CONSUME:
					effect.setHpConsume(reader.readShort());
					break;
				case KvjEffects.DURATION:
					effect.setDuration(reader.readInt());
					break;
				case KvjEffects.X:
					effect.setX(reader.readInt());
					break;
				case KvjEffects.Y:
					effect.setY(reader.readInt());
					break;
				case KvjEffects.Z:
					effect.setZ(reader.readInt());
					break;
				case KvjEffects.DAMAGE:
					effect.setDamage(reader.readShort());
					break;
				case KvjEffects.LT:
					effect.setLt(reader.readShort(), reader.readShort());
					break;
				case KvjEffects.RB:
					effect.setRb(reader.readShort(), reader.readShort());
					break;
				case KvjEffects.MOB_COUNT:
					effect.setMobCount(reader.readByte());
					break;
				case KvjEffects.PROP:
					effect.setProp(reader.readShort());
					break;
				case KvjEffects.MASTERY:
					effect.setMastery(reader.readByte());
					break;
				case KvjEffects.COOLTIME:
					effect.setCooltime(reader.readShort());
					break;
				case KvjEffects.RANGE:
					effect.setRange(reader.readShort());
					break;
				case KvjEffects.WATK:
					effect.setWatk(reader.readShort());
					break;
				case KvjEffects.WDEF:
					effect.setWdef(reader.readShort());
					break;
				case KvjEffects.MATK:
					effect.setMatk(reader.readShort());
					break;
				case KvjEffects.MDEF:
					effect.setMdef(reader.readShort());
					break;
				case KvjEffects.ACC:
					effect.setAcc(reader.readShort());
					break;
				case KvjEffects.AVOID:
					effect.setAvoid(reader.readShort());
					break;
				case KvjEffects.HP:
					effect.setHpRecoverRate(reader.readShort());
					break;
				case KvjEffects.MP:
					effect.setMpRecoverRate(reader.readShort());
					break;
				case KvjEffects.SPEED:
					effect.setSpeed(reader.readShort());
					break;
				case KvjEffects.JUMP:
					effect.setJump(reader.readShort());
					break;
				case KvjEffects.ATTACK_COUNT:
					effect.setAttackCount(reader.readByte());
					break;
				case KvjEffects.BULLET_COUNT:
					effect.setBulletCount(reader.readByte());
					break;
				case KvjEffects.ITEM_CONSUME:
					effect.setItemConsume(reader.readInt());
					break;
				case KvjEffects.ITEM_CONSUME_COUNT:
					effect.setItemConsumeCount(reader.readByte());
					break;
				case KvjEffects.BULLET_CONSUME:
					effect.setBulletConsume(reader.readShort());
					break;
				case KvjEffects.MONEY_CONSUME:
					effect.setMoneyConsume(reader.readShort());
					break;
				case KvjEffects.MORPH:
					effect.setMorph(reader.readInt());
					break;
				case KvjEffects.SUMMON:
					//shout level 4 is glitched, they put the value for prop in the key
					//and the value is 38??? Because the key is an integer, we consider it a summon...
					//assert (skillid == Skills.SHOUT && level == 4);
					//first byte is mobIndex, which was just Byte.parseByte(key) in KvjCompiler
					effect.setProp(reader.readByte());
					/*assert (*/reader.readInt()/* == 38)*/;
					break;
				case KvjEffects.END_EFFECT:
					break loop;
			}
		}
		return effect;
	}

	private void doMobWork(LittleEndianReader reader) {
		MobSkillStats stats = null;
		byte level;
		short skillid = -1;
		for (byte now = reader.readByte(); now != -1; now = reader.readByte()) {
			switch (now) {
				case NEXT_SKILL:
					stats = new MobSkillStats();
					skillid = (short) reader.readInt();
					mobSkillStats.put(Short.valueOf(skillid), stats);
					break;
				case DELAY:
					stats.setDelay(reader.readInt());
					break;
				case NEXT_LEVEL:
					level = reader.readByte();
					stats.addLevel(level, processMobEffect(skillid, level, reader));
					break;
			}
		}
	}

	private MobSkillEffectsData processMobEffect(short skillid, byte level, LittleEndianReader reader) {
		MobSkillEffectsData effect = new MobSkillEffectsData(skillid, level);
		loop:
		for (byte now = reader.readByte(); now != -1; now = reader.readByte()) {
			switch (now) {
				case KvjEffects.MP_CONSUME:
					effect.setMpConsume(reader.readShort());
					break;
				case KvjEffects.DURATION:
					effect.setDuration(reader.readInt());
					break;
				case KvjEffects.X:
					effect.setX(reader.readInt());
					break;
				case KvjEffects.Y:
					effect.setY(reader.readInt());
					break;
				case KvjEffects.LT:
					effect.setLt(reader.readShort(), reader.readShort());
					break;
				case KvjEffects.RB:
					effect.setRb(reader.readShort(), reader.readShort());
					break;
				case KvjEffects.PROP:
					effect.setProp(reader.readShort());
					break;
				case KvjEffects.COOLTIME:
					effect.setCooltime(reader.readShort());
					break;
				case KvjEffects.HP:
					effect.setMaxHpPercent(reader.readShort());
					break;
				case KvjEffects.SUMMON_EFFECT:
					effect.setSummonEffect(reader.readByte());
					break;
				case KvjEffects.LIMIT:
					effect.setLimit(reader.readShort());
					break;
				case KvjEffects.SUMMON:
					effect.addSummon(reader.readByte(), reader.readInt());
					break;
				case KvjEffects.END_EFFECT:
					break loop;
			}
		}
		return effect;
	}
}
