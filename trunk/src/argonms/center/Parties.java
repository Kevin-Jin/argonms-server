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

package argonms.center;

import argonms.common.util.DatabaseManager;
import argonms.common.util.DatabaseManager.DatabaseType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class Parties {
	private static final Logger LOG = Logger.getLogger(Parties.class.getName());

	private static int getStartingPartyId(int world) {
		int partyId = -1;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			con = DatabaseManager.getConnection(DatabaseType.STATE);
			ps = con.prepareStatement("SELECT MAX(`partyid`) FROM `parties` WHERE `world` = ?");
			ps.setInt(1, world);
			rs = ps.executeQuery();
			partyId = rs.getInt(1);
		} catch (SQLException ex) {
			LOG.log(Level.WARNING, "Could not get starting party id for world " + world, ex);
		} finally {
			DatabaseManager.cleanup(DatabaseType.STATE, rs, ps, con);
		}
		return partyId;
	}

	private final AtomicInteger nextPartyId;
	private final Map<Integer, Party> parties;

	public Parties(int world) {
		nextPartyId = new AtomicInteger(getStartingPartyId(world));
		parties = new ConcurrentHashMap<Integer, Party>();
	}

	/**
	 * 
	 * @param creator
	 * @return the unique partyId of the newly created party
	 */
	public int makeNewParty(Party.Member creator) {
		//make sure we never return 0, because the client treats 0 specially
		int partyId = nextPartyId.incrementAndGet();
		parties.put(Integer.valueOf(partyId), new Party(creator));
		return partyId;
	}

	public Party remove(int partyId) {
		return parties.remove(Integer.valueOf(partyId));
	}

	public Party get(int partyId) {
		return parties.get(Integer.valueOf(partyId));
	}
}
