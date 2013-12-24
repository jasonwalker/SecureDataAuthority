package com.jmw.sda.dbProviders;

import java.io.UnsupportedEncodingException;
import java.util.Date;

import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.Utils;

public class Mailbox {
	/*
	 * name of the mailbox
	 */
	protected String name;
	/*
	 * public key to encrypt data for this mailbox
	 */
	protected String publicKey;
	/*
	 * date that the mailbox was created
	 */
	protected String createDate;
	/*
	 * date that last message was uploaded to this mailbox
	 */
	protected String lastUpload;
	/*
	 * number of bytes received during this period (length TBD)
	 */
	protected Long receivebytes;
	/*
	 * number of bytes sent during this period (length TBD)
	 */
	protected Long sendbytes;
	
	protected String serverSignature;
	
	protected Mailbox(){
	}
	
	public Mailbox(String name, String publicKey, String serverSignature){
		this.name = name;
		this.publicKey = publicKey;
		this.serverSignature = serverSignature;
		String date = Utils.getTimestamp();
		this.createDate = date;
		this.lastUpload = date;
		this.receivebytes = 0L;
		this.sendbytes = 0L;
	}
	
	public byte[] getBytes() throws JavaInstallationMissingComponentsException {
		try{
			return Utils.packIntoBigArray(
					this.name.getBytes(Utils.ENCODING),
					this.publicKey.getBytes(Utils.ENCODING), 
					this.serverSignature.getBytes(Utils.ENCODING),
					this.createDate.getBytes(Utils.ENCODING),
					this.lastUpload.getBytes(Utils.ENCODING),
					Utils.longToByteArray(this.receivebytes),
					Utils.longToByteArray(this.sendbytes));
		}catch(UnsupportedEncodingException e){
			throw new JavaInstallationMissingComponentsException(e);
		}
	}
	
	public static Mailbox putBytes(byte[] in) throws JavaInstallationMissingComponentsException, FailedCryptException{
		byte[][] back = Utils.unpackBigArray(in);
		Mailbox mailbox = new Mailbox();
		try {
			mailbox.name = new String(back[0], Utils.ENCODING);
			mailbox.publicKey = new String(back[1], Utils.ENCODING);
			mailbox.serverSignature = new String(back[2], Utils.ENCODING);
			mailbox.createDate = new String(back[3], Utils.ENCODING);
			mailbox.lastUpload = new String(back[4], Utils.ENCODING);
			mailbox.receivebytes = Utils.byteArrayToLong(back[5]);
			mailbox.sendbytes = Utils.byteArrayToLong(back[6]);
			return mailbox;
		} catch (UnsupportedEncodingException e) {
			throw new JavaInstallationMissingComponentsException(e);
		}
	}
	
	
	public String getPublicKey() {
		return this.publicKey;
	}
	
	public String getServerSignature(){
		return this.serverSignature;
	}

	public String getCreateDate() {
		return this.createDate;
	}

	public String getLastUpload() {
		return this.lastUpload;
	}
	
	public void setLastUpload(Date d) {
		this.lastUpload = Utils.dateToString(d);
	}


	public Long getReceivebytes() {
		return this.receivebytes;
	}


	public Long getSendbytes() {
		return this.sendbytes;
	}

	public String getName() {
		return this.name;
	}	
	
}
