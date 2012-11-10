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

package argonms.game.command;

import argonms.common.UserPrivileges;
import argonms.common.character.PlayerStatusEffect;
import argonms.common.util.TimeTool;
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
import java.awt.Point;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
	private final Map<String, AbstractCommandDefinition<GameCharacterCommandCaller>> inGameOnlyCommands;
	private final Map<String, AbstractCommandDefinition<RemoteAdminCommandCaller>> byTelnetOnlyCommands;
	private final Map<String, AbstractCommandDefinition<CommandCaller>> universalCommands;

	static {
		argSplit = Pattern.compile(" ");
		singleton = new CommandProcessor();
	}

	private CommandProcessor() {
		inGameOnlyCommands = new LinkedHashMap<String, AbstractCommandDefinition<GameCharacterCommandCaller>>();
		byTelnetOnlyCommands = new LinkedHashMap<String, AbstractCommandDefinition<RemoteAdminCommandCaller>>();
		universalCommands = new LinkedHashMap<String, AbstractCommandDefinition<CommandCaller>>();
		populateDefinitions();
	}

	private void populateDefinitions() {
		universalCommands.put("!id", new SearchCommandHandler());
		universalCommands.put("!stat", new StatCommandHandler());
		universalCommands.put("!heal", new CommandDefinition<CommandCaller>(new CommandAction<CommandCaller>() {
			@Override
			public String getUsage() {
				return "Usage: !heal [<target>]";
			}

			@Override
			public void doAction(CommandCaller caller, CommandArguments args, CommandOutput resp) {
				String targetName = args.extractTarget(null, caller.getName());
				if (targetName == null) {
					resp.printErr(getUsage());
					return;
				}
				CommandTarget target = args.getTargetByName(targetName, caller);
				if (target == null) {
					resp.printErr("The character " + targetName + " does not exist.");
					resp.printErr(getUsage());
					return;
				}

				List<CommandTarget.CharacterManipulation> changes = new ArrayList<CommandTarget.CharacterManipulation>();
				changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.CANCEL_DEBUFFS, new PlayerStatusEffect[] {
						PlayerStatusEffect.CURSE, PlayerStatusEffect.DARKNESS,
						PlayerStatusEffect.POISON, PlayerStatusEffect.SEAL,
						PlayerStatusEffect.WEAKEN
				}));
				changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.SET_HP, Short.valueOf((short) 30000))); //receiver will limit this to their max HP
				changes.add(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.SET_MP, Short.valueOf((short) 30000))); //receiver will limit this to their max MP
				target.mutate(changes);
			}
		}, "Cures a player of all diseases and restores all of his/her HP and MP", UserPrivileges.GM));
		universalCommands.put("!tp", new CommandDefinition<CommandCaller>(new CommandAction<CommandCaller>() {
			@Override
			public String getUsage() {
				return "Usage: !tp <target> <destination>";
			}

			@Override
			public void doAction(CommandCaller caller, CommandArguments args, CommandOutput resp) {
				String warpeeName = args.extractTarget(null, null);
				if (warpeeName == null) {
					resp.printErr(getUsage());
					return;
				}
				CommandTarget warpee = args.getTargetByName(warpeeName, caller);
				if (warpee == null) {
					resp.printErr("The character " + warpeeName + " does not exist.");
					resp.printErr(getUsage());
					return;
				}

				String warpToName = args.extractTarget(null, null);
				if (warpToName == null) {
					resp.printErr(getUsage());
					return;
				}
				CommandTarget warpTo = args.getTargetByName(warpToName, caller);
				if (warpTo == null) {
					resp.printErr("The character " + warpToName + " does not exist.");
					resp.printErr(getUsage());
					return;
				}

				CommandTarget.MapValue destination = (CommandTarget.MapValue) warpTo.access(CommandTarget.CharacterProperty.MAP);
				byte destChannel = ((Byte) warpTo.access(CommandTarget.CharacterProperty.CHANNEL)).byteValue();
				warpee.mutate(Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.CHANGE_MAP, destination)));
				if (destChannel == 0) {
					resp.printOut(warpToName + " is currently offline. " + warpeeName + " has been warped to where he/she will spawn on next login.");
				} else {
					byte sourceChannel = ((Byte) warpee.access(CommandTarget.CharacterProperty.CHANNEL)).byteValue();
					if (sourceChannel == 0) {
						resp.printOut(warpeeName + " is currently offline He/she will spawn on next login where " + warpToName + " is now.");
					} else if (sourceChannel != destChannel) {
						warpee.mutate(Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.CHANGE_CHANNEL, Byte.valueOf(destChannel))));
						resp.printOut(warpeeName + " has been transferred to channel " + destChannel + ".");
					}
				}
			}
		}, "Teleport a player to another", UserPrivileges.GM));
		universalCommands.put("!town", new TownCommandHandler());
		universalCommands.put("!map", new CommandDefinition<CommandCaller>(new CommandAction<CommandCaller>() {
			@Override
			public String getUsage() {
				return "Usage: !map [-t <target>] <mapid> [<spawn point>]";
			}

			@Override
			public void doAction(CommandCaller caller, CommandArguments args, CommandOutput resp) {
				String targetName = args.extractOptionalTarget(caller.getName());
				if (targetName == null) {
					resp.printErr(getUsage());
					return;
				}
				CommandTarget target = args.getTargetByName(targetName, caller);
				if (target == null) {
					resp.printErr("The character " + targetName + " does not exist.");
					resp.printErr(getUsage());
					return;
				}

				if (!args.hasNext()) {
					resp.printErr(getUsage());
					return;
				}
				int mapId;
				try {
					mapId = Integer.parseInt(args.next());
				} catch (NumberFormatException ex) {
					resp.printErr("Invalid map id.");
					resp.printErr(getUsage());
					return;
				}
				GameMap map = GameServer.getChannel(caller.getChannel()).getMapFactory().getMap(mapId);
				if (map == null) {
					resp.printErr("Invalid map id.");
					resp.printErr(getUsage());
					return;
				}

				byte spawnPoint = 0;
				if (args.hasNext()) {
					String param = args.next();
					try {
						spawnPoint = Byte.parseByte(param);
						if (!map.getStaticData().getPortals().containsKey(Byte.valueOf(spawnPoint))) {
							resp.printErr("Invalid spawn point.");
							resp.printErr(getUsage());
							return;
						}
					} catch (NumberFormatException ex) {
						spawnPoint = map.getPortalIdByName(param);
						if (spawnPoint == -1) {
							resp.printErr("Invalid spawn point.");
							resp.printErr(getUsage());
							return;
						}
					}
				}

				target.mutate(Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.CHANGE_MAP, new CommandTarget.MapValue(mapId, spawnPoint))));
			}
		}, "Warp a player to a specific map and spawn point", UserPrivileges.GM));
		universalCommands.put("!ban", new CommandDefinition<CommandCaller>(new CommandAction<CommandCaller>() {
			@Override
			public String getUsage() {
				return "Usage: !ban <target> <reason> [<expire date>|perm]";
			}

			@Override
			public void doAction(CommandCaller caller, CommandArguments args, CommandOutput resp) {
				String targetName = args.extractTarget(null, null);
				if (targetName == null) {
					resp.printErr(getUsage());
					return;
				}
				CommandTarget target = args.getTargetByName(targetName, caller);
				if (target == null) {
					resp.printErr("The character " + targetName + " does not exist.");
					resp.printErr(getUsage());
					return;
				}

				if (!args.hasNext()) {
					resp.printErr(getUsage());
					return;
				}
				String reason = args.next();

				long expireTimestamp;
				if (!args.hasNext()) {
					resp.printErr(getUsage());
					return;
				}
				String param = args.next();
				try {
					int iDate = Integer.parseInt(param);
					Calendar expireCal = TimeTool.intDateToCalendar(iDate);
					if (expireCal == null) {
						resp.printErr("Expire date must be in the form of YYYYMMDD.");
						resp.printErr(getUsage());
						return;
					}

					expireCal.set(Calendar.HOUR_OF_DAY, 0);
					expireCal.set(Calendar.MINUTE, 0);
					expireCal.set(Calendar.SECOND, 0);
					expireCal.set(Calendar.MILLISECOND, 0);

					if (expireCal.before(Calendar.getInstance())) {
						resp.printErr("Expire date must not be in the past.");
						resp.printErr(getUsage());
						return;
					}
					expireTimestamp = expireCal.getTimeInMillis();
				} catch (NumberFormatException e) {
					if (!param.equalsIgnoreCase("perm")) {
						resp.printErr("Expire date must be in the form of YYYYMMDD.");
						resp.printErr(getUsage());
						return;
					}
					expireTimestamp = TimeTool.NO_EXPIRATION;
				}

				target.mutate(Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.BAN, new CommandTarget.BanValue(caller.getName(), reason, expireTimestamp))));
			}
		}, "Raise a player's infraction level past the tolerance to ban them", UserPrivileges.GM));
		universalCommands.put("!skill", new CommandDefinition<CommandCaller>(new CommandAction<CommandCaller>() {
			@Override
			public String getUsage() {
				return "Usage: !skill [-t <target>] <skillid> <level> [<master level>]";
			}

			@Override
			public void doAction(CommandCaller caller, CommandArguments args, CommandOutput resp) {
				int skillId;
				byte skillLevel, masterLevel = -1;
				SkillStats s;

				String targetName = args.extractOptionalTarget(caller.getName());
				if (targetName == null) {
					resp.printErr(getUsage());
					return;
				}
				CommandTarget target = args.getTargetByName(targetName, caller);
				if (target == null) {
					resp.printErr("The character " + targetName + " does not exist.");
					resp.printErr(getUsage());
					return;
				}

				if (!args.hasNext()) {
					resp.printErr(getUsage());
					return;
				}
				String param = args.next();
				try {
					skillId = Integer.parseInt(param);
					s = SkillDataLoader.getInstance().getSkill(skillId);
					if (s == null)
						throw new NumberFormatException();
				} catch (NumberFormatException e) {
					resp.printErr(param + " is not a valid skillid.");
					resp.printErr(getUsage());
					return;
				}

				if (!args.hasNext()) {
					resp.printErr(getUsage());
					return;
				}
				param = args.next();
				try {
					skillLevel = Byte.parseByte(param);
					if (skillLevel != 0 && s.getLevel(skillLevel) == null)
						throw new NumberFormatException();
				} catch (NumberFormatException e) {
					resp.printErr(param + " is not a valid level of skill " + skillId + ".");
					resp.printErr(getUsage());
					return;
				}

				if (args.hasNext()) {
					param = args.next();
					try {
						masterLevel = Byte.parseByte(param);
						if (masterLevel != 0 && s.getLevel(masterLevel) == null)
							throw new NumberFormatException();
					} catch (NumberFormatException e) {
						resp.printErr(param + " is not a valid master level of skill " + skillId + ".");
						resp.printErr(getUsage());
						return;
					}
				}

				target.mutate(Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.SET_SKILL_LEVEL, new CommandTarget.SkillValue(skillId, skillLevel, masterLevel))));
			}
		}, "Change a player's skill level", UserPrivileges.GM));
		universalCommands.put("!maxequips", new MaxStatCommandHandlers.MaxEquipStatsHandler());
		universalCommands.put("!maxskills", new MaxStatCommandHandlers.MaxSkillsHandler());
		universalCommands.put("!maxall", new MaxStatCommandHandlers.MaxAllHandler());
		universalCommands.put("!give", new CommandDefinition<CommandCaller>(new CommandAction<CommandCaller>() {
			@Override
			public String getUsage() {
				return "Usage: !give [-t <target>] <itemid> [<quantity>]";
			}

			@Override
			public void doAction(CommandCaller caller, CommandArguments args, CommandOutput resp) {
				int itemId;
				int quantity = 1;

				String targetName = args.extractOptionalTarget(caller.getName());
				if (targetName == null) {
					resp.printErr(getUsage());
					return;
				}
				CommandTarget target = args.getTargetByName(targetName, caller);
				if (target == null) {
					resp.printErr("The character " + targetName + " does not exist.");
					resp.printErr(getUsage());
					return;
				}

				if (!args.hasNext()) {
					resp.printErr(getUsage());
					return;
				}
				String param = args.next();
				try {
					itemId = Integer.parseInt(param);
					//TODO: check if item is valid. Throw NumberFormatException if not
				} catch (NumberFormatException e) {
					resp.printErr(param + " is not a valid itemid.");
					resp.printErr(getUsage());
					return;
				}

				if (args.hasNext()) {
					param = args.next();
					try {
						quantity = Integer.parseInt(param);
					} catch (NumberFormatException e) {
						resp.printErr(param + " is not a valid item quantity.");
						resp.printErr(getUsage());
						return;
					}
				}

				target.mutate(Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.ADD_ITEM, new CommandTarget.ItemValue(itemId, quantity))));
			}
		}, "Give a player an item", UserPrivileges.GM));
		inGameOnlyCommands.put("!spawn", new SpawnCommandHandler());
		inGameOnlyCommands.put("!playernpc", new CommandDefinition<GameCharacterCommandCaller>(new CommandAction<GameCharacterCommandCaller>() {
			@Override
			public String getUsage() {
				return "Usage: !playernpc [<scriptid>]";
			}

			@Override
			public void doAction(GameCharacterCommandCaller caller, CommandArguments args, CommandOutput resp) {
				int scriptId = 9901000;
				
				if (args.hasNext()) {
					String param = args.next();
					try {
						scriptId = Integer.parseInt(param);
						if (scriptId < 9901000 || scriptId > 9901319) {
							resp.printErr("Scriptid must be between 9901000 and 9901319.");
							return;
						}
					} catch (NumberFormatException e) {
						resp.printErr(param + " is not a valid scriptid.");
						resp.printErr(getUsage());
						return;
					}
				}

				PlayerNpc npc = new PlayerNpc(caller.getBackingCharacter(), scriptId);
				caller.getMap().spawnPlayerNpc(npc);
			}
		}, "Spawn a temporary player NPC of yourself.", UserPrivileges.GM));
		universalCommands.put("!cleardrops", new CommandDefinition<CommandCaller>(new CommandAction<CommandCaller>() {
			@Override
			public String getUsage() {
				return "Usage: !cleardrops";
			}

			private void clearDrop(ItemDrop drop, GameMap map) {
				drop.expire();
				map.destroyEntity(drop);
			}

			@Override
			public void doAction(CommandCaller caller, CommandArguments args, CommandOutput resp) {
				Collection<MapEntity> drops = caller.getMap().getAllEntities(EntityType.DROP);
				for (MapEntity ent : drops)
					clearDrop((ItemDrop) ent, caller.getMap());
			}
		}, "Expires all dropped items on the map.", UserPrivileges.GM));
		universalCommands.put("!clearmobs", new CommandDefinition<CommandCaller>(new CommandAction<CommandCaller>() {
			@Override
			public String getUsage() {
				return "Usage: !clearmobs [-k]";
			}

			@Override
			public void doAction(CommandCaller caller, CommandArguments args, CommandOutput resp) {
				GameCharacter killer;
				if (args.hasOpt("-k")) {
					if (caller.isInGame()) {
						killer = ((GameCharacterCommandCaller) caller).getBackingCharacter();
					} else {
						resp.printErr("The -k flag cannot be used over telnet.");
						resp.printErr(getUsage());
						return;
					}
				} else {
					killer = null;
				}

				for (MapEntity ent : caller.getMap().getAllEntities(EntityType.MONSTER)) {
					Mob mob = (Mob) ent;
					mob.hurt(killer, mob.getHp());
					caller.getMap().killMonster(mob, killer);
				}
			}
		}, "Removes all monsters on the map, either killing them for drops and an exp reward (specify with -k) or simply just wipe them out.", UserPrivileges.GM));
		universalCommands.put("!info", new CommandDefinition<CommandCaller>(new CommandAction<CommandCaller>() {
			@Override
			public String getUsage() {
				return "Usage: !info [<target>]";
			}

			@Override
			public void doAction(CommandCaller caller, CommandArguments args, CommandOutput resp) {
				String targetName = args.extractTarget(null, caller.getName());
				if (targetName == null) {
					resp.printErr(getUsage());
					return;
				}
				CommandTarget target = args.getTargetByName(targetName, caller);
				if (target == null) {
					resp.printErr("The character " + targetName + " does not exist.");
					resp.printErr(getUsage());
					return;
				}
				Point pos = (Point) target.access(CommandTarget.CharacterProperty.POSITION);
				byte channel = ((Byte) target.access(CommandTarget.CharacterProperty.CHANNEL)).byteValue();
				StringBuilder sb = new StringBuilder();
				sb.append("PlayerId=").append(target.access(CommandTarget.CharacterProperty.PLAYER_ID));
				if (channel != 0)
					sb.append("; Channel=").append(channel);
				else
					sb.append("; Offline");
				sb.append("; Map=").append(((CommandTarget.MapValue) target.access(CommandTarget.CharacterProperty.MAP)).mapId);
				sb.append("; Position(").append(pos.x).append(",").append(pos.y).append(")");
				resp.printOut(sb.toString());
			}
		}, "Show location info of a player", UserPrivileges.GM));
		universalCommands.put("!rate", new CommandDefinition<CommandCaller>(new CommandAction<CommandCaller>() {
			@Override
			public String getUsage() {
				return "Usage: !rate exp|meso|drop <new rate>";
			}

			@Override
			public void doAction(CommandCaller caller, CommandArguments args, CommandOutput resp) {
				short rate;

				if (!args.hasNext()) {
					resp.printErr(getUsage());
					return;
				}
				String key = args.next();

				if (!args.hasNext()) {
					resp.printErr(getUsage());
					return;
				}
				String value = args.next();
				try {
					rate = (short) Math.min(Integer.parseInt(value), Short.MAX_VALUE);
				} catch (NumberFormatException ex) {
					resp.printErr(getUsage());
					return;
				}

				if (key.equalsIgnoreCase("exp")) {
					GameServer.getVariables().setExpRate(rate);
					resp.printOut("The exp rate of this game server has been set to "
							+ rate + ". Changes will be reverted on the next server restart.");
				} else if (key.equalsIgnoreCase("meso")) {
					GameServer.getVariables().setMesoRate(rate);
					resp.printOut("The meso rate of this game server has been set to "
							+ rate + ". Changes will be reverted on the next server restart.");
				} else if (key.equalsIgnoreCase("drop")) {
					GameServer.getVariables().setDropRate(rate);
					resp.printOut("The drop rate of this game server has been set to "
							+ rate + ". Changes will be reverted on the next server restart.");
				} else {
					resp.printErr(getUsage());
				}
			}
		}, "Change the exp, meso, or drop rate of this game server",
				UserPrivileges.SUPER_GM));
		byTelnetOnlyCommands.put("!cc", new CommandDefinition<RemoteAdminCommandCaller>(new CommandAction<RemoteAdminCommandCaller>() {
			@Override
			public String getUsage() {
				return "Usage: !cc <destination channel>";
			}

			@Override
			public void doAction(RemoteAdminCommandCaller caller, CommandArguments args, CommandOutput resp) {
				if (!args.hasNext()) {
					resp.printErr(getUsage());
					return;
				}
				byte dest;
				try {
					dest = Byte.parseByte(args.next());
				} catch (NumberFormatException e) {
					resp.printErr(getUsage());
					return;
				}

				caller.changeChannel(dest);
			}
		}, "Change the channel that commands apply to",
				UserPrivileges.GM));
		universalCommands.put("!who", new CommandDefinition<CommandCaller>(new CommandAction<CommandCaller>() {
			@Override
			public String getUsage() {
				return "Usage: !who [<minimum privilege level>]";
			}

			@Override
			public void doAction(CommandCaller caller, CommandArguments args, CommandOutput resp) {
				byte privilegeLevelLimit = UserPrivileges.USER;
				if (args.hasNext()) {
					String param = args.next();
					try {
						privilegeLevelLimit = Byte.parseByte(param);
					} catch (NumberFormatException e) {
						resp.printErr(getUsage());
						return;
					}
				}

				StringBuilder sb = new StringBuilder();
				for (GameCharacter c : GameServer.getChannel(caller.getChannel()).getConnectedPlayers())
					if (c.getPrivilegeLevel() >= privilegeLevelLimit)
						sb.append(c.getName()).append(",");
				if (sb.length() > 0) //remove terminal delimiter
					sb.delete(sb.length() - 1, sb.length());
				resp.printOut("Connected users: " + sb);
			}
		}, "List all online users in this channel (and optionally filter them by privilege level)",
				UserPrivileges.GM));
		universalCommands.put("!uptime", new CommandDefinition<CommandCaller>(new CommandAction<CommandCaller>() {
			@Override
			public String getUsage() {
				return "Usage: !uptime [-gc]";
			}

			@Override
			public void doAction(CommandCaller caller, CommandArguments args, CommandOutput resp) {
				if (args.hasOpt("-gc"))
					System.gc();

				long startMillis = GameServer.getChannel(caller.getChannel()).getTimeStarted();
				long upTimeMillis = System.currentTimeMillis() - startMillis;
				DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.LONG);
				resp.printOut("This game server was started on " + fmt.format(startMillis) + ".");

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
		}, "Print general info about the server's resource usage. Pass -gc flag to attempt to run the garbage collector before collecting heap info", UserPrivileges.ADMIN));
		universalCommands.put("!shutdown", new ShutdownCommandHandler());
		universalCommands.put("!help", new CommandDefinition<CommandCaller>(new HelpCommandHandler(),
				"List available commands and their descriptions. Specify a command to read only its description", UserPrivileges.USER));
	}

	public void process(GameCharacter p, String line) {
		String[] args = argSplit.split(line);
		AbstractCommandDefinition<CommandCaller> def1 = universalCommands.get(args[0].toLowerCase());
		CommandOutput resp = new CommandOutput.InChat(p.getClient());
		if (def1 != null && p.getPrivilegeLevel() >= def1.minPrivilegeLevel()) {
			CommandArguments argsContainer = new CommandArguments(args);
			if (!argsContainer.hasOpt("--help")) {
				def1.execute(new GameCharacterCommandCaller(p), argsContainer, resp);
			} else {
				resp.printOut(def1.getUsage());
				resp.printOut(def1.getHelpMessage());
			}
		} else {
			AbstractCommandDefinition<GameCharacterCommandCaller> def2 = inGameOnlyCommands.get(args[0].toLowerCase());
			if (def2 != null && p.getPrivilegeLevel() >= def2.minPrivilegeLevel()) {
				CommandArguments argsContainer = new CommandArguments(args);
				if (!argsContainer.hasOpt("--help")) {
					def2.execute(new GameCharacterCommandCaller(p), argsContainer, resp);
				} else {
					resp.printOut(def2.getUsage());
					resp.printOut(def2.getHelpMessage());
				}
			} else {
				resp.printErr(args[0] + " is not a valid command. Type !help to get a list of valid commands.");
			}
		}
	}

	public static CommandProcessor getInstance() {
		return singleton;
	}

	private class HelpCommandHandler implements CommandAction<CommandCaller> {
		@Override
		public String getUsage() {
			return "Usage: !help [<command>]";
		}

		@Override
		public void doAction(CommandCaller caller, CommandArguments args, CommandOutput resp) {
			if (!args.hasNext()) {
				for (Entry<String, AbstractCommandDefinition<CommandCaller>> entry : universalCommands.entrySet())
					if (caller.getPrivilegeLevel() >= entry.getValue().minPrivilegeLevel())
						resp.printOut(entry.getKey() + " - " + entry.getValue().getHelpMessage());
				if (caller.isInGame()) {
					for (Entry<String, AbstractCommandDefinition<GameCharacterCommandCaller>> entry : inGameOnlyCommands.entrySet())
						if (caller.getPrivilegeLevel() >= entry.getValue().minPrivilegeLevel())
							resp.printOut(entry.getKey() + " - " + entry.getValue().getHelpMessage());
				} else {
					for (Entry<String, AbstractCommandDefinition<RemoteAdminCommandCaller>> entry : byTelnetOnlyCommands.entrySet())
						if (caller.getPrivilegeLevel() >= entry.getValue().minPrivilegeLevel())
							resp.printOut(entry.getKey() + " - " + entry.getValue().getHelpMessage());
				}
			} else {
				String param = args.next();
				AbstractCommandDefinition def = universalCommands.get(param.toLowerCase());
				if (def == null || caller.getPrivilegeLevel() < def.minPrivilegeLevel())
					if (caller.isInGame())
						def = inGameOnlyCommands.get(param.toLowerCase());
					else
						def = byTelnetOnlyCommands.get(param.toLowerCase());
				if (def == null || caller.getPrivilegeLevel() < def.minPrivilegeLevel()) {
					resp.printErr(param + " is not a valid command.");
					resp.printErr(getUsage());
					return;
				}
				resp.printOut(param + " - " + def.getHelpMessage());
			}
		}
	}
}
