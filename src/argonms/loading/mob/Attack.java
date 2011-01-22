package argonms.loading.mob;

/**
 *
 * @author GoldenKevin
 */
public class Attack {
	private boolean deadlyAttack;
	private int mpBurn;
	private int diseaseSkill;
	private int diseaseLevel;
	private int conMp;
	
	public void setDeadlyAttack(boolean value) {
		this.deadlyAttack = value;
	}
	
	public void setMpBurn(int burn) {
		this.mpBurn = burn;
	}
	
	public void setDiseaseSkill(int skill) {
		this.diseaseSkill = skill;
	}
	
	public void setDiseaseLevel(int level) {
		this.diseaseLevel = level;
	}
	
	public void setMpConsume(int con) {
		this.conMp = con;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DeadlyAttack=").append(deadlyAttack);
		builder.append(", MpBurn=").append(mpBurn);
		builder.append(", Disease (Skill=").append(diseaseSkill);
		builder.append(", Level=").append(diseaseLevel).append(')');
		builder.append(", MpConsume=").append(conMp);
		return builder.toString();
	}
}
