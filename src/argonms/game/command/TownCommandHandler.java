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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public class TownCommandHandler extends AbstractCommandDefinition<CommandCaller> {
	private final Map<String, Integer> lookup;

	public TownCommandHandler() {
		lookup = new HashMap<String, Integer>();
		lookup.put("southperry", Integer.valueOf(60000));
		lookup.put("amherst", Integer.valueOf(1010000));
		lookup.put("henesys", Integer.valueOf(100000000));
		lookup.put("ellinia", Integer.valueOf(101000000));
		lookup.put("perion", Integer.valueOf(102000000));
		lookup.put("kerning city", Integer.valueOf(103000000));
		lookup.put("lith harbor", Integer.valueOf(104000000));
		lookup.put("sleepywood", Integer.valueOf(105040300));
		lookup.put("florina beach", Integer.valueOf(110000000));
		lookup.put("nautilus harbor", Integer.valueOf(120000000));
		lookup.put("gm map", Integer.valueOf(180000000));
		lookup.put("orbis", Integer.valueOf(200000000));
		lookup.put("el nath", Integer.valueOf(211000000));
		lookup.put("ludibrium", Integer.valueOf(220000000));
		lookup.put("omega sector", Integer.valueOf(221000000));
		lookup.put("korean folk town", Integer.valueOf(222000000));
		lookup.put("aquarium", Integer.valueOf(230000000));
		lookup.put("leafre", Integer.valueOf(240000000));
		lookup.put("mu lung", Integer.valueOf(250000000));
		lookup.put("herb town", Integer.valueOf(251000000));
		lookup.put("singapore", Integer.valueOf(540010000));
		lookup.put("new leaf city", Integer.valueOf(600000000));
		lookup.put("mushroom shrine", Integer.valueOf(800000000));
		lookup.put("showa town", Integer.valueOf(801000000));
	}

	@Override
	public String getHelpMessage() {
		return "Warp a player to a town or place of interest. Use \"!town list\" for a list of locations.";
	}

	@Override
	public String getUsage() {
		return "Usage: !town [-t <target>] <name>|list";
	}

	@Override
	public byte minPrivilegeLevel() {
		return UserPrivileges.GM;
	}

	private String getList() {
		StringBuilder sb = new StringBuilder();
		for (String name : lookup.keySet())
			sb.append(name).append(", ");
		return sb.substring(0, sb.length() - 2);
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
			resp.printErr(getUsage());
			return;
		}
		String name = args.restOfString();

		if (name.equalsIgnoreCase("list")) {
			resp.printOut("Valid locations: " + getList());
		} else {
			Integer mapId = lookup.get(name.toLowerCase());
			if (mapId != null)
				target.mutate(Collections.singletonList(new CommandTarget.CharacterManipulation(CommandTarget.CharacterManipulationKey.CHANGE_MAP, new CommandTarget.MapValue(mapId))));
			else
				resp.printErr(name + " is not a recognized location. Type \"!town list\" for a list of locations.");
		}
	}
}
