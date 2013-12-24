package com.jmw.sda.transport.objects;

import java.io.UnsupportedEncodingException;

import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.IPrivateKey;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.Utils;

public class TotalListItem{
	private String id;
	private String timestamp;
	private String fromIpAddress;
	private byte[] encryptedMailListData;
	protected EncryptedListItem decrypted;
	
	public TotalListItem(String id, String timestamp, String fromIpAddress, byte[] encryptedMailListData){
		this.id = id;
		this.timestamp = timestamp;
		this.fromIpAddress = fromIpAddress;
		this.encryptedMailListData = encryptedMailListData;
	}
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder("id: ");
		sb.append(this.id);
		sb.append(", timestamp: ");
		sb.append(this.timestamp);
		sb.append(", fromIPAddress: ");
		sb.append(this.fromIpAddress);
		return sb.toString();
	}
	
	public TotalListItem(){
	}	
	
	public byte[] getBytes() throws JavaInstallationMissingComponentsException{
		try{
			return Utils.packIntoBigArray(
					this.id.getBytes(Utils.ENCODING),
					this.timestamp.getBytes(Utils.ENCODING), 
					this.fromIpAddress.getBytes(Utils.ENCODING),
					this.encryptedMailListData);
		}catch(UnsupportedEncodingException e){
			throw new JavaInstallationMissingComponentsException(e);
		}
	}
	
	public static TotalListItem putBytes(byte[] in) throws JavaInstallationMissingComponentsException, FailedCryptException{
		byte[][] back = Utils.unpackBigArray(in);
		try {
			TotalListItem mailInfo = new TotalListItem();
			mailInfo.id = new String(back[0], Utils.ENCODING);
			mailInfo.timestamp = new String(back[1], Utils.ENCODING);
			mailInfo.fromIpAddress = new String(back[2], Utils.ENCODING);
			mailInfo.encryptedMailListData = back[3];
			return mailInfo;
		} catch (UnsupportedEncodingException e) {
			throw new JavaInstallationMissingComponentsException(e);
		}
	}
	
	public String getId() {
		return this.id;
	}

	public String getFromIPAddress() {
		return this.fromIpAddress;
	}

	public String getTimestamp() {
		return this.timestamp;
	}
	
	public EncryptedListItem getEncryptedListItem(IPrivateKey myKey) throws FailedCryptException, JavaInstallationMissingComponentsException{
		if (this.decrypted == null){
			this.decrypted =  EncryptedListItem.decrypt(myKey, this.encryptedMailListData);
		}
		return this.decrypted;
	}

}
