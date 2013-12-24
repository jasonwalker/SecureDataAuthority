package com.jmw.sda.crypto;

/**
 * thrown when signature verify fails
 * @author jwalker
 *
 */
public class InvalidSignatureException extends Exception {
	private static final long serialVersionUID = -4562997036513642518L;

	public InvalidSignatureException(Exception e){
		super(e);
	}
	public InvalidSignatureException(String msg, Exception e){
		super(msg, e);
	}
	public InvalidSignatureException(String msg){
		super(msg);
	}
	
}
