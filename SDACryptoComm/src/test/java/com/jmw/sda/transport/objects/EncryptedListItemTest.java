package com.jmw.sda.transport.objects;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

import com.jmw.sda.Tests;
import com.jmw.sda.crypto.AbstractCrypto;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.transport.objects.AttachmentInfo;
import com.jmw.sda.transport.objects.EncryptedListItem;

public class EncryptedListItemTest extends Tests {
	protected static String fromBox;
	protected static String subject;
	protected static String note;
	
	@BeforeClass
	public static void setup(){
		fromBox = randomString(50);
		subject = randomString(80);
		note = randomString(400);
	}
	
	@SuppressWarnings("static-method")
	@Test
	public void TestItem() throws FailedCryptException, JavaInstallationMissingComponentsException{
		List<AttachmentInfo> attachments = new ArrayList<>();
		for (int i = 0 ; i < 10 ; i++){
			AttachmentInfo attachment = new AttachmentInfo(Integer.valueOf(i).toString(), crypto.makeSecretKey(), AbstractCrypto.genIV());
			attachment.setServerId(Integer.valueOf(i).toString());
			attachments.add(attachment);
		}
		EncryptedListItem originalEli = new EncryptedListItem(fromBox, subject, note,  attachments);
		byte[] encrypted = originalEli.encrypt(othersKeys.getPublicKey(), myKeys.getPrivateKey());
		EncryptedListItem decryptedEli = EncryptedListItem.decrypt(othersKeys.getPrivateKey(), encrypted);
		assertEquals("EncryptedListItem failed to decrypt properly", originalEli.toString(), decryptedEli.toString());
		assertTrue("EncryptedListItem failed to verify correctly", decryptedEli.verify(myKeys.getPublicKey()));
		
	}
	
	@SuppressWarnings("static-method")
	@Test
	public void TestItemWrongDecryptionKey() throws FailedCryptException, JavaInstallationMissingComponentsException{
		List<AttachmentInfo> attachments = new ArrayList<>();
		for (int i = 0 ; i < 10 ; i++){
			AttachmentInfo attachment = new AttachmentInfo(Integer.valueOf(i).toString(), crypto.makeSecretKey(), AbstractCrypto.genIV());
			attachment.setServerId(Integer.valueOf(i).toString());
			attachments.add(attachment);
		}
		EncryptedListItem originalEli = new EncryptedListItem(fromBox, subject, note,  attachments);
		byte[] encrypted = originalEli.encrypt(othersKeys.getPublicKey(), myKeys.getPrivateKey());
		try{
			EncryptedListItem.decrypt(myKeys.getPrivateKey(), encrypted);
			fail("Should not have been able to decrypt list item");
		}catch(FailedCryptException e){
			//expected
		}
	}
	
	@SuppressWarnings("static-method")
	@Test
	public void TestItemWrongVerificationKey() throws FailedCryptException, JavaInstallationMissingComponentsException{
		List<AttachmentInfo> attachments = new ArrayList<>();
		for (int i = 0 ; i < 10 ; i++){
			AttachmentInfo attachment = new AttachmentInfo(Integer.valueOf(i).toString(), crypto.makeSecretKey(), AbstractCrypto.genIV());
			attachment.setServerId(Integer.valueOf(i).toString());
			attachments.add(attachment);
		}
		EncryptedListItem originalEli = new EncryptedListItem(fromBox, subject, note,  attachments);
		byte[] encrypted = originalEli.encrypt(othersKeys.getPublicKey(), myKeys.getPrivateKey());
		EncryptedListItem decryptedEli = EncryptedListItem.decrypt(othersKeys.getPrivateKey(), encrypted);
		assertEquals("EncryptedListItem failed to decrypt properly", originalEli.toString(), decryptedEli.toString());
		assertFalse("EncryptedListItem should have failed verification", decryptedEli.verify(othersKeys.getPublicKey()));
	}

}
