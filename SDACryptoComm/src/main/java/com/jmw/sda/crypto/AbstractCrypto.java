package com.jmw.sda.crypto;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.codec.binary.Base64;

import com.jmw.sda.crypto.lbcImpl.LBCCrypto;

public abstract class AbstractCrypto {
	public static final BigInteger PUBLIC_EXPONENT = new BigInteger("65537");
	protected static final int BLOCK_SIZE = 16;
	// public static final int SECRET_STRENGTH = 112;
	// public static final int RSA_STRENGTH = 2048;
	public static final int SECRET_STRENGTH = 256;
	//public static final int RSA_STRENGTH = 3072;
//	 public static final int SECRET_STRENGTH = 192;
	 //public static final int RSA_STRENGTH = 7680;
	// public static final int SECRET_STRENGTH = 256;
	// public static final int RSA_STRENGTH = 15360;
	
	public static final AbstractCrypto crypto = new LBCCrypto();
	//public static final AbstractCrypto crypto = new ProviderCrypto();

	public static final AbstractCrypto getCrypto(){
		return crypto;
	}	
	
	
	/**
	 * Creates a secret key pair.  We use this function because we generate a keypair for encryption and one for signing simultaneously in separate threads
	 * @return
	 * @throws JavaInstallationMissingComponentsException
	 * @throws FailedCryptException
	 */
	abstract protected Object getSingleKeyPair(int strength) throws JavaInstallationMissingComponentsException, FailedCryptException;

	/**
	 * Generates secret keys
	 * @return object containing secret key
	 * @throws JavaInstallationMissingComponentsException if Java installation does not contain libraries it needs
	 * @throws FailedCryptException if encryption fails
	 */
	abstract public ISecretKey makeSecretKey() throws JavaInstallationMissingComponentsException, FailedCryptException;
	
	/**
	 * Create object that holds RSA public/private key pair
	 * @return
	 * @throws JavaInstallationMissingComponentsException
	 */
	abstract public RSAKeys makeRSAKeys(int strength)
			throws JavaInstallationMissingComponentsException, FailedCryptException;
	
	/**
	 * converts bytes to secret key--the bytes are not encrypted at this point
	 * @param bytes
	 * @return
	 */
	abstract public ISecretKey bytesToSecretKey(final byte[] bytes);
	
	/**
	 * Generate a secret key and initialization vector, wrap secret key with public key, 
	 * encrypt message with secret key and return byte array containing wrapped key, iv and encrypted message
	 * @param publicKey
	 * @param message
	 * @return encrypted message
	 * @throws FailedCryptException
	 * @throws JavaInstallationMissingComponentsException
	 */
	abstract public byte[] encrypt(final IPublicKey publicKey, final byte[] message)
			throws FailedCryptException,
			JavaInstallationMissingComponentsException;

	/**
	 * remove wrapped secret key and iv from bytes, retrieve secret key with private key, return decrypted data
	 * @param privateKey
	 * @param encryptedData
	 * @return
	 * @throws FailedCryptException
	 * @throws JavaInstallationMissingComponentsException
	 */
	abstract public byte[] decrypt(final IPrivateKey privateKey, final byte[] encryptedData)
			throws FailedCryptException,
			JavaInstallationMissingComponentsException;

	/**
	 * Create an object that can sign a bytes with a private key
	 * @param privateKey
	 * @return
	 * @throws JavaInstallationMissingComponentsException
	 * @throws FailedCryptException
	 */
	abstract public ISignature getSigner(final IPrivateKey privateKey)
			throws JavaInstallationMissingComponentsException, FailedCryptException;

	/**
	 * Create an object that can verify bytes with a public key
	 * @param publicKey
	 * @return
	 * @throws FailedCryptException
	 * @throws JavaInstallationMissingComponentsException
	 */
	abstract public ISignature getVerifier(final IPublicKey publicKey)
			throws FailedCryptException, JavaInstallationMissingComponentsException;

	/**
	 * creates an object that can encrypt data with the secret key
	 * @param key
	 * @param iv
	 * @return
	 * @throws JavaInstallationMissingComponentsException
	 * @throws FailedCryptException
	 */
	abstract public ICipher getCipher(final ISecretKey key, final byte[] iv, boolean encrypt)
			throws JavaInstallationMissingComponentsException,
			FailedCryptException;

