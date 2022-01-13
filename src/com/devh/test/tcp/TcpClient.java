package com.devh.test.tcp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import com.devh.test.config.Config.Tcp;
import com.devh.test.util.TestUtils;

import lombok.extern.slf4j.Slf4j;

/*
 * <pre>
 * Description : 
 *     MINA String Codec을 이용한 TCPClient
 *     주어진 정보에 따라 접속을 시도하고,
 *     접속에 성공하면 초당 1개의 샘플 메시지를 전송
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
public class TcpClient extends IoHandlerAdapter implements ITcpInitializer {
	
	private String mClientIp;
	private int mClientPort;
	private String mServerIp;
	private int mServerPort;
	private File mSampleFile;
	private int mIntervalSampleLog;
	
	private final List<String> mSampleLogLines = new ArrayList<String>();
	
	private static TcpClient instance;
	public static TcpClient getInstance() {
		if(instance == null)
			instance = new TcpClient();
		return instance;
	}
	
	@Override
	public void start(Tcp tcp) {
		final Tcp.Client client = tcp.getClient();
		final Tcp.Server server = tcp.getServer();
		this.mClientIp          = client.getIp();
		this.mClientPort        = client.getPort();
		this.mServerIp          = server.getIp();
		this.mServerPort        = server.getPort();
		this.mSampleFile        = new File(client.getSampleFile());
		this.mIntervalSampleLog = client.getInterval().getSampleLog();
		
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(mSampleFile)));
			
			String line = null;
			while ( (line = br.readLine()) != null ) {
				mSampleLogLines.add(line);
			}
			
			br.close();
		} catch (Exception e) {
			log.debug(TestUtils.stackTraceToString(e));
			log.warn(String.format("Exception while reading sample file. - %s", e.getMessage()));
		}
		new Thread(new TcpClientConnector()).start();
	}
	
	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		log.error(String.format("\n[%s]: %s\n", session.getRemoteAddress().toString().replace("/", ""), cause.getMessage()));
	}

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		log.info(String.format("To. [%s]: %s", session.getRemoteAddress().toString().replace("/", ""), message));
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		log.info(String.format("session closed [%s]", session.getRemoteAddress().toString().replace("/", "")));
	}

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		log.info(String.format("session created [%s]", session.getRemoteAddress().toString().replace("/", "")));
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		log.info(String.format("session opened [%s]", session.getRemoteAddress().toString().replace("/", "")));
	}
	
	private class TcpClientConnector implements Runnable {
		
		private IoSession mServerSession;

		@Override
		public void run() {
			
			final NioSocketConnector socketConnector = new NioSocketConnector();
			socketConnector.setConnectTimeoutMillis(60*1000L);
			
			final DefaultIoFilterChainBuilder chain = socketConnector.getFilterChain();
			chain.addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"))));
			chain.addLast("logger", TestUtils.getLoggingFilter());
			
			socketConnector.setHandler(TcpClient.getInstance());
			
			final InetSocketAddress clientAddress = new InetSocketAddress(mClientIp, mClientPort);
			final InetSocketAddress serverAddress = new InetSocketAddress(mServerIp, mServerPort);
			
			while(!Thread.currentThread().isInterrupted()) {
				log.warn(String.format("Try to connect [ %s ] -> [ %s ]", clientAddress, serverAddress));
				
				try {
					final ConnectFuture future = socketConnector.connect(serverAddress, clientAddress);
					future.awaitUninterruptibly();
					this.mServerSession = future.getSession();
					log.info(String.format("Success to connect [ %s ]", serverAddress));
					new Timer().schedule(new SampleMessageTimer(), Calendar.getInstance().getTime(), mIntervalSampleLog * 1000L);
					log.info("sample message timer start !");
					mServerSession.getCloseFuture().awaitUninterruptibly();
					try { Thread.sleep(3000L); } catch (InterruptedException ignored) { }
				} catch (Exception e) {
					log.debug(TestUtils.stackTraceToString(e));
					try { Thread.sleep(3000L); } catch (InterruptedException ignored) { }
				}
			}
			
		}
		
		private class SampleMessageTimer extends TimerTask {
			
			@Override
			public void run() {
				if(mServerSession != null && mServerSession.isActive()) {
					for(String line : mSampleLogLines) {
						mServerSession.write(line);
					}
				}
				
			}
		}
		
	}
	
}
