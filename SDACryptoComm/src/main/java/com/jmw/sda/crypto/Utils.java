package com.jmw.sda.crypto;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.Base64;

public class Utils {
	public static final long MILLSINDAY = 86400000L;
	public static final int BUFSIZE = 32768;// 16384;
	public static final String dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	public static final String ENCODING = "UTF-8";
	
	public static String dateToString(Date date){
		//simpleDateformat not thread safe
		return new SimpleDateFormat(dateFormat).format(date);
	}

	
	public static InputStream inputStreamForFile(File file) throws FileNotFoundException{
		return new BufferedInputStream(new FileInputStream(file));
	}
	
	public static OutputStream outputStreamForFile(File file) throws FileNotFoundException{
		return new BufferedOutputStream(new FileOutputStream(file));
	}
	
	public static final String join(String ...str){
		StringBuilder sb = new StringBuilder();
		for (String s : str){
			sb.append(s);
		}
		return sb.toString();
	}
	
	public static final void writeFromInputToOutputStream(final InputStream is, final OutputStream os) throws IOException{
		byte[] buffer = new byte[BUFSIZE];
		int amt = is.read(buffer);
		while (amt != -1){
			os.write(buffer, 0, amt);
			amt = is.read(buffer);
		}
	}	
	
	public static String byteArrayToString(byte[] b) throws JavaInstallationMissingComponentsException{
		try {
			return new String(b, Utils.ENCODING);
		} catch (UnsupportedEncodingException e) {
			throw new JavaInstallationMissingComponentsException(e);
		}
	}
	
	public static String byteArrayToB64(byte[] b){
		return Base64.encodeBase64String(b);
	}
	public static byte[] b64ToByteArray(String b64){
		return Base64.decodeBase64(b64);
	}
	
	public static String getTimestamp() {
		return dateToString(new Date());
	}
	public static Date stringToDate(String dateString){
		try{
			return new SimpleDateFormat(dateFormat).parse(dateString);
		}catch(ParseException pe){
			return null;
		}
	}	
	
	public static String ppStackTrace(Exception e){
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();		
	}
	
	public static final String bigIntToString(BigInteger b){
		return Base64.encodeBase64URLSafeString(b.toByteArray());
	}
	
	public static final BigInteger stringToBigInteger(String s){
		return new BigInteger(Base64.decodeBase64(s));
	}
	
	//guarantees to pull count bytes and only count bytes from input stream
	public static byte[] readNumBytes(InputStream is, int numBytes) throws IOException{
		byte[] retVal = new byte[numBytes];
		int curLen = is.read(retVal);
		int totalLen = curLen;
		while (totalLen < numBytes){
			curLen = is.read(retVal, totalLen, numBytes-totalLen);
			if (curLen == -1 && totalLen < numBytes){
				throw new IOException("Could not get specified number of bytes before EOS");
			}
			totalLen += curLen;
		}
		return retVal;
	}
	
	public static byte[] readByteArray(InputStream is) throws IOException{
		byte[] sizeArray = readNumBytes(is, 4);
		int size = Utils.byteArrayToInt(sizeArray);
		return readNumBytes(is, size);
	}

	
	public static void writeByteArray(OutputStream os, byte[] data) throws IOException{
		byte[] sizeArray = Utils.intToByteArray(data.length);
		os.write(sizeArray);
		os.write(data);
	}	
	
	public static final short byteArrayToShort(byte[] b) {
		short returnVal = 0;
		for (int i = 0 ; i < 2 ; i++){
			returnVal += (b[i] & 0xFF) << (1-i) * 8;
		}
		return returnVal;
	}
	
	public static final byte[] shortToByteArray(short value) {
		byte[] returnVal = new byte[2];
		for (int i = 0 ; i < 2 ; i++){
			returnVal[i] = (byte)(value >>> ((1-i) * 8));
		}
		return returnVal;
	}	
	

	public static final int byteArrayToInt(byte[] b) {
		int returnVal = 0;
		for (int i = 0 ; i < 4 ; i++){
			returnVal += (b[i] & 0xFF) << (3-i) * 8;
		}
		return returnVal;
	}
	
	public static final byte[] intToByteArray(int value) {
		byte[] returnVal = new byte[4];
		for (int i = 0 ; i < 4 ; i++){
			returnVal[i] = (byte)(value >>> ((3-i) * 8));
		}
		return returnVal;
	}	
	
