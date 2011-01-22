package argonms.loading.map;

/**
 *
 * @author GoldenKevin
 */
public class Area {
	private int x1;
	private int y1;
	private int x2;
	private int y2;
	
	public void setX1(int x1) {
		this.x1 = x1;
	}
	
	public void setY1(int y1) {
		this.y1 = y1;
	}
	
	public void setX2(int x2) {
		this.x2 = x2;
	}
	
	public void setY2(int y2) {
		this.y2 = y2;
	}
	
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append('(').append(x1).append(", ").append(y1).append("), (").append(x2).append(", ").append(y2).append(")");
		return ret.toString();
	}
}
