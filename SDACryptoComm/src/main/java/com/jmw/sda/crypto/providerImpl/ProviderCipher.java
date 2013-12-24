package com.jmw.sda.crypto.providerImpl;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.ICipher;

public class ProviderCipher implements ICipher {
	protected Cipher cipher;
	public ProviderCipher(Cipher cipher) {
		this.cipher = cipher;
	}	
	
	@Override
	public byte[] update(byte[] input) {
		return this.cipher.update(input);
	}
	
	@Override
	public byte[] update(byte[] input, int offset, int length) {
		return this.cipher.update(input, offset, length);
	}
	
	@Override
	public byte[] doFinal() throws FailedCryptException{
		try {
			return this.cipher.doFinal();
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			throw new FailedCryptException(e);
		}
	}
	@Override
	public byte[] doFinal(byte[] input) throws FailedCryptException {
		try {
			return this.cipher.doFinal(input);
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			throw new FailedCryptException(e);
		}
	}
	@Override
	public byte[] getIV() {
		return this.cipher.getIV();
	}


}
