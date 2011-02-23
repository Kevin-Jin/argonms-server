package argonms.login;

import java.awt.Point;

public class Message {
	private Point pos;
	private String text;

	public Message(Point pos, String text) {
		this.pos = pos;
		this.text = text;
	}

	public Point getPosition() {
		return pos;
	}

	public String getText() {
		return text;
	}
}
