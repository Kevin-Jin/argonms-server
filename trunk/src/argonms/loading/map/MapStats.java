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

package argonms.loading.map;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public class MapStats {
	private int mapid;
	private Map<Byte, Portal> portals;
	private Map<String, Area> areas;
	private int returnMapId;
	private float monsterRate;
	private Map<Integer, Foothold> footholds;
	private boolean town;
	private boolean clock;
	private boolean everlast;
	private boolean boat;
	private int forcedReturn;
	private int protectItem;
	private int decHp;
	private int timeLimit;
	private Map<Integer, Life> life;
	private Map<Integer, Reactor> reactors;

	protected MapStats() {
		portals = new HashMap<Byte, Portal>();
		areas = new HashMap<String, Area>();
		footholds = new HashMap<Integer, Foothold>();
		life = new HashMap<Integer, Life>();
		reactors = new HashMap<Integer, Reactor>();
	}

	protected void setTown() {
		this.town = true;
	}

	protected void setReturnMap(int mapid) {
		this.returnMapId = mapid;
	}

	protected void setForcedReturn(int mapid) {
		this.forcedReturn = mapid;
	}

	protected void setMobRate(float rate) {
		this.monsterRate = rate;
	}

	protected void setDecHp(int dec) {
		this.decHp = dec;
	}

	protected void setTimeLimit(int limit) {
		this.timeLimit = limit;
	}

	protected void setProtectItem(int item) {
		this.protectItem = item;
	}

	protected void setEverlast() {
		this.everlast = true;
	}

	protected void addLife(int id, Life l) {
		life.put(Integer.valueOf(id), l);
	}

	protected void addArea(String id, Area a) {
		areas.put(id, a);
	}

	protected void setClock() {
		this.clock = true;
	}

	protected void setShip() {
		this.boat = true;
	}

	protected void addReactor(int id, Reactor rt) {
		reactors.put(Integer.valueOf(id), rt);
	}

	protected void addFoothold(int id, Foothold fh) {
		footholds.put(Integer.valueOf(id), fh);
	}

	protected void addPortal(int id, Portal p) {
		portals.put(Byte.valueOf((byte) id), p);
	}

	public boolean isTown() {
		return town;
	}

	public int getMapId() {
		return mapid;
	}

	public int getReturnMap() {
		return returnMapId;
	}

	public int getForcedReturn() {
		return forcedReturn;
	}

	public float getMobRate() {
		return monsterRate;
	}

	public int getDecHp() {
		return decHp;
	}

	public int getTimeLimit() {
		return timeLimit;
	}

	public int getProtectItem() {
		return protectItem;
	}

	public boolean isEverlast() {
		return everlast;
	}

	public Map<Integer, Life> getLife() {
		return life;
	}

	public Map<String, Area> gddAreas() {
		return areas;
	}

	public boolean hasClock() {
		return clock;
	}

	public boolean hasShip() {
		return boat;
	}

	public Map<Integer, Reactor> getReactors() {
		return reactors;
	}

	public Map<Integer, Foothold> getFootholds() {
		return footholds;
	}

	public Map<Byte, Portal> getPortals() {
		return portals;
	}
}
