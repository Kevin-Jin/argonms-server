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

package argonms.game.net.external.handler;

import argonms.common.character.inventory.InventoryTools;
import argonms.common.net.external.CheatTracker;
import argonms.common.net.external.ClientSendOps;
import argonms.common.util.DatabaseManager;
import argonms.common.util.TimeTool;
import argonms.common.util.input.LittleEndianReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;
import argonms.common.util.output.LittleEndianWriter;
import argonms.game.GameServer;
import argonms.game.character.GameCharacter;
import argonms.game.character.GuildList;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;
import argonms.game.script.binding.ScriptNpc;
import argonms.game.script.binding.ScriptObjectManipulator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class GuildListHandler {
	private static final Logger LOG = Logger.getLogger(GuildListHandler.class.getName());

	private static final byte //guild receive op codes
		CREATE = 0x02,
		INVITE = 0x05,
		JOIN = 0x06,
		LEAVE = 0x07,
		EXPEL = 0x08,
		CHANGE_RANK_STRING = 0x0D,
		CHANGE_PLAYER_RANK = 0x0E,
		CHANGE_EMBLEM = 0x0F,
		CHANGE_NOTICE = 0x10,
		GUILD_CONTRACT_RESPONSE = 0x1E
	;

	private static final byte //guild BBS receive op codes
		EDIT_TOPIC_STARTER = 0x00,
		DELETE_TOPIC = 0x01,
		LIST_TOPICS = 0x02,
		LOAD_TOPIC = 0x03,
		NEW_REPLY = 0x04,
		DELETE_REPLY = 0x05
	;

	public static final byte //guild send op codes
		ASK_NAME = 0x01,
		GENERAL_ERROR = 0x02,
		GUILD_CONTRACT = 0x03,
		INVITE_SENT = 0x05,
		ASK_EMBLEM = 0x11,
		LIST = 0x1A,
		NAME_TAKEN = 0x1C,
		LEVEL_TOO_LOW = 0x23,
		JOINED_GUILD = 0x27,
		ALREADY_IN_GUILD = 0x28,
		CANNOT_FIND = 0x2A,
		LEFT_GUILD = 0x2C,
		EXPELLED_FROM_GUILD = 0x2F,
		DISBANDED_GUILD = 0x32,
		INVITE_DENIED = 0x37,
		CAPACITY_CHANGED = 0x3A,
		LEVEL_JOB_CHANGED = 0x3C,
		CHANNEL_CHANGE = 0x3D,
		RANK_TITLES_CHANGED = 0x3E,
		RANK_CHANGED = 0x40,
		EMBLEM_CHANGED = 0x42,
		NOTICE_CHANGED = 0x44,
		GUILD_GP_CHANGED = 0x48,
		SHOW_GUILD_RANK_BOARD = 0x49
	;

	private static final byte //guild BBS send op codes
		TOPIC_LIST = 0x06,
		REPLY_LIST = 0x07
	;

	public static void handleListModification(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		GuildList currentGuild = p.getGuild();
		switch (packet.readByte()) {
			case CREATE: {
				String name = packet.readLengthPrefixedString();
				ScriptNpc npc = gc.getNpc();
				if (npc != null)
					ScriptObjectManipulator.guildNameReceived(npc, name);
				break;
			}
			case INVITE: {
				//invites only check players on current channel
				String name = packet.readLengthPrefixedString();
				GameCharacter invited = GameServer.getChannel(gc.getChannel()).getPlayerByName(name);
				if (currentGuild != null)
					if (!currentGuild.isFull())
						if (invited != null)
							if (invited.getGuild() == null)
								invited.getClient().getSession().send(writeGuildInvite(currentGuild.getId(), p.getName()));
							else
								gc.getSession().send(GamePackets.writeSimpleGuildListMessage(ALREADY_IN_GUILD));
						else
							gc.getSession().send(GamePackets.writeSimpleGuildListMessage(CANNOT_FIND));
					else
						CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to invite player to full guild");
				else
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to invite player to nonexistent guild");
				break;
			}
			case JOIN: {
				int guildId = packet.readInt();
				int characterId = packet.readInt();
				if (characterId != p.getId()) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.CERTAIN_PACKET_EDITING, "Tried to join guild without being invited");
					return;
				}
				//TODO: check if player was actually invited

				if (currentGuild == null)
					GameServer.getChannel(gc.getChannel()).getCrossServerInterface().sendJoinGuild(p, guildId);
				else
					gc.getSession().send(GamePackets.writeSimpleGuildListMessage(ALREADY_IN_GUILD));
				break;
			}
			case LEAVE: {
				int characterId = packet.readInt();
				String characterName = packet.readLengthPrefixedString();
				if (characterId != p.getId() || !p.getName().equals(characterName) || currentGuild == null) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to leave guild without being in one");
					return;
				}

				GameServer.getChannel(gc.getChannel()).getCrossServerInterface().sendLeaveGuild(p, currentGuild.getId());
				break;
			}
			case EXPEL: {
				int expelled = packet.readInt();
				/*String expelledName = */packet.readLengthPrefixedString();
				if (currentGuild == null || currentGuild.getMember(p.getId()).getRank() > 2) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to expel guild member without having privileges");
					return;
				}

				GameServer.getChannel(gc.getChannel()).getCrossServerInterface().sendExpelGuildMember(currentGuild.getMember(expelled), currentGuild.getId());
				break;
			}
			case CHANGE_RANK_STRING: {
				String[] titles = new String[5];
				for (int i = 0; i < 5; i++) {
					titles[i] = packet.readLengthPrefixedString();
					if (titles[i].length() > 12 || (i <= 2 || i > 2 && !titles[i].isEmpty()) && titles[i].length() < 4) {
						CheatTracker.get(gc).suspicious(CheatTracker.Infraction.CERTAIN_PACKET_EDITING, "Tried to set invalid guild title");
						return;
					}
				}
				if (currentGuild == null || currentGuild.getMember(p.getId()).getRank() > 2) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to edit guild titles without having privileges");
					return;
				}

				GameServer.getChannel(gc.getChannel()).getCrossServerInterface().sendUpdateGuildTitles(currentGuild, titles);
				break;
			}
			case CHANGE_PLAYER_RANK: {
				int characterId = packet.readInt();
				byte newRank = packet.readByte();
				if (currentGuild == null || currentGuild.getMember(p.getId()).getRank() > 2 || newRank >= 2 && currentGuild.getMember(p.getId()).getRank() > 1) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to edit guild rankings without having privileges");
					return;
				}

				GameServer.getChannel(gc.getChannel()).getCrossServerInterface().sendUpdateGuildRank(currentGuild, characterId, newRank);
				break;
			}
			case CHANGE_EMBLEM: {
				short background = packet.readShort();
				byte backgroundColor = packet.readByte();
				short design = packet.readShort();
				byte designColor = packet.readByte();
				ScriptNpc npc = gc.getNpc();
				if (npc != null)
					ScriptObjectManipulator.guildEmblemReceived(npc, background, backgroundColor, design, designColor);
				break;
			}
			case CHANGE_NOTICE: {
				String notice = packet.readLengthPrefixedString();
				if (notice.length() > 100) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.CERTAIN_PACKET_EDITING, "Tried to set invalid guild notice");
					return;
				}

				GameServer.getChannel(gc.getChannel()).getCrossServerInterface().sendUpdateGuildNotice(currentGuild, notice);
				break;
			}
			case GUILD_CONTRACT_RESPONSE: {
				int characterId = packet.readInt();
				boolean accept = packet.readBool();
				if (characterId != p.getId()) {
					CheatTracker.get(gc).suspicious(CheatTracker.Infraction.CERTAIN_PACKET_EDITING, "Tried to accept guild contract of another player");
					return;
				}

				GameServer.getChannel(gc.getChannel()).getCrossServerInterface().sendVoteGuildContract(currentGuild, characterId, accept);
				break;
			}
		}
	}

	public static void handleDenyRequest(LittleEndianReader packet, GameClient gc) {
		packet.readByte();
		String from = packet.readLengthPrefixedString();
		String to = packet.readLengthPrefixedString();
		GameCharacter inviter = GameServer.getChannel(gc.getChannel()).getPlayerByName(from);
		if (inviter != null) //check if inviter changed channels or logged off
			inviter.getClient().getSession().send(writeGuildInviteRejected(to));
	}

	private static class BbsReply {
		public final int replyId;
		public final int poster;
		public final long postTime;
		public final String content;

		public BbsReply(int replyId, int poster, long postTime, String content) {
			this.replyId = replyId;
			this.poster = poster;
			this.postTime = postTime;
			this.content = content;
		}
	}

	private static class BbsTopic {
		public final int topicId;
		public final int poster;
		public final long postTime;
		public final String subject;
		public final String content;
		public final int icon;
		public final List<BbsReply> replies;

		public BbsTopic(int topicId, int poster, long postTime, String subject, String content, int icon, List<BbsReply> replies) {
			this.topicId = topicId;
			this.poster = poster;
			this.postTime = postTime;
			this.subject = subject;
			this.content = content;
			this.icon = icon;
			this.replies = replies;
		}
	}

	private static BbsTopic loadTopic(Connection con, ResultSet rs, int guildId, int topicId) throws SQLException {
		int topicsId = rs.getInt(1);
		int poster = rs.getInt(2);
		long postTime = rs.getLong(3);
		String subject = rs.getString(4);
		String content = rs.getString(5);
		int icon = rs.getInt(6);

		List<BbsReply> replies = new ArrayList<BbsReply>();
		PreparedStatement ps = null;
		ResultSet rrs = null;
		try {
			ps = con.prepareStatement("SELECT `replyid`,`poster`,`posttime`,`content` FROM `guildbbsreplies` WHERE `topicsid` = ?");
			ps.setInt(1, topicsId);
			rrs = ps.executeQuery();
			while (rrs.next())
				replies.add(new BbsReply(rrs.getInt(1), rrs.getInt(2), rrs.getLong(3), rrs.getString(4)));
		} finally {
			DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, rrs, ps, null);
		}
		return new BbsTopic(topicId, poster, postTime, subject, content, icon, Collections.unmodifiableList(replies));
	}

	private static BbsTopic loadTopic(Connection con, int guildId, int topicId) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("SELECT `topicsid`,`poster`,`posttime`,`subject`,`content`,`icon` FROM `guildbbstopics` WHERE `guildid` = ? AND `topicid` = ?");
			ps.setInt(1, guildId);
			ps.setInt(2, topicId);
			rs = ps.executeQuery();
			if (!rs.next())
				return null;

			return loadTopic(con, rs, guildId, topicId);
		} finally {
			DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, rs, ps, null);
		}
	}

	private static List<BbsTopic> loadTopics(Connection con, int guildId, int page) throws SQLException {
		List<BbsTopic> topics = new ArrayList<BbsTopic>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("SELECT `topicsid`,`poster`,`posttime`,`subject`,`content`,`icon`,`topicid` FROM `guildbbstopics` WHERE `guildid` = ? AND `topicid` <> 0 ORDER BY `topicid` DESC LIMIT ?,10");
			ps.setInt(1, guildId);
			ps.setInt(2, page * 10);
			rs = ps.executeQuery();
			while (rs.next())
				topics.add(loadTopic(con, rs, guildId, rs.getInt(7)));
		} finally {
			DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, rs, ps, null);
		}
		return Collections.unmodifiableList(topics);
	}

	private static List<BbsReply> loadReplies(Connection con, int guildId, int topicId) throws SQLException {
		List<BbsReply> replies = new ArrayList<BbsReply>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement("SELECT `replyid`,`r`.`poster`,`r`.`posttime`,`r`.`content` FROM `guildbbsreplies` `r` LEFT JOIN `guildbbstopics` `t` ON `r`.`topicsid` = `t`.`topicsid` WHERE `guildid` = ? AND `topicid` = ?");
			ps.setInt(1, guildId);
			ps.setInt(2, topicId);
			rs = ps.executeQuery();
			while (rs.next())
				replies.add(new BbsReply(rs.getInt(1), rs.getInt(2), rs.getLong(3), rs.getString(4)));
		} finally {
			DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, rs, ps, null);
		}
		return Collections.unmodifiableList(replies);
	}

	private static String truncateTo(String str, int maxLength) {
		if (str.length() > maxLength)
			return str.substring(0, maxLength);
		return str;
	}

	private static int getAndIncrement(Connection con, String table, String field, String tableKey1, String tableKey2, int keyValue1, int keyValue2, String description) {
		int value = -1;
		String whereClause = "WHERE `" + tableKey1 + "` = ?";
		if (tableKey2 != null)
			whereClause += " AND `" + tableKey2 + "` = ?";

		int prevTransactionIsolation = Connection.TRANSACTION_REPEATABLE_READ;
		boolean prevAutoCommit = true;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			prevTransactionIsolation = con.getTransactionIsolation();
			prevAutoCommit = con.getAutoCommit();
			con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
			con.setAutoCommit(false);
			ps = con.prepareStatement("SELECT `" + field + "` FROM `" + table + "` " + whereClause + " FOR UPDATE");
			ps.setInt(1, keyValue1);
			if (tableKey2 != null)
				ps.setInt(2, keyValue2);
			rs = ps.executeQuery();
			if (!rs.next())
				return value;

			value = rs.getInt(1);
			rs.close();
			ps.close();
			ps = con.prepareStatement("UPDATE `" + table + "` SET `" + field + "` = `" + field + "` + 1 " + whereClause);
			ps.setInt(1, keyValue1);
			if (tableKey2 != null)
				ps.setInt(2, keyValue2);
			ps.executeUpdate();
			con.commit();
			return value;
		} catch (Throwable ex) {
			LOG.log(Level.WARNING, "Could not get new " + description + " for guild BBS. Rolling back all changes...", ex);
			if (con != null) {
				try {
					con.rollback();
				} catch (SQLException ex2) {
					LOG.log(Level.WARNING, "Error rolling back new " + description + ".", ex2);
				}
			}
			return value;
		} finally {
			if (con != null) {
				try {
					con.setAutoCommit(prevAutoCommit);
					con.setTransactionIsolation(prevTransactionIsolation);
				} catch (SQLException ex) {
					LOG.log(Level.WARNING, "Could not reset Connection config after getting new " + description + " for guild BBS", ex);
				}
			}
			DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, rs, ps, null);
		}
	}

	public static void handleGuildBbs(LittleEndianReader packet, GameClient gc) {
		GameCharacter p = gc.getPlayer();
		GuildList guild = p.getGuild();
		if (guild == null)
			return; //player has just been expelled from guild or is packet editing

		switch (packet.readByte()) {
			case EDIT_TOPIC_STARTER: {
				BbsTopic topic;
				guild.lockBbsWrite();
				try {
					int topicId = -1; //new topic
					if (packet.readBool())
						topicId = packet.readInt(); //edit existing topic
					if (packet.readBool())
						topicId = 0; //create new notice topic
					String subject = truncateTo(packet.readLengthPrefixedString(), 25);
					String content = truncateTo(packet.readLengthPrefixedString(), 600);
					int icon = packet.readInt();
					if (icon >= 100 && icon <= 106) {
						if (!InventoryTools.hasItem(p, 5290000 - 100 + icon, 1)) {
							CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to use message icon for guild BBS without owning item");
							return;
						}
					} else if (icon < 0 || icon > 2) {
						CheatTracker.get(gc).suspicious(CheatTracker.Infraction.CERTAIN_PACKET_EDITING, "Tried to use invalid message icon for guild BBS topic starter");
						return;
					}

					long now = System.currentTimeMillis();
					Connection con = null;
					PreparedStatement ps = null;
					try {
						con = DatabaseManager.getConnection(DatabaseManager.DatabaseType.STATE);
						if (topicId == -1 || topicId == 0) {
							String query = "INSERT INTO `guildbbstopics` (`guildid`,`topicid`,`poster`,`posttime`,`subject`,`content`,`icon`) VALUES (?,?,?,?,?,?,?)";
							if (topicId == -1)
								topicId = getAndIncrement(con, "guilds", "nextbbstopicid", "id", null, guild.getId(), -1, "topic ID");
							else if (topicId == 0)
								query += " ON DUPLICATE KEY UPDATE `posttime` = ?, `subject` = ?, `content` = ?, `icon` = ?";
							ps = con.prepareStatement(query);
							ps.setInt(1, guild.getId());
							ps.setInt(2, topicId);
							ps.setInt(3, p.getId());
							ps.setLong(4, now);
							ps.setString(5, subject);
							ps.setString(6, content);
							ps.setInt(7, icon);
							if (topicId == 0) {
								ps.setLong(8, now);
								ps.setString(9, subject);
								ps.setString(10, content);
								ps.setInt(11, icon);
							}
							ps.executeUpdate();
							if (topicId == -1)
								//assume no replies were made yet
								topic = new BbsTopic(topicId, p.getId(), now, subject, content, icon, Collections.<BbsReply>emptyList());
							else
								topic = new BbsTopic(topicId, p.getId(), now, subject, content, icon, loadReplies(con, guild.getId(), topicId));
						} else {
							ps = con.prepareStatement("UPDATE `guildbbstopics` SET `posttime` = ?, `subject` = ?, `content` = ?, `icon` = ? WHERE `guildid` = ? AND `topicid` = ? AND (`poster` = ? OR ?)");
							ps.setLong(1, now);
							ps.setString(2, subject);
							ps.setString(3, content);
							ps.setInt(4, icon);
							ps.setInt(5, guild.getId());
							ps.setInt(6, topicId);
							ps.setInt(7, p.getId());
							ps.setBoolean(8, guild.getMember(p.getId()).getRank() <= 2);
							int updateRows = ps.executeUpdate();
							if (updateRows == 0) {
								//either topic has been deleted in the meantime
								//or player is not the original poster and is
								//not the guild's master or a junior master
								CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to edit BBS topic starter without permission");
								return;
							}
							topic = new BbsTopic(topicId, p.getId(), now, subject, content, icon, loadReplies(con, guild.getId(), topicId));
						}
					} catch (SQLException ex) {
						LOG.log(Level.WARNING, "Could not edit guild BBS topic starter", ex);
						return;
					} finally {
						DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, null, ps, con);
					}
				} finally {
					guild.unlockBbsWrite();
				}

				gc.getSession().send(writeBbsTopic(topic));
				break;
			}
			case DELETE_TOPIC: {
				guild.lockBbsWrite();
				try {
					int topicId = packet.readInt();

					Connection con = null;
					PreparedStatement ps = null;
					try {
						con = DatabaseManager.getConnection(DatabaseManager.DatabaseType.STATE);
						ps = con.prepareStatement("DELETE FROM `guildbbstopics` WHERE `guildid` = ? AND `topicid` = ? AND (`poster` = ? OR ?)");
						ps.setInt(1, guild.getId());
						ps.setInt(2, topicId);
						ps.setInt(3, p.getId());
						ps.setBoolean(4, guild.getMember(p.getId()).getRank() <= 2);
						int updateRows = ps.executeUpdate();
						if (updateRows == 0) {
							//either topic has been deleted in the meantime or
							//player is not the original poster and is not the
							//guild's master or a junior master
							CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to delete BBS topic without permission");
						}
					}  catch (SQLException ex) {
						LOG.log(Level.WARNING, "Could not delete guild BBS topic", ex);
					} finally {
						DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, null, ps, con);
					}
				} finally {
					guild.unlockBbsWrite();
				}
				break;
			}
			case LIST_TOPICS: {
				BbsTopic notice;
				List<BbsTopic> topics;
				int totalTopics;
				guild.lockBbsRead();
				try {
					int page = packet.readInt();

					Connection con = null;
					PreparedStatement ps = null;
					ResultSet rs = null;
					try {
						con = DatabaseManager.getConnection(DatabaseManager.DatabaseType.STATE);
						notice = loadTopic(con, guild.getId(), 0);
						topics = loadTopics(con, guild.getId(), page);
						ps = con.prepareStatement("SELECT COUNT(*) FROM `guildbbstopics` WHERE `guildid` = ? AND `topicid` <> 0");
						ps.setInt(1, guild.getId());
						rs = ps.executeQuery();
						rs.next();
						totalTopics = rs.getInt(1);
					}  catch (SQLException ex) {
						LOG.log(Level.WARNING, "Could not list guild BBS topics", ex);
						return;
					} finally {
						DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, rs, ps, con);
					}
				} finally {
					guild.unlockBbsRead();
				}

				gc.getSession().send(writeBbs(notice, topics, totalTopics));
				break;
			}
			case LOAD_TOPIC: {
				int topicId = packet.readInt();
				
				BbsTopic topic;
				Connection con = null;
				try {
					con = DatabaseManager.getConnection(DatabaseManager.DatabaseType.STATE);
					topic = loadTopic(con, guild.getId(), topicId);
					if (topic == null)
						return;
				} catch (SQLException ex) {
					LOG.log(Level.WARNING, "Could not load guild BBS topic", ex);
					return;
				} finally {
					DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, null, null, con);
				}

				gc.getSession().send(writeBbsTopic(topic));
				break;
			}
			case NEW_REPLY: {
				BbsTopic topic;
				guild.lockBbsWrite();
				try {
					int topicId = packet.readInt();
					String content = truncateTo(packet.readLengthPrefixedString(), 25);

					Connection con = null;
					PreparedStatement ps = null;
					try {
						con = DatabaseManager.getConnection(DatabaseManager.DatabaseType.STATE);
						int replyId = getAndIncrement(con, "guildbbstopics", "nextreplyid", "guildid", "topicid", guild.getId(), topicId, "reply ID");
						ps = con.prepareStatement("INSERT INTO `guildbbsreplies` (`topicsid`,`replyid`,`poster`,`posttime`,`content`) SELECT `topicsid`,?,?,?,? FROM `guildbbstopics` WHERE `guildid` = ? AND `topicid` = ?");
						ps.setInt(1, replyId);
						ps.setInt(2, p.getId());
						ps.setLong(3, System.currentTimeMillis());
						ps.setString(4, content);
						ps.setInt(5, guild.getId());
						ps.setInt(6, topicId);
						ps.executeUpdate();

						topic = loadTopic(con, guild.getId(), topicId);
					} catch (SQLException ex) {
						LOG.log(Level.WARNING, "Could not create guild BBS topic reply", ex);
						return;
					} finally {
						DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, null, ps, con);
					}
				} finally {
					guild.unlockBbsWrite();
				}

				gc.getSession().send(writeBbsTopic(topic));
				break;
			}
			case DELETE_REPLY: {
				BbsTopic topic;
				guild.lockBbsWrite();
				try {
					int topicId = packet.readInt();
					int replyId = packet.readInt();

					Connection con = null;
					PreparedStatement ps = null;
					try {
						con = DatabaseManager.getConnection(DatabaseManager.DatabaseType.STATE);
						ps = con.prepareStatement("DELETE `r` FROM `guildbbsreplies` `r` LEFT JOIN `guildbbstopics` `t` ON `r`.`topicsid` = `t`.`topicsid` WHERE `guildid` = ? AND `topicid` = ? AND `replyid` = ? AND (`r`.`poster` = ? OR ?)");
						ps.setInt(1, guild.getId());
						ps.setInt(2, topicId);
						ps.setInt(3, replyId);
						ps.setInt(4, p.getId());
						ps.setBoolean(5, guild.getMember(p.getId()).getRank() <= 2);
						int updateRows = ps.executeUpdate();
						if (updateRows == 0) {
							//either reply has been deleted in the meantime or
							//player is not the replier and is not the guild's
							//master or a junior master
							CheatTracker.get(gc).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to delete BBS reply without permission");
							return;
						}

						topic = loadTopic(con, guild.getId(), topicId);
					}  catch (SQLException ex) {
						LOG.log(Level.WARNING, "Could not delete guild BBS reply", ex);
						return;
					} finally {
						DatabaseManager.cleanup(DatabaseManager.DatabaseType.STATE, null, ps, con);
					}
				} finally {
					guild.unlockBbsWrite();
				}

				gc.getSession().send(writeBbsTopic(topic));
				break;
			}
		}
	}

	private static byte[] writeGuildInviteRejected(String name) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(5 + name.length());

		lew.writeShort(ClientSendOps.GUILD_LIST);
		lew.writeByte(INVITE_DENIED);
		lew.writeLengthPrefixedString(name);

		return lew.getBytes();
	}

	private static byte[] writeGuildInvite(int guildId, String inviter) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(9 + inviter.length());

		lew.writeShort(ClientSendOps.GUILD_LIST);
		lew.writeByte(INVITE_SENT);
		lew.writeInt(guildId);
		lew.writeLengthPrefixedString(inviter);

		return lew.getBytes();
	}

	private static void writeBbsEntry(LittleEndianWriter lew, BbsTopic topic) {
		lew.writeInt(topic.topicId);
		lew.writeInt(topic.poster);
		lew.writeLengthPrefixedString(topic.subject);
		lew.writeLong(TimeTool.unixToWindowsTime(topic.postTime));
		lew.writeInt(topic.icon);
		lew.writeInt(topic.replies.size());
	}

	private static byte[] writeBbs(BbsTopic notice, List<BbsTopic> topics, int totalTopics) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.BBS_OPERATION);
		lew.writeByte(TOPIC_LIST);
		lew.writeBool(notice != null);
		if (notice != null)
			writeBbsEntry(lew, notice);
		lew.writeInt(totalTopics);
		lew.writeInt(topics.size());
		for (BbsTopic topic : topics)
			writeBbsEntry(lew, topic);

		return lew.getBytes();
	}

	private static byte[] writeBbsTopic(BbsTopic topic) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.BBS_OPERATION);
		lew.writeByte(REPLY_LIST);
		lew.writeInt(topic.topicId);
		lew.writeInt(topic.poster);
		lew.writeLong(TimeTool.unixToWindowsTime(topic.postTime));
		lew.writeLengthPrefixedString(topic.subject);
		lew.writeLengthPrefixedString(topic.content);
		lew.writeInt(topic.icon);
		lew.writeInt(topic.replies.size());
		for (BbsReply reply : topic.replies) {
			lew.writeInt(reply.replyId);
			lew.writeInt(reply.poster);
			lew.writeLong(TimeTool.unixToWindowsTime(reply.postTime));
			lew.writeLengthPrefixedString(reply.content);
		}

		return lew.getBytes();
	}
}
