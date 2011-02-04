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

package argonms.loading.skill;

import argonms.loading.KvjEffects;
import argonms.tools.input.LittleEndianByteArrayReader;
import argonms.tools.input.LittleEndianReader;
import java.awt.Point;
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
		IS_BUFF = 3,
		DELAY = 4,
		IS_CHARGE = 5,
		NEXT_LEVEL = 6
	;

	private String dataPath;

	protected KvjSkillDataLoader(String wzPath) {
		this.dataPath = wzPath;
	}

	//TODO: Actually do real work to see if the skill exists in the file so we
	//can name this method exists() instead of loadable?
	protected void canLoadPlayerSkill(int skillid) {
		String id = String.format("%07d", skillid);

		try {
			File f = new File(new StringBuilder(dataPath).append("Skill.wz").append(File.separator).append(id.substring(0, 3)).append(".img.kvj").toString());
			if (f.exists())
				doWork(new LittleEndianByteArrayReader(f));
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Could not read KVJ data file for skill " + skillid, e);
		}
	}

	//TODO: Actually do real work to see if the skill exists in the file so we
	//can name this method exists() instead of loadable?
	protected void canLoadMobSkill(int skillid) {
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
	public boolean validPlayerSkill(int skillid) {
		String id = String.format("%07d", skillid);
		File f = new File(new StringBuilder(dataPath).append("Skill.wz").append(File.separator).append(id.substring(0, 3)).append(".img.kvj").toString());
		return f.exists();
	}

	//TODO: Actually do real work to see if the skill exists in the file so we
	//can name this method exists() instead of loadable?
	public boolean validMobSkill(int skillid) {
		File f = new File(new StringBuilder(dataPath).append("Skill.wz").append(File.separator).append("MobSkill.img.kvj").toString());
		return f.exists();
	}

	private void doWork(LittleEndianReader reader) {
		SkillStats stats = null;
		byte level;
		for (byte now = reader.readByte(); now != -1; now = reader.readByte()) {
			switch (now) {
				case NEXT_SKILL:
					stats = new SkillStats();
					skillStats.put(Integer.valueOf(reader.readInt()), stats);
					break;
				case ELEM_ATTR:
					stats.setElemAttr(reader.readNullTerminatedString());
					break;
				case IS_BUFF:
					stats.setBuff();
					break;
				case DELAY:
					stats.setDelay(reader.readInt());
					break;
				case IS_CHARGE:
					stats.setChargedSkill();
					break;
				case NEXT_LEVEL:
					level = reader.readByte();
					stats.addLevel(level, processEffect(reader));
					break;
			}
		}
	}

	private SkillEffect processEffect(LittleEndianReader reader) {
		SkillEffect effect = new SkillEffect();
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
					effect.setProp(reader.readInt() / 100.0);
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
				case KvjEffects.ACCY:
					effect.setAcc(reader.readShort());
					break;
				case KvjEffects.AVOID:
					effect.setAvoid(reader.readShort());
					break;
				case KvjEffects.HP_BONUS:
					effect.setHp(reader.readShort());
					break;
				case KvjEffects.MP_BONUS:
					effect.setMp(reader.readShort());
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
				case KvjEffects.END_EFFECT:
					break loop;
			}
		}
		return effect;
	}

	private void doMobWork(LittleEndianReader reader) {
		MobSkillStats stats = null;
		byte level;
		for (byte now = reader.readByte(); now != -1; now = reader.readByte()) {
			switch (now) {
				case NEXT_SKILL:
					stats = new MobSkillStats();
					mobSkillStats.put(Integer.valueOf(reader.readInt()), stats);
					break;
				case ELEM_ATTR:
					stats.setElemAttr(reader.readNullTerminatedString());
					break;
				case IS_BUFF:
					stats.setBuff();
					break;
				case DELAY:
					stats.setDelay(reader.readInt());
					break;
				case IS_CHARGE:
					stats.setChargedSkill();
					break;
				case NEXT_LEVEL:
					level = reader.readByte();
					stats.addLevel(level, processMobEffect(reader));
					break;
			}
		}
	}

	private MobSkillEffect processMobEffect(LittleEndianReader reader) {
		MobSkillEffect effect = new MobSkillEffect();
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
					effect.setLt(new Point(reader.readShort(), reader.readShort()));
					break;
				case KvjEffects.RB:
					effect.setRb(new Point(reader.readShort(), reader.readShort()));
					break;
				case KvjEffects.PROP:
					effect.setProp(reader.readInt() / 100.0);
					break;
				case KvjEffects.COOLTIME:
					effect.setCooltime(reader.readShort());
					break;
				case KvjEffects.HP_BONUS:
					effect.setHp(reader.readShort());
					break;
				case KvjEffects.END_EFFECT:
					break loop;
			}
		}
		return effect;
	}
}
