package com.jmw.sda.crypto;

public interface IPrivateKey {
	Object getActualEncryptionKey();
	Object getActualSigningKey();
	int bitLength();
}
