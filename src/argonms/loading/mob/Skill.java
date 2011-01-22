package argonms.loading.mob;

/**
 *
 * @author GoldenKevin
 */
public class Skill {
	private int skill;
	private int level;
	
	public void setSkill(int id) {
		this.skill = id;
	}
	
	public void setLevel(int level) {
		this.level = level;
	}
	
	public String toString() {
		return "Id=" + skill + ", Level=" + level;
	}
}
