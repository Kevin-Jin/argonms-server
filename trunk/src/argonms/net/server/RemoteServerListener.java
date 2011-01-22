package argonms.net.server;

import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
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

/**
 * 
 * @author GoldenKevin
 */
public class RemoteServerListener {
	private static final Logger LOG = Logger.getLogger(RemoteServerListener.class.getName());
	private String interServerPassword;
	private ServerBootstrap bootstrap;

	public RemoteServerListener(String password, boolean useNio) {
		this.interServerPassword = password;
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
		this.bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = Channels.pipeline();
				pipeline.addLast("decoder", new InterServerPacketDecoder());
				pipeline.addLast("encoder", new InterServerPacketEncoder());
				pipeline.addLast("handler", new CenterServerHandler());
				return pipeline;
			}
		});
		bootstrap.setOption("child.bufferFactory", new HeapChannelBufferFactory(ByteOrder.LITTLE_ENDIAN));
	}

	public RemoteServerListener(String password, ServerBootstrap b) {
		this.interServerPassword = password;
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

	public static final ChannelLocal<CenterRemoteInterface> sessions = new ChannelLocal<CenterRemoteInterface>();

	private class CenterServerHandler extends SimpleChannelUpstreamHandler {
		public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			LOG.log(Level.FINEST, "Accepting remote server from {0}", e.getChannel().getRemoteAddress());
		}

		public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			LOG.log(Level.FINE, "Remote server connected from {0}", e.getChannel().getRemoteAddress());
			sessions.set(e.getChannel(), new CenterRemoteInterface(e.getChannel(), interServerPassword));
		}

		public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			LOG.log(Level.FINE, "Remote server from {0} disconnected", e.getChannel().getRemoteAddress());
			sessions.get(e.getChannel()).disconnect();
		}

		public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			LOG.log(Level.FINEST, "Removing remote server from {0}", e.getChannel().getRemoteAddress());
			sessions.remove(e.getChannel());
		}

		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
			sessions.get(e.getChannel()).process((byte[]) e.getMessage());
		}

		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
			LOG.log(Level.FINE, "Exception raised with remote server " + e.getChannel().getRemoteAddress(), e.getCause());
		}
	}
}
