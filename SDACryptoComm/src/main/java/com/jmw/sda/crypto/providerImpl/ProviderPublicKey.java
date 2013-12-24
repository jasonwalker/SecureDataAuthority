package com.jmw.sda.crypto.providerImpl;

import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

import com.jmw.sda.crypto.IPublicKey;

public class ProviderPublicKey implements IPublicKey {

	protected PublicKey encryptionKey;
	protected PublicKey signingKey;
	public ProviderPublicKey(PublicKey encryptionKey, PublicKey signingKey) {
		this.encryptionKey = encryptionKey;
		this.signingKey = signingKey;
	}


	@Override
	public Object getActualEncryptionKey() {
		return this.encryptionKey;
	}

	@Override
	public Object getActualSigningKey() {
		return this.signingKey;
	}


	@Override
	public int bitLength() {
		return ((RSAPublicKey) this.encryptionKey).getModulus().bitLength();
	}
}
