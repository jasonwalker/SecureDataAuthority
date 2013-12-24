package com.jmw.sda.view.event;

public class HostDeleteEvent {
	protected final String name;
	public HostDeleteEvent(String hostName) {
		this.name = hostName;
	}
	public String getHostName(){
		return this.name;
	}

}
