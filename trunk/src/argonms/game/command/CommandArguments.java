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

package argonms.game.command;

import argonms.common.character.Player;
import argonms.game.GameServer;
import argonms.game.character.GameCharacter;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 * @author GoldenKevin
 */
public class CommandArguments implements Iterator<String> {
	private final String[] values;
	private int index;

	public CommandArguments(String[] array) {
		values = array;
		index = 1;
	}

	public boolean hasOpt(String search) {
		for (int i = 0; i < values.length; i++)
			if (values[i].equalsIgnoreCase(search))
				return true;
		return false;
	}

	public String getOptValue(String search) throws Exception {
		for (int i = 0; i < values.length; i++) {
			if (values[i].equalsIgnoreCase(search)) {
				if (i + 1 < values.length) {
					String value = "";
					String word;

					for (int quoteCount = 0, previousQuote; i < values.length; i++) {
						word = values[i + 1];
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

	@Override
	public boolean hasNext() {
		return index < values.length;
	}

	@Override
	public String next() {
		if (!hasNext())
			throw new NoSuchElementException("Reached the end of command");
		return values[index++];
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("CommandArguments can not be mutated");
	}

	public String restOfString() {
		if (!hasNext())
			throw new NoSuchElementException("Reached the end of command");

		StringBuilder sb = new StringBuilder();
		while (hasNext())
			sb.append(next()).append(' ');
		return sb.substring(0, sb.length() - 1);
	}

	/**
	 * Get the name of the character a command is to be applied to. 
	 * @param flag the argument that signifies that an optional target parameter
	 * was specified. If null, the target parameter is not preceded by a
	 * signifying argument.
	 * @param def the default value if no target was specified - usually the
	 * character that called the command.
	 * @return the target character's name, or null if there was a parsing
	 * error.
	 */
	public String extractTarget(String flag, String def) {
		if (flag == null)
			if (hasNext())
				return next();
			else
				return def;

		if (hasNext() && values[index].equalsIgnoreCase(flag))
			if (++index >= values.length)
				return null;
			else
				return next();
		return def;
	}

	/**
	 * Get the name of the character a command is to be applied to. If the
	 * target parameter exists, its value is retrieved and the index is
	 * incremented past it. The target parameter consists of the flag "-T"
	 * followed by a space and the character's name.
	 * @param def the default value if no target was specified - usually the
	 * character that called the command.
	 * @return the target character's name, or null if there was a parsing
	 * error.
	 */
	public String extractOptionalTarget(String def) {
		return extractTarget("-T", def);
	}

	public CommandTarget getTargetByName(String name, GameCharacter caller) {
		if (name.equalsIgnoreCase(caller.getName()))
			return new LocalChannelCommandTarget(caller);

		GameCharacter onLocalChannel = GameServer.getChannel(caller.getClient().getChannel()).getPlayerByName(name);
		if (onLocalChannel != null)
			return new LocalChannelCommandTarget(onLocalChannel);

		byte channel = GameServer.getChannel(caller.getClient().getChannel()).getInterChannelInterface().getChannelOfPlayer(name);
		if (channel != 0)
			return new CrossChannelCommandTarget(channel, name);

		if (Player.characterExists(name))
			return new OfflineCharacterCommandTarget(name);

		return null;
	}
}
