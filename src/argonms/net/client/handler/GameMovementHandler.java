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

package argonms.net.client.handler;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import argonms.character.Player;
import argonms.game.GameClient;
import argonms.map.MapObject;
import argonms.map.movement.AbsoluteLifeMovement;
import argonms.map.movement.ChairMovement;
import argonms.map.movement.ChangeEquipSpecialAwesome;
import argonms.map.movement.JumpDownMovement;
import argonms.map.movement.LifeMovement;
import argonms.map.movement.LifeMovementFragment;
import argonms.map.movement.RelativeLifeMovement;
import argonms.map.movement.TeleportMovement;
import argonms.net.client.RemoteClient;
import argonms.tools.input.LittleEndianReader;

/**
 *
 * @author GoldenKevin
 */
public class GameMovementHandler {
	private static final Logger LOG = Logger.getLogger(GameMovementHandler.class.getName());

	public static void handleMovePlayer(LittleEndianReader packet, RemoteClient rc) {
		packet.readByte();
		packet.readInt();
		List<LifeMovementFragment> res = parseMovement(packet);
		if (res != null) {
			if (packet.available() != 18) {
				LOG.log(Level.WARNING, "Received unusual movement packet w/ {0} bytes remaining: {1}",
						new Object[] { packet.available(), packet });
				return;
			}
			Player player = ((GameClient) rc).getPlayer();
			player.move(res);
			updatePosition (res, player, 0);
		}
	}

	private static List<LifeMovementFragment> parseMovement(LittleEndianReader packet) {
		List<LifeMovementFragment> res = new ArrayList<LifeMovementFragment>();
		int numCommands = packet.readByte();
		for (int i = 0; i < numCommands; i++) {
			byte command = packet.readByte();
			switch (command) {
				case 0: // normal move
				case 5:
				case 17: { //float
					short xpos = packet.readShort();
					short ypos = packet.readShort();
					short xwobble = packet.readShort();
					short ywobble = packet.readShort();
					short unk = packet.readShort();
					byte newstate = packet.readByte();
					short duration = packet.readShort();
					AbsoluteLifeMovement alm = new AbsoluteLifeMovement(command, new Point(xpos, ypos), duration, newstate);
					alm.setUnk(unk);
					alm.setPixelsPerSecond(new Point(xwobble, ywobble));
					res.add(alm);
					break;
				} case 1:
				case 2:
				case 6: // fj
				case 12:
				case 13: // Shot-jump-back thing
				case 16: { //float
					short xmod = packet.readShort();
					short ymod = packet.readShort();
					byte newstate = packet.readByte();
					short duration = packet.readShort();
					RelativeLifeMovement rlm = new RelativeLifeMovement(command, new Point(xmod, ymod), duration, newstate);
					res.add(rlm);
					break;
				} case 3:
				case 4: // tele... -.-
				case 7: // assaulter
				case 8: // assassinate
				case 9: // rush
				case 14: {
					short xpos = packet.readShort();
					short ypos = packet.readShort();
					short xwobble = packet.readShort();
					short ywobble = packet.readShort();
					byte newstate = packet.readByte();
					TeleportMovement tm = new TeleportMovement(command, new Point(xpos, ypos), newstate);
					tm.setPixelsPerSecond(new Point(xwobble, ywobble));
					res.add(tm);
					break;
				} case 10: { //change equip???
					res.add(new ChangeEquipSpecialAwesome(packet.readByte()));
					break;
				} case 11: { //chair
					short xpos = packet.readShort();
					short ypos = packet.readShort();
					short unk = packet.readShort();
					byte newstate = packet.readByte();
					short duration = packet.readShort();
					ChairMovement cm = new ChairMovement(command, new Point(xpos, ypos), duration, newstate);
					cm.setUnk(unk);
					res.add(cm);
					break;
				} case 15: {
					short xpos = packet.readShort();
					short ypos = packet.readShort();
					short xwobble = packet.readShort();
					short ywobble = packet.readShort();
					short unk = packet.readShort();
					short fh = packet.readShort();
					byte newstate = packet.readByte();
					short duration = packet.readShort();
					JumpDownMovement jdm = new JumpDownMovement(command, new Point(xpos, ypos), duration, newstate);
					jdm.setUnk(unk);
					jdm.setPixelsPerSecond(new Point(xwobble, ywobble));
					jdm.setFH(fh);
					res.add(jdm);
					break;
				} default: {
					return null;
				}
			}
		}
		return res;
	}

	//TODO: I don't like instanceof...
	private static void updatePosition(List<LifeMovementFragment> movement, MapObject target, int yoffset) {
		for (LifeMovementFragment move : movement) {
			if (move instanceof LifeMovement) {
				if (move instanceof AbsoluteLifeMovement) {
					Point position = ((LifeMovement) move).getPosition();
					position.y += yoffset;
					target.setPosition(position);
				}
				target.setStance(((LifeMovement) move).getNewstate());
			}
		}
	}
}
