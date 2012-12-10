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
import java.util.Collections;

/**
 *
 * @author GoldenKevin
 */
public class ClearInventoryCommandHandler extends AbstractCommandDefinition<CommandCaller> {
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
