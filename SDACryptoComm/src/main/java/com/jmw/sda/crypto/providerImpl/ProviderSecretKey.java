package com.jmw.sda.crypto.providerImpl;

import java.util.Arrays;

import javax.crypto.SecretKey;

import com.jmw.sda.crypto.ISecretKey;

public class ProviderSecretKey implements ISecretKey {
	protected SecretKey key;
	public ProviderSecretKey(SecretKey key) {
		this.key = key;
	}
	@Override
	public SecretKey getActualKey(){
		return this.key;
	}
	
	@Override
	public byte[] getBytes() {
		return this.key.getEncoded();
	}
	

	@Override
	public boolean equals(ISecretKey otherKey){
		if (!(otherKey.getActualKey() instanceof SecretKey)){
			return false;
		}
		return Arrays.equals(this.key.getEncoded(), ((SecretKey)otherKey.getActualKey()).getEncoded());
	}

}
