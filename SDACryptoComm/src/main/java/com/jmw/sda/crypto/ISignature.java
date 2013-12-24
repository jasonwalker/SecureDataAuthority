package com.jmw.sda.crypto;

public interface ISignature {
	void update(byte[] data) throws InvalidSignatureException;
	byte[] sign() throws InvalidSignatureException;
	boolean verify(byte[] signature) throws InvalidSignatureException;
}
