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

package argonms.game.loading.reactor;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public class ReactorStats {
	private int reactorId;
	private int link;
	private Map<Byte, State> states;
	private String action;

	protected ReactorStats(int reactorId) {
		this.reactorId = reactorId;
		states = new HashMap<Byte, State>();
	}

	public int getReactorId() {
		return reactorId;
	}

	protected void setLink(int reactorid) {
		this.link = reactorid;
	}

	protected void addState(byte stateid, State s) {
		states.put(Byte.valueOf(stateid), s);
	}

	protected void setScript(String action) {
		this.action = action;
	}

	protected int getLink() {
		return link;
	}

	public Map<Byte, State> getStates() {
		return states;
	}

	public String getScript() {
		return action;
	}
}
