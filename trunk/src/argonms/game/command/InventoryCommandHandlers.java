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
import argonms.common.character.inventory.Inventory;
import argonms.common.loading.item.ItemDataLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public class InventoryCommandHandlers implements CommandCollection<CommandCaller> {
	private static class ClearInventoryCommandHandler extends AbstractCommandDefinition<CommandCaller> {
		@Override
		public String getHelpMessage() {
			return "Clear a range of inventory slots. Leave out slot numbers to clear the entire selected inventory.";
		}

		@Override
		public String getUsage() {
			return "Usage: !clearinv [-t <target>] equip|use|setup|etc|cash [<slot>|(<start slot>-<end slot>)]";
		}

		@Override
		public byte minPrivilegeLevel() {
			return UserPrivileges.GM;
		}

		@Override
		public void execute(CommandCaller caller, CommandArguments args, CommandOutput resp) {
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
				resp.printErr("Please specify the inventory type you wish to clear slots of.");
				resp.printErr(getUsage());
				return;
			}
			Inventory.InventoryType type;
			try {
				type = Inventory.InventoryType.valueOf(args.next().toUpperCase());
			} catch (IllegalArgumentException e) {
				resp.printErr("Please specify the inventory type you wish to clear slots of.");
				resp.printErr(getUsage());
				return;
			}

			short startSlot = 1, endSlot = 0xFF;
			if (args.hasNext()) {
				String[] range = args.restOfString().replaceAll("\\s", "").split("-");
				switch (range.length) {
					case 1:
						try {
							endSlot = startSlot = Short.parseShort(range[0]);
						} catch (NumberFormatException e) {
							resp.printErr("Please specify a valid range of inventory slots.");
							resp.printErr(getUsage());
							return;
						}
						break;
					case 2:
						try {
							startSlot = Short.parseShort(range[0]);
							endSlot = Short.parseShort(range[1]);
						} catch (NumberFormatException e) {
							resp.printErr("Please specify a valid range of inventory slots.");
							resp.printErr(getUsage());
							return;
						}
						break;
					default:
						resp.printErr("Please specify a valid range of inventory slots.");
						resp.printErr(getUsage());
						return;
				}
			}
			target.mutate(Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.CLEAR_INVENTORY_SLOTS, new CommandTarget.InventorySlotRangeValue(type, startSlot, endSlot))));
		}
	}

	private static abstract class ChangeItemQuantityCommandHandler extends AbstractCommandDefinition<CommandCaller> {
		private final int defaultQuantity, factor;

		private ChangeItemQuantityCommandHandler(int defaultQuantity, int factor) {
			this.defaultQuantity = defaultQuantity;
			this.factor = factor;
		}

		@Override
		public byte minPrivilegeLevel() {
			return UserPrivileges.GM;
		}

		@Override
		public void execute(CommandCaller caller, CommandArguments args, CommandOutput resp) {
			int itemId;
			int quantity = defaultQuantity;

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
			if (!ItemDataLoader.getInstance().canLoad(itemId)) {
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
				if (quantity <= 0) {
					resp.printErr("Quantity must be positive.");
					resp.printErr(getUsage());
					return;
				}
			}

			target.mutate(Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.ADD_ITEM, new CommandTarget.ItemValue(itemId, quantity * factor))));
		}
	}

	@Override
	public Map<String, AbstractCommandDefinition<CommandCaller>> getDefinitions() {
		Map<String, AbstractCommandDefinition<CommandCaller>> definitions = new HashMap<String, AbstractCommandDefinition<CommandCaller>>();
		definitions.put("!clearinv", new ClearInventoryCommandHandler());
		definitions.put("!give", new ChangeItemQuantityCommandHandler(1, 1) {
			@Override
			public String getHelpMessage() {
				return "Give a player an item";
			}

			@Override
			public String getUsage() {
				return "Usage: !give [-t <target>] <itemid> [<quantity>]";
			}
		});
		definitions.put("!take", new ChangeItemQuantityCommandHandler(Integer.MIN_VALUE, -1) {
			@Override
			public String getHelpMessage() {
				return "Take all items that match the specify itemid from a player, or take up to a certain amount";
			}

			@Override
			public String getUsage() {
					return "Usage: !take [-t <target>] <itemid> [<quantity>]";
			}
		});
		return Collections.unmodifiableMap(definitions);
	}
}