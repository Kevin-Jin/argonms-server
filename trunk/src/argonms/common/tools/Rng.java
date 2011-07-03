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

package argonms.common.tools;

import java.util.Random;

/**
 * Provides a central area to acquire a ThreadLocal random number generator.
 * @author GoldenKevin
 */
public class Rng {
	//here is what Sun documented in Math.random():
	//This method is properly synchronized to allow correct use by more than one
	//thread. However, if many threads need to generate pseudorandom numbers at
	//a great rate, it may reduce contention for each thread to have its own
	///pseudorandom-number generator.
	//So, although java.util.Random is thread-safe, I trust Sun and decided to
	//make this ThreadLocal to improve performance.
	//In addition, in section 15.3 of Java Concurrency in Practice, the graphs
	//clearly show the increased throughput of ThreadLocal RNGs.
	private final static ThreadLocal<Random> GENERATOR;

	static {
		GENERATOR = new ThreadLocal<Random>() {
			 protected Random initialValue() {
				 return new Random();
			 }
		};
	}

	public static Random getGenerator() {
		return GENERATOR.get();
	}
}
