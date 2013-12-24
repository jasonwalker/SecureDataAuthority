package com.jmw.sda.crypto.lbcImpl;

import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;

import com.jmw.sda.crypto.IPrivateKey;

public class LBCPrivateKey implements IPrivateKey {
	protected RSAPrivateCrtKeyParameters privateEncryptionKey;
	protected RSAPrivateCrtKeyParameters privateSigningKey;
	
	public LBCPrivateKey(RSAPrivateCrtKeyParameters privateEncryptionKey, RSAPrivateCrtKeyParameters privateSigningKey) {
		this.privateEncryptionKey = privateEncryptionKey;
		this.privateSigningKey = privateSigningKey;
	}

	@Override
	public RSAPrivateCrtKeyParameters getActualEncryptionKey() {
		return this.privateEncryptionKey;
	}

	@Override
	public RSAPrivateCrtKeyParameters getActualSigningKey() {
		return this.privateSigningKey;
	}

	@Override
	public int bitLength() {
		return this.privateEncryptionKey.getModulus().bitLength();
	}
}
