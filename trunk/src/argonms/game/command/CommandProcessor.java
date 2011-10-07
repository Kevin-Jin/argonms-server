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

package argonms.game.command;

import argonms.common.UserPrivileges;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.character.inventory.InventoryTools.UpdatedSlots;
import argonms.game.GameServer;
import argonms.game.character.GameCharacter;
import argonms.game.command.CommandDefinition.CommandAction;
import argonms.game.field.GameMap;
import argonms.game.field.MapEntity;
import argonms.game.field.MapEntity.EntityType;
import argonms.game.field.entity.ItemDrop;
import argonms.game.field.entity.Mob;
import argonms.game.field.entity.PlayerNpc;
import argonms.game.loading.skill.SkillDataLoader;
import argonms.game.loading.skill.SkillStats;
import argonms.game.net.external.GamePackets;
import java.awt.Point;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 *
 * @author GoldenKevin
 */
public class CommandProcessor {
	private static final Pattern argSplit;
	private static final CommandProcessor singleton;
	private final Map<String, AbstractCommandDefinition> definitions;

	static {
		argSplit = Pattern.compile(" ");
		singleton = new CommandProcessor();
	}

	private CommandProcessor() {
		definitions = new LinkedHashMap<String, AbstractCommandDefinition>();
		populateDefinitions();
	}

