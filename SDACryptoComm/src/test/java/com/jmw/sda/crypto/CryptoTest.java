package com.jmw.sda.crypto;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.jmw.sda.Tests;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.IPrivateKey;
import com.jmw.sda.crypto.IPublicKey;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;

public class CryptoTest extends Tests {

	@SuppressWarnings("static-method")
	@Test
	public void testSignVerify() throws JavaInstallationMissingComponentsException, FailedCryptException{
		String signature = crypto.sign(myKeys.getPrivateKey(), message);
		assertTrue("Signature failed to validate", crypto.verify(myKeys.getPublicKey(), signature, message));
	}
	
	@SuppressWarnings("static-method")
	@Test
	public void testFailSignVerify() throws JavaInstallationMissingComponentsException, FailedCryptException{
		String signature = crypto.sign(myKeys.getPrivateKey(),  message);
		assertFalse("Signature was incorrectly validated", crypto.verify(othersKeys.getPublicKey(), signature, message));
	}
	
	@SuppressWarnings("static-method")
	@Test
	public void testEncryptString() throws 
		JavaInstallationMissingComponentsException,FailedCryptException{
		String randMessage = randomString(2000);
		String enc = crypto.encrypt(myKeys.getPublicKey(), randMessage);
		assertEquals(crypto.decrypt(myKeys.getPrivateKey(), enc), randMessage);
	}
	
	@SuppressWarnings("static-method")
	public void testFailedEncryptString() throws FailedCryptException, JavaInstallationMissingComponentsException{
		String randMessage = randomString(2000);
		String enc = crypto.encrypt(myKeys.getPublicKey(), randMessage);
		assertNotEquals(crypto.decrypt(othersKeys.getPrivateKey(), enc), randMessage);		
	}

	@SuppressWarnings("static-method")
	@Test
	public void reconstituteRSAKeys() throws JavaInstallationMissingComponentsException, FailedCryptException{
		String priv = crypto.privateKeyToString(myKeys.getPrivateKey());
		IPrivateKey privateKey = crypto.stringToPrivateKey(priv);
		String signature = crypto.sign(privateKey, message);
		String pub = crypto.publicKeyToString(myKeys.getPublicKey());
		IPublicKey publicKey = crypto.stringToPublicKey(pub);
		assertTrue("Signature failed to validate on reconstituted keys", crypto.verify(publicKey, signature, message));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testEncryptBytes() throws FailedCryptException, JavaInstallationMissingComponentsException {
		byte[] secretMessage = randomBytes(4000);
		byte[] encrypted = crypto.encrypt(myKeys.getPublicKey(), secretMessage);
		byte[] decrypted = crypto.decrypt(myKeys.getPrivateKey(), encrypted);
		assertArrayEquals("decryption did not work", secretMessage, decrypted);
	}
	
	
}

