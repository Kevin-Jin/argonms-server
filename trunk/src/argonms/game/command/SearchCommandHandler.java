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
import argonms.common.loading.quest.QuestDataLoader;
import argonms.common.loading.string.StringDataLoader;
import argonms.game.character.GameCharacter;
import java.util.List;

/**
 *
 * @author GoldenKevin
 */
public class SearchCommandHandler extends AbstractCommandDefinition {
	public String getHelpMessage() {
		return "Look up the WZ data ID of an object based on its name.";
	}

	public byte minPrivilegeLevel() {
		return UserPrivileges.GM;
	}

	private String getUsage() {
		return "Syntax: !id [item/mob/map/skill/npc/quest] [name]";
	}

	private String restOfString(String[] splitted, int start) {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < splitted.length; i++)
			sb.append(splitted[i]).append(' ');
		return sb.substring(0, sb.length() - 1); //assume we have at least one arg
	}

	public void execute(GameCharacter p, String[] args, ClientNoticeStream resp) {
		if (args.length < 3) {
			resp.printErr("Invalid usage. " + getUsage());
			return;
		}
		String type = args[1];
		String query = restOfString(args, 2);

		List<String> matches;
		String typeName;
		if (type.equalsIgnoreCase("ITEM") || type.equalsIgnoreCase("ITEMS")) {
			matches = StringDataLoader.getInstance().getSimilarNamedItems(query);
			typeName = "items";
		} else if (type.equalsIgnoreCase("MOB") || type.equalsIgnoreCase("MOBS")
				|| type.equalsIgnoreCase("MONSTER") || type.equalsIgnoreCase("MONSTERS")) {
			matches = StringDataLoader.getInstance().getSimilarNamedMobs(query);
			typeName = "mobs";
		} else if (type.equalsIgnoreCase("MAP") || type.equalsIgnoreCase("MAPS")) {
			matches = StringDataLoader.getInstance().getSimilarNamedMaps(query);
			typeName = "maps";
		} else if (type.equalsIgnoreCase("SKILL") || type.equalsIgnoreCase("SKILLS")) {
			matches = StringDataLoader.getInstance().getSimilarNamedSkills(query);
			typeName = "skills";
		} else if (type.equalsIgnoreCase("NPC") || type.equalsIgnoreCase("NPCS")) {
			matches = StringDataLoader.getInstance().getSimilarNamedNpcs(query);
			typeName = "NPCs";
		} else if (type.equalsIgnoreCase("QUEST") || type.equalsIgnoreCase("QUESTS")) {
			matches = QuestDataLoader.getInstance().getSimilarNamedQuests(query);
			typeName = "quests";
		} else {
			resp.printErr(type + " is not a valid search type. " + getUsage());
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
