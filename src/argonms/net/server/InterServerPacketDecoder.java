package argonms.net.server;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

/**
 * Reads in a packet that is prefixed with its length in a 4-byte integer.
 * @author GoldenKevin
 */
public class InterServerPacketDecoder extends FrameDecoder {
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {
		if (buf.readableBytes() < 4)
			return null;

		buf.markReaderIndex();
		int length = buf.readInt();

		if (buf.readableBytes() < length) {
			buf.resetReaderIndex();
			return null;
		}

		byte[] message = new byte[length];
		buf.readBytes(message);
		return message;
	}
}