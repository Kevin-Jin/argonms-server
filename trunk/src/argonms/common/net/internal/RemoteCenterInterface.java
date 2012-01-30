/*
/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2012  GoldenKevin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package argonms.common.net.internal;

import argonms.common.LocalServer;
import argonms.common.net.SessionDataModel;
import argonms.common.util.input.LittleEndianByteArrayReader;
import argonms.common.util.output.LittleEndianByteArrayWriter;

/**
 *
 * @author GoldenKevin
 */
public abstract class RemoteCenterInterface implements SessionDataModel {
	private RemoteCenterSession<?> session;
	private CenterRemotePacketProcessor pp;

	protected RemoteCenterInterface(CenterRemotePacketProcessor pp) {
		this.pp = pp;
	}

	/* package-private */ void setSession(RemoteCenterSession<?> s) {
		this.session = s;
	}

	public abstract LocalServer getLocalServer();

	/* package-private */ void process(byte[] message) {
		pp.process(new LittleEndianByteArrayReader(message), this);
	}

	protected byte[] auth(String pwd) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4 + pwd.length());

		lew.writeByte(RemoteCenterOps.AUTH);
		lew.writeByte(getServerId());
		lew.writeLengthPrefixedString(pwd);

		return lew.getBytes();
	}

	public abstract void serverReady();

	protected abstract byte getServerId();

	@Override
	public RemoteCenterSession<?> getSession() {
		return session;
	}

	/**
	 * DO NOT USE THIS METHOD TO FORCE CLOSE THE CONNECTION. USE
	 * getSession().close() INSTEAD.
	 */
	@Override
	public void disconnected() {
		getLocalServer().unregisterCenter();
	}
}
