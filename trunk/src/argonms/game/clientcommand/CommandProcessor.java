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

package argonms.game.clientcommand;

import argonms.UserPrivileges;
import argonms.character.Player;
import argonms.character.inventory.Inventory;
import argonms.character.inventory.Inventory.InventoryType;
import argonms.character.inventory.InventorySlot;
import argonms.character.inventory.InventoryTools;
import argonms.game.GameServer;
import argonms.game.clientcommand.CommandDefinition.CommandAction;
import argonms.loading.skill.SkillDataLoader;
import argonms.loading.skill.SkillStats;
import argonms.net.client.CommonPackets;
import argonms.tools.Pair;
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
		definitions.put("!stat", new StatCommandHandler());
		definitions.put("!skill", new CommandDefinition(new CommandAction() {
			private String getUsage() {
				return "Syntax: !skill [skillid] [level] <master level>";
			}

			public void doAction(Player p, String[] args, ClientNoticeStream resp) {
				if (args.length < 3) {
					resp.printErr("Invalid usage. " + getUsage());
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
					resp.printErr(args[1] + " is not a valid skillid. " + getUsage());
					return;
				}
				try {
					skillLevel = Byte.parseByte(args[2]);
					if (s.getLevel(skillLevel) == null)
						throw new NumberFormatException();
				} catch (NumberFormatException e) {
					resp.printErr(args[2] + " is not a valid level of skill " + skillId + ". " + getUsage());
					return;
				}
				if (args.length > 3) {
					try {
						masterLevel = Byte.parseByte(args[3]);
						if (s.getLevel(masterLevel) == null)
							throw new NumberFormatException();
					} catch (NumberFormatException e) {
						resp.printErr(args[3] + " is not a valid master level of skill " + skillId + ". " + getUsage());
						return;
					}
				}
				p.setSkillLevel(skillId, skillLevel, masterLevel);
			}
		}, "", UserPrivileges.GM));
		definitions.put("!give", new CommandDefinition(new CommandAction() {
			private String getUsage() {
				return "Syntax: !give [itemid] <quantity>";
			}

			public void doAction(Player p, String[] args, ClientNoticeStream resp) {
				if (args.length < 2) {
					resp.printErr("Invalid usage. " + getUsage());
					return;
				}
				int itemId;
				short quantity = 1;
				try {
					itemId = Integer.parseInt(args[1]);
				} catch (NumberFormatException e) {
					resp.printErr(args[1] + " is not a valid itemid. " + getUsage());
					return;
				}
				if (args.length > 2) {
					try {
						quantity = Short.parseShort(args[2]);
					} catch (NumberFormatException e) {
						resp.printErr(args[2] + " is not a valid item quantity. " + getUsage());
						return;
					}
				}
				//TODO: check if item is valid before adding to inventory.
				InventoryType type = InventoryTools.getCategory(itemId);
				Inventory inv = p.getInventory(type);
				Pair<List<Short>, List<Short>> changedSlots = InventoryTools.addToInventory(inv, itemId, quantity);
				short pos;
				InventorySlot slot;
				for (Short s : changedSlots.getLeft()) { //modified
					pos = s.shortValue();
					slot = inv.get(pos);
					p.getClient().getSession().send(CommonPackets.writeInventorySlotUpdate(type, pos, slot, false, false));
				}
				for (Short s : changedSlots.getRight()) { //added
					pos = s.shortValue();
					slot = inv.get(pos);
					p.getClient().getSession().send(CommonPackets.writeInventorySlotUpdate(type, pos, slot, false, true));
				}
			}
		}, "", UserPrivileges.GM));
		definitions.put("!rate", new CommandDefinition(new CommandAction() {
			private String getUsage() {
				return "Syntax: !rate [exp/meso/drop] [new rate]";
			}

			public void doAction(Player p, String[] args, ClientNoticeStream resp) {
				if (args.length < 3 || !AbstractCommandDefinition.isNumber(args[2])) {
					resp.printErr("Invalid usage. " + getUsage());
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
					resp.printErr("Invalid usage. " + getUsage());
					return;
				}
			}
		}, "Change the exp, meso, or drop rate of this game server.",
				UserPrivileges.SUPER_GM));
		definitions.put("!help", new CommandDefinition(new HelpCommandHandler(),
				"Displays this message", UserPrivileges.USER));
	}

	public void process(Player p, String line) {
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
		public void doAction(Player p, String[] args, ClientNoticeStream resp) {
			for (Entry<String, AbstractCommandDefinition> entry : definitions.entrySet())
				if (p.getPrivilegeLevel() >= entry.getValue().minPrivilegeLevel())
					resp.printOut(entry.getKey() + " - " + entry.getValue().getHelpMessage());
		}
	}
}
