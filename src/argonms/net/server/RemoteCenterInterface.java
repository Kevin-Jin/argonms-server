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

package argonms.net.server;

import argonms.LocalServer;
import argonms.tools.input.LittleEndianByteArrayReader;
import argonms.tools.output.LittleEndianByteArrayWriter;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.HeapChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.oio.OioClientSocketChannelFactory;

/**
 *
 * @author GoldenKevin
 */
public abstract class RemoteCenterInterface {
	private static final Logger LOG = Logger.getLogger(RemoteCenterInterface.class.getName());

	private LocalServer local;
	private String interServerPwd;
	private Channel ch;
	private CenterRemotePacketProcessor pp;
	private ClientBootstrap bootstrap;

	public RemoteCenterInterface(LocalServer local, String password, CenterRemotePacketProcessor pp) {
		this.local = local;
		this.interServerPwd = password;
		this.pp = pp;

		bootstrap = new ClientBootstrap(
			new OioClientSocketChannelFactory(
				Executors.newSingleThreadExecutor()
			)
		);
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = Channels.pipeline();
				pipeline.addLast("decoder", new InterServerPacketDecoder());
				pipeline.addLast("encoder", new InterServerPacketEncoder());
				pipeline.addLast("handler", new InterServerHandler());
				return pipeline;
			}
		});
		bootstrap.setOption("bufferFactory", new HeapChannelBufferFactory(ByteOrder.LITTLE_ENDIAN));
	}

	/**
	 * This method will block until we lose connection with the center server,
	 * so be careful where you place it!
	 * @param ip the name of the host of the center server
	 * @param port the center server's listening port
	 */
	public void connect(String ip, int port) {
		ChannelFuture future = bootstrap.connect(new InetSocketAddress(ip, port));
		if (future.isSuccess())
			future.getChannel().getCloseFuture().awaitUninterruptibly();
		else
			LOG.log(Level.SEVERE, "Could not connect to center server at " + ip + ":" + port, future.getCause());
		bootstrap.releaseExternalResources();
	}

	private void init() {
		send(auth(getWorld(), interServerPwd));
	}

	public LocalServer getLocalServer() {
		return local;
	}

	private void process(byte[] message) {
		pp.process(new LittleEndianByteArrayReader(message), this);
	}

	public void serverReady() {
		send(serverReady(local.getExternalIp(), local.getClientPorts()));
	}

	//maybe we should encrypt because we do send a plaintext password after all
	public void send(byte[] b) {
		ch.write(b);
	}

	/**
	 * Notify the LocalServer that we are going to disconnect from the center
	 * server.
	 * DO NOT USE THIS METHOD TO FORCE CLOSE THE CONNECTION. USE close()
	 * INSTEAD.
	 */
	public void disconnect() {
		local.centerDisconnected();
	}

	public void close() {
		ch.disconnect();
	}

	protected abstract byte getWorld();

	private class InterServerHandler extends SimpleChannelUpstreamHandler {
		public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			LOG.log(Level.FINEST, "Accepting center server from {0}", e.getChannel().getRemoteAddress());
		}

		public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			LOG.log(Level.FINE, "Center server connected from {0}", e.getChannel().getRemoteAddress());
			ch = e.getChannel();
			init();
		}

		public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			LOG.log(Level.FINE, "Center server from {0} disconnected", e.getChannel().getRemoteAddress());
			disconnect();
		}

		public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
			LOG.log(Level.FINEST, "Removing center server from {0}", e.getChannel().getRemoteAddress());
		}

		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
			process((byte[]) e.getMessage());
		}

		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
			LOG.log(Level.FINE, "Exception raised with center server", e.getCause());
		}
	}

	private static byte[] auth(byte world, String pwd) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4 + pwd.length());

		lew.writeByte(RemoteCenterOps.AUTH);
		lew.writeByte(world);
		lew.writeLengthPrefixedString(pwd);

		return lew.getBytes();
	}

	private static byte[] serverReady(String ip, int[] ports) {
		LittleEndianByteArrayWriter lew = new LittleEndianByteArrayWriter(4 + ip.length() + 4 * ports.length);

		lew.writeByte(RemoteCenterOps.ONLINE);
		lew.writeLengthPrefixedString(ip);
		byte length = (byte) ports.length;
		lew.writeByte(length);
		for (int i = 0; i < length; i++)
			lew.writeInt(ports[i]);

		return lew.getBytes();
	}
}
