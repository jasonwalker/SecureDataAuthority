package com.jmw.sda.view.event;

public class NameChangeEvent {
	protected String newName;
	public NameChangeEvent(String newName) {
		this.newName = newName;
	}
	
	public String getName(){
		return this.newName;
	}
}

