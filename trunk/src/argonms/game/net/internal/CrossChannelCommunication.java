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

package argonms.game.net.internal;

import argonms.common.util.collections.Pair;
import argonms.game.character.PlayerContinuation;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;

/**
 *
 * @author GoldenKevin
 */
public interface CrossChannelCommunication {
	public byte[] getIpAddress() throws UnknownHostException;

	public int getPort();

	public void sendPlayerContext(int playerId, PlayerContinuation context);

	public void sendChannelChangeAcceptance(int playerId);

	public void callPlayerExistsCheck(BlockingQueue<Pair<Byte, Object>> resultConsumer, String name);

	public void sendPrivateChat(byte type, int[] recipients, String name, String message);

	public void sendWhisper(BlockingQueue<Pair<Byte, Object>> resultConsumer, String recipient, String sender, String message);

	public boolean sendSpouseChat(int recipient, String sender, String message);
}
