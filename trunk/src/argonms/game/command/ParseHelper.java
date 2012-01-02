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

package argonms.game.command;

/**
 *
 * @author GoldenKevin
 */
public class ParseHelper {
	public static boolean hasOpt(String[] array, String search) {
		for (int i = 0; i < array.length; i++)
			if (array[i].equalsIgnoreCase(search))
				return true;
		return false;
	}

	public static String getOptValue(String[] array, String search) throws Exception {
		for (int i = 0; i < array.length; i++) {
			if (array[i].equalsIgnoreCase(search)) {
				if (i + 1 < array.length) {
					String value = "";
					String word;

					for (int quoteCount = 0, previousQuote; i < array.length; i++) {
						word = array[i + 1];
						previousQuote = -1;
						while ((previousQuote = word.indexOf('\"', previousQuote + 1)) != -1) {
							//check if the quote is escaped
							if (previousQuote == 0 || word.charAt(previousQuote - 1) != '\\') {
								//remove the quote
								word = word.substring(0, previousQuote) + word.substring(previousQuote + 1, word.length());
								//we removed a character, so next time we use indexOf, check the character at previousQuote's index again
								previousQuote--;
								quoteCount++;
							} else {
								//remove the escape character
								word = word.substring(0, previousQuote - 1) + word.substring(previousQuote, word.length());
								//we removed a character, so next time we use indexOf, check the character at previousQuote's index again
								previousQuote--;
							}
						}
						value += word ;
						if (quoteCount % 2 == 0) //even amount of quotes means all of them are matched
							return value;
						//otherwise, look for the final end quote in the next word if we can
						value += ' ';
					}
					//could not find even amount of quotes
					throw new IllegalArgumentException("Missing end quote");
				}
				throw new IllegalArgumentException("Missing value after " + search);
			}
		}
		//could not find search
		return null;
	}

	public static String restOfString(String[] splitted, int start) {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < splitted.length; i++)
			sb.append(splitted[i]).append(' ');
		return sb.substring(0, sb.length() - 1); //assume we have at least one arg
	}
}
