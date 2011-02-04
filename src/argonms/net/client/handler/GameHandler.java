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

package argonms.net.client.handler;

import argonms.character.Player;
import argonms.game.GameClient;
import argonms.game.GameServer;
import argonms.game.WorldChannel;
import argonms.net.client.RemoteClient;
import argonms.tools.input.LittleEndianReader;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class GameHandler {
	private static final Logger LOG = Logger.getLogger(GameHandler.class.getName());

	public static void handlePlayerConnection(LittleEndianReader packet, RemoteClient rc) {
		GameClient client = (GameClient) rc;
		int cid = packet.readInt();
		Player player = null;
		player = Player.loadPlayer(client, cid);
		if (player == null)
			return;
		client.setPlayer(player);
		boolean allowLogin = true;
		byte state = client.getOnlineState();
		//if already online, has to be on this world since loadPlayer checks
		//if this world and world from db match.
		if (state == RemoteClient.STATUS_MIGRATION)
			allowLogin = GameServer.getInstance().channelOfPlayer(player.getId()) == -1;
		else
			allowLogin = false;
		if (!allowLogin) {
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
		}
		c.getSession().write(MaplePacketCreator.getCharInfo(player));
		if (player.isGM()) {
			// need to be hidden before being on the map o.o
			SkillFactory.getSkill(9101004).getEffect(1).applyTo(player);
		}
		player.getMap().addPlayer(player);

		try {
			Collection<BuddylistEntry> buddies = player.getBuddylist().getBuddies();
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
		}
		player.sendKeymap();
		player.sendMacros();

		for (MapleQuestStatus status : player.getStartedQuests()) {
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
}