	/**
	 * convert an RSA public key to a string
	 * @param key
	 * @return
	 */
	abstract public String publicKeyToString(final IPublicKey key);

	/**
	 * convert an RSA private key to a string
	 * @param key
	 * @return
	 * @throws FailedCryptException
	 * @throws JavaInstallationMissingComponentsException
	 */
	abstract public String privateKeyToString(final IPrivateKey key)
			throws FailedCryptException,
			JavaInstallationMissingComponentsException;

	/**
	 * turn string of proper format to a private key
	 * @param s
	 * @return
	 * @throws JavaInstallationMissingComponentsException
	 * @throws FailedCryptException
	 */
	abstract public IPrivateKey stringToPrivateKey(final String s)
			throws JavaInstallationMissingComponentsException,
			FailedCryptException;
	
	/**
	 * turn string of proper format to a public key
	 * @param s
	 * @return
	 * @throws JavaInstallationMissingComponentsException
	 * @throws FailedCryptException
	 */
	abstract public IPublicKey stringToPublicKey(final String s)
			throws JavaInstallationMissingComponentsException,
			FailedCryptException;

	/** Implemented methods 
	 * @throws JavaInstallationMissingComponentsException **/
	public static SecureRandom getPRNG() throws JavaInstallationMissingComponentsException{
		try {
			return SecureRandom.getInstance("SHA1PRNG", "SUN");
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			throw new JavaInstallationMissingComponentsException(e);
		}
	}
	
	/**
	 * generate a random array of bytes for an initialization vector
	 * @return
	 * @throws JavaInstallationMissingComponentsException
	 */
	public static byte[] genIV() throws JavaInstallationMissingComponentsException {
	    byte[] iv = new byte[BLOCK_SIZE];
	    SecureRandom randomGen = getPRNG();
	    randomGen.nextBytes(iv);		
	    return iv;
	}	
	
	/**
	 * encrypt a string with a newly-generated secret key that is wrapped with the public key
	 * @param key
	 * @param msg
	 * @return
	 * @throws FailedCryptException
	 * @throws JavaInstallationMissingComponentsException
	 */
	public String encrypt(final IPublicKey key, final String msg)
			throws FailedCryptException,
			JavaInstallationMissingComponentsException {
		try {
			byte[] encrypted = encrypt(key, msg.getBytes(Utils.ENCODING));
			return Base64.encodeBase64URLSafeString(encrypted);
		} catch (UnsupportedEncodingException e) {
			throw new JavaInstallationMissingComponentsException(e);
		}
	}

	/**
	 * decrypt a string with the secret key (provided in message) unwrapped with the private key
	 * @param key
	 * @param encrypted
	 * @return
	 * @throws FailedCryptException
	 * @throws JavaInstallationMissingComponentsException
	 */
	public String decrypt(final IPrivateKey key, final String encrypted)
			throws FailedCryptException,
			JavaInstallationMissingComponentsException {
		try {
			byte[] realMsg = Base64.decodeBase64(encrypted);
			return new String(decrypt(key, realMsg), Utils.ENCODING);

		} catch (UnsupportedEncodingException e) {
			throw new JavaInstallationMissingComponentsException(e);
		}
	}	

	/**
	 * generate one signature for several byte array messages
	 * @param pkey
	 * @param messages
	 * @return
	 * @throws JavaInstallationMissingComponentsException
	 * @throws FailedCryptException
	 */
	public byte[] sign(final IPrivateKey pkey, final byte[]... messages)
			throws JavaInstallationMissingComponentsException,
			FailedCryptException {
		try {
			ISignature signer = getSigner(pkey);
			for (byte[] message : messages) {
				signer.update(message);
			}
			return signer.sign();
		} catch (InvalidSignatureException e) {
			throw new FailedCryptException(e);
		}
	}

	/**
	 * generate one signature for several String messages
	 * @param pkey
	 * @param messages
	 * @return
	 * @throws JavaInstallationMissingComponentsException
	 * @throws FailedCryptException
	 */	
	public String sign(final IPrivateKey pkey, final String... messages)
			throws JavaInstallationMissingComponentsException,
			FailedCryptException {
		try {
			ISignature signer = getSigner(pkey);
			for (String message : messages) {
				signer.update(message.getBytes(Utils.ENCODING));
			}
			return Utils.byteArrayToB64(signer.sign());
		} catch ( UnsupportedEncodingException e) {
			throw new JavaInstallationMissingComponentsException(e);
		} catch (InvalidSignatureException e) {
			throw new FailedCryptException(e);
		}
	}

