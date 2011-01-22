package argonms.loading;

/**
 *
 * @author GoldenKevin
 */
public abstract class StatEffects {
	private int duration;
	private short watk;
	private short wdef;
	private short matk;
	private short mdef;
	private short acc;
	private short avoid;
	private short hp;
	private short mp;
	private short speed;
	private short jump;
	private int morph;

	public void setDuration(int time) {
		this.duration = time;
	}

	public void setWatk(short pad) {
		this.watk = pad;
	}

	public void setWdef(short pdd) {
		this.wdef = pdd;
	}

	public void setMatk(short mad) {
		this.matk = mad;
	}

	public void setMdef(short mdd) {
		this.mdef = mdd;
	}

	public void setAcc(short acc) {
		this.acc = acc;
	}

	public void setAvoid(short eva) {
		this.avoid = eva;
	}

	public void setHp(short hp) {
		this.hp = hp;
	}

	public void setMp(short mp) {
		this.mp = mp;
	}

	public void setSpeed(short speed) {
		this.speed = speed;
	}

	public void setJump(short jump) {
		this.jump = jump;
	}

	public void setMorph(int id) {
		this.morph = id;
	}
}
