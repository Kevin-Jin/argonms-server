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

package argonms.game.net.external;

import argonms.common.net.external.RemoteClient;
import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import argonms.game.GameServer;
import argonms.game.character.GameCharacter;
import argonms.game.script.NpcConversationActions;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class GameClient extends RemoteClient {
	private static final Logger LOG = Logger.getLogger(GameClient.class.getName());

	public interface NpcMiniroom {

	}

	private GameCharacter player;
	private NpcConversationActions npc;
	private NpcMiniroom npcRoom;

	public GameClient(byte world, byte channel) {
		setWorld(world);
		setChannel(channel);
	}

	public byte getOnlineState() {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		byte ret;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT `connected` FROM `accounts` WHERE `id` = ?");
			ps.setInt(1, getAccountId());
			rs = ps.executeQuery();
			ret = rs.next() ? rs.getByte(1) : -1;
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not get connected status of account " + getAccountId(), ex);
			ret = -1;
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
		return ret;
	}

	public void setNpc(NpcConversationActions npc) {
		this.npc = npc;
	}

	public NpcConversationActions getNpc() {
		return npc;
	}

	public void setNpcRoom(NpcMiniroom room) {
		this.npcRoom = room;
	}

	public NpcMiniroom getNpcRoom() {
		return npcRoom;
	}

	public void setPlayer(GameCharacter p) {
		this.player = p;
	}

	@Override
	public GameCharacter getPlayer() {
		return player;
	}

	@Override
	public byte getServerId() {
		return GameServer.getInstance().getServerId();
	}

	private void dissociate(boolean changingChannels) {
		if (player != null) {
			if (!changingChannels)
				player.prepareLogOff();
			player.disconnect();
			GameServer.getChannel(getChannel()).removePlayer(player);
			player = null;
		}
		getSession().removeClient();
		setSession(null);
	}

	@Override
	public void disconnected() {
		final boolean changingChannels = isMigrating();
		if (npc != null)
			npc.endConversation();
		if (getSession().getQueuedReads() == 0) {
			dissociate(changingChannels);
		} else {
			getSession().setEmptyReadQueueHandler(new Runnable() {
				@Override
				public void run() {
					dissociate(changingChannels);
				}
			});
		}
		if (!changingChannels)
			updateState(STATUS_NOTLOGGEDIN);
	}
}
