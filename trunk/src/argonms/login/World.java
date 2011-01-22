package argonms.login;

/**
 *
 * @author GoldenKevin
 */
public class World {
	private static final String[] names = { "Scania", "Bera", "Broa", "Windia", "Khaini", "Bellocan", "Mardia", "Kradia", "Yellonde", "Demethos", "Elnido", "Kastia", "Judis", "Arkenia", "Plana" };

	private String name;
	private String host;
	private int[] channelPorts;
	private short[] loads;
	private byte flag;
	private String eventMessage;

	public World(String name, String host, int[] ports) {
		this.name = name;
		this.host = host;
		this.channelPorts = ports;
		this.loads = new short[channelPorts.length];
		this.eventMessage = "";
	}

	public World(byte worldId, String host, int[] ports) {
		this(names[worldId], host, ports);
	}

	public void incrementLoad(byte channel) {
		loads[channel]++;
	}

	public void decrementLoad(byte channel) {
		loads[channel]--;
	}

	public String getName() {
		return name;
	}

	public String getHost() {
		return host;
	}

	public int[] getPorts() {
		return channelPorts;
	}

	public byte getFlag() {
		return flag;
	}

	public String getMessage() {
		return eventMessage;
	}

	public short[] getLoads() {
		return loads;
	}
}
