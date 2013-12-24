package com.jmw.sda.crypto.lbcImpl;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.signers.RSADigestSigner;

import com.jmw.sda.crypto.ISignature;
import com.jmw.sda.crypto.InvalidSignatureException;

public class LBCSignature implements ISignature {
	protected RSADigestSigner signer;
	
	public LBCSignature(RSADigestSigner signer) {
		this.signer = signer;
	}

	@Override
	public void update(byte[] data)  {
		this.signer.update(data, 0, data.length);
	}

	@Override
	public byte[] sign() throws InvalidSignatureException {
		try {
			return this.signer.generateSignature();
		} catch (DataLengthException | CryptoException e) {
			throw new InvalidSignatureException(e);
		}
	}

	@Override
	public boolean verify(byte[] signature) {
	    return this.signer.verifySignature(signature);
	}
}
