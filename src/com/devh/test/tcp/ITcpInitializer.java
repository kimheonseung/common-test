package com.devh.test.tcp;

import com.devh.test.config.Config.Tcp;

public interface ITcpInitializer {
	abstract void start(Tcp tcp);
}