	public static final long byteArrayToLong(byte [] b) {
		long returnVal = 0;
		for (int i = 0 ; i < 8 ; i++){
			returnVal += ((long)(b[i] & 0xFF)) << (7-i) * 8;
		}
		return returnVal;
	}
	
	public static final byte[] longToByteArray(long value) {
		byte[] returnVal = new byte[8];
		for (int i = 0 ; i < 8 ; i++){
			returnVal[i] = (byte)(value >>> ((7-i) * 8));
		}
		return returnVal;
	}	
	
	public static final byte[] packStringsIntoBigArray(String ...vals) throws JavaInstallationMissingComponentsException{
		byte[][] byteVals = new byte[vals.length][];
		for (int i = 0 ; i < vals.length ; i++){
			try {
				byteVals[i] = vals[i].getBytes(Utils.ENCODING);
			} catch (UnsupportedEncodingException e) {
				throw new JavaInstallationMissingComponentsException(e);
			}
		}
		return packIntoBigArray(byteVals);
	}
	
	public static final String[] unpackIntoStringArray(byte[] val) throws JavaInstallationMissingComponentsException, FailedCryptException  {
		try{
			byte[][] byteArray = unpackBigArray(val);
			String[] stringArray = new String[byteArray.length];
			for (int i = 0 ; i < byteArray.length ; i++){
				stringArray[i] = new String(byteArray[i], Utils.ENCODING);
			}
			return stringArray;
		}catch(UnsupportedEncodingException e){
			throw new JavaInstallationMissingComponentsException(e);
		}
	}
	
	public static final byte[] packIntoBigArray(byte[] ...vals){
		int totalSize = 4;
		for (byte[] val : vals){
			totalSize += 4;
			totalSize += val.length;
		}
		byte[] returnVal = new byte[totalSize];
		//first copy in number of byte arrays passed in
		System.arraycopy(Utils.intToByteArray(vals.length), 0, returnVal, 0, 4);
		int currentLoc = 4;
		for (byte[] val : vals){
			System.arraycopy(Utils.intToByteArray(val.length), 0, returnVal, currentLoc, 4);
			currentLoc += 4;
			System.arraycopy(val, 0, returnVal, currentLoc, val.length);
			currentLoc += val.length;
		}		
		return returnVal;
	}
	
	public static final byte[][] unpackBigArray(byte[] val, int startLoc) throws FailedCryptException{
		byte[] numberOfArrays = new byte[4];
		System.arraycopy(val, startLoc, numberOfArrays, 0, 4);
		int currentLoc = startLoc + 4;
		int numArrays = Utils.byteArrayToInt(numberOfArrays);
		//checking to make sure passed-in data didn't get messed up, 2000 is simply larger than number ever needed
		if (numArrays > 2000 || numArrays < 0){
			throw new FailedCryptException("Array failed to unpack correctly");
		}
		byte[][] returnVal = new byte[numArrays][];
		byte[] sizeArray = new byte[4];
		
		for (int i = 0 ; i < numArrays ; i++){
			System.arraycopy(val, currentLoc, sizeArray, 0, 4);
			currentLoc += 4;
			int size = Utils.byteArrayToInt(sizeArray);
			byte[] currentArray = new byte[size];
			System.arraycopy(val, currentLoc, currentArray, 0, size);
			currentLoc += size;
			returnVal[i] = currentArray;
		}
		return returnVal;
	}
	public static final byte[][] unpackBigArray(byte[] val) throws FailedCryptException{
		return unpackBigArray(val, 0);
	}
	
	public static final byte[] listToBigArray(List<byte[]> bytes){
		int totalLength = 0;
		for (byte[] b : bytes){
			totalLength += b.length;
		}
		byte[] retVal = new byte[totalLength];
		int currentLoc = 0;
		for (byte[] b : bytes){
			System.arraycopy(b, 0, retVal, currentLoc, b.length);
			currentLoc += b.length;
		}
		return retVal;
	}
	
	public static byte[] readFully(InputStream input) throws IOException{
	    byte[] buffer = new byte[8192];
	    int bytesRead;
	    ByteArrayOutputStream output = new ByteArrayOutputStream();
	    while ((bytesRead = input.read(buffer)) != -1) {
	        output.write(buffer, 0, bytesRead);
	    }
	    return output.toByteArray();
	}
	
	
}

