package com.jmw.sda.transport.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import com.jmw.sda.crypto.AbstractCrypto;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.ICipher;
import com.jmw.sda.crypto.IPrivateKey;
import com.jmw.sda.crypto.IPublicKey;
import com.jmw.sda.crypto.ISecretKey;
import com.jmw.sda.crypto.ISignature;
import com.jmw.sda.crypto.InvalidSignatureException;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.Utils;


public class MeasuredStream {
	protected static AbstractCrypto crypto = AbstractCrypto.getCrypto();
	public static final int CONTROL_ENDCURRENT = -1;
	public static final int CONTROL_END = -2;
	public static final int CONTROL_MORE = -3;
	public static final int CONTROL_HASH_FOLLOWS = -4;
	
	public static final void writeMore(OutputStream os) throws IOException{
		os.write(Utils.intToByteArray(CONTROL_MORE));
	}
	public static final void writeEnd(OutputStream os) throws IOException{
		os.write(Utils.intToByteArray(CONTROL_END));
	}
	public static final void writeEndCurrent(OutputStream os) throws IOException{
		os.write(Utils.intToByteArray(CONTROL_ENDCURRENT));
	}
	public static final void writeHashFollows(OutputStream os) throws IOException{
		os.write(Utils.intToByteArray(CONTROL_HASH_FOLLOWS));
	}

	public static final int readControl(InputStream is) throws IOException{
		return Utils.byteArrayToInt(Utils.readNumBytes(is, 4));
	}
	
	protected static final void write(OutputStream os, byte[] data) throws IOException{
		byte[] intBytes = Utils.intToByteArray(data.length);
		os.write(intBytes);
		os.write(data);
	}
	protected static final byte[] read(InputStream is) throws IOException{
		byte[] intBytes = Utils.readNumBytes(is, 4);
		int len = Utils.byteArrayToInt(intBytes);
		if (len < 0){
			return null;
		}
		return Utils.readNumBytes(is, len);	
	}
	
	protected static final void writeAndSign(OutputStream os, ISignature signer, byte[] data) throws IOException, FailedCryptException{
		try{
			byte[] intBytes = Utils.intToByteArray(data.length);
			os.write(intBytes);
			os.write(data);
			signer.update(intBytes);
			signer.update(data);	
		}catch(InvalidSignatureException e){
			throw new FailedCryptException(e);
		}
	}
	
	protected static final byte[] readAndVerify(InputStream is, ISignature verifier) throws IOException, InvalidSignatureException{
		byte[] intBytes = Utils.readNumBytes(is, 4);
		int len = Utils.byteArrayToInt(intBytes);
		if (len < 0){
			return null;
		}
		verifier.update(intBytes);
		byte[] total = Utils.readNumBytes(is, len);	
		verifier.update(total);
		return total;
	}
	
	public static void writeEncryptedOutput(final ISecretKey secretKey, byte[] iv, final IPrivateKey privateKey, final InputStream is, final OutputStream os) throws 
	FailedCryptException, JavaInstallationMissingComponentsException, IOException{
		try{
			ISignature signer = crypto.getSigner(privateKey);
			ICipher aesCipher = crypto.getCipher(secretKey, iv, true);
			byte[] buffer = new byte[Short.MAX_VALUE];
			int len = is.read(buffer);
			byte[] encrypted;
			while(len >= 0){
				encrypted = aesCipher.update(buffer, 0, len);
				writeAndSign(os, signer, encrypted);
				len = is.read(buffer);
			}
			encrypted = aesCipher.doFinal();
			writeAndSign(os, signer, encrypted);
			writeHashFollows(os);
			byte[] signature = signer.sign();
			write(os, signature);
			writeEndCurrent(os);	
		}catch(InvalidSignatureException e){
			throw new FailedCryptException(e);
		}

	}	
	
