package com.devh.test.config;

import java.util.Set;

import lombok.Data;

/*
 * <pre>
 * Description : 
 *     yml 설정파일 대응 객체
 * ===============================
 * Memberfields :
 *     
 * ===============================
 * 
 * Author : HeonSeung Kim
 * Date   : 2022. 1. 6.
 * </pre>
 */
@Data
public class Config {
	private Tcp tcp;
	private Udp udp;
	private Log log;
	
	@Data
	public static class Tcp {
		private Server server;
		private Client client;
		@Data
		public static class Server {
			private String ip;
			private int port;
			private Set<String> acceptIpSet;
			private Interval interval;
			@Data
			public static class Interval {
				private int sessionList;
			}
		}
		@Data
		public static class Client {
			private String ip;
			private int port;
			private String sampleFile;
			private Interval interval;
			@Data
			public static class Interval {
				private int sampleLog;
			}
		}
	}
	
	@Data
	public static class Udp {
		private String encoding;
		private Server server;
		private Client client;
		@Data
		public static class Server {
			private String ip;
			private int port;
			private int bufferSize;
			private Set<String> acceptIpSet;
			private Interval interval;
			@Data
			public static class Interval {
				private int sessionList;
			}
		}
		@Data
		public static class Client {
			private String ip;
			private int port;
			private String sampleFile;
			private Interval interval;
			@Data
			public static class Interval {
				private int sampleLog;
			}
		}
	}
	
	@Data
	public static class Log {
		private int interval;
		private String sampleFile;
		private String outputFile;
	}
}
