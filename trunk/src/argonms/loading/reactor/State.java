package argonms.loading.reactor;

/**
 *
 * @author GoldenKevin
 */
public class State {
	private int type;
	private int nextState;
	
	//item event only
	private int itemid, quantity;
	private int ltx, lty;
	private int rbx, rby;
	
	public void setType(int type) {
		this.type = type;
	}
	
	public void setNextState(int state) {
		this.nextState = state;
	}
	
	public void setItem(int id, int quantity) {
		this.itemid = id;
		this.quantity = quantity;
	}
	
	public void setLt(int x, int y) {
		this.ltx = x;
		this.lty = y;
	}
	
	public void setRb(int x, int y) {
		this.rbx = x;
		this.rby = y;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		boolean itemEvent = (type == 100);
		builder.append("type=").append(type).append(" (itemEvent=").append(itemEvent).append(')');
		builder.append(", nextState=").append(nextState);
		if (itemEvent) {
			builder.append(", itemid=").append(itemid).append(" (Qty=").append(quantity).append(')');
			builder.append(", lt=(").append(ltx).append(", ").append(lty).append(')');
			builder.append(", rb=(").append(rbx).append(", ").append(rby).append(')');
		}
		return builder.toString();
	}
}
