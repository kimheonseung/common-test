package com.devh.test.tcp;

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

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import com.devh.test.config.Config.Tcp;
import com.devh.test.util.TestUtils;

import lombok.extern.slf4j.Slf4j;

/*
 * <pre>
 * Description : 
 *     MINA String Codec을 이용한 TCPServer
 *     주어진 정보에 따라 소켓을 Open
 *     연결된 세션 리스트를 10초에 1번씩 출력
 * ===============================
 * Memberfields :
 *     
 * ===============================
 * 
 * Author : HeonSeung Kim
 * Date   : 2022. 1. 3.
 * </pre>
 */
@Slf4j
public class TcpServer extends IoHandlerAdapter implements ITcpInitializer {
	
	private String mServerIp;
	private int mServerPort;
	private int mIntervalSessionList;
	private Set<String> mAcceptIpSet;
	
	private List<IoSession> mSessionList = new ArrayList<IoSession>();
	
	private static TcpServer instance;
	public static TcpServer getInstance() {
		if(instance == null)
			instance = new TcpServer();
		return instance;
	}
	
	@Override
	public void start(Tcp tcp) {
		final Tcp.Server server   = tcp.getServer();
		this.mServerIp            = server.getIp();
		this.mServerPort          = server.getPort();
		this.mIntervalSessionList = server.getInterval().getSessionList();
		this.mAcceptIpSet         = server.getAcceptIpSet();
		
		new Thread(new TcpServerAcceptor()).start();
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
			
			log.info(String.format("From. [%s]: %s", session.getRemoteAddress().toString().replace("/", ""), message));
			
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

	private class TcpServerAcceptor implements Runnable {

		@Override
		public void run() {
			final NioSocketAcceptor socketAcceptor = new NioSocketAcceptor();
			final DefaultIoFilterChainBuilder chain = socketAcceptor.getFilterChain();
			chain.addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"))));
			chain.addLast("logger", TestUtils.getLoggingFilter());
			
			socketAcceptor.setHandler(TcpServer.getInstance());
			
			final InetSocketAddress serverAddress = new InetSocketAddress(mServerIp, mServerPort);
			
			while(!Thread.currentThread().isInterrupted()) {
				try {
					socketAcceptor.bind(serverAddress);
					log.info(String.format("Success to open [ %s ]", serverAddress));
					break;
				} catch (IOException e) {
					log.debug(TestUtils.stackTraceToString(e));
					log.warn(String.format("Failed to bind address [ %s ] - %s", serverAddress, e.getMessage()));
					try { Thread.sleep(3000L); } catch (InterruptedException ignored) { }
				}
			}
			
			new Timer().schedule(new TcpHealthTimer(), Calendar.getInstance().getTime(), mIntervalSessionList * 1000L);
			
		}
		
		private class TcpHealthTimer extends TimerTask {

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
				} catch (Exception ignored) {ignored.printStackTrace();}
			}
		}
	}
}
