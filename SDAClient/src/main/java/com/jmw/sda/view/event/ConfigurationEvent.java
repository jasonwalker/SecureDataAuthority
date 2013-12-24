package com.jmw.sda.view.event;

public class ConfigurationEvent {
	protected final String hostName;
	public ConfigurationEvent(String hostName) {
		this.hostName = hostName;
	}

	public String getHostName(){
		return this.hostName;
	}
}
