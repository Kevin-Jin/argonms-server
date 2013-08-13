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

package argonms.shop.loading.limitedcommodity;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author GoldenKevin
 */
public class LimitedCommodity {
	private final List<Number> serialNumbers;
	private int initial, remaining;
	private int beginDate, endDate;
	private int beginHour, endHour;

	public LimitedCommodity() {
		serialNumbers = new ArrayList<Number>();
	}

	public void addSerialNumbers(List<?> sns) {
		for (Object sn : sns) {
			if (sn instanceof Number) {
				serialNumbers.add((Number) sn);
				if (serialNumbers.size() == 10)
					break;
			}
		}
	}

	public void setStartAmount(int initialStock) {
		this.initial = initialStock;
	}

	public void setUsedAmount(int used) {
		this.remaining = initial - used;
	}

	public void setBeginDate(int beginDate) {
		this.beginDate = beginDate;
	}

	public void setEndDate(int endDate) {
		this.endDate = endDate;
	}

	public void setBeginHour(int beginHour) {
		this.beginHour = beginHour;
	}

	public void setEndHour(int endHour) {
		this.endHour = endHour;
	}

	public int incrementUsed() {
		return initial - --remaining;
	}

	public List<Number> getSerialNumbers() {
		return serialNumbers;
	}

	public int getInitialStock() {
		return initial;
	}

	public int getRemainingStock() {
		return remaining;
	}

	public int getBeginDate() {
		return beginDate;
	}

	public int getEndDate() {
		return endDate;
	}

	public int getBeginHour() {
		return beginHour;
	}

	public int getEndHour() {
		return endHour;
	}
}
