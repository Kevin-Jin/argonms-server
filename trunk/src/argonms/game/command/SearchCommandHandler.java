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

package argonms.game.command;

import argonms.common.UserPrivileges;
import argonms.common.loading.string.StringDataLoader;
import argonms.game.loading.quest.QuestDataLoader;
import java.util.List;

/**
 *
 * @author GoldenKevin
 */
public class SearchCommandHandler extends AbstractCommandDefinition<CommandCaller> {
	@Override
	public String getHelpMessage() {
		return "Look up the WZ data ID of an object based on its name.";
	}

	@Override
	public String getUsage() {
		return "Usage: !id item|mob|map|skill|npc|quest <name>";
	}

	@Override
	public byte minPrivilegeLevel() {
		return UserPrivileges.GM;
	}

	@Override
	public void execute(CommandCaller caller, CommandArguments args, CommandOutput resp) {
		if (!args.hasNext()) {
			resp.printErr(getUsage());
			return;
		}
		String type = args.next();

		if (!args.hasNext()) {
			resp.printErr(getUsage());
			return;
		}
		String query = args.restOfString();

		List<String> matches;
		String typeName;
		if (type.equalsIgnoreCase("item")) {
			matches = StringDataLoader.getInstance().getSimilarNamedItems(query);
			typeName = "items";
		} else if (type.equalsIgnoreCase("mob")) {
			matches = StringDataLoader.getInstance().getSimilarNamedMobs(query);
			typeName = "mobs";
		} else if (type.equalsIgnoreCase("map")) {
			matches = StringDataLoader.getInstance().getSimilarNamedMaps(query);
			typeName = "maps";
		} else if (type.equalsIgnoreCase("skill")) {
			matches = StringDataLoader.getInstance().getSimilarNamedSkills(query);
			typeName = "skills";
		} else if (type.equalsIgnoreCase("npc")) {
			matches = StringDataLoader.getInstance().getSimilarNamedNpcs(query);
			typeName = "NPCs";
		} else if (type.equalsIgnoreCase("quest")) {
			matches = QuestDataLoader.getInstance().getSimilarNamedQuests(query);
			typeName = "quests";
		} else {
			resp.printErr(type + " is not a valid search type.");
			resp.printErr(getUsage());
			return;
		}

		resp.printOut("<<Type: " + type + " | Search Term: " + query + ">>");
		if (matches.size() > 64) {
			resp.printErr("Too many results. Please narrow your search.");
		} else if (!matches.isEmpty()) {
			for (String match : matches)
				resp.printOut(match);
		} else {
			resp.printErr("No matching " + typeName + " found.");
		}
	}
}
