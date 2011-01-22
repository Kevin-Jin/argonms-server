package argonms.login;

import argonms.LocalServer;
import argonms.ServerType;
import argonms.loading.DataFileType;
import argonms.loading.item.ItemDataLoader;
import argonms.net.client.ClientListener;
import argonms.tools.DatabaseConnection;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GoldenKevin
 */
public class LoginServer implements LocalServer {
	private static final Logger LOG = Logger.getLogger(LoginServer.class.getName());

	private static LoginServer instance;

	private ClientListener handler;
	private LoginCenterInterface lci;
	private String address;
	private int port;
	private boolean usePin;
	private Map<Byte, World> onlineWorlds;
	private boolean preloadAll;
	private DataFileType wzType;
	private String wzPath;
	private boolean useNio;
	private boolean centerConnected;

	private LoginServer() {
		onlineWorlds = new HashMap<Byte, World>();
	}

	public void init() {
		Properties prop = new Properties();
		try {
			FileReader fr = new FileReader(System.getProperty("argonms.db.config.file", "db.properties"));
			prop.load(fr);
			fr.close();
			DatabaseConnection.setProps(prop);
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, "Could not load database properties!", ex);
		}
		DatabaseConnection.getConnection();
		prop = new Properties();
		String centerIp;
		int centerPort;
		String authKey;
		try {
			FileReader fr = new FileReader(System.getProperty("argonms.login.config.file", "login.properties"));
			prop.load(fr);
			fr.close();
			address = prop.getProperty("argonms.login.host");
			port = Integer.parseInt(prop.getProperty("argonms.login.port"));
			usePin = Boolean.parseBoolean(prop.getProperty("argonms.login.pin"));
			wzType = DataFileType.valueOf(prop.getProperty("argonms.login.data.type"));
			//wzPath = prop.getProperty("argonms.login.data.dir");
			preloadAll = Boolean.parseBoolean(prop.getProperty("argonms.login.data.preload"));
			centerIp = prop.getProperty("argonms.login.center.ip");
			centerPort = Integer.parseInt(prop.getProperty("argonms.login.center.port"));
			authKey = prop.getProperty("argonms.login.auth.key");
			useNio = Boolean.parseBoolean(prop.getProperty("argonms.login.usenio"));
		} catch (IOException ex) {
			LOG.log(Level.SEVERE, "Could not load login server properties!", ex);
			return;
		}
		wzPath = System.getProperty("argonms.data.dir");
		lci = new LoginCenterInterface(authKey, this);
		lci.connect(centerIp, centerPort);
	}

	private void initializeData(boolean preloadAll, DataFileType wzType, String wzPath) {
		ItemDataLoader.setInstance(wzType, wzPath);
		if (preloadAll) {
			long start, end;
			start = System.nanoTime();
			System.out.print("Loading Item data...");
			ItemDataLoader.getInstance().loadAll();
			System.out.println("\tDone!");
			end = System.nanoTime();
			System.out.println("Preloaded all data in " + ((end - start) / 1000000.0) + "ms.");
		}
	}

	public void centerConnected() {
		LOG.log(Level.FINE, "Link with Center server established.");
		centerConnected = true;
		initializeData(preloadAll, wzType, wzPath);
		handler = new ClientListener(ServerType.LOGIN, (byte) -1, useNio);
		if (handler.bind(port)) {
			LOG.log(Level.INFO, "Login Server is online.");
			lci.serverReady();
		}
	}

	public void centerDisconnected() {
		if (centerConnected) {
			LOG.log(Level.SEVERE, "Center server disconnected.");
			centerConnected = false;
		}
	}

	public void gameConnected(byte world, String host, int[] ports) {
		LOG.log(Level.INFO, "{0} server accepted from {1}.", new Object[] { ServerType.getName(world), host });
		onlineWorlds.put(Byte.valueOf(world), new World(world, host, ports));
	}

	public void gameDisconnected(byte world) {
		LOG.log(Level.INFO, "{0} server disconnected.", ServerType.getName(world));
		onlineWorlds.remove(Byte.valueOf(world));
	}

	public void changePopulation(byte world, byte channel, boolean increase) {
		if (increase)
			onlineWorlds.get(Byte.valueOf(world)).incrementLoad(channel);
		else
			onlineWorlds.get(Byte.valueOf(world)).decrementLoad(channel);
	}

	public boolean usePin() {
		return usePin;
	}

	public Map<Byte, World> getWorlds() {
		return onlineWorlds;
	}

	public static LoginServer getInstance() {
		return instance;
	}

	public String getExternalIp() {
		return address;
	}

	public int[] getClientPorts() {
		return new int[] { port };
	}

	public static void main(String[] args) {
		instance = new LoginServer();
		instance.init();
	}
}
