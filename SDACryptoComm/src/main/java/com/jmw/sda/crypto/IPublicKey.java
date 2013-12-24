package com.jmw.sda.crypto;

public interface IPublicKey {
	Object getActualEncryptionKey();
	Object getActualSigningKey();
	int bitLength();
}
