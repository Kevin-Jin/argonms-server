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

package argonms.game.handler;

import argonms.UserPrivileges;
import argonms.character.KeyBinding;
import argonms.character.Player;
import argonms.character.SkillMacro;
import argonms.character.skill.Skills;
import argonms.character.skill.StatusEffectTools;
import argonms.game.GameClient;
import argonms.game.GameServer;
import argonms.game.WorldChannel;
import argonms.net.external.ClientSendOps;
import argonms.net.external.CommonPackets;
import argonms.net.external.RemoteClient;
import argonms.tools.Rng;
import argonms.tools.TimeUtil;
import argonms.tools.input.LittleEndianReader;
import argonms.tools.output.LittleEndianByteArrayWriter;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class GameEnterHandler {
	private static final Logger LOG = Logger.getLogger(GameEnterHandler.class.getName());

	public static void handlePlayerConnection(LittleEndianReader packet, RemoteClient rc) {
		GameClient client = (GameClient) rc;
		int cid = packet.readInt();
		Player player = null;
		player = Player.loadPlayer(client, cid);
		if (player == null)
			return;
		client.setPlayer(player);
		boolean allowLogin;
		byte state = client.getOnlineState();
		//if already online, has to be on this world since loadPlayer checks
		//if this world and world from db match.
		if (state == RemoteClient.STATUS_MIGRATION)
			allowLogin = GameServer.getInstance().channelOfPlayer(player.getId()) == -1;
		else
			allowLogin = false;
		if (!allowLogin) {
			LOG.log(Level.WARNING, "Player {0} tried to double login on world"
					+ " {1}", new Object[] { player.getName(), rc.getWorld() });
			client.getSession().close();
			return;
		}
		client.updateState(RemoteClient.STATUS_INGAME);

		WorldChannel cserv = GameServer.getChannel(client.getChannel());
		cserv.addPlayer(player);
		/*if (cserv.useBuffCooldownStorage()) {
			try {
				WorldChannelInterface wci = ChannelServer.getInstance(c.getChannel()).getWorldInterface();
				if (ChannelServer.useCooldown()) {
					List<PlayerCoolDownValueHolder> cooldowns = wci.getCooldownsFromStorage(cid);
					if (cooldowns != null) {
						player.giveCoolDowns(cooldowns);
					}
				}
				//List<PlayerBuffValueHolder> buffs = wci.getBuffsFromStorage(cid);
				List<PlayerBuffSourceValueHolder> buffs = wci.getBuffSourcesFromStorage(cid);
				if (buffs != null) {
					//player.silentGiveBuffs(buffs);
					player.silentGiveBuffSources(buffs);
				}
			} catch (RemoteException e) {
				c.getChannelServer().reconnectWorld();
			}
		}*/
		client.getSession().send(writeEnterMap(player));
		if (player.getPrivilegeLevel() > UserPrivileges.USER) //hide
			StatusEffectTools.useSkill(player, Skills.HIDE, ((byte) 1));
		player.getMap().spawnPlayer(player);

		/*try {
			Collection<BuddylistEntry> buddies = player.getBuddyList().getBuddies();
			int buddyIds[] = player.getBuddylist().getBuddyIds();

			cserv.getWorldInterface().loggedOn(player.getName(), player.getId(), c.getChannel(), buddyIds);
			if (player.getParty() != null) {
				channelServer.getWorldInterface().updateParty(player.getParty().getId(), PartyOperation.LOG_ONOFF, new MaplePartyCharacter(player));
			}

			CharacterIdChannelPair[] onlineBuddies = cserv.getWorldInterface().multiBuddyFind(player.getId(), buddyIds);
			for (CharacterIdChannelPair onlineBuddy : onlineBuddies) {
				BuddylistEntry ble = player.getBuddylist().get(onlineBuddy.getCharacterId());
				ble.setChannel(onlineBuddy.getChannel());
				player.getBuddylist().put(ble);
			}
			c.getSession().write(MaplePacketCreator.updateBuddylist(buddies));

			if (player.getGuildId() > 0) {
				c.getChannelServer().getWorldInterface().setGuildMemberOnline(
						player.getMGC(), true, c.getChannel());
				c.getSession().write(MaplePacketCreator.showGuildInfo(player));
			}
		} catch (RemoteException e) {
			LOG.info("REMOTE THROW", e);
			channelServer.reconnectWorld();
		}
		player.updatePartyMemberHP();
		try {
			c.getPlayer().showNote();
		} catch (SQLException e) {
			LOG.error("LOADING NOTE", e);
		}*/
		client.getSession().send(writeKeymap(player.getKeyMap()));
		client.getSession().send(writeMacros(player.getMacros()));

		/*for (MapleQuestStatus status : player.getStartedQuests()) {
			if (status.hasMobKills()) {
				c.getSession().write(MaplePacketCreator.updateQuestMobKills(status));
			}
		}

		CharacterNameAndId pendingBuddyRequest = player.getBuddylist().pollPendingRequest();
		if (pendingBuddyRequest != null) {
			player.getBuddylist().put(new BuddylistEntry(pendingBuddyRequest.getName(), pendingBuddyRequest.getId(), -1, false));
			c.getSession().write(MaplePacketCreator.requestBuddylistAdd(pendingBuddyRequest.getId(), pendingBuddyRequest.getName()));
		}

		player.checkMessenger();
		player.checkBerserk();
		player.expirationTask();*/
	}

	private static byte[] writeEnterMap(Player p) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.CHANGE_MAP);
		lew.writeInt(p.getClient().getChannel() - 1);
		lew.writeByte((byte) 1);
		lew.writeByte((byte) 1);
		lew.writeShort((short) 0);
		Random generator = Rng.getGenerator();
		lew.writeInt(generator.nextInt());
		lew.writeInt(generator.nextInt());
		lew.writeInt(generator.nextInt());

		CommonPackets.writeCharData(lew, p);

		lew.writeLong(TimeUtil.unixToWindowsTime(System.currentTimeMillis()));
		return lew.getBytes();
	}

	private static byte[] writeKeymap(KeyBinding[] bindings) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(453);

		lew.writeShort(ClientSendOps.KEYMAP);
		lew.writeByte((byte) 0);

		for (int i = 0; i < 90; i++) {
			KeyBinding binding = bindings[i];
			if (binding != null) {
				lew.writeByte(binding.getType());
				lew.writeInt(binding.getAction());
			} else {
				lew.writeByte((byte) 0);
				lew.writeInt(0);
			}
		}
		return lew.getBytes();
	}

	private static byte[] writeMacros(List<SkillMacro> macros) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter();

		lew.writeShort(ClientSendOps.SKILL_MACRO);
		lew.writeByte((byte) macros.size()); // number of macros
		for (SkillMacro macro : macros) {
			lew.writeLengthPrefixedString(macro.getName());
			lew.writeBool(macro.shout());
			lew.writeInt(macro.getFirstSkill());
			lew.writeInt(macro.getSecondSkill());
			lew.writeInt(macro.getThirdSkill());
		}

		return lew.getBytes();
	}
}
