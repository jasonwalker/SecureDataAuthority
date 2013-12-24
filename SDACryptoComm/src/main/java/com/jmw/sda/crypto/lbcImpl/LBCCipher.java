package com.jmw.sda.crypto.lbcImpl;

import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;

import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.ICipher;

public class LBCCipher implements ICipher {
	protected PaddedBufferedBlockCipher cipher;
	protected byte[] iv;
	
	public LBCCipher(PaddedBufferedBlockCipher cipher, byte[] iv) {
		this.cipher = cipher;
		this.iv = iv;
	}

	@Override
	public byte[] update(byte[] input) {
		int outputSize = this.cipher.getUpdateOutputSize(input.length);
		byte[] out = new byte[outputSize];
		int amtWritten = this.cipher.processBytes(input, 0, input.length, out, 0);
		out = fitArrayToData(amtWritten, out);
		return out;
	}

	@Override
	public byte[] update(byte[] input, int start, int length) {
		int outputSize = this.cipher.getUpdateOutputSize(length);
		byte[] out = new byte[outputSize];
		int amtWritten = this.cipher.processBytes(input, start, length, out, 0);
		out = fitArrayToData(amtWritten, out);
		return out;
	}

	@Override
	public byte[] doFinal() throws FailedCryptException {
		try {
			int outputSize = this.cipher.getOutputSize(0);
			byte[] out = new byte[outputSize];
			int amtWritten = this.cipher.doFinal(out, 0);
			out = fitArrayToData(amtWritten, out);
			return out;
		} catch (DataLengthException | IllegalStateException
				| InvalidCipherTextException e) {
			throw new FailedCryptException(e);
		}
	}

	@Override
	public byte[] doFinal(byte[] input) throws FailedCryptException {
		try {
			int outputSize = this.cipher.getOutputSize(input.length);
			byte[] out = new byte[outputSize];
			int offset = this.cipher.processBytes(input, 0, input.length, out, 0);
			offset += this.cipher.doFinal(out, offset);
			out = fitArrayToData(offset, out);
			return out;
		} catch (DataLengthException | IllegalStateException
				| InvalidCipherTextException e) {
			throw new FailedCryptException(e);
		}
	}
	/**
	 * 
	 * @param realSize actual number of bytes copied to array
	 * @param inputArray array of data
	 */
	protected static byte[] fitArrayToData(int realSize, byte[] inputArray){
		if (realSize == inputArray.length){
			return inputArray;
		}
		byte[] newOut = new byte[realSize];
		System.arraycopy(inputArray, 0, newOut, 0, realSize);
		return newOut;		
	}

	@Override
	public byte[] getIV() {
		return this.iv;
	}

}
