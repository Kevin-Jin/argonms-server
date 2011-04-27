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

package argonms.game.handler;

import argonms.character.Player;
import argonms.game.GameClient;
import argonms.map.MapEntity.EntityType;
import argonms.map.entity.Reactor;
import argonms.net.external.RemoteClient;
import argonms.tools.input.LittleEndianReader;

/**
 *
 * @author GoldenKevin
 */
public class ReactorHandler {
	public static void handleReactorTrigger(LittleEndianReader packet, RemoteClient rc) {
		int entId = packet.readInt();
		/*Point currentPos = */packet.readPos();
		short stance = packet.readShort();
		Player p = ((GameClient) rc).getPlayer();
		Reactor r = (Reactor) p.getMap().getEntityById(EntityType.REACTOR, entId);
		if (r != null)
			r.hit(p, stance);
	}

	public static void handleReactorTouch(LittleEndianReader packet, RemoteClient rc) {
		int entId = packet.readInt();
		boolean enter = packet.readBool();
		Player p = ((GameClient) rc).getPlayer();
		Reactor r = (Reactor) p.getMap().getEntityById(EntityType.REACTOR, entId);
		if (r != null) {
			if (enter)
				r.touched(p);
			else
				r.untouched(p);
		}
	}
}
