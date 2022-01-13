package com.devh.test.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;

import com.devh.test.config.Config.Udp;
import com.devh.test.util.TestUtils;

import lombok.extern.slf4j.Slf4j;

/*
 * <pre>
 * Description : 
 *     Mina IoBuffer 메시지 기반 UDP Server
 *     주어진 정보에 따라 UDP Open
 *     주어진 인코딩으로 문자열 변환 실패 시 기본 UTF-8로 문자열 변환
 * ===============================
 * Memberfields :
 *     
 * ===============================
 * 
 * Author : HeonSeung Kim
 * Date   : 2022. 1. 5.
 * </pre>
 */
@Slf4j
public class UdpServer extends IoHandlerAdapter implements IUdpInitializer {
	
	private String mServerIp;
	private int mServerPort;
	private int mBufferSize;
	private String mEncoding;
	private int mIntervalSessionList;
	
	private Set<String> mAcceptIpSet;
	
	private List<IoSession> mSessionList = new ArrayList<IoSession>();
	
	private static UdpServer instance;
	public static UdpServer getInstance() {
		if(instance == null)
			instance = new UdpServer();
		return instance;
	}
	
	@Override
	public void start(Udp udp) {
		final Udp.Server server   = udp.getServer(); 
		this.mServerIp            = server.getIp();
		this.mServerPort          = server.getPort();
		this.mBufferSize          = server.getBufferSize();
		this.mEncoding            = udp.getEncoding();
		this.mAcceptIpSet         = server.getAcceptIpSet();
		this.mIntervalSessionList = server.getInterval().getSessionList();
		
		new Thread(new UdpServerAcceptor()).start();
	}
	
	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		log.error(String.format("\n[%s]: %s\n", session.getRemoteAddress().toString().replace("/", ""), cause.getMessage()));
		try {
			session.closeNow();
		} catch (Exception ignored) {}
		mSessionList.remove(session);
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		
		try {
			
			if(!TestUtils.isAcceptIp(mAcceptIpSet, session))
				return;
			
			if(message instanceof IoBuffer) {
				final IoBuffer buffer = (IoBuffer) message;
				String msg;
				
				try {
					msg = buffer.getString(Charset.forName(mEncoding).newDecoder());
				} catch (Exception e) {
					msg = buffer.getString(Charset.forName("UTF-8").newDecoder());
				}
				
				log.info(String.format("From. [%s]: %s", session.getRemoteAddress().toString().replace("/", ""), msg));
			}
			
		} catch (Exception e) {
			log.debug(TestUtils.stackTraceToString(e));
			log.warn(String.format("Exception while messageReceived. [%s]", e.getMessage()));
		}
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		log.info(String.format("session closed [%s]", session.getRemoteAddress().toString().replace("/", "")));
		mSessionList.remove(session);
	}

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		log.info(String.format("session created [%s]", session.getRemoteAddress().toString().replace("/", "")));
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		log.info(String.format("session opened [%s]", session.getRemoteAddress().toString().replace("/", "")));
		mSessionList.add(session);
	}
	
	private class UdpServerAcceptor implements Runnable {

		@Override
		public void run() {
			final NioDatagramAcceptor datagramAcceptor = new NioDatagramAcceptor();
			final DefaultIoFilterChainBuilder chain = datagramAcceptor.getFilterChain();
			chain.addLast("logger", TestUtils.getLoggingFilter());
			
			datagramAcceptor.setHandler(UdpServer.getInstance());
			datagramAcceptor.getSessionConfig().setReuseAddress(true);
			datagramAcceptor.getSessionConfig().setSendBufferSize(mBufferSize);
			datagramAcceptor.getSessionConfig().setReadBufferSize(mBufferSize);
			datagramAcceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10);
			
			final InetSocketAddress serverAddress = new InetSocketAddress(mServerIp, mServerPort);
			
			while(!Thread.currentThread().isInterrupted()) {
				try {
					datagramAcceptor.bind(serverAddress);
					log.info(String.format("Success to open [ %s ]", serverAddress));
					break;
				} catch (IOException e) {
					log.debug(TestUtils.stackTraceToString(e));
					log.warn(String.format("Failed to bind address [ %s ] - %s", serverAddress, e.getMessage()));
					try { Thread.sleep(3000L); } catch (InterruptedException ignored) { }
				}
			}
			
			new Timer().schedule(new UdpHealthTimer(), Calendar.getInstance().getTime(), mIntervalSessionList * 1000L);
			
		}
		
		private class UdpHealthTimer extends TimerTask {

			private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			
			@Override
			public void run() {
				try {
					StringBuffer sbHealthLog = new StringBuffer("\n[Sessions]");
					
					if(mSessionList.size() == 0) {
						sbHealthLog.append(System.lineSeparator()).append("No Sessions.");
					} else {
						for(IoSession session : mSessionList) {
							StringBuffer sessionInfoLog = new StringBuffer();
							sessionInfoLog
								.append(System.lineSeparator())
								.append(String.format("<%s>", sdf.format(session.getCreationTime()))).append(" ")
								.append(session.getRemoteAddress().toString().replace("/", ""))
								.append(" (").append(TestUtils.convertBytesToString(session.getReadBytes())).append(") ")
								.append(session.isActive() ? "active" : "not active").append(", ")
								.append(session.isConnected() ? "connected" : "not connected");
							
							sbHealthLog.append(sessionInfoLog.toString());
						}
					}
					log.info(sbHealthLog.toString() + System.lineSeparator());
				} catch (Exception ignored) {}
			}
			
		}
		
	}
}
