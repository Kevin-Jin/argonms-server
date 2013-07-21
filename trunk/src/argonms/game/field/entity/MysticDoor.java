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

package argonms.game.field.entity;

import argonms.common.GlobalConstants;
import argonms.common.character.PlayerStatusEffect;
import argonms.common.net.external.CheatTracker;
import argonms.common.util.Scheduler;
import argonms.game.GameServer;
import argonms.game.character.GameCharacter;
import argonms.game.character.PartyList;
import argonms.game.character.PlayerStatusEffectValues;
import argonms.game.character.StatusEffectTools;
import argonms.game.field.AbstractEntity;
import argonms.game.field.GameMap;
import argonms.game.net.external.GamePackets;
import java.awt.Point;
import java.lang.ref.WeakReference;

/**
 *
 * @author GoldenKevin
 */
public class MysticDoor extends AbstractEntity {
	public static final byte OUT_OF_TOWN_PORTAL_ID = (byte) 0x80;

	public static final byte
		SPAWN_ANIMATION_FALL = 0,
		SPAWN_ANIMATION_NONE = 1
	;

	private static final byte
		DESTROY_ANIMATION_FADE = 0,
		DESTROY_ANIMATION_NONE = 1
	;

	private final WeakReference<GameCharacter> owner;
	private final GameMap map;
	private final byte townPortalId;
	private MysticDoor pipe;
	private byte mod;

	private MysticDoor(GameCharacter owner, GameMap map, Point position, byte townPortalId) {
		setPosition(position);
		this.owner = new WeakReference<GameCharacter>(owner);
		this.map = map;
		this.townPortalId = townPortalId;
		this.mod = DESTROY_ANIMATION_NONE;
	}

	private void setComplement(MysticDoor complement) {
		this.pipe = complement;
	}

	@Override
	public EntityType getEntityType() {
		return EntityType.DOOR;
	}

	public boolean isInTown() {
		return townPortalId != OUT_OF_TOWN_PORTAL_ID;
	}

	public byte getPortalId() {
		return townPortalId;
	}

	@Override
	public boolean isAlive() {
		return false;
	}

	@Override
	public boolean isVisible() {
		return !isInTown();
	}

	@Override
	public byte[] getShowNewSpawnMessage() {
		return GamePackets.writeShowMysticDoor(this, SPAWN_ANIMATION_FALL);
	}

	@Override
	public byte[] getShowExistingSpawnMessage() {
		return GamePackets.writeShowMysticDoor(this, SPAWN_ANIMATION_NONE);
	}

	@Override
	public byte[] getDestructionMessage() {
		return GamePackets.writeRemoveMysticDoor(this, mod);
	}

	public GameCharacter getOwner() {
		return owner.get();
	}

	public int getMapId() {
		return map.getDataId();
	}

	public GameMap getMap() {
		return map;
	}

	public MysticDoor getComplement() {
		return pipe;
	}

	public void setNoDestroyAnimation() {
		mod = DESTROY_ANIMATION_NONE;
		pipe.mod = DESTROY_ANIMATION_NONE;
	}

	/**
	 * 
	 * @param owner
	 * @param position
	 * @return MysticDoor instance spawned in owner's map.
	 */
	public static MysticDoor open(GameCharacter owner, Point position) {
		//waiting until StatusEffectTools.applyEffects is called in order to
		//dispel any existing doors is problematic because we overwrite owner's
		//door state in this method, so just cancel the existing buff here if applicable
		PlayerStatusEffectValues existingDoor = owner.getEffectValue(PlayerStatusEffect.MYSTIC_DOOR);
		if (existingDoor != null) {
			owner.getDoor().setNoDestroyAnimation();
			StatusEffectTools.dispelEffectsAndShowVisuals(owner, existingDoor.getEffectsData());
		}

		GameMap sourceMap = owner.getMap();
		if (sourceMap.getReturnMap() == GlobalConstants.NULL_MAP) {
			CheatTracker.get(owner.getClient()).suspicious(CheatTracker.Infraction.POSSIBLE_PACKET_EDITING, "Tried to open mystic door with no return map");
			return null;
		}

		PartyList party = owner.getParty();
		GameMap destinationMap = GameServer.getChannel(owner.getClient().getChannel()).getMapFactory().getMap(sourceMap.getReturnMap());
		byte partyPosition = 0;
		if (party != null) {
			party.lockRead();
			try {
				partyPosition = party.getPositionById(owner.getId());
			} finally {
				party.unlockRead();
			}
		}
		byte destinationPortal = destinationMap.getMysticDoorPortalId(partyPosition);
		if (destinationPortal == -1)
			return null;

		final MysticDoor source = new MysticDoor(owner, sourceMap, position, OUT_OF_TOWN_PORTAL_ID);
		final MysticDoor destination = new MysticDoor(owner, destinationMap, destinationMap.getPortalPosition(destinationPortal), destinationPortal);
		source.setComplement(destination);
		destination.setComplement(source);
		sourceMap.spawnEntity(source);
		destinationMap.spawnEntity(destination);
		owner.setDoor(source);

		//byte[] spawnPacket = destination.getShowNewSpawnMessage();
		if (party != null) {
			byte[] updatedParty = GamePackets.writePartyList(party);
			party.lockRead();
			try {
				for (PartyList.LocalMember mem : party.getMembersInLocalChannel()) {
					GameCharacter memPlayer = mem.getPlayer();
					//if (memPlayer.getMapId() == destination.getMap())
						//memPlayer.getClient().getSession().send(spawnPacket);
					memPlayer.getClient().getSession().send(updatedParty);
				}
			} finally {
				party.unlockRead();
			}
		} else {
			owner.getClient().getSession().send(GamePackets.writeSpawnPortal(destination));
			//assert owner.getMapId() != destination.getMap(); //no need to send spawnPacket
		}

		Scheduler.getInstance().runAfterDelay(new Runnable() {
			@Override
			public void run() {
				source.mod = DESTROY_ANIMATION_FADE;
				destination.mod = DESTROY_ANIMATION_FADE;
			}
		}, 2000);

		return source;
	}

	public static void close(GameCharacter owner) {
		MysticDoor door = owner.getDoor();
		PartyList party = owner.getParty();
		if (door == null)
			return;

		if (!door.isInTown())
			door = door.pipe;
		door.map.destroyEntity(door);
		door.pipe.map.destroyEntity(door.pipe);
		owner.setDoor(null);

		byte[] destroyPacket = door.getDestructionMessage();
		if (party != null) {
			byte[] updatedParty = GamePackets.writePartyList(party);
			party.lockRead();
			try {
				for (PartyList.LocalMember mem : party.getMembersInLocalChannel()) {
					GameCharacter memPlayer = mem.getPlayer();
					memPlayer.getClient().getSession().send(updatedParty);
					if (memPlayer.getMapId() == door.getMapId())
						memPlayer.getClient().getSession().send(destroyPacket);
				}
			} finally {
				party.unlockRead();
			}
		} else {
			owner.getClient().getSession().send(GamePackets.writeRemovePortal());
			if (owner.getMapId() == door.getMapId())
				owner.getClient().getSession().send(destroyPacket);
		}
	}
}
