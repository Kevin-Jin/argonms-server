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

import argonms.UserPrivileges;
import argonms.character.Player;
import argonms.game.GameClient;
import argonms.game.GameServer;
import argonms.loading.map.PortalData;
import argonms.net.external.CommonPackets;
import argonms.net.external.RemoteClient;
import argonms.tools.input.LittleEndianReader;
import java.awt.Point;

/**
 *
 * @author GoldenKevin
 */
public class GameGoToHandler {
	public static void handleChangeChannel(LittleEndianReader packet, RemoteClient rc) {
		byte destCh = (byte) (packet.readByte() + 1);
		byte curCh = rc.getChannel();
		GameServer.getChannel(curCh).requestChannelChange(((GameClient) rc).getPlayer(), destCh);
	}

	public static void handleMapChange(LittleEndianReader packet, RemoteClient rc) {
		/*byte type = */packet.readByte(); //1 on first portal enter, 2 on subsequents...?
		int dest = packet.readInt();
		String portalName = packet.readLengthPrefixedString();
		Player p = ((GameClient) rc).getPlayer();
		if (dest == -1) { //entered portal
			if (!p.getMap().enterPortal(p, portalName))
				rc.getSession().send(CommonPackets.writeEnableActions());
		} else if (dest == 0 && !p.isAlive()) { //warp when dead and clicked ok
			//TODO: cancel all buffs and all that good stuff
			p.setHp((short) 50);
			p.setStance((byte) 0);
			p.changeMap(p.getMap().getReturnMap());
		} else { //client map command
			if (p.getPrivilegeLevel() <= UserPrivileges.USER || !p.changeMap(dest))
				rc.getSession().send(CommonPackets.writeEnableActions());
		}
	}

	public static void handleEnteredSpecialPortal(LittleEndianReader packet, RemoteClient rc) {
		packet.readByte();
		String portalName = packet.readLengthPrefixedString();
		packet.readByte();
		packet.readByte(); //sourcefm?

		Player p = ((GameClient) rc).getPlayer();
		if (!p.getMap().enterPortal(p, portalName))
			rc.getSession().send(CommonPackets.writeEnableActions());
	}

	public static void handleEnteredInnerPortal(LittleEndianReader packet, RemoteClient rc) {
		packet.readByte();
		String portalName = packet.readLengthPrefixedString();

		Player p = ((GameClient) rc).getPlayer();
		PortalData portal = p.getMap().getStaticData().getPortals().get(Byte.valueOf(p.getMap().getPortalIdByName(portalName)));
		Point startPos = packet.readPos();
		Point endPos = packet.readPos();
		if (portal == null) {
			//TODO: hacking
		} else if (portal.getPosition().distanceSq(startPos) > (150 * 150) || portal.getPosition().distanceSq(p.getPosition()) > (150 * 150)) {
			//TODO: hacking
		} else if (!p.getMap().getStaticData().getPortals().get(Byte.valueOf(p.getMap().getPortalIdByName(portal.getTargetName()))).getPosition().equals(endPos)) {
			//TODO: hacking
		}
	}

	public static void handleWarpCs(LittleEndianReader packet, RemoteClient rc) {
		
	}
}
