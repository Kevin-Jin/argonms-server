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
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
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
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;

/**
 * TODO: This stuff doesn't handle special Telnet characters (any byte that has
 * a value greater than 127 (0x7F)! The processing of these special characters
 * is important, especially for initial negotiation and handshake.
 * Also make sure that we don't echo back the user's password when they're
 * typing it (or at least replace them with stars)!
 * @author GoldenKevin
 */
public class TelnetListener {
	private static final Logger LOG = Logger.getLogger(TelnetListener.class.getName());
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

	public TelnetListener(int port, ServerBootstrap b) {
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
			pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
			pipeline.addLast("decoder", new StringDecoder());
			pipeline.addLast("encoder", new StringEncoder());
			pipeline.addLast("handler", new TelnetServerHandler());
			return pipeline;
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
