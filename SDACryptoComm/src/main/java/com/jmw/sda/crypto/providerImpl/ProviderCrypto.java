package com.jmw.sda.crypto.providerImpl;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.jmw.sda.crypto.AbstractCrypto;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.ICipher;
import com.jmw.sda.crypto.IPrivateKey;
import com.jmw.sda.crypto.IPublicKey;
import com.jmw.sda.crypto.ISecretKey;
import com.jmw.sda.crypto.ISignature;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.RSAKeys;
import com.jmw.sda.crypto.Utils;

public final class ProviderCrypto extends AbstractCrypto {
	
	static{
		Security.insertProviderAt(new BouncyCastleProvider(), 1);
	}
	
	public static final String SECRET_KEY = "AES";
	public static final String PUBLIC_KEY = "RSA";
	public static final String SIGNATURE_INSTANCE = "SHA512withRSA";
	//public static final String SIGNATURE_INSTANCE = "RIPEMD256withRSA";
	public static final String RSA_CIPHER_INSTANCE = "RSA/ECB/PKCS1Padding";
	public static final String SECRET_CIPHER_INSTANCE = "AES/CBC/PKCS5Padding";
	//public static final String SECRET_CIPHER_INSTANCE = "Twofish/CBC/PKCS5Padding";
	public static final String HASH_ALGORITHM = "SHA-512";
	
	public static void printOutCryptoInfo() {
        for (Provider provider : Security.getProviders()){
        	System.out.println("Provider: " + provider.getName());
        	for (Provider.Service service : provider.getServices()){
        		String algorithm = service.getAlgorithm();
        		String type = service.getType();
        		if (type.equalsIgnoreCase("SecureRandom")){
        			System.out.println("Type: " + type);
        			try{
        				System.out.println(String.format("Algorithm: %s, Type: %s, Strength: %d",algorithm,type,Cipher.getMaxAllowedKeyLength(algorithm)));
        			}catch(NoSuchAlgorithmException e){
        				System.out.println(String.format("Algorithm: %s, Type: %s",algorithm,type));
        			}
        		}
        	}
         } 
	}

	@Override
	public ISecretKey makeSecretKey() throws JavaInstallationMissingComponentsException{
		KeyGenerator kgen;
		try {
			kgen = KeyGenerator.getInstance(SECRET_KEY);
			kgen.init(AbstractCrypto.SECRET_STRENGTH);
			return new ProviderSecretKey(kgen.generateKey());
		} catch (NoSuchAlgorithmException e) {
			throw new JavaInstallationMissingComponentsException(e);
		}
	}
	
	/**
	 * this is NOT wrapped
	 * @param secretKey
	 * @return
	 */
	@Override
	public ISecretKey bytesToSecretKey(final byte[] bytes){
		return new ProviderSecretKey(new SecretKeySpec(bytes, 0, bytes.length, SECRET_KEY));
	}
	
	private static final byte[] wrapSecretKey(final ISecretKey secretKey, final IPublicKey publicKey) 
			throws JavaInstallationMissingComponentsException, FailedCryptException{
		try {
			Cipher c = Cipher.getInstance(PUBLIC_KEY);
			c.init(Cipher.WRAP_MODE, (PublicKey)publicKey.getActualEncryptionKey());
			return c.wrap((SecretKey)secretKey.getActualKey());
		} catch (InvalidKeyException | IllegalBlockSizeException e) {
			throw new FailedCryptException(e);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			throw new JavaInstallationMissingComponentsException(e);
		}		
	}
	
	private static final ISecretKey unwrapSecretKey(final byte[] wrapped, final IPrivateKey privateKey) 
			throws JavaInstallationMissingComponentsException, FailedCryptException{
		try {
			Cipher c = Cipher.getInstance(PUBLIC_KEY);
			c.init(Cipher.UNWRAP_MODE, (PrivateKey)privateKey.getActualEncryptionKey());
			return new ProviderSecretKey((SecretKey)c.unwrap(wrapped, SECRET_KEY, Cipher.SECRET_KEY));
		} catch (InvalidKeyException  e) {
			throw new FailedCryptException(e);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			throw new JavaInstallationMissingComponentsException(e);
		}
	}
	
	@Override
	public byte[] encrypt(final IPublicKey publicKey, final byte[] message) throws 
			FailedCryptException, JavaInstallationMissingComponentsException{
		ISecretKey secretKey = makeSecretKey();
		ICipher secretCipher = getCipher(secretKey, AbstractCrypto.genIV(), true);
		byte[] encryptedMessage = secretCipher.doFinal(message);
		byte[] wrapped = wrapSecretKey(secretKey, publicKey);
		return Utils.packIntoBigArray(encryptedMessage, wrapped, secretCipher.getIV());
	}	
	
