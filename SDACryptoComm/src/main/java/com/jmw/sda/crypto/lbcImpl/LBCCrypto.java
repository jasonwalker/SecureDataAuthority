package com.jmw.sda.crypto.lbcImpl;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.generators.KDF2BytesGenerator;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.kems.RSAKeyEncapsulation;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.signers.RSADigestSigner;

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

public class LBCCrypto extends AbstractCrypto {
	
	@Override
	public ISecretKey makeSecretKey() throws JavaInstallationMissingComponentsException{
		byte[] key = new byte[AbstractCrypto.SECRET_STRENGTH/8];
	    SecureRandom randomGen = getPRNG();
	    randomGen.nextBytes(key);
	    return new LBCSecretKey(new KeyParameter(key));
	}
	
	@Override
	public ISecretKey bytesToSecretKey(final byte[] bytes){
		return new LBCSecretKey(new KeyParameter(bytes));
	}
	
//	@Override
//	//wrap using RSAES-OAEP method
//    public byte[] wrapSecretKey(ISecretKey secretKey, IPublicKey publicKey) throws FailedCryptException{
//        AsymmetricBlockCipher eng = new OAEPEncoding(new RSAEngine());
//        byte[] data = ((KeyParameter)secretKey.getActualKey()).getKey();
//        eng.init(true, ((RSAKeyParameters)publicKey.getActualKey()));
//        try{
//            return eng.processBlock(data, 0, data.length);
//        }catch(InvalidCipherTextException e){
//        	throw new FailedCryptException(e);
//        }
//    }
//    //unwrap using RSAES-OAEP method
//    public ISecretKey unwrapSecretKey(byte[] wrapped, IPrivateKey privateKey) throws FailedCryptException{
//    	AsymmetricBlockCipher eng = new OAEPEncoding(new RSAEngine());
//    	eng.init(false, (RSAPrivateCrtKeyParameters)privateKey.getActualKey());
//        try{
//            byte[] unwrapped = eng.processBlock(wrapped, 0, wrapped.length);
//            return new LBCSecretKey(new KeyParameter(unwrapped));
//        }
//        catch (InvalidCipherTextException e){
//        	throw new FailedCryptException(e);
//        }
//    }
	
	@Override
	public byte[] encrypt(final IPublicKey publicKey, final byte[] message) throws FailedCryptException,
			JavaInstallationMissingComponentsException {
		//using RSA-KEM method to generate/encrypt secret key
		KDF2BytesGenerator kdf = new KDF2BytesGenerator(new SHA512Digest());
		SecureRandom randomGen = getPRNG();
		RSAKeyEncapsulation kem = new RSAKeyEncapsulation(kdf, randomGen);
		byte[] wrapped = new byte[publicKey.bitLength()/8];
        kem.init((RSAKeyParameters)publicKey.getActualEncryptionKey());
        ISecretKey secretKey = new LBCSecretKey((KeyParameter)kem.encrypt(wrapped, SECRET_STRENGTH/8));
		ICipher cipher = getCipher(secretKey, AbstractCrypto.genIV(), true);
		byte[] encryptedMessage =  cipher.doFinal(message);
		return Utils.packIntoBigArray(encryptedMessage, wrapped, cipher.getIV());

	}

	@Override
	public byte[] decrypt(final IPrivateKey privateKey,
			final byte[] encryptedData) throws FailedCryptException {
		
		byte[][] packed = Utils.unpackBigArray(encryptedData);
		byte[] encryptedMessage = packed[0];
		byte[] wrapped = packed[1];
		byte[] iv = packed[2];
		//using RSA-KEM method to decrypt secret key
	    KDF2BytesGenerator kdf = new KDF2BytesGenerator(new SHA512Digest());
        RSAKeyEncapsulation kem = new RSAKeyEncapsulation(kdf, null);
        kem.init((RSAPrivateCrtKeyParameters)privateKey.getActualEncryptionKey());
        ISecretKey secretKey = new LBCSecretKey((KeyParameter)kem.decrypt(wrapped, SECRET_STRENGTH/8)); 
        ICipher cipher = getCipher(secretKey, iv, false);
        return cipher.doFinal(encryptedMessage);
	}
	@Override
	protected AsymmetricCipherKeyPair getSingleKeyPair(int strength) throws JavaInstallationMissingComponentsException{
		RSAKeyPairGenerator generator = new RSAKeyPairGenerator();
		generator.init(new RSAKeyGenerationParameters(
				AbstractCrypto.PUBLIC_EXPONENT,
				AbstractCrypto.getPRNG(),//prng
		        strength,//strength
		        80//certainty
		    ));
		return generator.generateKeyPair();		
	}
	
	@Override
	public RSAKeys makeRSAKeys(int strength)
			throws JavaInstallationMissingComponentsException, FailedCryptException {
		Object[] twoKeyPairs = makeTwoPairsOfKeysSimultaneously(strength);
		AsymmetricCipherKeyPair encPair = (AsymmetricCipherKeyPair)twoKeyPairs[0];
		AsymmetricCipherKeyPair signPair = (AsymmetricCipherKeyPair)twoKeyPairs[1];
		RSAKeys keyPair = new RSAKeys(
				new LBCPublicKey((RSAKeyParameters)encPair.getPublic(), 
						          (RSAKeyParameters)signPair.getPublic()), 
				new LBCPrivateKey((RSAPrivateCrtKeyParameters)encPair.getPrivate(), 
						           (RSAPrivateCrtKeyParameters)signPair.getPrivate()));
		return keyPair;
	}

