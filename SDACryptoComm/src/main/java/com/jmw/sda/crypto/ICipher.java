package com.jmw.sda.crypto;

/**
 * Object used to encrypt data
 * @author jwalker
 *
 */
public interface ICipher {
	byte[] update(byte[] input);
	
	byte[] update(byte[] input, int start, int length);
	
	byte[] doFinal() throws FailedCryptException;
	
	byte[] doFinal(byte[] input) throws FailedCryptException;
	
	byte[] getIV();
}
