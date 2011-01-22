package argonms.net.server;

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
