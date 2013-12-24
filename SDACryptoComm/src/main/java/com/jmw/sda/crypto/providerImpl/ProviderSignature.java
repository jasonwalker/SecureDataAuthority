package com.jmw.sda.crypto.providerImpl;

import java.security.Signature;
import java.security.SignatureException;

import com.jmw.sda.crypto.ISignature;
import com.jmw.sda.crypto.InvalidSignatureException;

public class ProviderSignature implements ISignature {

	protected final Signature signature;
	public ProviderSignature(Signature signature) {
		this.signature = signature;
	}
	@Override
	public void update(byte[] data) throws InvalidSignatureException {
		try{
			this.signature.update(data);
		}catch(SignatureException e){
			throw new InvalidSignatureException(e);
		}
	}
	@Override
	public byte[] sign() throws InvalidSignatureException {
		try{
			return this.signature.sign();
		}catch(SignatureException e){
			throw new InvalidSignatureException(e);
		}
	}
	@Override
	public boolean verify(byte[] incomingSignature) throws InvalidSignatureException {
		try {
			return this.signature.verify(incomingSignature);
		} catch (SignatureException e) {
			throw new InvalidSignatureException(e);
		}
	}
	

}
