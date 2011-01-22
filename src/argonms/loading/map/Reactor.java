package argonms.loading.map;

/**
 *
 * @author GoldenKevin
 */
public class Reactor {
	private int id;
	private int x;
	private int y;
	private int reactorTime;
	private String name;
	
	public void setDataId(int id) {
		this.id = id;
	}
	
	public void setX(int x) {
		this.x = x;
	}
	
	public void setY(int y) {
		this.y = y;
	}
	
	public void setReactorTime(int time) {
		this.reactorTime = time;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("id=").append(id);
		if (!name.isEmpty()) builder.append(" (").append(name).append(')');
		builder.append(", loc=(").append(x).append(", ").append(y).append(')');
		builder.append(", time=").append(reactorTime);
		return builder.toString();
	}
}
