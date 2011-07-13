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

package argonms.game.field.entity;

import argonms.common.character.inventory.Inventory;
import argonms.game.character.GameCharacter;
import java.awt.Point;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public class PlayerNpc extends Npc {
	private String name;
	private byte gender;
	private byte skin;
	private int eyes;
	private int hair;
	private Map<Short, Integer> equips;

	public PlayerNpc(GameCharacter p, int npcId) {
		super(npcId);
		setPosition(new Point(p.getPosition()));
		//setStance(p.getStance()); //it's entertaining to see a player NPC stuck in an alert pose, flying pose, or ladder climbing pose!
		setStance((byte) (p.getStance() & 0x01)); //only uses lsb, which is what determines direction
		setFoothold(p.getMap().getStaticData().getFootholds().findBelow(getPosition()).getId());
		setCy((short) getPosition().y);
		setRx((short) getPosition().x, (short) getPosition().x);
		name = p.getName();
		gender = p.getGender();
		skin = p.getSkinColor();
		eyes = p.getEyes();
		hair = p.getHair();
		equips = p.getInventory(Inventory.InventoryType.EQUIPPED).getItemIds();
	}

	public String getPlayerName() {
		return name;
	}

	public byte getGender() {
		return gender;
	}

	public byte getSkinColor() {
		return skin;
	}

	public int getEyes() {
		return eyes;
	}

	public int getHair() {
		return hair;
	}

	public Map<Short, Integer> getEquips() {
		return equips;
	}

	@Override
	public boolean isPlayerNpc() {
		return true;
	}
}
