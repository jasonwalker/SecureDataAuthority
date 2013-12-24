package com.jmw.sda.communication;

public class CommunicationFailureException extends Exception {
	private static final long serialVersionUID = -3944874575740496831L;
	public CommunicationFailureException(String msg){
		super(msg);
	}
	public CommunicationFailureException(Exception e){
		super(e);
	}
	public CommunicationFailureException(String msg, Exception e){
		super(msg, e);
	}
}
