package argonms.loading.mob;

/**
 *
 * @author GoldenKevin
 */
public class SelfDestruct {
	private int action;
	private int hp;
	private int removeAfter;
	
	public void setAction(int action) {
		this.action = action;
	}
	
	public void setHp(int points) {
		this.hp = points;
	}
	
	public void setRemoveAfter(int time) {
		this.removeAfter = time;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Action=").append(action);
		builder.append(", Hp=").append(hp);
		builder.append(", RemoveAfter=").append(removeAfter);
		return builder.toString();
	}
}
