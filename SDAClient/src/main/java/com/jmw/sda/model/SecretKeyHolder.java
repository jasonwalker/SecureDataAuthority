package com.jmw.sda.model;

import com.jmw.sda.crypto.RSAKeys;

public class SecretKeyHolder {
	protected RSAKeys rsaKeys;
	
	public RSAKeys getRsaKeys() {
		return this.rsaKeys;
	}

	public void setRsaKeys(RSAKeys rsaKeys) {
		this.rsaKeys = rsaKeys;
	}
}