	private void populateDefinitions() {
		definitions.put("!id", new SearchCommandHandler());
		definitions.put("!stat", new StatCommandHandler());
		definitions.put("!tp", new CommandDefinition(new CommandAction() {
			private String getUsage() {
				return "Usage: !TP {name of player to warp} {name of player to warp to}";
			}

			@Override
			public void doAction(GameCharacter p, String[] args, ClientNoticeStream resp) {
				if (args.length < 3) {
					resp.printErr(getUsage());
					return;
				}
				GameCharacter warpee = GameServer.getChannel(p.getClient().getChannel()).getPlayerByName(args[1]);
				if (warpee == null) {
					resp.printErr("No player named " + args[1] + " is online on this channel.");
					resp.printErr(getUsage());
					return;
				}
				GameCharacter warpTo = GameServer.getChannel(p.getClient().getChannel()).getPlayerByName(args[2]);
				if (warpTo == null) {
					resp.printErr("No player named " + args[2] + " is online on this channel.");
					resp.printErr(getUsage());
					return;
				}
				GameMap map = warpTo.getMap();
				warpee.changeMap(map.getDataId(), map.nearestSpawnPoint(warpTo.getPosition()));
			}
		}, "Teleport a player on this channel to another on this channel", UserPrivileges.GM));
		definitions.put("!town", new TownCommandHandler());
		definitions.put("!skill", new CommandDefinition(new CommandAction() {
			private String getUsage() {
				return "Usage: !SKILL {skillid} {level} [master level]";
			}

			@Override
			public void doAction(GameCharacter p, String[] args, ClientNoticeStream resp) {
				if (args.length < 3) {
					resp.printErr(getUsage());
					return;
				}
				int skillId;
				byte skillLevel, masterLevel = -1;
				SkillStats s;
				try {
					skillId = Integer.parseInt(args[1]);
					s = SkillDataLoader.getInstance().getSkill(skillId);
					if (s == null)
						throw new NumberFormatException();
				} catch (NumberFormatException e) {
					resp.printErr(args[1] + " is not a valid skillid.");
					resp.printErr(getUsage());
					return;
				}
				try {
					skillLevel = Byte.parseByte(args[2]);
					if (skillLevel != 0 && s.getLevel(skillLevel) == null)
						throw new NumberFormatException();
				} catch (NumberFormatException e) {
					resp.printErr(args[2] + " is not a valid level of skill " + skillId + ".");
					resp.printErr(getUsage());
					return;
				}
				if (args.length > 3) {
					try {
						masterLevel = Byte.parseByte(args[3]);
						if (masterLevel != 0 && s.getLevel(masterLevel) == null)
							throw new NumberFormatException();
					} catch (NumberFormatException e) {
						resp.printErr(args[3] + " is not a valid master level of skill " + skillId + ".");
						resp.printErr(getUsage());
						return;
					}
				}
				p.setSkillLevel(skillId, skillLevel, masterLevel);
			}
		}, "Change the level of one of your skills", UserPrivileges.GM));
		definitions.put("!maxequips", new MaxStatCommandHandlers.MaxEquipStatsHandler());
		definitions.put("!maxskills", new MaxStatCommandHandlers.MaxSkillsHandler());
		definitions.put("!maxall", new MaxStatCommandHandlers.MaxAllHandler());
		definitions.put("!give", new CommandDefinition(new CommandAction() {
			private String getUsage() {
				return "Usage: !GIVE {itemid} [quantity]";
			}

			@Override
			public void doAction(GameCharacter p, String[] args, ClientNoticeStream resp) {
				if (args.length < 2) {
					resp.printErr(getUsage());
					return;
				}
				int itemId;
				short quantity = 1;
				try {
					itemId = Integer.parseInt(args[1]);
					//TODO: check if item is valid. Throw NumberFormatException if not
				} catch (NumberFormatException e) {
					resp.printErr(args[1] + " is not a valid itemid.");
					resp.printErr(getUsage());
					return;
				}
				if (args.length > 2) {
					try {
						quantity = Short.parseShort(args[2]);
					} catch (NumberFormatException e) {
						resp.printErr(args[2] + " is not a valid item quantity.");
						resp.printErr(getUsage());
						return;
					}
				}
				InventoryType type = InventoryTools.getCategory(itemId);
				Inventory inv = p.getInventory(type);
				UpdatedSlots changedSlots = InventoryTools.addToInventory(inv, itemId, quantity);
				short pos;
				InventorySlot slot;
				for (Short s : changedSlots.modifiedSlots) {
					pos = s.shortValue();
					slot = inv.get(pos);
					p.getClient().getSession().send(GamePackets.writeInventoryUpdateSlotQuantity(type, pos, slot));
				}
				for (Short s : changedSlots.addedOrRemovedSlots) {
					pos = s.shortValue();
					slot = inv.get(pos);
					p.getClient().getSession().send(GamePackets.writeInventoryAddSlot(type, pos, slot));
				}
				p.itemCountChanged(itemId);
			}
		}, "Give yourself an item", UserPrivileges.GM));
		definitions.put("!spawn", new SpawnCommandHandler());
		definitions.put("!playernpc", new CommandDefinition(new CommandAction() {
			private String getUsage() {
				return "Usage: !PLAYERNPC [scriptid]";
			}

			@Override
			public void doAction(GameCharacter p, String[] args, ClientNoticeStream resp) {
				int scriptId = 9901000;
				if (args.length > 1) {
					try {
						scriptId = Integer.parseInt(args[1]);
						if (scriptId < 9901000 || scriptId > 9901319) {
							resp.printErr("Scriptid must be between 9901000 and 9901319.");
							return;
						}
					} catch (NumberFormatException e) {
						resp.printErr(args[2] + " is not a valid scriptid.");
						resp.printErr(getUsage());
						return;
					}
				}
				PlayerNpc npc = new PlayerNpc(p, scriptId);
				p.getMap().spawnPlayerNpc(npc);
			}
		}, "Spawn a temporary player NPC of yourself.", UserPrivileges.GM));
		definitions.put("!cleardrops", new CommandDefinition(new CommandAction() {
			private void clearDrop(ItemDrop drop, GameCharacter p) {
				synchronized (drop) {
					drop.expire();
					p.getMap().destroyEntity(drop);
				}
			}

			@Override
			public void doAction(final GameCharacter p, String[] args, ClientNoticeStream resp) {
				Collection<MapEntity> drops = new ArrayList<MapEntity>(p.getMap().getAllEntities(EntityType.DROP));
				for (MapEntity ent : drops)
					clearDrop((ItemDrop) ent, p);
			}
		}, "Expires all dropped items on the map.", UserPrivileges.GM));
		definitions.put("!clearmobs", new CommandDefinition(new CommandAction() {
			private boolean contains(String[] array, String search) {
				for (int i = 0; i < array.length; i++)
					if (array[i].equalsIgnoreCase(search))
						return true;
				return false;
			}

			private void clearMob(String[] args, GameCharacter p, Mob mob) {
				if (contains(args, "-K")) {
					synchronized (mob) {
						mob.hurt(p, mob.getHp());
						p.getMap().killMonster(mob, p);
					}
				} else {
					synchronized (mob) {
						mob.hurt(p, mob.getHp());
						p.getMap().removeMonster(mob);
					}
				}
			}

			@Override
			public void doAction(final GameCharacter p, final String[] args, ClientNoticeStream resp) {
				Collection<MapEntity> monsters = new ArrayList<MapEntity>(p.getMap().getAllEntities(EntityType.MONSTER));
				for (MapEntity ent : monsters)
					clearMob(args, p, (Mob) ent);
			}
		}, "Removes all monsters on the map, either killing them for drops and an exp reward (specify with -k) or simply just wipe them out.", UserPrivileges.GM));
		definitions.put("!info", new CommandDefinition(new CommandAction() {
			private String getUsage() {
				return "Usage: !INFO [player's name]";
			}

			@Override
			public void doAction(GameCharacter p, String[] args, ClientNoticeStream resp) {
				if (args.length > 1) {
					p = GameServer.getChannel(p.getClient().getChannel()).getPlayerByName(args[1]);
					if (p == null) {
						resp.printErr("No player named " + args[1] + " is online on this channel.");
						resp.printErr(getUsage());
						return;
					}
				}
				Point pos = p.getPosition();
				resp.printOut("PlayerId=" + p.getId() + "; Map=" + p.getMapId() + "; Position(" + pos.x + "," + pos.y + ")");
			}
		}, "Show location info of yourself or another player", UserPrivileges.GM));
		definitions.put("!rate", new CommandDefinition(new CommandAction() {
			private String getUsage() {
				return "Usage: !RATE {EXP | MESO | DROP} {new rate}";
			}

			@Override
			public void doAction(GameCharacter p, String[] args, ClientNoticeStream resp) {
				if (args.length < 3 || !AbstractCommandDefinition.isNumber(args[2])) {
					resp.printErr(getUsage());
					return;
				}
				short rate = (short) Math.min(Integer.parseInt(args[2]), Short.MAX_VALUE);
				if (args[1].equalsIgnoreCase("exp")) {
					GameServer.getVariables().setExpRate(rate);
					resp.printOut("The exp rate of this game server has been set to "
							+ rate + ". Changes will be reverted on the next server restart.");
				} else if (args[1].equalsIgnoreCase("meso")) {
					GameServer.getVariables().setMesoRate(rate);
					resp.printOut("The meso rate of this game server has been set to "
							+ rate + ". Changes will be reverted on the next server restart.");
				} else if (args[1].equalsIgnoreCase("drop")) {
					GameServer.getVariables().setDropRate(rate);
					resp.printOut("The drop rate of this game server has been set to "
							+ rate + ". Changes will be reverted on the next server restart.");
				} else {
					resp.printErr(getUsage());
					return;
				}
			}
		}, "Change the exp, meso, or drop rate of this game server.",
				UserPrivileges.SUPER_GM));
		definitions.put("!who", new CommandDefinition(new CommandAction() {
			private String getUsage() {
				return "Usage: !WHO [minimum privilege level]";
			}

			@Override
			public void doAction(GameCharacter p, String[] args, ClientNoticeStream resp) {
				byte privilegeLevelLimit = UserPrivileges.USER;
				if (args.length > 1) {
					try {
						privilegeLevelLimit = Byte.parseByte(args[1]);
					} catch (NumberFormatException e) {
						resp.printErr(getUsage());
						return;
					}
				}
				StringBuilder sb = new StringBuilder();
				for (GameCharacter c : GameServer.getChannel(p.getClient().getChannel()).getConnectedPlayers())
					if (c.getPrivilegeLevel() >= privilegeLevelLimit)
						sb.append(c.getName()).append(",");
				if (sb.length() > 0) //remove terminal delimiter
					sb.delete(sb.length() - 1, sb.length());
				resp.printOut("Connected users: " + sb);
			}
		}, "Lists all online users in this channel (and optionally filter them by privilege level).",
				UserPrivileges.GM));
		definitions.put("!uptime", new CommandDefinition(new CommandAction() {
			@Override
			public void doAction(GameCharacter p, String[] args, ClientNoticeStream resp) {
				long startMillis = GameServer.getChannel(p.getClient().getChannel()).getTimeStarted();
				long upTimeMillis = System.currentTimeMillis() - startMillis;
				Calendar startDate = Calendar.getInstance();
				startDate.setTimeInMillis(startMillis);
				DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.LONG);
				resp.printOut("This game server was started on " + fmt.format(startDate.getTime()) + ".");

				long upDays = upTimeMillis / (1000 * 60 * 60 * 24);
				long upHours = (upTimeMillis % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
				long upMinutes = ((upTimeMillis % (1000 * 60 * 60 * 24)) % (1000 * 60 * 60)) / (1000 * 60);
				long upSeconds = (((upTimeMillis % (1000 * 60 * 60 * 24)) % (1000 * 60 * 60)) % (1000 * 60)) / 1000;
				String upTimeStr = "It has been up for ";
				if (upDays > 0)
					upTimeStr += upDays + (upDays != 1 ? " days, " : " day, ");
				if (upHours > 0)
					upTimeStr += upHours + (upHours != 1 ? " hours, " : " hour, ");
				if (upMinutes > 0)
					upTimeStr += upMinutes + (upMinutes != 1 ? " minutes, " : " minute, ");
				if (upSeconds > 0)
					upTimeStr += upSeconds + (upSeconds != 1 ? " seconds, " : " second, ");
				resp.printOut(upTimeStr.substring(0, upTimeStr.length() - 2) + ".");

				long heapNow = Runtime.getRuntime().totalMemory() / (1024 * 1024);
				long heapMax = Runtime.getRuntime().maxMemory() / (1024 * 1024);
				long heapFree = Runtime.getRuntime().freeMemory() / (1024 * 1024);
				resp.printOut("Current heap usage: " + (heapNow - heapFree) + "MB/" + heapNow + "MB. "
						+ "Can add a max of " + (heapMax - (heapNow - heapFree)) + "MB to heap without OutOfMemoryError.");
			}
		}, "Print general info about the server's resource usage", UserPrivileges.ADMIN));
		definitions.put("!help", new CommandDefinition(new HelpCommandHandler(),
				"Displays this message", UserPrivileges.USER));
	}

	public void process(GameCharacter p, String line) {
		String[] args = argSplit.split(line);
		AbstractCommandDefinition def = definitions.get(args[0].toLowerCase());
		ClientNoticeStream resp = new ClientNoticeStream(p.getClient());
		if (def != null && p.getPrivilegeLevel() >= def.minPrivilegeLevel()) {
			def.execute(p, args, resp);
		} else {
			resp.printErr(args[0] + " is not a valid command. Type !help to get a list of valid commands.");
		}
	}

	public static CommandProcessor getInstance() {
		return singleton;
	}

	private class HelpCommandHandler implements CommandAction {
		@Override
		public void doAction(GameCharacter p, String[] args, ClientNoticeStream resp) {
			for (Entry<String, AbstractCommandDefinition> entry : definitions.entrySet())
				if (p.getPrivilegeLevel() >= entry.getValue().minPrivilegeLevel())
					resp.printOut(entry.getKey() + " - " + entry.getValue().getHelpMessage());
		}
	}
}
