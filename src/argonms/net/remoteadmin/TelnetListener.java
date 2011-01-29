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

package argonms.net.remoteadmin;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelLocal;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.oio.OioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.jboss.netty.handler.codec.string.StringDecoder;

/**
 * TODO: Don't echo backspace when user goes behind > or Login: Also don't
 * include backspace in ascii string.
 * @author GoldenKevin
 */
public class TelnetListener {
	private static final Logger LOG = Logger.getLogger(TelnetListener.class.getName());
	private static final Charset asciiEncoder = Charset.forName("US-ASCII");

	private ServerBootstrap bootstrap;

	public TelnetListener(boolean useNio) {
		ChannelFactory chFactory;
		if (useNio) {
			chFactory = new NioServerSocketChannelFactory(
				Executors.newSingleThreadExecutor(),
				Executors.newCachedThreadPool()
			);
		} else {
			chFactory = new OioServerSocketChannelFactory(
				Executors.newSingleThreadExecutor(),
				Executors.newCachedThreadPool()
			);
		}
		this.bootstrap = new ServerBootstrap(chFactory);
		this.bootstrap.setPipelineFactory(new TelnetServerPipelineFactory());
	}

	public TelnetListener(ServerBootstrap b) {
		this.bootstrap = b;
	}

	public void bind(int port) {
		try {
			bootstrap.bind(new InetSocketAddress(port));
			LOG.log(Level.INFO, "Listening on port {0}", port);
		} catch (ChannelException ex) {
			LOG.log(Level.SEVERE, "Could not bind on port " + port, ex);
		}
	}

	public static final ChannelLocal<TelnetSession> sessions = new ChannelLocal<TelnetSession>();

	private static final class TelnetServerPipelineFactory implements ChannelPipelineFactory {
		public ChannelPipeline getPipeline() throws Exception {
			ChannelPipeline pipeline = Channels.pipeline();
			pipeline.addLast("specialChars", new TelnetDecoder());
			pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
			pipeline.addLast("decoder", new StringDecoder());
			pipeline.addLast("encoder", new TelnetEncoder());
			pipeline.addLast("handler", new TelnetServerHandler());
			return pipeline;
		}
	}

	private static final class TelnetDecoder extends OneToOneDecoder {
		protected Object decode(ChannelHandlerContext ctx, Channel chn, Object o) throws Exception {
			boolean echo = sessions.get(chn).willEcho();
			ChannelBuffer buf = (ChannelBuffer) o;
			byte[] array = buf.array();
			byte[] ascii = new byte[array.length];
			int j = 0;
			for (int i = 0; i < array.length; i++)
				if (array[i] == TelnetSession.IAC)
					i += sessions.get(chn).protocolCmd(array, i + 1);
				else
					ascii[j++] = array[i];
			buf.skipBytes(array.length);
			ChannelBuffer text = ChannelBuffers.copiedBuffer(ascii, 0, j);
			if (echo)
				chn.write(text);
			return text;
		}
	}

	private static final class TelnetEncoder extends OneToOneEncoder {
		protected Object encode(ChannelHandlerContext chc, Channel chnl, Object o) throws Exception {
			if (o instanceof String)
				return ChannelBuffers.copiedBuffer(((String) o).getBytes(asciiEncoder));
			else if (o instanceof byte[])
				return ChannelBuffers.copiedBuffer((byte[]) o);
			return o;
		}
	}

	private static final class TelnetServerHandler extends SimpleChannelUpstreamHandler {
		public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			LOG.log(Level.FINEST, "Accepting telnet user from {0}", e.getChannel().getRemoteAddress());
		}

		public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			LOG.log(Level.FINE, "Telnet user connected from {0}", e.getChannel().getRemoteAddress());
			sessions.set(e.getChannel(), new TelnetSession(e.getChannel()));
		}

		public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			LOG.log(Level.FINE, "Telnet user from {0} disconnected", e.getChannel().getRemoteAddress());
		}

		public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			LOG.log(Level.FINEST, "Removing telnet user from {0}", e.getChannel().getRemoteAddress());
			sessions.remove(e.getChannel());
		}

		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
			String message = (String) e.getMessage();
			sessions.get(e.getChannel()).process(message);
		}

		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
			LOG.log(Level.FINE, "Exception raised with telnet user " + e.getChannel().getRemoteAddress(), e.getCause());
		}
	}
}
