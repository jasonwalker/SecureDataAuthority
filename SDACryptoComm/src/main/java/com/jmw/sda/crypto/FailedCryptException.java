package com.jmw.sda.crypto;

public class FailedCryptException extends Exception {
	private static final long serialVersionUID = 3357239831466480470L;

	public FailedCryptException(final Exception e){
		super(e);
	}
	
	public FailedCryptException(final String msg, final Exception e){
		super(msg, e);
	}
	public FailedCryptException(final String msg){
		super(msg);
	}
}
