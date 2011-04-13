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

package argonms.net.client;

import argonms.GlobalConstants;
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
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.oio.OioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelUpstreamHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.HashedWheelTimer;

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
		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = Channels.pipeline();
				pipeline.addLast("decoder", new MaplePacketDecoder());
				pipeline.addLast("encoder", new MaplePacketEncoder());
				pipeline.addLast("timeout", new IdleStateHandler(new HashedWheelTimer(), 0, 0, 30));
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
			if (sessions.get(channel) != null) {
				byte[] input = (byte[]) msg;
				MapleAESOFB sendCypher = sessions.get(channel).getSendCypher();
				int length = input.length;
				byte[] header = sendCypher.getPacketHeader(length);
				byte[] body = new byte[length];
				System.arraycopy(input, 0, body, 0, length);
				sendCypher.crypt(MapleAESOFB.encryptData(body));
				return ChannelBuffers.copiedBuffer(header, body);
			} else {
				return ChannelBuffers.copiedBuffer((byte[]) msg);
			}
		}
	}

	private class MapleServerHandler extends IdleStateAwareChannelUpstreamHandler {
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

		public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e) throws Exception {
			LOG.log(Level.FINER, "Client from {0} went idle -> pinging", e.getChannel().getRemoteAddress());
			sessions.get(e.getChannel()).getClient().startPingTask();
		}

		public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			LOG.log(Level.FINE, "Client from {0} disconnected", e.getChannel().getRemoteAddress());
			sessions.get(e.getChannel()).getClient().disconnected();
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

		lew.writeShort((short) 0x0D);
		lew.writeShort(GlobalConstants.MAPLE_VERSION);
		lew.writeShort((short) 0);
		lew.writeBytes(recvCypher.getIv());
		lew.writeBytes(sendCypher.getIv());
		lew.writeByte((byte) 8);

		return lew.getBytes();
	}
}
