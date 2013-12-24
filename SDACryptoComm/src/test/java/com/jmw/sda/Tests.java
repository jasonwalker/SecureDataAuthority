package com.jmw.sda;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import com.jmw.sda.crypto.AbstractCrypto;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.ISecretKey;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.RSAKeys;
import com.jmw.sda.crypto.Utils;

public class Tests {
	protected static final int STRENGTH = 3072;
	protected static final String message = "This is a test of the emergency broadcast system";
	protected static final String ENCODING = "UTF-8";
	protected static AbstractCrypto crypto = AbstractCrypto.getCrypto();
	protected static RSAKeys myKeys;
	protected static RSAKeys othersKeys;
	protected static ISecretKey aesKey;
	static{
		try {
			myKeys = crypto.makeRSAKeys(STRENGTH);
			othersKeys = crypto.makeRSAKeys(STRENGTH);
			aesKey = crypto.makeSecretKey();
		} catch (JavaInstallationMissingComponentsException
				| FailedCryptException e) {
			e.printStackTrace();
		}
	}
	
	protected static final boolean compareTwoFiles(File f1, File f2) throws IOException{
		try(InputStream in1 = Utils.inputStreamForFile(f1);
			InputStream in2 = Utils.inputStreamForFile(f2);){
			int b1 = in1.read();
			int b2 = in2.read();
			while(b1 == b2){
				if (b1 == -1){
					return true;
				}
				b1 = in1.read();
				b2 = in2.read();
			}
			return false;
		}
	}
	protected static void addRandomDataToFile(File f, int amount) throws IOException{
		Random random = new Random();
		byte[] bytes = new byte[amount];
		random.nextBytes(bytes);
		try(OutputStream fos = Utils.outputStreamForFile(f);){
			fos.write(bytes);
		}
	}
	
	protected static byte[] randomBytes(int length){
		byte[] returnVal = new byte[length];
		Random random = new Random();
		random.nextBytes(returnVal);
		return returnVal;
	}	

	protected static String randomString(int length){
		char[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
		StringBuilder sb = new StringBuilder();
		Random random = new Random();
		for (int i = 0; i < length; i++) {
		    char c = chars[random.nextInt(chars.length)];
		    sb.append(c);
		}
		return sb.toString();
	}
	

}
