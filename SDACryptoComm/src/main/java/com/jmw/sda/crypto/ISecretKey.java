package com.jmw.sda.crypto;

public interface ISecretKey {
	Object getActualKey();
	boolean equals(ISecretKey key);
	byte[] getBytes();
}
