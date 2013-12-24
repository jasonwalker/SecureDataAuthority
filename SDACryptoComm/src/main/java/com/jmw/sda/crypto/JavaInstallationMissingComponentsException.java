package com.jmw.sda.crypto;

public class JavaInstallationMissingComponentsException extends Exception {
	private static final long serialVersionUID = 380303018478474788L;

	public JavaInstallationMissingComponentsException(Exception exception){
		super(exception);
	}
	public JavaInstallationMissingComponentsException(String msg, Exception exception){
		super(msg, exception);
	}
}
