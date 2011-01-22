package argonms.loading.skill;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author GoldenKevin
 */
public class SkillStats {
	private int skillid;
	private Map<Byte, SkillEffect> levels;
	private String elemAttr;
	private boolean isBuff, isCharged;
	private int animationTime;

	public SkillStats() {
		levels = new HashMap<Byte, SkillEffect>();
	}

	public SkillEffect getLevel(byte level) {
		return levels.get(Byte.valueOf(level));
	}

	public void addLevel(byte level, SkillEffect effect) {
		levels.put(Byte.valueOf(level), effect);
	}

	public void setElemAttr(String attr) {
		elemAttr = attr;
	}

	public void setBuff() {
		isBuff = true;
	}

	public void setChargedSkill() {
		isCharged = true;
	}

	public void setDelay(int delay) {
		animationTime = delay;
	}
}
