package com.jmw.sda.view.event;

public class StringEvent {
	protected String name;
	protected int index;
	public StringEvent(String name, int index){
		this.name = name;
		this.index = index;
	}
	
	public String getName(){
		return this.name;
	}
	public int getIndex(){
		return this.index;
	}
}
