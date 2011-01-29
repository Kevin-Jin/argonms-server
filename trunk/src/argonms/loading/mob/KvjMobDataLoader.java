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

package argonms.loading.mob;

import argonms.tools.input.LittleEndianByteArrayReader;
import argonms.tools.input.LittleEndianReader;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class KvjMobDataLoader extends MobDataLoader {
	private static final Logger LOG = Logger.getLogger(KvjMobDataLoader.class.getName());

	private static final byte
		LEVEL = 1,
		MAX_HP = 2,
		MAX_MP = 3,
		PHYSICAL_DAMAGE = 4,
		EXP = 5,
		UNDEAD = 6,
		ELEM_ATTR = 7,
		REMOVE_AFTER = 8,
		HIDE_HP = 9,
		HIDE_NAME = 10,
		HP_TAG_COLOR = 11,
		HP_TAG_BG_COLOR = 12,
		BOSS = 13,
		SELF_DESTRUCT = 14,
		LOSE_ITEM = 15,
		INVINCIBLE = 16,
		REVIVE = 17,
		FIRST_ATTACK = 18,
		ATTACK = 19,
		SKILL = 20,
		BUFF = 21,
		DELAY = 22
	;

	private String dataPath;

	protected KvjMobDataLoader(String wzPath) {
		this.dataPath = wzPath;
	}

	protected void load(int mobid) {
		String id = String.format("%07d", mobid);

		MobStats stats = null;
		try {
			File f = new File(new StringBuilder(dataPath).append("Mob.wz").append(File.separator).append(id).append(".img.kvj").toString());
			if (f.exists()) {
				stats = new MobStats();
				doWork(new LittleEndianByteArrayReader(f), stats);
			}
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Could not read KVJ data file for mob " + mobid, e);
		}
		mobStats.put(Integer.valueOf(mobid), stats);
	}

	public boolean loadAll() {
		try {
			File root = new File(dataPath + "Mob.wz");
			for (String kvj : root.list()) {
				MobStats stats = new MobStats();
				doWork(new LittleEndianByteArrayReader(new File(root.getAbsolutePath() + File.separatorChar + kvj)), stats);
				//InputStream is = new BufferedInputStream(new FileInputStream(prefFolder.getAbsolutePath() + File.separatorChar + kvj));
				//doWork(new LittleEndianStreamReader(is), stats);
				//is.close();
				mobStats.put(Integer.valueOf(kvj.substring(0, kvj.lastIndexOf(".img.kvj"))), stats);
			}
			return true;
		} catch (IOException ex) {
			LOG.log(Level.WARNING, "Could not load all mob data from KVJ files.", ex);
			return false;
		}
	}

	public boolean canLoad(int mobid) {
		String id = String.format("%07d", mobid);
		File f = new File(new StringBuilder(dataPath).append("Mob.wz").append(File.separator).append(id).append(".img.kvj").toString());
		return f.exists();
	}

	private void doWork(LittleEndianReader reader, MobStats stats) {
		for (byte now = reader.readByte(); now != -1; now = reader.readByte()) {
			switch (now) {
				case LEVEL:
					stats.setLevel(reader.readShort());
					break;
				case MAX_HP:
					stats.setMaxHp(reader.readInt());
					break;
				case MAX_MP:
					stats.setMaxMp(reader.readInt());
					break;
				case PHYSICAL_DAMAGE:
					stats.setPhysicalDamage(reader.readInt());
					break;
				case EXP:
					stats.setExp(reader.readInt());
					break;
				case UNDEAD:
					stats.setUndead();
					break;
				case ELEM_ATTR:
					stats.setElementAttribute(reader.readNullTerminatedString());
					break;
				case REMOVE_AFTER:
					stats.setRemoveAfter(reader.readInt());
					break;
				case HIDE_HP:
					stats.setHideHp();
					break;
				case HIDE_NAME:
					stats.setHideName();
					break;
				case HP_TAG_COLOR:
					stats.setHpTagColor(reader.readByte());
					break;
				case HP_TAG_BG_COLOR:
					stats.setHpTagBgColor(reader.readByte());
					break;
				case BOSS:
					stats.setBoss();
					break;
				case SELF_DESTRUCT:
					processSelfDestruct(reader, stats);
					break;
				case LOSE_ITEM:
					stats.addLoseItem(reader.readInt());
					break;
				case INVINCIBLE:
					stats.setInvincible();
					break;
				case REVIVE:
					stats.addSummon(reader.readInt());
					break;
				case FIRST_ATTACK:
					stats.setFirstAttack();
					break;
				case ATTACK:
					processAttack(reader, stats);
					break;
				case SKILL:
					processSkill(reader, stats);
					break;
				case BUFF:
					stats.setBuffToGive(reader.readInt());
					break;
				case DELAY:
					String name = reader.readNullTerminatedString();
					int delay = reader.readInt();
					stats.addDelay(name, delay);
					break;
			}
		}
	}

	private void processSelfDestruct(LittleEndianReader reader, MobStats stats) {
		reader.readInt(); //TODO: Update KVJ Compiler. Do we really need "action"?
		stats.setSelfDestructHp(reader.readInt());
		stats.setRemoveAfter(reader.readInt());
		//also remove removeAfter from the SelfDestruct object and just place it
		//anywhere in the file. If we remove "action", we don't need a SelfDestruct
		//object at all, and can replace it with an int for hp only.
	}

	private void processAttack(LittleEndianReader reader, MobStats stats) {
		int attackid = reader.readInt();
		Attack a = new Attack();
		a.setDeadlyAttack(reader.readBool());
		a.setMpBurn(reader.readInt());
		a.setDiseaseSkill(reader.readInt());
		a.setDiseaseLevel(reader.readInt());
		a.setMpConsume(reader.readInt());
		stats.addAttack(attackid, a);
	}

	private void processSkill(LittleEndianReader reader, MobStats stats) {
		Skill s = new Skill();
		s.setSkill(reader.readShort());
		s.setLevel(reader.readByte());
		stats.addSkill(s);
	}
}
