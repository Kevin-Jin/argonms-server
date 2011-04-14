/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011  GoldenKevin
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

package argonms.net.internal;

import java.nio.ByteOrder;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

/**
 * Prepends the length of the packet in a 4-byte integer using little endian
 * byte order.
 * @author GoldenKevin
 */
public class InterServerPacketEncoder extends OneToOneEncoder {
	protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		byte[] content = (byte[]) msg;
		ChannelBuffer buf = ChannelBuffers.buffer(ByteOrder.LITTLE_ENDIAN, content.length + 4);
		buf.writeInt(content.length);
		buf.writeBytes(content);
		return buf;
	}
}
