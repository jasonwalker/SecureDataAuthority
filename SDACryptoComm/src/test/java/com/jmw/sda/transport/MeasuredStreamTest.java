package com.jmw.sda.transport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.jmw.sda.Tests;
import com.jmw.sda.crypto.AbstractCrypto;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.InvalidSignatureException;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.Utils;
import com.jmw.sda.transport.stream.MeasuredStream;

public class MeasuredStreamTest extends Tests {
	protected static File testDir;
	protected static File randData;
	protected static File encryptedData;
	protected static File savedData;
	protected static File decryptedData;
	protected static final int RANDOM_BYTE_AMOUNT = 65536;
	
	
	@BeforeClass
	public static void setUp() throws IOException{
		testDir = new File("./MeasuredStreamTest");
		testDir.mkdirs();
		encryptedData = File.createTempFile("measuredstream", ".encrypted", testDir);
		randData = File.createTempFile("measuredstream", ".out", testDir);
		savedData = File.createTempFile("measuredstream", ".saved", testDir);
		decryptedData = File.createTempFile("measuredstream", ".decrypted", testDir);
		addRandomDataToFile(randData, RANDOM_BYTE_AMOUNT);
	}
	
	@AfterClass
	public static void cleanup() throws IOException{
		FileUtils.deleteDirectory(testDir);
	}
	
	@SuppressWarnings("static-method")
	@Test
	public void testSendReceive() throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		MeasuredStream.writeOutput(message.getBytes(ENCODING), baos);
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		byte[] retVal = MeasuredStream.readInput(bais);
		assertEquals("Measured stream did not recreate string", message, new String(retVal, ENCODING));
	}

	@SuppressWarnings("static-method")
	@Test
	public void testEncryptedWrite() throws IOException, FailedCryptException, JavaInstallationMissingComponentsException, InvalidSignatureException{
		byte[] iv = AbstractCrypto.genIV();
		try(InputStream plainDataIS = Utils.inputStreamForFile(randData);
		    OutputStream encryptedOS = Utils.outputStreamForFile(encryptedData);){
			MeasuredStream.writeEncryptedOutput(aesKey, iv, myKeys.getPrivateKey(), plainDataIS, encryptedOS);
		}
		try(InputStream encryptedInput = Utils.inputStreamForFile(encryptedData);
			OutputStream savingEncryptedOS = Utils.outputStreamForFile(savedData);){
			MeasuredStream.readInputKeepMeasures(encryptedInput, savingEncryptedOS);
		}
		try(InputStream toDecryptIS = Utils.inputStreamForFile(savedData);
		    OutputStream decryptedOS = Utils.outputStreamForFile(decryptedData);){
			boolean verified = MeasuredStream.readEncryptedInput(aesKey, iv, myKeys.getPublicKey(), toDecryptIS, decryptedOS);
			assertTrue("Decrypted data's signature failed to verify", verified);
		}
		assertTrue("Measured stream data did not decrypt properly", compareTwoFiles(decryptedData, randData));
	}
	
}
