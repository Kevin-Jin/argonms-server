package argonms.loading.map;

/**
 *
 * @author GoldenKevin
 */
public class Portal {
	private String pn;
	private int pt;
	private int x;
	private int y;
	private int tm;
	private String tn;
	private String script;
	
	public void setPortalName(String pn) {
		this.pn = pn;
	}
	
	public void setPortalType(int pt) {
		this.pt = pt;
	}
	
	public void setX(int x) {
		this.x = x;
	}
	
	public void setY(int y) {
		this.y = y;
	}
	
	public void setTargetMapId(int tm) {
		this.tm = tm;
	}
	
	public void setTargetName(String tn) {
		this.tn = tn;
	}
	
	public void setScript(String script) {
		this.script = script;
	}
	
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append("Name=").append(pn).append(", Type=").append(pt).append(", (").append(x).append(", ").append(y).append("), target=").append(tm);
		if (!tn.isEmpty())
			ret.append(" (").append(tn).append(")");
		if (!script.isEmpty())
			ret.append(", Script=").append(script);
		return ret.toString();
	}
}