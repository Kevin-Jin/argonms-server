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

import java.awt.Point;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * @author Matze
 */
public class FootholdTree {
	private FootholdTree nw;
	private FootholdTree ne;
	private FootholdTree sw;
	private FootholdTree se;
	private List<Foothold> footholds;
	private Point p1;
	private Point p2;
	private Point center;
	private int depth = 0;
	private static int maxDepth = 8;
	private int maxDropX;
	private int minDropX;

	public FootholdTree() {
		this.footholds = new LinkedList<Foothold>();
	}

	private FootholdTree(Point p1, Point p2, int depth) {
		this.footholds = new LinkedList<Foothold>();
		this.p1 = p1;
		this.p2 = p2;
		this.depth = depth;
		center = new Point((p2.x - p1.x) / 2, (p2.y - p1.y) / 2);
	}

	protected void load(Foothold f) {
		footholds.add(f);
	}

	protected void finished() {
		List<Foothold> loaded = footholds;
		footholds = new LinkedList<Foothold>();
		Point lBound = new Point();
		Point uBound = new Point();
		for (Foothold fh : loaded) {
			if (fh.getX1() < lBound.x)
				lBound.x = fh.getX1();
			if (fh.getX2() > uBound.x)
				uBound.x = fh.getX2();
			if (fh.getY1() < lBound.y)
				lBound.y = fh.getY1();
			if (fh.getY2() > uBound.y)
				uBound.y = fh.getY2();
		}
		this.p1 = lBound;
		this.p2 = uBound;
		this.center = new Point((uBound.x - lBound.x) / 2, (uBound.y - lBound.y) / 2);
		for (Foothold fh : loaded)
			insert(fh);
	}

	private void insert(Foothold f) {
		if (depth == 0) {
			if (f.getX1() > maxDropX)
				maxDropX = f.getX1();
			if (f.getX1() < minDropX)
				minDropX = f.getX1();
			if (f.getX2() > maxDropX)
				maxDropX = f.getX2();
			if (f.getX2() < minDropX)
				minDropX = f.getX2();
		}
		if (/*footholds.size() == 0 || */depth == maxDepth || 
			(f.getX1() >= p1.x && f.getX2() <= p2.x &&
			f.getY1() >= p1.y && f.getY2() <= p2.y)) {
			footholds.add(f);
		} else {
			if (nw == null) {
				nw = new FootholdTree(p1, center, depth + 1);
				ne = new FootholdTree(new Point(center.x, p1.y), new Point(p2.x, center.y), depth + 1);
				sw = new FootholdTree(new Point(p1.x, center.y), new Point(center.x, p2.y), depth + 1);
				se = new FootholdTree(center, p2, depth + 1);
			}
			if (f.getX2() <= center.x && f.getY2() <= center.y)
				nw.insert(f);
			else if (f.getX1() > center.x && f.getY2() <= center.y)
				ne.insert(f);
			else if (f.getX2() <= center.x && f.getY1() > center.y)
				sw.insert(f);
			else
				se.insert(f);
		}
	}
	
	private List<Foothold> getRelevants(Point p) {
		return getRelevants(p, new LinkedList<Foothold>());
	}
	
	private List<Foothold> getRelevants(Point p, List<Foothold> list) {
		list.addAll(footholds);
		if (nw != null) {
			if (p.x <= center.x && p.y <= center.y)
				nw.getRelevants(p, list);
			else if (p.x > center.x && p.y <= center.y)
				ne.getRelevants(p, list);
			else if (p.x <= center.x && p.y > center.y)
				sw.getRelevants(p, list);
			else
				se.getRelevants(p, list);
		}
		return list;
	}
	
	private Foothold findWallR(Point p1, Point p2) {
		Foothold ret;
		for (Foothold f : footholds) {
			//if (f.isWall()) System.out.println(f.getX1() + " " + f.getX2());
			if (f.isWall() && f.getX1() >= p1.x && f.getX1() <= p2.x &&
				f.getY1() >= p1.y && f.getY2() <= p1.y)
				return f;
		}
		if (nw != null) {
			if (p1.x <= center.x && p1.y <= center.y) {
				ret = nw.findWallR(p1, p2);
				if (ret != null) return ret;
			}
			if ((p1.x > center.x || p2.x > center.x) && p1.y <= center.y) {
				ret = ne.findWallR(p1, p2);
				if (ret != null) return ret;
			}
			if (p1.x <= center.x && p1.y > center.y) {
				ret = sw.findWallR(p1, p2);
				if (ret != null) return ret;
			}
			if ((p1.x > center.x || p2.x > center.x) && p1.y > center.y) {
				ret = se.findWallR(p1, p2);
				if (ret != null) return ret;
			}
		}		
		return null;
	}
	
	public Foothold findWall(Point p1, Point p2) {
		if (p1.y != p2.y)
			throw new IllegalArgumentException();
		return findWallR(p1, p2);
	}
	
	public Foothold findBelow(Point p) {
		List<Foothold> relevants = getRelevants(p);
		// find fhs with matching x coordinates
		List<Foothold> xMatches = new LinkedList<Foothold>();
		for (Foothold fh : relevants) {
			if (fh.getX1() <= p.x && fh.getX2() >= p.x) xMatches.add(fh);
		}
		Collections.sort(xMatches);
		for (Foothold fh : xMatches) {
			if (!fh.isWall() && fh.getY1() != fh.getY2()) {
				int calcY;
				double s1 = Math.abs(fh.getY2() - fh.getY1());
				double s2 = Math.abs(fh.getX2() - fh.getX1());
				double s4 = Math.abs(p.x - fh.getX1());
				double alpha = Math.atan(s2 / s1);
				double beta = Math.atan(s1 / s2);
				double s5 = Math.cos(alpha) * (s4 / Math.cos(beta));
				if (fh.getY2() < fh.getY1()) {
					calcY = fh.getY1() - (int) s5;
				} else {
					calcY = fh.getY1() + (int) s5;
				}
				if (calcY >= p.y) return fh;
			} else if (!fh.isWall()) {
				if (fh.getY1() >= p.y) return fh;
			}
		}
		return null;
	}
	
	public int getX1() {
		return p1.x;
	}
	
	public int getX2() {
		return p2.x;
	}
	
	public int getY1() {
		return p1.y;
	}
	
	public int getY2() {
		return p2.y;
	}

	public int getMaxDropX() {
		return maxDropX;
	}

	public int getMinDropX() {
		return minDropX;
	}
}
