package argonms.loading.map;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public class MapStats {
	private int mapid;
	private Map<Integer, Portal> portals;
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

	public MapStats() {
		portals = new HashMap<Integer, Portal>();
		areas = new HashMap<String, Area>();
		footholds = new HashMap<Integer, Foothold>();
		life = new HashMap<Integer, Life>();
		reactors = new HashMap<Integer, Reactor>();
	}

	public void setTown() {
		this.town = true;
	}

	public void setReturnMap(int mapid) {
		this.returnMapId = mapid;
	}

	public void setForcedReturn(int mapid) {
		this.forcedReturn = mapid;
	}

	public void setMobRate(float rate) {
		this.monsterRate = rate;
	}

	public void setDecHp(int dec) {
		this.decHp = dec;
	}

	public void setTimeLimit(int limit) {
		this.timeLimit = limit;
	}

	public void setProtectItem(int item) {
		this.protectItem = item;
	}

	public void setEverlast() {
		this.everlast = true;
	}

	public void addLife(int id, Life l) {
		life.put(Integer.valueOf(id), l);
	}

	public void addArea(String id, Area a) {
		areas.put(id, a);
	}

	public void setClock() {
		this.clock = true;
	}

	public void setShip() {
		this.boat = true;
	}

	public void addReactor(int id, Reactor rt) {
		reactors.put(Integer.valueOf(id), rt);
	}

	public void addFoothold(int id, Foothold fh) {
		footholds.put(Integer.valueOf(id), fh);
	}

	public void addPortal(int id, Portal p) {
		portals.put(Integer.valueOf(id), p);
	}
}
