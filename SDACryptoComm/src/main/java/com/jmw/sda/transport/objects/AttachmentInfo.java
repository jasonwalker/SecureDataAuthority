package com.jmw.sda.transport.objects;

import java.io.UnsupportedEncodingException;

import com.jmw.sda.crypto.AbstractCrypto;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.ISecretKey;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.Utils;

/**
 * This is the data identifying an attachment on the server and secret info needed to decrypt that attachment
 * It is encrypted when it becomes part of an EncryptedOutputEntity
 * @author jwalker
 *
 */
public class AttachmentInfo extends Object{
	protected ISecretKey secretKey;
	protected String serverId;
	protected String name;
	protected byte[] iv;
	public AttachmentInfo(String name, ISecretKey secretKey, byte[] iv) {
		this.secretKey = secretKey;
		this.name = name;
		this.iv = iv;
	}
	
	@Override
	public String toString(){
		return this.name;
	}

	public String getName(){
		return this.name;
	}
	public ISecretKey getSecretKey() {
		return this.secretKey;
	}
	public void setServerId(String serverId) {
		this.serverId = serverId;
	}
	public String getServerId() {
		return this.serverId;
	}
	public byte[] getIV(){
		return this.iv;
	}
	public byte[] getBytes() throws JavaInstallationMissingComponentsException{
		try {
			if (this.serverId == null){
				throw new NullPointerException("Cannot call getBytes() on AttachmentInfo until server id has been assigned");
			}
			return Utils.packIntoBigArray(this.name.getBytes(Utils.ENCODING), this.secretKey.getBytes(), this.iv, this.serverId.getBytes(Utils.ENCODING));
		} catch (UnsupportedEncodingException e) {
			throw new JavaInstallationMissingComponentsException(e);
		}
	}
	
	public static final AttachmentInfo fromBytes(byte[] bytes) throws JavaInstallationMissingComponentsException, FailedCryptException {
		byte[][] a = Utils.unpackBigArray(bytes);
		try {
			String name = new String(a[0], Utils.ENCODING);
			ISecretKey secretKey = AbstractCrypto.getCrypto().bytesToSecretKey(a[1]);
			byte[] iv = a[2];
			AttachmentInfo info = new AttachmentInfo(name, secretKey, iv);
			String id = new String(a[3], Utils.ENCODING);
			info.setServerId(id);
			return info;
		} catch (UnsupportedEncodingException e) {
			throw new JavaInstallationMissingComponentsException(e);
		}
	}

}
