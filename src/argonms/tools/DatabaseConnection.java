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

package argonms.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

//TODO: use a connection pool instead of thread local? Depends on how many
//threads that require access to the database run at the same time...
/**
 * All servers maintain a Database Connection. This class therefore "singletonizes" the connection per process.
 *
 * Taken from an OdinMS-derived source with a few modifications.
 *
 * @author Frz
 * @version 1.1
 */
public class DatabaseConnection {
	private final static Logger LOG = Logger.getLogger(DatabaseConnection.class.getName());

	private static ThreadLocal<Connection> con;
	private static ThreadLocal<Connection> wzs;
	private static String driver, url, wz, user, password;

	public static Connection getConnection() {
		return con.get();
	}

	public static Connection getWzConnection() {
		return wzs.get();
	}

	public static void setProps(Properties props, boolean useMcdb) {
		driver = props.getProperty("driver");
		url = props.getProperty("url");
		user = props.getProperty("user");
		password = props.getProperty("password");
		con = new ThreadLocalConnection();
		if (useMcdb) {
			wz = props.getProperty("mcdb");
			wzs = new WzThreadLocalConnection();
		}
	}

	public static void closeAll() throws SQLException {
		for (Connection connection : ThreadLocalConnection.allConnections)
			connection.close();
		for (Connection connection : WzThreadLocalConnection.allConnections)
			connection.close();
	}

	private static class ThreadLocalConnection extends ThreadLocal<Connection> {
		public static Collection<Connection> allConnections = new LinkedList<Connection>();
		
		@Override
		protected Connection initialValue() {
			try {
				Class.forName(driver); // touch the mysql driver
			} catch (ClassNotFoundException e) {
				LOG.log(Level.SEVERE, "Could not find JDBC library. Do you have MySQL Connector/J?", e);
			}
			try {
				Connection con = DriverManager.getConnection(url, user, password);
				allConnections.add(con);
				LOG.log(Level.FINE, "New database connection created. {0} have been created.", allConnections.size());
				return con;
			} catch (SQLException e) {
				LOG.log(Level.SEVERE, "Could not connect to the database.", e);
				return null;
			}
		}
	}

	private static class WzThreadLocalConnection extends ThreadLocal<Connection> {
		public static Collection<Connection> allConnections = new LinkedList<Connection>();

		@Override
		protected Connection initialValue() {
			try {
				Class.forName(driver); // touch the mysql driver
			} catch (ClassNotFoundException e) {
				LOG.log(Level.SEVERE, "Could not find JDBC library. Do you have MySQL Connector/J?", e);
			}
			try {
				Connection con = DriverManager.getConnection(wz, user, password);
				allConnections.add(con);
				LOG.log(Level.FINE, "New database connection created. {0} have been created.", allConnections.size());
				return con;
			} catch (SQLException e) {
				LOG.log(Level.SEVERE, "Could not connect to the database.", e);
				return null;
			}
		}
	}
}
