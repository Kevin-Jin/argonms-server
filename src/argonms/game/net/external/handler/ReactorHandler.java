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

import argonms.common.util.input.LittleEndianReader;
import argonms.game.character.GameCharacter;
import argonms.game.field.MapEntity.EntityType;
import argonms.game.field.entity.Reactor;
import argonms.game.net.external.GameClient;

/**
 *
 * @author GoldenKevin
 */
public final class ReactorHandler {
	public static void handleReactorTrigger(LittleEndianReader packet, GameClient gc) {
		int entId = packet.readInt();
		/*Point currentPos = */packet.readPos();
		short stance = packet.readShort();
		GameCharacter p = gc.getPlayer();
		Reactor r = (Reactor) p.getMap().getEntityById(EntityType.REACTOR, entId);
		if (r != null)
			r.hit(p, stance);
	}

	public static void handleReactorTouch(LittleEndianReader packet, GameClient gc) {
		int entId = packet.readInt();
		boolean enter = packet.readBool();
		GameCharacter p = gc.getPlayer();
		Reactor r = (Reactor) p.getMap().getEntityById(EntityType.REACTOR, entId);
		if (r != null) {
			if (enter)
				r.touched(p);
			else
				r.untouched(p);
		}
	}

	private ReactorHandler() {
		//uninstantiable...
	}
}
