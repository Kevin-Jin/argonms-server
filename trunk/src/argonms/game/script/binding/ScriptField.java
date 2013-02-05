/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2013  GoldenKevin
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

package argonms.game.script.binding;

import argonms.game.character.GameCharacter;
import argonms.game.field.GameMap;
import argonms.game.field.MapEntity;
import argonms.game.field.entity.Mob;
import argonms.game.loading.mob.MobDataLoader;
import argonms.game.net.external.GamePackets;
import argonms.game.net.external.handler.ChatHandler;
import java.awt.Point;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 *
 * @author GoldenKevin
 */
public class ScriptField {
	private final Scriptable globalScope;
	private final GameMap map;

	public ScriptField(GameMap map, Scriptable globalScope) {
		this.globalScope = globalScope;
		this.map = map;
	}

	protected GameMap getMap() {
		return map;
	}

	public int getId() {
		return map.getDataId();
	}

	public int getPlayerCount() {
		return map.getPlayerCount();
	}

	public void portalEffect(String name) {
		map.sendToAll(GamePackets.writeMapEffect((byte) 2, name));
	}

	public void screenEffect(String name) {
		map.sendToAll(GamePackets.writeMapEffect((byte) 3, name));
	}

	public void soundEffect(String name) {
		map.sendToAll(GamePackets.writeMapEffect((byte) 4, name));
	}

	public void blueMessage(String message) {
		map.sendToAll(GamePackets.writeServerMessage(ChatHandler.TextStyle.LIGHT_BLUE_TEXT_CLEAR_BG.byteValue(), message, (byte) -1, true));
	}

	public void showTimer(int seconds) {
		map.sendToAll(GamePackets.writeTimer(seconds));
	}

	public void showUndockShip() {
		map.sendToAll(GamePackets.writeShipEffect((short) 520));
	}

	public void showBalrogShip() {
		map.sendToAll(GamePackets.writeShipEffect((short) 1034));
	}

	public void showDockShip() {
		map.sendToAll(GamePackets.writeShipEffect((short) 1548));
	}

	public void overridePortal(String portalName, String script) {
		map.overridePortal(portalName, script);
	}

	public void overrideReactor(String reactorName, String script) {
		map.overrideReactor(reactorName, script);
	}

	public void revertReactor(String reactorName) {
		map.overrideReactor(reactorName, null);
	}

	public void revertPortal(String portalName) {
		map.revertPortal(portalName);
	}

	public void transferPlayers(int mapId) {
		for (MapEntity player : map.getAllEntities(MapEntity.EntityType.PLAYER))
			((GameCharacter) player).changeMap(mapId);
	}

	public void resetReactors() {
		map.respawnReactors();
	}

	public Object spawnMob(int mobId, int x, int y) {
		Mob mob = new Mob(MobDataLoader.getInstance().getMobStats(mobId), map);
		mob.setPosition(new Point(x, y));
		map.spawnMonster(mob);
		return Context.javaToJS(new ScriptMob(mob), globalScope);
	}

	public Object spawnMob(int mobId, int x, int y, boolean faceRight) {
		Mob mob = new Mob(MobDataLoader.getInstance().getMobStats(mobId), map, faceRight ? (byte) 4 : (byte) 5);
		mob.setPosition(new Point(x, y));
		map.spawnMonster(mob);
		return Context.javaToJS(new ScriptMob(mob), globalScope);
	}

	public void setNoSpawn(boolean value) {
		map.setNoSpawn(value);
	}

	public void clearMobs() {
		for (MapEntity ent : map.getAllEntities(MapEntity.EntityType.MONSTER))
			map.killMonster((Mob) ent, null);
	}
}
