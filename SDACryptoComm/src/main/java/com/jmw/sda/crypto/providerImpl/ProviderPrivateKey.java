package com.jmw.sda.crypto.providerImpl;

import java.security.PrivateKey;
import java.security.spec.RSAPrivateCrtKeySpec;

import com.jmw.sda.crypto.IPrivateKey;

public class ProviderPrivateKey implements IPrivateKey {

	protected PrivateKey encryptionKey;
	protected PrivateKey signingKey;
	public ProviderPrivateKey(PrivateKey encryptionKey, PrivateKey signingKey) {
		this.encryptionKey = encryptionKey;
		this.signingKey = signingKey;
	}

	@Override
	public PrivateKey getActualEncryptionKey() {
		return this.encryptionKey;
	}
	@Override
	public PrivateKey getActualSigningKey() {
		return this.signingKey;
	}

	@Override
	public int bitLength() {
		return ((RSAPrivateCrtKeySpec)this.encryptionKey).getModulus().bitLength();
	}

}
