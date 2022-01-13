package com.devh.test.udp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import com.devh.test.config.Config.Udp;
import com.devh.test.util.TestUtils;

import lombok.extern.slf4j.Slf4j;

/*
 * <pre>
 * Description : 
 *     MINA UDP Client
 *     주어진 정보에 따라 초당 1개의 샘플 메시지를 전송
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
public class UdpClient extends IoHandlerAdapter implements IUdpInitializer{
	
	private String mServerIp;
	private int mServerPort;
	private String mClientIp;
	private int mClientPort;
	private String mEncoding;
	private File mSampleFile;
	private int mIntervalSampleLog;
	
	private final List<String> mSampleLogLines = new ArrayList<String>();
	
	private static UdpClient instance;
	public static UdpClient getInstance() {
		if(instance == null)
			instance = new UdpClient();
		return instance;
	}
	
	@Override
	public void start(Udp udp) {
		final Udp.Server server = udp.getServer();
		final Udp.Client client = udp.getClient();
		this.mServerIp          = server.getIp();
		this.mServerPort        = server.getPort();
		this.mClientIp          = client.getIp();
		this.mClientPort        = client.getPort();
		this.mEncoding          = udp.getEncoding();
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
		
		new Thread(new UdpClientConnector()).start();
	}
	
	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		log.error(String.format("exception \n\t[%s]: %s", session.getRemoteAddress().toString().replace("/", ""), cause.getMessage()));
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
	
	private class UdpClientConnector implements Runnable {

		private DatagramSocket mDatagramSocket;
		
		@Override
		public void run() {
			final InetSocketAddress serverAddress = new InetSocketAddress(mServerIp, mServerPort);
			final InetSocketAddress clientAddress = new InetSocketAddress(mClientIp, mClientPort);

			while(!Thread.currentThread().isInterrupted()) {
				try {
					this.mDatagramSocket = new DatagramSocket(clientAddress);
					while(true) {
						try {
							for(String line : mSampleLogLines) {
								final byte[] buffer = line.getBytes(Charset.forName(mEncoding));
								this.mDatagramSocket.send(
										new DatagramPacket(buffer, buffer.length, InetAddress.getByName(mServerIp), mServerPort)
								);
								log.info(String.format("To. [%s:%d]: %s", mServerIp, mServerPort, line));
							}
							Thread.sleep(mIntervalSampleLog * 1000L);
						} catch (Exception e) {
							log.debug(TestUtils.stackTraceToString(e));
							log.warn(String.format("Failed to send message. - %s", e.getMessage()));
						}
					}
					
				} catch (SocketException e) {
					log.debug(TestUtils.stackTraceToString(e));
					log.warn(String.format("Failed to connect [%s] - %s", serverAddress, e.getMessage()));
				}
			}
			
		}
		
	}
	
}
