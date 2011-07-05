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

import argonms.common.tools.collections.Pair;
import argonms.game.GameCommonPackets;
import argonms.game.character.GameCharacter;
import argonms.game.field.MapEntity;
import argonms.game.loading.reactor.ReactorStats;
import argonms.game.loading.reactor.State;
import argonms.game.script.ReactorScriptManager;
import java.awt.Point;
import java.awt.Rectangle;

/**
 *
 * @author GoldenKevin
 */
public class Reactor extends MapEntity {
	public static final byte
		TYPE_ITEM_TRIGGERED = 100
	;

	private ReactorStats stats;
	private String name;
	private int delay;
	private byte state;
	private boolean alive;

	public Reactor(ReactorStats reactorStats) {
		this.stats = reactorStats;
		reset();
	}

	public int getDataId() {
		return stats.getReactorId();
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}

	public String getName() {
		return name;
	}

	public int getDelay() {
		return delay;
	}

	public byte getStateId() {
		return state;
	}

	public State getState() {
		return stats.getStates().get(Byte.valueOf(state));
	}

	public Pair<Integer, Short> getItemTrigger() {
		State s = getState();
		if (s == null || s.getType() != TYPE_ITEM_TRIGGERED)
			return null;
		return new Pair<Integer, Short>(Integer.valueOf(s.getItemId()), Short.valueOf(s.getItemQuantity()));
	}

	//precondition: getItemTrigger() does not return null.
	public Rectangle getItemTriggerZone() {
		State s = getState();
		Point rb = s.getRb();
		Point lt = s.getLt();

		Point center = getPosition();
		int x = center.x + lt.x;
		int y = center.y + lt.y;
		int width = rb.x - lt.x;
		int height = rb.y - lt.y;

		return new Rectangle(x, y, width, height);
	}

	private void triggered(GameCharacter p) {
		if (getState() != null) {
			p.getMap().sendToAll(GameCommonPackets.writeTriggerReactor(this));
		} else {
			//TODO: if script does not exist and we are using MCDB, try loading
			//from MCDB's drop table with the reactor's data id as the dropperid
			ReactorScriptManager.getInstance().runScript(getDataId(), this, p.getClient());
			alive = false;
			p.getMap().destroyReactor(this);
		}
	}

	public void hit(GameCharacter p, short stance) {
		state = getState().getNextState();
		triggered(p);
	}

	public void touched(GameCharacter p) {
		state++;
		triggered(p);
	}

	public void untouched(GameCharacter p) {
		state--;
		triggered(p);
	}

	public final void reset() {
		state = 0;
		alive = true;
	}

	public EntityType getEntityType() {
		return EntityType.REACTOR;
	}

	public boolean isAlive() {
		return alive;
	}

	public boolean isVisible() {
		return true;
	}

	public byte[] getShowNewSpawnMessage() {
		return GameCommonPackets.writeShowReactor(this);
	}

	public byte[] getShowExistingSpawnMessage() { //I guess there is nothing else...?
		return GameCommonPackets.writeShowReactor(this);
	}

	public byte[] getDestructionMessage() {
		return GameCommonPackets.writeRemoveReactor(this);
	}
}
