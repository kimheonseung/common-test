package com.devh.test.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.devh.test.config.Config.Log;
import com.devh.test.util.TestUtils;

import lombok.extern.slf4j.Slf4j;

/*
 * <pre>
 * Description : 
 *     로그 생성기
 *     설정된 파일에 특정 로그를 계속하여 기록
 * ===============================
 * Memberfields :
 *     
 * ===============================
 * 
 * Author : HeonSeung Kim
 * Date   : 2022. 1. 6.
 * </pre>
 */
@Slf4j
public class LogGenerator {
	
	private int mInterval;
	private File mSampleFile;
	private File mOutputFile;
	
	private final List<String> mSampleLogLines = new ArrayList<String>();
	
	private static LogGenerator instance;
	public static LogGenerator getInstance() {
		if(instance == null)
			instance = new LogGenerator();
		return instance;
	}
	
	public void start(Log logConfig) {
		this.mInterval   = logConfig.getInterval();
		this.mSampleFile = new File(logConfig.getSampleFile());
		this.mOutputFile = new File(logConfig.getOutputFile());
		
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
		
		new Thread(new LogGeneratorThread()).start();
	}
	
	private class LogGeneratorThread implements Runnable {

		@Override
		public void run() {
			
			while(!Thread.currentThread().isInterrupted()) {
				try {
					FileWriter fw = new FileWriter(mOutputFile, true);
					
					for(String line : mSampleLogLines) {
						fw.write(String.format("%s%s", line, System.lineSeparator()));
					}
					fw.flush();
					fw.close();
					log.info(String.format("Append complete. [%s] -> [%s]: %d lines", mSampleFile.getAbsolutePath(), mOutputFile.getAbsolutePath(), mSampleLogLines.size()));
				} catch (IOException e) {
					log.debug(TestUtils.stackTraceToString(e));
					log.warn(String.format("Failed to append [%s] -> [%s] - [%s]", mSampleFile.getAbsolutePath(), mOutputFile.getAbsoluteFile(), e.getMessage()));
				}
				
				try { Thread.sleep(mInterval * 1000L); } catch (InterruptedException ignored) {}
			}
			
		}
		
	}
}
