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

package argonms.game.net.external.handler;

import argonms.common.character.BuddyList;
import argonms.common.character.BuddyListEntry;
import argonms.common.net.external.RemoteClient;
import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import argonms.common.util.collections.Pair;
import argonms.common.util.input.LittleEndianReader;
import argonms.game.GameServer;
import argonms.game.character.GameCharacter;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;
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
public final class BuddyListHandler {
	private static final Logger LOG = Logger.getLogger(BuddyListHandler.class.getName());

	public static final byte //buddy list receive op codes
		INVITE = 0x01,
		ACCEPT = 0x02,
		DELETE = 0x03
	;

	public static final byte //buddy list send op codes
		FIRST = 0x07,
		INVITE_RECEIVED = 0x09,
		ADD = 0x0A,
		YOUR_LIST_FULL = 0x0B,
		THEIR_LIST_FULL = 0x0C,
		ALREADY_ON_LIST = 0x0D,
		NO_GM_INVITES = 0x0E,
		NONEXISTENT = 0x0F,
		REMOVE = 0x12,
		BUDDY_LOGGED_IN = 0x14,
		CAPACITY_CHANGE = 0x15
	;

	private static boolean accountLoggedIn(int playerId) {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT `a`.`connected` "
					+ "FROM `accounts` `a` LEFT JOIN `characters` `c` ON `c`.`accountid` = `a`.`id` "
					+ "WHERE `c`.`id` = ?");
			ps.setInt(1, playerId);
			rs = ps.executeQuery();
			if (!rs.next())
				return false;
			//if logged into login or shop, treat as if offline since they don't
			//overwrite the buddyentries SQL table
			return rs.getByte(1) == RemoteClient.STATUS_INGAME;
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Error checking if character " + playerId + " is online", e);
			return false;
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
	}

	private static byte inviteOfflinePlayer(Connection con, int invitee, int inviter, String inviterName) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("SELECT "
					+ "(`c`.`buddyslots` <= (SELECT COUNT(*) FROM `buddyentries` WHERE `owner` = `c`.`id`  AND `status` <> " + BuddyListEntry.STATUS_INVITED + ")) AS `full`,"
					+ "EXISTS (SELECT * FROM `buddyentries` WHERE `owner` = `c`.`id` AND `buddy` = ?) AS `readd` "
					+ "FROM `characters` `c` WHERE `id` = ?");
			ps.setInt(1, inviter);
			ps.setInt(2, invitee);
			rs = ps.executeQuery();
			if (!rs.next())
				return -1;
			if (rs.getBoolean(1))
				return THEIR_LIST_FULL;
			//assert row retrieved in subquery for `readd` had `status` == STATUS_HALF_OPEN
			boolean reAdd = rs.getBoolean(2);
			ps.close();

			if (!reAdd) {
				ps = con.prepareStatement("INSERT INTO `buddyentries` "
						+ "(`owner`,`buddy`,`buddyname`,`status`) VALUES (?,?,?," + BuddyListEntry.STATUS_INVITED + ")");
				ps.setInt(1, invitee);
				ps.setInt(2, inviter);
				ps.setString(3, inviterName);
				ps.executeUpdate();
				return Byte.MAX_VALUE;
			} else {
				ps = con.prepareStatement("UPDATE `buddyentries` SET `status` = " + BuddyListEntry.STATUS_MUTUAL + " WHERE `owner` = ? AND `buddy` = ?");
				ps.setInt(1, invitee);
				ps.setInt(2, inviter);
				ps.executeUpdate();
				return Byte.MIN_VALUE;
			}
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, null);
		}
	}

	private static void processSendInvite(String invitee, GameClient client) {
		GameCharacter p = client.getPlayer();
		BuddyList bList = p.getBuddyList();
		if (bList.isFull()) {
			client.getSession().send(GamePackets.writeSimpleBuddyListMessage(YOUR_LIST_FULL));
			return;
		}
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT `a`.`connected`,`c`.`world`,`c`.`id`,`c`.`name`,`a`.`gm` "
					+ "FROM `characters` `c` LEFT JOIN `accounts` `a` ON `c`.`accountid` = `a`.`id` "
					+ "WHERE `c`.`name` = ?");
			ps.setString(1, invitee);
			rs = ps.executeQuery();
			if (!rs.next() || rs.getByte(2) != client.getWorld()) {
				client.getSession().send(GamePackets.writeSimpleBuddyListMessage(NONEXISTENT));
				return;
			}
			if (rs.getByte(5) > p.getPrivilegeLevel()) {
				client.getSession().send(GamePackets.writeSimpleBuddyListMessage(NO_GM_INVITES));
				return;
			}
			int inviteeId = rs.getInt(3);
			if (bList.getBuddy(inviteeId) != null || bList.isInInvites(inviteeId)) {
				client.getSession().send(GamePackets.writeSimpleBuddyListMessage(ALREADY_ON_LIST));
				return;
			}
			switch (rs.getByte(1)) {
				case RemoteClient.STATUS_INGAME: {
					Pair<Byte, Byte> channelAndResult = GameServer.getChannel(client.getChannel()).getCrossServerInterface().sendBuddyInvite(p, inviteeId);
					byte result = channelAndResult.right.byteValue();
					if (result == Byte.MAX_VALUE) {
						bList.addBuddy(new BuddyListEntry(inviteeId, rs.getString(4), BuddyListEntry.STATUS_HALF_OPEN));
						client.getSession().send(GamePackets.writeBuddyList(ADD, bList));
						break;
					} else if (result == Byte.MIN_VALUE) {
						bList.addBuddy(new BuddyListEntry(inviteeId, rs.getString(4), BuddyListEntry.STATUS_MUTUAL, channelAndResult.left.byteValue()));
						client.getSession().send(GamePackets.writeBuddyList(ADD, bList));
						break;
					} else if (result != -1) {
						client.getSession().send(GamePackets.writeSimpleBuddyListMessage(result));
						break;
					}
					//apparently they are offline...
					//intentional fallthrough to inviteOfflinePlayer
				}
				default: {
					byte result = inviteOfflinePlayer(con, inviteeId, p.getId(), p.getName());
					if (result == Byte.MAX_VALUE) {
						bList.addBuddy(new BuddyListEntry(inviteeId, rs.getString(4), BuddyListEntry.STATUS_HALF_OPEN));
						client.getSession().send(GamePackets.writeBuddyList(ADD, bList));
					} else if (result == Byte.MIN_VALUE) {
						bList.addBuddy(new BuddyListEntry(inviteeId, rs.getString(4), BuddyListEntry.STATUS_MUTUAL));
						client.getSession().send(GamePackets.writeBuddyList(ADD, bList));
					} else if (result != -1) {
						client.getSession().send(GamePackets.writeSimpleBuddyListMessage(result));
					}
					//uhh, if result == -1, then I guess the player we're trying
					//to add just deleted himself while we were handling this
					//player's request...
					break;
				}
			}
		} catch (SQLException e) {
			LOG.log(Level.WARNING, "Error inviting " + invitee + " to buddy list of " + client.getPlayer().getName(), e);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
	}

	private static void processAcceptInvite(int inviterId, GameClient client) {
		GameCharacter p = client.getPlayer();
		BuddyList bList = p.getBuddyList();
		if (!bList.isInInvites(inviterId)) {
			client.getSession().send(GamePackets.writeSimpleBuddyListMessage(NONEXISTENT));
			return;
		}
		String name = bList.removeInvite(inviterId);
		if (bList.isFull()) {
			client.getSession().send(GamePackets.writeSimpleBuddyListMessage(YOUR_LIST_FULL));
			return;
		}
		bList.addBuddy(new BuddyListEntry(inviterId, name, BuddyListEntry.STATUS_MUTUAL));
		client.getSession().send(GamePackets.writeBuddyList(ADD, bList));

		//if (!accountLoggedIn(inviterId)) we are absolutely certain inviter is
		//offline.
		//otherwise, channel scan in sendBuddyAccepted because we have no idea
		//whether he is online, and if so, in what channel. sendBuddyInviteAccepted
		//will return false if he could not be found on any channel.
		//if we conclude he is offline, update his entry directly on the database
		if (!accountLoggedIn(inviterId) || !GameServer.getChannel(client.getChannel()).getCrossServerInterface().sendBuddyInviteAccepted(p, inviterId)) {
			//if inviter is concluded to be offline, attempt to make his entry MUTUAL on database
			Connection con = null;
			PreparedStatement ps = null;
			try {
				con = DatabaseManager.getConnection(DatabaseType.STATE);
				ps = con.prepareStatement("UPDATE `buddyentries` SET `status` = " + BuddyListEntry.STATUS_MUTUAL
						+ " WHERE `owner` = ? AND `buddy` = ?");
				ps.setInt(1, inviterId);
				ps.setInt(2, p.getId());
				ps.executeUpdate();
			} catch (SQLException e) {
				LOG.log(Level.WARNING, "Could not accept buddy invite", e);
			} finally {
				DatabaseManager.cleanup(DatabaseType.STATE, null, ps, con);
			}
		}
	}

	private static void processDeleteEntry(int deletedId, GameClient client) {
		GameCharacter p = client.getPlayer();
		BuddyList bList = p.getBuddyList();
		if (bList.isInInvites(deletedId)) {
			bList.removeInvite(deletedId);
			return;
		}
		BuddyListEntry removed = bList.removeBuddy(deletedId);
		if (removed == null) {
			client.getSession().send(GamePackets.writeSimpleBuddyListMessage(NONEXISTENT));
			return;
		}
		byte channel = removed.getChannel();
		//either we sent an invite and the other user has not responded yet,
		//or the other user deleted us from his/her own buddy list already.
		//doesn't hurt to try to retract the invite even if it's the second case
		//(note, removed.getStatus() == STATUS_MUTUAL is equivalent to !tryRetractInvite)
		boolean tryRetractInvite = (removed.getStatus() == BuddyListEntry.STATUS_HALF_OPEN);
		client.getSession().send(GamePackets.writeBuddyList(REMOVE, bList));

		//if (channel == BuddyListEntry.OFFLINE_CHANNEL && removed.getStatus() == STATUS_MUTUAL),
		//we are absolutely certain deleted buddy is offline - STATUS_MUTUAL entries always have accurate channels
		//otherwise, channel scan in sendBuddyDeleted because we have no idea
		//whether he is online, and if so, in what channel. sendBuddyInviteRetracted
		//will return false if he could not be found on any channel.
		//if we conclude he is offline, update his entry or delete invite to him
		//directly on the database
		if (channel != BuddyListEntry.OFFLINE_CHANNEL) {
			//if entry's channel is not OFFLINE_CHANNEL, it must be STATUS_MUTUAL
			assert !tryRetractInvite;
			GameServer.getChannel(client.getChannel()).getCrossServerInterface().sendBuddyDeleted(p, deletedId, channel);
		//sendBuddyInviteRetracted will attempt to remove our invite to buddy if he is found logged into a channel (no effect if tryRetractInvite is true and is the second case)
		} else if (!tryRetractInvite || !GameServer.getChannel(client.getChannel()).getCrossServerInterface().sendBuddyInviteRetracted(p, deletedId)) {
			//if buddy is concluded to be offline, attempt to remove invite to him on database (no effect if tryRetractInvite is true and is the second case) or make his entry HALF_OPEN
			assert !accountLoggedIn(deletedId) || GameServer.getChannel(client.getChannel()).getCrossServerInterface().scanChannelOfPlayer(removed.getName(), false) == 0;
			Connection con = null;
			PreparedStatement ps = null;
			try {
				con = DatabaseManager.getConnection(DatabaseType.STATE);
				if (!tryRetractInvite)
					ps = con.prepareStatement("UPDATE `buddyentries` SET `status` = " + BuddyListEntry.STATUS_HALF_OPEN
							+ " WHERE `owner` = ? AND `buddy` = ?");
				else
					ps = con.prepareStatement("DELETE FROM `buddyentries`"
							+ " WHERE `owner` = ? AND `buddy` = ?");
					//assert no rows deleted or deleted row had `status` == STATUS_INVITED.
				ps.setInt(1, deletedId);
				ps.setInt(2, p.getId());
				ps.executeUpdate();
			} catch (SQLException e) {
				LOG.log(Level.WARNING, "Could not delete buddy entry", e);
			} finally {
				DatabaseManager.cleanup(DatabaseType.STATE, null, ps, con);
			}
		}
	}

	public static void handleListModification(LittleEndianReader packet, GameClient gc) {
		switch (packet.readByte()) {
			case INVITE:
				processSendInvite(packet.readLengthPrefixedString(), gc);
				break;
			case ACCEPT:
				processAcceptInvite(packet.readInt(), gc);
				break;
			case DELETE:
				processDeleteEntry(packet.readInt(), gc);
				break;
		}
	}

	private BuddyListHandler() {
		//uninstantiable...
	}
}
