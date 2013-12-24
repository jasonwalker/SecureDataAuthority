package com.jmw.sda.dbProviders;

public class DBException extends Exception {
	private static final long serialVersionUID = 4720577216259636540L;
	public DBException(String msg, Exception e){
		super(msg, e);
	}
	public DBException(Exception e){
		super(e);
	}
	public DBException(String msg){
		super(msg);
	}
}
