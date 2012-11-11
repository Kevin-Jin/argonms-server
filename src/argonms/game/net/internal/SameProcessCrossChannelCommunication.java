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
import argonms.game.GameServer;
import argonms.game.character.PlayerContinuation;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;

/**
 *
 * @author GoldenKevin
 */
public class SameProcessCrossChannelCommunication implements CrossChannelCommunication {
	private final CrossServerCommunication handler;
	private SameProcessCrossChannelCommunication pipe;
	private final byte localCh;
	private final byte targetCh;

	public SameProcessCrossChannelCommunication(CrossServerCommunication self, byte localCh, byte remoteCh) {
		this.handler = self;
		this.localCh = localCh;
		this.targetCh = remoteCh;
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

	@Override
	public void callPlayerExistsCheck(BlockingQueue<Pair<Byte, Object>> resultConsumer, String name) {
		resultConsumer.offer(new Pair<Byte, Object>(Byte.valueOf(targetCh), Boolean.valueOf(returnPlayerExistsResult(name))));
	}

	private boolean returnPlayerExistsResult(String name) {
		return handler.makePlayerExistsResult(name);
	}

	@Override
	public void sendPrivateChat(byte type, int[] recipients, String name, String message) {
		pipe.receivedPrivateChat(type, recipients, name, message);
	}

	private void receivedPrivateChat(byte type, int[] recipients, String name, String message) {
		handler.receivedPrivateChat(type, recipients, name, message);
	}

	@Override
	public void sendWhisper(BlockingQueue<Pair<Byte, Object>> resultConsumer, String recipient, String sender, String message) {
		resultConsumer.offer(new Pair<Byte, Object>(Byte.valueOf(targetCh), Boolean.valueOf(pipe.returnWhisperResult(recipient, sender, message))));
	}

	private boolean returnWhisperResult(String recipient, String sender, String message) {
		return handler.makeWhisperResult(recipient, sender, message, targetCh);
	}

	@Override
	public boolean sendSpouseChat(int recipient, String sender, String message) {
		return pipe.receivedSpouseChat(recipient, sender, message);
	}

	private boolean receivedSpouseChat(int recipient, String sender, String message) {
		return handler.receivedSpouseChat(recipient, sender, message);
	}
}
