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

package argonms.game;

import argonms.character.Player;
import argonms.game.script.NpcConversationActions;
import argonms.net.client.RemoteClient;
import argonms.tools.DatabaseConnection;
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

	private Player player;
	private NpcConversationActions npc;

	public GameClient(byte world, byte channel) {
		setWorld(world);
		setChannel(channel);
	}

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player p) {
		this.player = p;
	}

	public byte getOnlineState() {
		Connection con = DatabaseConnection.getConnection();
		byte ret;
		try {
			PreparedStatement ps = con.prepareStatement("SELECT `connected` FROM `accounts` WHERE `id` = ?");
			ps.setInt(1, getAccountId());
			ResultSet rs = ps.executeQuery();
			ret = rs.next() ? rs.getByte(1) : -1;
			rs.close();
			ps.close();
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not get connected status of account " + getAccountId(), ex);
			ret = -1;
		}
		return ret;
	}

	public void setNpc(NpcConversationActions npc) {
		this.npc = npc;
	}

	public NpcConversationActions getNpc() {
		return npc;
	}

	public byte getServerId() {
		return GameServer.getInstance().getServerId();
	}

	public void disconnected() {
		stopPingTask();
		if (npc != null)
			npc.endConversation();
		if (player != null) {
			player.close();
			GameServer.getChannel(getChannel()).removePlayer(player);
			player = null;
		}
		getSession().removeClient();
		setSession(null);
		if (!isMigrating())
			updateState(STATUS_NOTLOGGEDIN);
	}
}
