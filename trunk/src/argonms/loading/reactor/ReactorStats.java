package argonms.loading.reactor;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public class ReactorStats {
	private int link;
	private Map<Integer, State> states;

	public ReactorStats() {
		states = new HashMap<Integer, State>();
	}

	public void setLink(int reactorid) {
		this.link = reactorid;
	}

	public void addState(int stateid, State s) {
		states.put(Integer.valueOf(stateid), s);
	}

	public int getLink() {
		return link;
	}
}
