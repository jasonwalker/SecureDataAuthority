package com.jmw.sda.transport.objects;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import com.jmw.sda.crypto.AbstractCrypto;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.IPrivateKey;
import com.jmw.sda.crypto.IPublicKey;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.Utils;

public class EncryptedListItem {
	protected String fromBox;
	protected String subject;
	protected String note;
	protected List<AttachmentInfo> attachments;
	protected String timestamp;
	protected byte[] signature;
	private Boolean verified = null;
	protected static AbstractCrypto crypto = AbstractCrypto.getCrypto();
	
	protected EncryptedListItem(){
	}
	public EncryptedListItem(String fromBox, String subject, String note, List<AttachmentInfo> attachments){
		this.fromBox = fromBox;
		this.subject = subject;
		this.note = note;
		this.attachments = attachments;
		this.timestamp = Utils.getTimestamp();
	}
	
	public static EncryptedListItem putBytes(byte[] bytes) throws JavaInstallationMissingComponentsException, FailedCryptException{
		try{
			EncryptedListItem mailListData = new EncryptedListItem();
			byte[][] data = Utils.unpackBigArray(bytes);
			mailListData.fromBox = new String(data[0],Utils.ENCODING); 
			mailListData.subject =  new String(data[1],Utils.ENCODING);
			mailListData.note = new String(data[2],Utils.ENCODING);
			mailListData.attachments = unpackByteArrayIntoAttachmentInfo(data[3]);
			mailListData.timestamp = new String(data[4],Utils.ENCODING);	
			mailListData.signature = data[5];
			return mailListData;
		}catch(UnsupportedEncodingException e){
			throw new JavaInstallationMissingComponentsException(e);
		}
	}
	
	private static final List<AttachmentInfo> unpackByteArrayIntoAttachmentInfo(byte[] bytes) throws JavaInstallationMissingComponentsException, FailedCryptException{
		byte[][] attachmentBytes = Utils.unpackBigArray(bytes);
		List<AttachmentInfo> returnVals = new ArrayList<>();
		for(byte[] abs : attachmentBytes){
			returnVals.add(AttachmentInfo.fromBytes(abs));
		}
		return returnVals;
	}
	
	
	private static final byte[] packAttachmentInfoIntoByteArray(List<AttachmentInfo> attachments) throws JavaInstallationMissingComponentsException{
		byte[][] attachmentBytes = new byte[attachments.size()][];
		int counter = 0;
		for(AttachmentInfo attachment : attachments){
			attachmentBytes[counter++] = attachment.getBytes();
		}
		return Utils.packIntoBigArray(attachmentBytes);
	}
	
	/**
	 * 
	 * @param privateKey
	 * @return
	 * @throws JavaInstallationMissingComponentsException 
	 * @throws FailedCryptException 
	 * @throws UnsupportedEncodingException 
	 */
	public byte[] encrypt(IPublicKey publicKey, IPrivateKey privateKey) throws FailedCryptException, JavaInstallationMissingComponentsException{
		return crypto.encrypt(publicKey, getSignedBytes(privateKey));
	}
	
	public static EncryptedListItem decrypt(IPrivateKey privateKey, byte[] encrypted) throws FailedCryptException, 
			JavaInstallationMissingComponentsException{
		byte[] decrypted = crypto.decrypt(privateKey, encrypted);
		return putBytes(decrypted);
	}
	
	protected byte[] getSignedBytes(IPrivateKey privateKey) throws JavaInstallationMissingComponentsException, FailedCryptException{
		try {
			return Utils.packIntoBigArray(this.fromBox.getBytes(Utils.ENCODING), this.subject.getBytes(Utils.ENCODING), this.note.getBytes(Utils.ENCODING), 
					packAttachmentInfoIntoByteArray(this.attachments), this.timestamp.getBytes(Utils.ENCODING), makeSignature(privateKey));
		} catch (UnsupportedEncodingException e) {
			throw new JavaInstallationMissingComponentsException(e);
		}
	}
	
	private byte[] makeSignature(IPrivateKey privateKey) throws JavaInstallationMissingComponentsException, FailedCryptException{
		return crypto.sign(privateKey, getBytesToSign());
	}
	
	private byte[] getBytesToSign() throws JavaInstallationMissingComponentsException{
		try {
			return Utils.packIntoBigArray(this.fromBox.getBytes(Utils.ENCODING), this.subject.getBytes(Utils.ENCODING), this.note.getBytes(Utils.ENCODING), 
					packAttachmentInfoIntoByteArray(this.attachments), this.timestamp.getBytes(Utils.ENCODING));
		} catch (UnsupportedEncodingException e) {
			throw new JavaInstallationMissingComponentsException(e);
		}
	}
	
	public boolean verify(IPublicKey publicKey) throws JavaInstallationMissingComponentsException, FailedCryptException{
		if (this.verified == null){
			this.verified =  crypto.verify(publicKey, this.signature, getBytesToSign());
		}
		return this.verified;
	}
	
	public String getFromBox() {
		return this.fromBox;
	}
	public String getSubject() {
		return this.subject;
	}
	public String getNote() {
		return this.note;
	}
	public List<AttachmentInfo> getAttachments(){
		return this.attachments;
	}
	public String getTimestamp() {
		return this.timestamp;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder("MailListData--");
		sb.append("FromBox: ");
		sb.append(this.fromBox);
		sb.append(", subject: ");
		sb.append(this.subject);
		sb.append(", note: ");
		sb.append(this.note);
		sb.append(", hasAttachment: ");
		for (AttachmentInfo attachment : this.attachments){
			sb.append(attachment.getName());
			sb.append(", ");
		}
		sb.delete(sb.length()-2, sb.length()-1);
		sb.append(", timestamp: ");
		sb.append(this.timestamp);
		return sb.toString();
	}
}
