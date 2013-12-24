package com.jmw.sda.crypto;

/**
 * holds RSA public/private key pair
 * @author jwalker
 *
 */
public class RSAKeys {
	private final IPrivateKey privateKey;
	private final IPublicKey publicKey;
	protected static AbstractCrypto crypto = AbstractCrypto.getCrypto();
	public RSAKeys(IPublicKey publicKey, IPrivateKey privateKey) {
		this.publicKey = publicKey;
		this.privateKey = privateKey;		
	}
	public RSAKeys(String publicKey, String privateKey) throws JavaInstallationMissingComponentsException, FailedCryptException{
		this.publicKey = crypto.stringToPublicKey(publicKey);
		this.privateKey = crypto.stringToPrivateKey(privateKey);
	}
	public IPublicKey getPublicKey(){
		return this.publicKey;
	}
	public IPrivateKey getPrivateKey(){
		return this.privateKey;
	}
	
	public String getPublicKeyString(){
		return crypto.publicKeyToString(this.publicKey);
	}
	public String getPrivateKeyString() throws FailedCryptException, JavaInstallationMissingComponentsException{
		return crypto.privateKeyToString(this.privateKey);
	}

}

