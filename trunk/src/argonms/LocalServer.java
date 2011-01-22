package argonms;

/**
 *
 * @author GoldenKevin
 */
public interface LocalServer {
	public void centerConnected();
	public void centerDisconnected();
	public String getExternalIp();
	public int[] getClientPorts();
}