	public static final void readInputKeepMeasures(InputStream is, OutputStream os) throws IOException{
		byte[] intBytes = Utils.readNumBytes(is, 4);
		os.write(intBytes);
		int len = Utils.byteArrayToInt(intBytes);
		while (len != CONTROL_ENDCURRENT){
			if (len == CONTROL_HASH_FOLLOWS){
				intBytes = Utils.readNumBytes(is, 4);
				os.write(intBytes);
				len = Utils.byteArrayToInt(intBytes);
			}
			byte[] message = Utils.readNumBytes(is, len);
			os.write(message);	
			intBytes = Utils.readNumBytes(is, 4);
			os.write(intBytes);
			len = Utils.byteArrayToInt(intBytes);
		}	
	}
	
	public static final boolean readEncryptedInput(final ISecretKey secretKey, byte[] iv, final IPublicKey publicKey,
			InputStream is, OutputStream os) throws IOException, JavaInstallationMissingComponentsException, FailedCryptException, 
			InvalidSignatureException{
		ISignature verifier = crypto.getVerifier(publicKey);
		ICipher aesCipher = crypto.getCipher(secretKey, iv, false);
		byte[] message = readAndVerify(is, verifier);
		while (message != null){
			os.write(aesCipher.update(message));
			message = readAndVerify(is, verifier);
		}
		os.write(aesCipher.doFinal());
		byte[] signature = read(is);
		return verifier.verify(signature);
	}
	
	public static final void writeOutput(InputStream is, OutputStream os, MessageDigest digest) throws IOException {
		byte[] buffer = new byte[Short.MAX_VALUE];
		int len = is.read(buffer);
		while(len >= 0){
			byte[] intBytes = Utils.intToByteArray(len);
			os.write(intBytes);
			os.write(buffer, 0, len);
			if (digest != null){
				digest.update(intBytes);
				digest.update(buffer, 0, len);
			}
			len = is.read(buffer);
		}
		writeEndCurrent(os);
	}	
	
	public static final void writeOutput(byte[] input, OutputStream os, MessageDigest digest) throws IOException {
		int len = input.length > Short.MAX_VALUE ? Short.MAX_VALUE : input.length;
		int counter = 0;
		while(counter < input.length){
			byte[] intBytes = Utils.intToByteArray(len);
			os.write(intBytes);
			os.write(input, counter, len);
			if (digest != null){
				digest.update(intBytes);
				digest.update(input, counter, len);
			}
			counter += len;
			len = (input.length - counter) > Short.MAX_VALUE ? Short.MAX_VALUE : input.length - counter;
		}
		writeEndCurrent(os);
		os.flush();
	}	
	
	public static final void writeOutput(byte[] input, OutputStream os) throws IOException {
		writeOutput(input, os, null);
	}

	public static final void readInput(InputStream is, OutputStream os, MessageDigest digest) throws IOException{
		byte[] intBytes = Utils.readNumBytes(is, 4);
		int len = Utils.byteArrayToInt(intBytes);
		while (len >= 0){
			byte[] message = Utils.readNumBytes(is, len);
			os.write(message);	
			if (digest != null){
				digest.update(intBytes);
				digest.update(message);
			}
			intBytes = Utils.readNumBytes(is, 4);
			len = Utils.byteArrayToInt(intBytes);
		}
	}
	
	public static final byte[] readInput(InputStream is, MessageDigest digest) throws IOException{
		byte[] intBytes = Utils.readNumBytes(is, 4);
		int len = Utils.byteArrayToInt(intBytes);
		
		List<byte[]> byteHolder = new ArrayList<>();
		while (len >= 0){
			byte[] message = Utils.readNumBytes(is, len);
			byteHolder.add(message);
			if (digest != null){
				digest.update(intBytes);
				digest.update(message);
			}
			len = Utils.byteArrayToInt(Utils.readNumBytes(is, 4));
		}	
		return Utils.listToBigArray(byteHolder);
	}
	
	public static final byte[] readInput(InputStream is) throws IOException{
		return readInput(is, (MessageDigest) null);
	}
}
