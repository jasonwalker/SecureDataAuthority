package com.jmw.sda.crypto.lbcImpl;

import org.bouncycastle.crypto.params.RSAKeyParameters;

import com.jmw.sda.crypto.IPublicKey;

public class LBCPublicKey implements IPublicKey {
	
	protected RSAKeyParameters publicEncryptionKey;
	protected RSAKeyParameters publicSigningKey;
	
	public LBCPublicKey(RSAKeyParameters publicEncryptionKey, RSAKeyParameters publicSigningKey) {
		this.publicEncryptionKey = publicEncryptionKey;
		this.publicSigningKey = publicSigningKey;
	}

	@Override
	public RSAKeyParameters getActualEncryptionKey() {
		return this.publicEncryptionKey;
	}

	@Override
	public RSAKeyParameters getActualSigningKey() {
		return this.publicSigningKey;
	}

	@Override
	public int bitLength() {
		return this.publicEncryptionKey.getModulus().bitLength();
	}


}
