package argonms.loading.map;

/**
 *
 * @author GoldenKevin
 */
public class Life {
	private char type;
	private int id;
	private int x;
	private int y;
	private int mobTime;
	private boolean f;
	private boolean hide;
	private int fh;
	private int cy;
	private int rx0;
	private int rx1;
	
	public void setType(char type) {
		this.type = type;
	}
	
	public void setDataId(int id) {
		this.id = id;
	}
	
	public void setX(int x) {
		this.x = x;
	}
	
	public void setY(int y) {
		this.y = y;
	}
	
	public void setMobTime(int time) {
		this.mobTime = time;
	}
	
	public void setF(boolean value) {
		this.f = value;
	}
	
	public void setHide(boolean value) {
		this.hide = value;
	}
	
	public void setFoothold(int fh) {
		this.fh = fh;
	}
	
	public void setCy(int cy) {
		this.cy = cy;
	}
	
	public void setRx0(int rx0) {
		this.rx0 = rx0;
	}
	
	public void setRx1(int rx1) {
		this.rx1 = rx1;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("type=").append(type);
		builder.append(", id=").append(id);
		builder.append(", loc=(").append(x).append(", ").append(y).append(')');
		builder.append(", mobtime=").append(mobTime);
		builder.append(", f=").append(f);
		builder.append(", hide=").append(hide);
		builder.append(", fh=").append(fh);
		builder.append(", cy=").append(cy);
		builder.append(", rx0=").append(rx0);
		builder.append(", rx1=").append(rx1);
		return builder.toString();
	}
}
