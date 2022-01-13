package com.devh.test;

import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.devh.test.config.Config;
import com.devh.test.log.LogGenerator;
import com.devh.test.tcp.TcpClient;
import com.devh.test.tcp.TcpServer;
import com.devh.test.udp.UdpClient;
import com.devh.test.udp.UdpServer;

public class App {

	private static Config CONFIG;
	private static final Map<Integer, String> MENU_MAP = new HashMap<Integer, String>();
	
	public static void loadConfig() {
		System.setProperty("log4j.configurationFile", "conf/log4j2.xml");
		
		/* yml parse */
		try {
			final String ymlPath = "conf/config.yml";
			CONFIG = new Yaml(new Constructor(Config.class)).load(new FileReader(ymlPath));
			
		} catch (Exception e) {
			System.out.println("Exception while reading config file. " + e.getMessage());
			System.exit(0);
		}
		
		MENU_MAP.put(1, "TCP Server Mode");
		MENU_MAP.put(2, "TCP Client Mode");
		MENU_MAP.put(3, "UDP Server Mode");
		MENU_MAP.put(4, "UDP Client Mode");
		MENU_MAP.put(5, "Log Generator Mode");
	}

	public static void main(String[] args) {
		loadConfig();
		
		String help = "[Choose Number & Press Enter]";
		for(Entry<Integer, String> entry : MENU_MAP.entrySet()) {
			help += String.format("\n  %d. [%s]", entry.getKey(), entry.getValue());
		}
		help += "\n: ";
		
		while (true) {
			System.out.print(help);
			Scanner sc = new Scanner(System.in);
			String inputNum = sc.next();

			int menuKey = 0;
			try {
				menuKey = Integer.parseInt(inputNum);
			} catch (Exception ignored) {}
			
			System.out.println(String.format("\n[%s] selected.\n", MENU_MAP.get(menuKey)));
			
			switch (menuKey) {
			case 1:
				TcpServer.getInstance().start(CONFIG.getTcp());
				break;
			case 2:
				TcpClient.getInstance().start(CONFIG.getTcp());
				break;
			case 3:
				UdpServer.getInstance().start(CONFIG.getUdp());
				break;
			case 4:
				UdpClient.getInstance().start(CONFIG.getUdp());
				break;
			case 5:
				LogGenerator.getInstance().start(CONFIG.getLog());
				break;
			default:
				System.out.println("----------------------------- Exception ------------------------------" + "\n");
				continue;
			}
			sc.close();
			break;
		}
	}
}