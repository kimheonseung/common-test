package com.devh.test.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.logging.LogLevel;
import org.apache.mina.filter.logging.LoggingFilter;

/*
 * <pre>
 * Description : 
 *     현 테스트 로직에서 필요한 공통 유틸
 * ===============================
 * Memberfields :
 *     
 * ===============================
 * 
 * Author : HeonSeung Kim
 * Date   : 2022. 1. 5.
 * </pre>
 */
public class TestUtils {
	private static final long BYTES_LIMIT 	  = 1024;
	private static final long KILOBYTES_LIMIT = 1024 * 1024;
	private static final long MEGABYTES_LIMIT = 1024 * 1024 * 1024;
	
	public static LoggingFilter getLoggingFilter() {
		final LoggingFilter loggingFilter = new LoggingFilter();
		loggingFilter.setMessageReceivedLogLevel(LogLevel.DEBUG);
		loggingFilter.setMessageSentLogLevel(LogLevel.DEBUG);
		loggingFilter.setSessionClosedLogLevel(LogLevel.DEBUG);
		loggingFilter.setSessionCreatedLogLevel(LogLevel.DEBUG);
		loggingFilter.setSessionOpenedLogLevel(LogLevel.DEBUG);
		return loggingFilter;
	}
	
	public static String convertBytesToString(long l) {
		String read = null;
		if(l < BYTES_LIMIT) {
			/* Bytes */
			read = String.valueOf(l) + " B";
		} else if(l < KILOBYTES_LIMIT) {
			/* Kilo Bytes */
			read = String.valueOf(Math.ceil(l / BYTES_LIMIT)) + " KB";
		} else if(l < MEGABYTES_LIMIT) {
			/* Mega Bytes */
			read = String.valueOf(Math.ceil(l / KILOBYTES_LIMIT)) + " MB";
		} 
		else {
			/* Giga Bytes */
			read = String.valueOf((l / MEGABYTES_LIMIT)) + " GB";
		}
		return read;
	}
	
	public static boolean isAcceptIp(Set<String> acceptIpSet, IoSession session) {
		if(acceptIpSet.size() == 0)
			return true;
		
		final String ip = session.getRemoteAddress().toString().replace("/", "").split(":")[0];
		return acceptIpSet.contains(ip);
	}
	
	/*
	 * <pre>
	 * Description : 
	 *     익셉션의 스택트레이스를 문자열로 반환
	 * ===============================
	 * Parameters :
	 *     Throwable cause
	 * Returns :
	 *     String
	 * Throws :
	 *     
	 * ===============================
	 * 
	 * Author : HeonSeung Kim
	 * Date   : 2021. 5. 17.
	 * </pre>
	 */
	public static String stackTraceToString(Throwable cause) {
		final StringWriter sw = new StringWriter();
		cause.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
	
}