	@Override
	public ISignature getSigner(final IPrivateKey privateKey) throws JavaInstallationMissingComponentsException{
    	RSADigestSigner eng = new RSADigestSigner(new SHA512Digest());
		eng.init(true, new ParametersWithRandom((RSAPrivateCrtKeyParameters)privateKey.getActualSigningKey(), getPRNG()));
		return new LBCSignature(eng);
	}

	@Override
	public ISignature getVerifier(final IPublicKey publicKey){
		RSADigestSigner eng = new RSADigestSigner(new SHA512Digest());
	    eng.init(false, (AsymmetricKeyParameter)publicKey.getActualSigningKey());
	    return new LBCSignature(eng);
	}

	@Override
	public ICipher getCipher(final ISecretKey key, final byte[] iv, final boolean encrypt) {
	    PaddedBufferedBlockCipher aes = new PaddedBufferedBlockCipher(new CBCBlockCipher(
	            new AESFastEngine()));
		    CipherParameters ivAndKey = new ParametersWithIV((KeyParameter)key.getActualKey(), iv);
		    aes.init(encrypt, ivAndKey);
		    return new LBCCipher(aes, iv);
	}

	@Override
	public String publicKeyToString(final IPublicKey publicKey){
		RSAKeyParameters encryptionKey = (RSAKeyParameters)publicKey.getActualEncryptionKey();
		String encKeyString = bigIntsToColonJoinedString(
				encryptionKey.getModulus(),
				encryptionKey.getExponent());
		RSAKeyParameters signingKey = (RSAKeyParameters)publicKey.getActualSigningKey();
		String signKeyString = bigIntsToColonJoinedString(
				signingKey.getModulus(),
				signingKey.getExponent());	
		return String.format("%s;%s", encKeyString, signKeyString);
	}
	
	@Override
	public String privateKeyToString(final IPrivateKey privateKey){
		RSAPrivateCrtKeyParameters encryptionKey = (RSAPrivateCrtKeyParameters)privateKey.getActualEncryptionKey();
		String encKeyString = bigIntsToColonJoinedString(
				encryptionKey.getModulus(),
				encryptionKey.getPublicExponent(),
				encryptionKey.getExponent(),
				encryptionKey.getP(),
				encryptionKey.getQ(),
				encryptionKey.getDP(),
				encryptionKey.getDQ(),
				encryptionKey.getQInv());
		RSAPrivateCrtKeyParameters signingKey = (RSAPrivateCrtKeyParameters)privateKey.getActualSigningKey();
		String signKeyString = bigIntsToColonJoinedString(
				signingKey.getModulus(),
				signingKey.getPublicExponent(),
				signingKey.getExponent(),
				signingKey.getP(),
				signingKey.getQ(),
				signingKey.getDP(),
				signingKey.getDQ(),
				signingKey.getQInv());
		return String.format("%s;%s", encKeyString, signKeyString);
	}
	
	@Override
	public IPrivateKey stringToPrivateKey(final String s) throws FailedCryptException{
		String[] Encryption_signing_keys = s.split(";");
		if (Encryption_signing_keys.length != 2){
			throw new FailedCryptException("The string is not a private key");
		}
		BigInteger[] encIntegers = colonJoinedStringToBigInts(Encryption_signing_keys[0]);		
		BigInteger[] signIntegers = colonJoinedStringToBigInts(Encryption_signing_keys[1]);	
		if (encIntegers.length != 8 || signIntegers.length != 8){
			throw new FailedCryptException("The string is not a private key");
		}
		return new LBCPrivateKey(
				new RSAPrivateCrtKeyParameters(
					encIntegers[0], //modulus
					encIntegers[1], //publicExponent
					encIntegers[2], //exponent
					encIntegers[3], //P
					encIntegers[4], //Q
					encIntegers[5], //DP
					encIntegers[6], //DQ
					encIntegers[7]),//QInv
				new RSAPrivateCrtKeyParameters(
					signIntegers[0], //modulus
					signIntegers[1], //publicExponent
					signIntegers[2], //exponent
					signIntegers[3], //P
					signIntegers[4], //Q
					signIntegers[5], //DP
					signIntegers[6], //DQ
					signIntegers[7]));//QInv
	}
	
	@Override
	public IPublicKey stringToPublicKey(final String s) throws FailedCryptException{
		String[] Encryption_signing_keys = s.split(";");
		if (Encryption_signing_keys.length != 2){
			throw new FailedCryptException("The string is not a public key");
		}	
		BigInteger[] encIntegers = colonJoinedStringToBigInts(Encryption_signing_keys[0]);		
		BigInteger[] signIntegers = colonJoinedStringToBigInts(Encryption_signing_keys[1]);	

		if (encIntegers.length != 2 || signIntegers.length != 2){
			throw new FailedCryptException("The string is not a public key");
		}
		return new LBCPublicKey(
				new RSAKeyParameters(false, 
					encIntegers[0], 
					encIntegers[1]),
				new RSAKeyParameters(false, 
					signIntegers[0], 
					signIntegers[1]));
	}

}

