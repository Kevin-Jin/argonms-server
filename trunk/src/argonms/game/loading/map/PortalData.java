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

package argonms.game.loading.map;

import argonms.common.GlobalConstants;
import java.awt.Point;

/**
 *
 * @author GoldenKevin
 */
public class PortalData {
	private String pn;
	private byte pt;
	private Point pos;
	private int tm;
	private String tn;
	private String script;

	protected PortalData() {
		this.tm = GlobalConstants.NULL_MAP;
	}

	protected void setPortalName(String pn) {
		this.pn = pn;
	}

	protected void setPortalType(byte pt) {
		this.pt = pt;
	}

	protected void setPosition(short x, short y) {
		this.pos = new Point(x, y);
	}

	protected void setTargetMapId(int tm) {
		this.tm = tm;
	}

	protected void setTargetName(String tn) {
		this.tn = tn;
	}

	protected void setScript(String script) {
		this.script = script;
	}

	public String getPortalName() {
		return pn;
	}

	public byte getPortalType() {
		return pt;
	}

	public Point getPosition() {
		return pos;
	}

	public int getTargetMapId() {
		return tm;
	}

	public String getTargetName() {
		return tn;
	}

	public String getScript() {
		return script;
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append("Name=").append(pn).append(", Type=").append(pt).append(", (").append(pos.x).append(", ").append(pos.y).append("), target=").append(tm);
		if (!tn.isEmpty())
			ret.append(" (").append(tn).append(")");
		if (!script.isEmpty())
			ret.append(", Script=").append(script);
		return ret.toString();
	}
}