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

package argonms.game.net.external;

import argonms.common.net.external.ClientPacketProcessor;
import argonms.common.net.external.ClientRecvOps;
import argonms.common.util.input.LittleEndianReader;
import argonms.game.net.external.handler.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class ClientGamePacketProcessor extends ClientPacketProcessor<GameClient> {
	private static final Logger LOG = Logger.getLogger(ClientPacketProcessor.class.getName());

	@Override
	public void process(LittleEndianReader reader, GameClient gc) {
		switch (reader.readShort()) {
			case ClientRecvOps.SERVERLIST_REREQUEST:
			case ClientRecvOps.EXIT_CHARLIST:
			case ClientRecvOps.PICK_ALL_CHAR:
			case ClientRecvOps.ENTER_EXIT_VIEW_ALL:
			case ClientRecvOps.CHAR_SELECT:
			case ClientRecvOps.RELOG:
				//lol, char loading lag...
				break;
			case ClientRecvOps.PLAYER_CONNECTED:
				GameEnterHandler.handlePlayerConnection(reader, gc);
				break;
			case ClientRecvOps.PONG:
				gc.getSession().receivedPong();
				break;
			case ClientRecvOps.CLIENT_ERROR:
				gc.clientError(reader.readLengthPrefixedString());
				break;
			case ClientRecvOps.AES_IV_UPDATE_REQUEST:
				//no-op
				break;
			case ClientRecvOps.CHANGE_MAP:
				GameGoToHandler.handleMapChange(reader, gc);
				break;
			case ClientRecvOps.CHANGE_CHANNEL:
				GameGoToHandler.handleChangeChannel(reader, gc);
				break;
			case ClientRecvOps.ENTER_CASH_SHOP:
				GameGoToHandler.handleWarpCs(reader, gc);
				break;
			case ClientRecvOps.MOVE_PLAYER:
				GameMovementHandler.handleMovePlayer(reader, gc);
				break;
			case ClientRecvOps.MELEE_ATTACK:
				DealDamageHandler.handleMeleeAttack(reader, gc);
				break;
			case ClientRecvOps.RANGED_ATTACK:
				DealDamageHandler.handleRangedAttack(reader, gc);
				break;
			case ClientRecvOps.MAGIC_ATTACK:
				DealDamageHandler.handleMagicAttack(reader, gc);
				break;
			case ClientRecvOps.ENERGY_CHARGE_ATTACK:
				DealDamageHandler.handleEnergyChargeAttack(reader, gc);
				break;
			case ClientRecvOps.TAKE_DAMAGE:
				TakeDamageHandler.handleTakeDamage(reader, gc);
				break;
			case ClientRecvOps.MAP_CHAT:
				GameChatHandler.handleMapChat(reader, gc);
				break;
			case ClientRecvOps.FACIAL_EXPRESSION:
				GamePlayerMiscHandler.handleEmote(reader, gc);
				break;
			case ClientRecvOps.NPC_TALK:
				GameNpcHandler.handleStartConversation(reader, gc);
				break;
			case ClientRecvOps.NPC_TALK_MORE:
				GameNpcHandler.handleContinueConversation(reader, gc);
				break;
			case ClientRecvOps.NPC_SHOP:
				NpcMiniroomHandler.handleNpcShopAction(reader, gc);
				break;
			case ClientRecvOps.NPC_STORAGE:
				NpcMiniroomHandler.handleNpcStorageAction(reader, gc);
				break;
			case ClientRecvOps.ITEM_MOVE:
				InventoryHandler.handleItemMove(reader, gc);
				break;
			case ClientRecvOps.USE_ITEM:
				GameBuffHandler.handleUseItem(reader, gc);
				break;
			case ClientRecvOps.CANCEL_ITEM:
				GameBuffHandler.handleCancelItem(reader, gc);
				break;
			case ClientRecvOps.DISTRIBUTE_AP:
				StatAllocationHandler.handleApAllocation(reader, gc);
				break;
			case ClientRecvOps.HEAL_OVER_TIME:
				GamePlayerMiscHandler.handleReplenishHpMp(reader, gc);
				break;
			case ClientRecvOps.DISTRIBUTE_SP:
				StatAllocationHandler.handleSpAllocation(reader, gc);
				break;
			case ClientRecvOps.USE_SKILL:
				GameBuffHandler.handleUseSkill(reader, gc);
				break;
			case ClientRecvOps.CANCEL_SKILL:
				GameBuffHandler.handleCancelSkill(reader, gc);
				break;
			case ClientRecvOps.PREPARED_SKILL:
				DealDamageHandler.handlePreparedSkill(reader, gc);
				break;
			case ClientRecvOps.MESO_DROP:
				InventoryHandler.handleMesoDrop(reader, gc);
				break;
			case ClientRecvOps.GIVE_FAME:
				PersonalInfoHandler.handleFameUp(reader, gc);
				break;
			case ClientRecvOps.OPEN_PERSONAL_INFO:
				PersonalInfoHandler.handleOpenInfo(reader, gc);
				break;
			case ClientRecvOps.CHANGE_MAP_SPECIAL:
				GameGoToHandler.handleEnteredSpecialPortal(reader, gc);
				break;
			case ClientRecvOps.USE_INNER_PORTAL:
				GameGoToHandler.handleEnteredInnerPortal(reader, gc);
				break;
			case ClientRecvOps.QUEST_ACTION:
				GameNpcHandler.handleQuestAction(reader, gc);
				break;
			case ClientRecvOps.PARTYCHAT:
				GameChatHandler.handlePrivateChat(reader, gc);
				break;
			case ClientRecvOps.CLIENT_COMMAND:
				GameChatHandler.handleClientCommand(reader, gc);
				break;
			case ClientRecvOps.SPOUSECHAT:
				GameChatHandler.handleSpouseChat(reader, gc);
				break;
			case ClientRecvOps.MINIROOM_ACT:
				MiniroomHandler.handleAction(reader, gc);
				break;
			case ClientRecvOps.BUDDYLIST_MODIFY:
				BuddyListHandler.handleListModification(reader, gc);
				break;
			case ClientRecvOps.CHANGE_BINDING:
				GamePlayerMiscHandler.handleBindingChange(reader, gc);
				break;
			case ClientRecvOps.MOVE_SUMMON:
				GameMovementHandler.handleMoveSummon(reader, gc);
				break;
			case ClientRecvOps.SUMMON_ATTACK:
				DealDamageHandler.handleSummonAttack(reader, gc);
				break;
			case ClientRecvOps.DAMAGE_SUMMON:
				TakeDamageHandler.handlePuppetTakeDamage(reader, gc);
				break;
			case ClientRecvOps.MOVE_MOB:
				GameMovementHandler.handleMoveMob(reader, gc);
				break;
			case ClientRecvOps.MOVE_NPC:
				GameMovementHandler.handleMoveNpc(reader, gc);
				break;
			case ClientRecvOps.ITEM_PICKUP:
				InventoryHandler.handleMapItemPickUp(reader, gc);
				break;
			case ClientRecvOps.DAMAGE_REACTOR:
				ReactorHandler.handleReactorTrigger(reader, gc);
				break;
			case ClientRecvOps.TOUCH_REACTOR:
				ReactorHandler.handleReactorTouch(reader, gc);
				break;
			case ClientRecvOps.PLAYER_UPDATE:
				gc.getPlayer().saveCharacter();
				break;
			case ClientRecvOps.MAPLE_TV:
				//I think we can all agree that we don't care...
				break;
			default:
				LOG.log(Level.FINE, "Received unhandled client packet {0} bytes long:\n{1}", new Object[] { reader.available() + 2, reader });
				break;
		}
	}
}
