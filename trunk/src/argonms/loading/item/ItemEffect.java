package argonms.loading.item;

import argonms.loading.StatEffects;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author GoldenKevin
 */
public class ItemEffect extends StatEffects {
	private boolean poison, seal, darkness, weakness, curse;
	private boolean consumeOnPickup;
	private int moveTo;
	private List<Integer> petConsumableBy;

	public ItemEffect() {
		petConsumableBy = new ArrayList<Integer>();
	}

	public void setMoveTo(int map) {
		this.moveTo = map;
	}

	public void setPoison() {
		this.poison = true;
	}

	public void setSeal() {
		this.seal = true;
	}

	public void setDarkness() {
		this.darkness = true;
	}

	public void setWeakness() {
		this.weakness = true;
	}

	public void setCurse() {
		this.curse = true;
	}

	public void setConsumeOnPickup() {
		this.consumeOnPickup = true;
	}

	public void addPetConsumableBy(int petid) {
		petConsumableBy.add(Integer.valueOf(petid));
	}
}