	/**
	 * verify a signature that accompanies several byte array messages
	 * @param pubKey
	 * @param signature
	 * @param messages
	 * @return
	 * @throws JavaInstallationMissingComponentsException
	 * @throws FailedCryptException
	 */
	public boolean verify(final IPublicKey pubKey, final byte[] signature,
			final byte[]... messages)
			throws JavaInstallationMissingComponentsException,
			FailedCryptException {
		try {
			ISignature verifier = getVerifier(pubKey);
			for (byte[] message : messages) {
				verifier.update(message);
			}
			return verifier.verify(signature);
		} catch (InvalidSignatureException e) {
			throw new FailedCryptException(e);
		}
	}
	/**
	 * verify a signature that accompanies several string messages
	 * @param pubKey
	 * @param signature
	 * @param messages
	 * @return
	 * @throws JavaInstallationMissingComponentsException
	 * @throws FailedCryptException
	 */
	public boolean verify(final IPublicKey pubKey, final String signature,
			final String... messages)
			throws JavaInstallationMissingComponentsException,
			FailedCryptException {
		try {
			ISignature verifier = getVerifier(pubKey);
			for (String message : messages) {
				verifier.update(message.getBytes(Utils.ENCODING));
			}
			return verifier.verify(Utils.b64ToByteArray(signature));
		} catch (UnsupportedEncodingException e) {
			throw new JavaInstallationMissingComponentsException(e);
		} catch (InvalidSignatureException e) {
			throw new FailedCryptException(e);
		}
	}
	
	protected static final String bigIntsToColonJoinedString(final BigInteger ...vals){
		if (vals == null || vals.length == 0){
			return "";
		}
		StringBuilder sb = new StringBuilder(Utils.bigIntToString(vals[0]));
		for (int i = 1 ; i < vals.length ; i++){
			sb.append(":");
			sb.append(Utils.bigIntToString(vals[i]));
		}
		return sb.toString();
	}
	
	protected static final BigInteger[] colonJoinedStringToBigInts(final String s){
		String[] strings = s.split(":");
		BigInteger[] retVals = new BigInteger[strings.length];
		for (int i = 0 ; i < retVals.length ; i++){
			retVals[i] = Utils.stringToBigInteger(strings[i]);
		}
		return retVals;
	}
	
	/**
	 * Generate 2 RSA key pairs simultaneously in two separate threads
	 * @return
	 * @throws JavaInstallationMissingComponentsException
	 * @throws FailedCryptException
	 */
	protected final Object[] makeTwoPairsOfKeysSimultaneously(int strength)
			throws FailedCryptException {
		try{
			CountDownLatch latch = new CountDownLatch(2);
			GeneratePublicKeyPair pair1 = new GeneratePublicKeyPair(strength, latch);
			GeneratePublicKeyPair pair2 = new GeneratePublicKeyPair(strength, latch);
			pair1.start();
			pair2.start();
			latch.await();
			Object p1 = pair1.getPair();
			Object p2 = pair2.getPair(); 
			if (p1 == null){
				throw new FailedCryptException(pair1.getException());
			}
			if (p2 == null){
				throw new FailedCryptException(pair2.getException());
			}
			return new Object[] {p1, p2};
		}catch(InterruptedException e){
			throw new FailedCryptException(e);
		}
	}	
	
	private final class GeneratePublicKeyPair extends Thread{
		protected Object pair =  null;
		protected Exception exception;
		protected CountDownLatch latch;
		protected int strength;
		public GeneratePublicKeyPair(int strength, CountDownLatch latch){
			this.latch = latch;
			this.strength = strength;
		}
		@Override
		public void run(){
			try {
				this.pair = getSingleKeyPair(this.strength);
			} catch (JavaInstallationMissingComponentsException | FailedCryptException e) {
				//pair return will be null to signal failure
				this.exception = e;
			}finally{
				this.latch.countDown();
			}
		}
		public Object getPair() {
			return this.pair;
		}
		public Exception getException() {
			return this.exception;
		}
	}
}