	@Override
	public byte[] decrypt(final IPrivateKey privateKey, final byte[] encryptedData) throws 
			FailedCryptException, JavaInstallationMissingComponentsException{
		byte[][] packed = Utils.unpackBigArray(encryptedData);
		byte[] encryptedMessage = packed[0];
		byte[] wrapped = packed[1];
		byte[] iv = packed[2];
		ISecretKey secretKey = unwrapSecretKey(wrapped, privateKey);
		ICipher secretCipher = getCipher(secretKey, iv, false);
		return secretCipher.doFinal(encryptedMessage);
	}		
	
	@Override
	protected KeyPair getSingleKeyPair(int strength) throws JavaInstallationMissingComponentsException, FailedCryptException{
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance(PUBLIC_KEY);
			RSAKeyGenParameterSpec rsaSpec = new RSAKeyGenParameterSpec(strength, RSAKeyGenParameterSpec.F4);
			kpg.initialize(rsaSpec);
			// Generate the keys -- might take sometime on slow computers
			return kpg.generateKeyPair();	
		} catch (InvalidAlgorithmParameterException e) {
			throw new FailedCryptException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new JavaInstallationMissingComponentsException(e);
		}
	}	
	
	@Override
	public RSAKeys makeRSAKeys(int strength)
			throws JavaInstallationMissingComponentsException, FailedCryptException {
		Object[] twoKeyPairs = makeTwoPairsOfKeysSimultaneously(strength);
		KeyPair encPair = (KeyPair)twoKeyPairs[0];
		KeyPair signPair = (KeyPair)twoKeyPairs[1];			
		RSAKeys keyPair = new RSAKeys(
				new ProviderPublicKey(encPair.getPublic(), signPair.getPublic()), 
				new ProviderPrivateKey(encPair.getPrivate(), signPair.getPrivate()));
		return keyPair;

	}	
	
	@Override
	public ISignature getSigner(final IPrivateKey privateKey) throws JavaInstallationMissingComponentsException, FailedCryptException {
		Signature signature;
		try {
			signature = Signature.getInstance(SIGNATURE_INSTANCE);
		    signature.initSign((PrivateKey)privateKey.getActualSigningKey(), getPRNG());
		    return new ProviderSignature(signature);
		} catch (NoSuchAlgorithmException e) {
			throw new JavaInstallationMissingComponentsException(e);
		} catch(InvalidKeyException e){
			throw new FailedCryptException(e);
		}

	}
	
	@Override
	public ISignature getVerifier(final IPublicKey publicKey) throws JavaInstallationMissingComponentsException, FailedCryptException {
		Signature signature;
		try {
			signature = Signature.getInstance(SIGNATURE_INSTANCE);
			signature.initVerify((PublicKey)publicKey.getActualSigningKey());
			return new ProviderSignature(signature);
		} catch (NoSuchAlgorithmException e) {
			throw new JavaInstallationMissingComponentsException(e);
		} catch(InvalidKeyException e){
			throw new FailedCryptException(e);
		}

	}
	
	@Override
	public ICipher getCipher(final ISecretKey key, final byte[] iv, final boolean encrypt) throws JavaInstallationMissingComponentsException, FailedCryptException{
		byte[] secretKey = ((SecretKey)key.getActualKey()).getEncoded();
		SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, SECRET_KEY);
		int mode = encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
		try {
			Cipher secretCipher = Cipher.getInstance(ProviderCrypto.SECRET_CIPHER_INSTANCE);
			secretCipher.init(mode, secretKeySpec, new IvParameterSpec(iv));
			return new ProviderCipher(secretCipher);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			throw new JavaInstallationMissingComponentsException(e);
		} catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
			throw new FailedCryptException(e);
		}
	}
	
	@Override
	public String publicKeyToString(final IPublicKey publicKey){
		RSAPublicKey encryptionKey = (RSAPublicKey)publicKey.getActualEncryptionKey();
		String encKeyString = bigIntsToColonJoinedString(
				encryptionKey.getModulus(),
				encryptionKey.getPublicExponent());
		RSAPublicKey rsaSigningKey = (RSAPublicKey)publicKey.getActualSigningKey();
		String signKeyString = bigIntsToColonJoinedString(
				rsaSigningKey.getModulus(),
				rsaSigningKey.getPublicExponent());	
		return String.format("%s;%s", encKeyString, signKeyString);
	}
	@Override
	public String privateKeyToString(final IPrivateKey privateKey) throws FailedCryptException, JavaInstallationMissingComponentsException{
		RSAPrivateCrtKeySpec encryptionSpec = getPrivateKeyComponents((PrivateKey)privateKey.getActualEncryptionKey());
		
		String encKeyString = bigIntsToColonJoinedString(
				encryptionSpec.getModulus(),
				encryptionSpec.getPublicExponent(),
				encryptionSpec.getPrivateExponent(),
				encryptionSpec.getPrimeP(),
				encryptionSpec.getPrimeQ(),
				encryptionSpec.getPrimeExponentP(),
				encryptionSpec.getPrimeExponentQ(),
				encryptionSpec.getCrtCoefficient());
		RSAPrivateCrtKeySpec signingSpec = getPrivateKeyComponents((PrivateKey)privateKey.getActualSigningKey());
		String signKeyString = bigIntsToColonJoinedString(
				signingSpec.getModulus(),
				signingSpec.getPublicExponent(),
				signingSpec.getPrivateExponent(),
				signingSpec.getPrimeP(),
				signingSpec.getPrimeQ(),
				signingSpec.getPrimeExponentP(),
				signingSpec.getPrimeExponentQ(),
				signingSpec.getCrtCoefficient());
		return String.format("%s;%s", encKeyString, signKeyString);
	}

	@Override
	public IPrivateKey stringToPrivateKey(final String s) throws FailedCryptException, JavaInstallationMissingComponentsException{
		try{
			String[] Encryption_signing_keys = s.split(";");
			if (Encryption_signing_keys.length != 2){
				throw new FailedCryptException("The string is not a private key");
			}
			BigInteger[] encIntegers = colonJoinedStringToBigInts(Encryption_signing_keys[0]);		
			BigInteger[] signIntegers = colonJoinedStringToBigInts(Encryption_signing_keys[1]);	
			if (encIntegers.length != 8 || signIntegers.length != 8){
				throw new FailedCryptException("The string is not a private key");
			}
			KeyFactory keyFactory = KeyFactory.getInstance(PUBLIC_KEY);
			
			PrivateKey encKey = keyFactory.generatePrivate(
					new RSAPrivateCrtKeySpec(
						encIntegers[0], //modulus
						encIntegers[1], //publicExponent
						encIntegers[2], //privateExponent
						encIntegers[3], //PrimeP
						encIntegers[4], //PrimeQ
						encIntegers[5], //PrimeExponentP
						encIntegers[6], //PrimeExponentQ
						encIntegers[7])); //CrtCoefficient
			PrivateKey signKey = keyFactory.generatePrivate(
					new RSAPrivateCrtKeySpec(
						signIntegers[0], //modulus
						signIntegers[1], //publicExponent
						signIntegers[2], //privateExponent
						signIntegers[3], //PrimeP
						signIntegers[4], //PrimeQ
						signIntegers[5], //PrimeExponentP
						signIntegers[6], //PrimeExponentQ
						signIntegers[7])); //CrtCoefficient

			return new ProviderPrivateKey(encKey, signKey);
		}catch(NoSuchAlgorithmException | InvalidKeySpecException e){
			throw new JavaInstallationMissingComponentsException(e);
		}
	}
	
	@Override
	public IPublicKey stringToPublicKey(final String s) throws FailedCryptException, JavaInstallationMissingComponentsException{
		try{
			String[] Encryption_signing_keys = s.split(";");
			if (Encryption_signing_keys.length != 2){
				throw new FailedCryptException("The string is not a public key");
			}	
			BigInteger[] encIntegers = colonJoinedStringToBigInts(Encryption_signing_keys[0]);		
			BigInteger[] signIntegers = colonJoinedStringToBigInts(Encryption_signing_keys[1]);	
	
			if (encIntegers.length != 2 || signIntegers.length != 2){
				throw new FailedCryptException("The string is not a public key");
			}
			KeyFactory keyFactory = KeyFactory.getInstance(PUBLIC_KEY);
			
			PublicKey encKey = keyFactory.generatePublic(new RSAPublicKeySpec(
					encIntegers[0], 
					encIntegers[1]));
			PublicKey signKey = keyFactory.generatePublic(new RSAPublicKeySpec(
					signIntegers[0], 
					signIntegers[1]));
			return new ProviderPublicKey(encKey, signKey);
		}catch(NoSuchAlgorithmException | InvalidKeySpecException e){
			throw new JavaInstallationMissingComponentsException(e);
		}
	}
	
	protected final static RSAPrivateCrtKeySpec getPrivateKeyComponents(final PrivateKey key) throws FailedCryptException, JavaInstallationMissingComponentsException{
        try{
			KeyFactory keyFac = KeyFactory.getInstance(PUBLIC_KEY);
	        return keyFac.getKeySpec(key, RSAPrivateCrtKeySpec.class);
        }catch (NoSuchAlgorithmException e) {
			throw new JavaInstallationMissingComponentsException(e);
		}catch(InvalidKeySpecException e){
			throw new FailedCryptException(e);
		}
	}
}

