package argonms.net.client;

import argonms.tools.input.LittleEndianByteArrayReader;
import argonms.tools.output.LittleEndianByteArrayWriter;
import java.net.InetSocketAddress;
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
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

/**
 *
 * @author GoldenKevin
 */
public class ClientListener {
	private static final Logger LOG = Logger.getLogger(ClientListener.class.getName());
	private ServerBootstrap bootstrap;
	private byte world, channel;
	private ClientPacketProcessor pp;

	public ClientListener(byte world, byte channel, boolean useNio) {
		this.world = world;
		this.channel = channel;
		this.pp = ClientPacketProcessor.getProcessor(world);
		ChannelFactory chFactory;
		if (useNio) {
			chFactory = new NioServerSocketChannelFactory(
				Executors.newSingleThreadExecutor(), //we don't need more than one boss thread
				Executors.newCachedThreadPool()
			);
		} else {
			chFactory = new OioServerSocketChannelFactory(
				Executors.newSingleThreadExecutor(), //we don't need more than one boss thread
				Executors.newFixedThreadPool(1200) //max players
			);
		}
		bootstrap = new ServerBootstrap(chFactory);
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = Channels.pipeline();
				pipeline.addLast("decoder", new MaplePacketDecoder());
				pipeline.addLast("encoder", new MaplePacketEncoder());
				pipeline.addLast("handler", new MapleServerHandler());
				return pipeline;
			}
		});
	}

	public boolean bind(int port) {
		try {
			bootstrap.bind(new InetSocketAddress(port));
			LOG.log(Level.INFO, "Listening on port {0}", port);
			return true;
		} catch (ChannelException ex) {
			LOG.log(Level.SEVERE, "Could not bind on port " + port, ex);
			return false;
		}
	}

	public static final ChannelLocal<ClientSession> sessions = new ChannelLocal<ClientSession>();

	private static final class MaplePacketDecoder extends FrameDecoder {
		protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buf) throws Exception {
			if (buf.readableBytes() < 4)
				return null;

			buf.markReaderIndex();
			byte[] message = new byte[4];
			MapleAESOFB recvCypher = sessions.get(channel).getRecvCypher();
			buf.readBytes(message);

			if (recvCypher.checkPacket(message)) {
				int length = MapleAESOFB.getPacketLength(message);

				if (buf.readableBytes() < length) {
					buf.resetReaderIndex();
					return null;
				}

				message = new byte[length];
				buf.readBytes(message);

				return MapleAESOFB.decryptData(recvCypher.crypt(message));
			} else {
				LOG.log(Level.FINE, "Client from {0} failed packet check -> Disconnecting.", channel.getRemoteAddress());
				sessions.get(channel).close();
				return null;
			}
		}
	}

	private static final class MaplePacketEncoder extends OneToOneEncoder {
		protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
			byte[] encrypted;
			if (sessions.get(channel) != null) {
				byte[] input = (byte[]) msg;
				byte[] unencrypted = new byte[input.length];
				System.arraycopy(input, 0, unencrypted, 0, input.length);

				encrypted = new byte[unencrypted.length + 4];

				MapleAESOFB sendCypher = sessions.get(channel).getSendCypher();
				byte[] header = sendCypher.getPacketHeader(unencrypted.length);
				MapleAESOFB.encryptData(unencrypted);
				sendCypher.crypt(unencrypted);

				System.arraycopy(header, 0, encrypted, 0, 4);
				System.arraycopy(unencrypted, 0, encrypted, 4, unencrypted.length);
			} else {
				encrypted = (byte[]) msg;
			}
			return ChannelBuffers.copiedBuffer(encrypted);
		}
	}

	private class MapleServerHandler extends SimpleChannelUpstreamHandler {
		public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			LOG.log(Level.FINEST, "Accepting client from {0}", e.getChannel().getRemoteAddress());
		}

		public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			LOG.log(Level.FINE, "Client connected from {0}", e.getChannel().getRemoteAddress());
			ClientSession session = new ClientSession(e.getChannel(), world, channel);
			session.getClient().setSession(session);
			e.getChannel().write(getHello(session.getRecvCypher(), session.getSendCypher()));
			sessions.set(e.getChannel(), session);
		}

		public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			LOG.log(Level.FINE, "Client from {0} disconnected", e.getChannel().getRemoteAddress());
			sessions.get(e.getChannel()).getClient().disconnect();
		}

		public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			LOG.log(Level.FINEST, "Removing client from {0}", e.getChannel().getRemoteAddress());
			sessions.remove(e.getChannel());
		}

		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
			byte[] message = (byte[]) e.getMessage();
			pp.process(new LittleEndianByteArrayReader(message), sessions.get(e.getChannel()).getClient());
		}

		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
			LOG.log(Level.FINE, "Exception raised with client " + e.getChannel().getRemoteAddress(), e.getCause());
		}
	}

	private static byte[] getHello(MapleAESOFB recvCypher, MapleAESOFB sendCypher) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(15);

		lew.writeShort((short) 0x0d);
		lew.writeShort((byte) 62);
		lew.writeShort((short) 0);
		lew.writeBytes(recvCypher.getIv());
		lew.writeBytes(sendCypher.getIv());
		lew.writeByte((byte) 8);

		return lew.getBytes();
	}
}
