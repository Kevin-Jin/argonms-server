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

import argonms.game.GameServer;
import argonms.game.character.PlayerContinuation;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *
 * @author GoldenKevin
 */
public class SameProcessCrossChannelCommunication implements CrossChannelCommunication {
	private final CrossServerCommunication handler;
	private SameProcessCrossChannelCommunication pipe;
	private final byte targetCh;

	public SameProcessCrossChannelCommunication(CrossServerCommunication self, byte channel) {
		this.handler = self;
		this.targetCh = channel;
	}

	public void connect(SameProcessCrossChannelCommunication other) {
		this.pipe = other;
		other.pipe = this;
	}

	@Override
	public byte[] getIpAddress() throws UnknownHostException {
		return InetAddress.getByName(GameServer.getInstance().getExternalIp()).getAddress();
	}

	@Override
	public int getPort() {
		return GameServer.getChannel(targetCh).getPort();
	}

	@Override
	public void sendPlayerContext(int playerId, PlayerContinuation context) {
		pipe.receivedPlayerContext(playerId, context);
	}

	private void receivedPlayerContext(int playerId, PlayerContinuation context) {
		handler.receivedChannelChangeRequest(targetCh, playerId, context);
	}

	@Override
	public void sendChannelChangeAcceptance(int playerId) {
		pipe.receivedChannelChangeAcceptance(playerId);
	}

	private void receivedChannelChangeAcceptance(int playerId) {
		handler.receivedChannelChangeAcceptance(targetCh, playerId);
	}
}
