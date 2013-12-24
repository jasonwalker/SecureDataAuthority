package com.jmw.sda.crypto.lbcImpl;

import java.util.Arrays;

import org.bouncycastle.crypto.params.KeyParameter;

import com.jmw.sda.crypto.ISecretKey;

public class LBCSecretKey implements ISecretKey {
	
	protected KeyParameter secretKey;
	
	public LBCSecretKey(KeyParameter secretKey) {
		this.secretKey = secretKey;
	}

	@Override
	public KeyParameter getActualKey() {
		return this.secretKey;
	}
	
	@Override
	public byte[] getBytes(){
		return this.secretKey.getKey();
	}
	
	
	@Override
	public boolean equals(ISecretKey key){
		if (!(key.getActualKey() instanceof KeyParameter)){
			return false;
		}
		return Arrays.equals(this.secretKey.getKey(), ((KeyParameter)key.getActualKey()).getKey());
	}

}
