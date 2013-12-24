package com.jmw.sda.transport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.jmw.sda.crypto.AbstractCrypto;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.IPrivateKey;
import com.jmw.sda.crypto.IPublicKey;
import com.jmw.sda.crypto.ISecretKey;
import com.jmw.sda.crypto.InvalidSignatureException;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.Utils;
import com.jmw.sda.transport.objects.AttachmentInfo;
import com.jmw.sda.transport.objects.EncryptedListItem;
import com.jmw.sda.transport.objects.ServerMailMetaData;
import com.jmw.sda.transport.objects.TotalListItem;
import com.jmw.sda.transport.stream.MeasuredStream;

public class ClientDataMover {
	protected static AbstractCrypto crypto = AbstractCrypto.getCrypto();
	/**
	 * Sends over inputstream timestamp, toMailbox, fromMailbox  of combined earlier data unencrypted.  This
	 * is the info that the server uses to store the data sent after this method called.
	 * 
	 * Then sends secret key and initialization vector encrypted with sender's private key
	 * 
	 * Then returns an encryption stream to be used to send the large data
	 * Closes output stream
	 * @throws InvalidSignatureException 
	 * 
	 */
	public static void sendToServer(final IPrivateKey privateKey, final IPublicKey[] publicKeys, 
			final String[] toBoxes, final String fromBox, final String subject, 
			final String note, OutputStream unencryptedOutputStream, List<AttachmentInfo> attachments) throws JavaInstallationMissingComponentsException, FailedCryptException, IOException{
		String timestamp = Utils.getTimestamp();
		ServerMailMetaData serverData = new ServerMailMetaData(privateKey, timestamp, toBoxes, fromBox);
		MeasuredStream.writeOutput(serverData.getBytes(), unencryptedOutputStream);
		EncryptedListItem mailListData = new EncryptedListItem(fromBox, subject, note, attachments);
		for (IPublicKey publicKey : publicKeys){
			MeasuredStream.writeMore(unencryptedOutputStream);
			MeasuredStream.writeOutput(mailListData.encrypt(publicKey, privateKey), unencryptedOutputStream);
		}
		MeasuredStream.writeEnd(unencryptedOutputStream);
	}

	public static final List<AttachmentInfo> sendAttachments(OutputStream unencryptedOutputStream, 
			IPrivateKey privateKey, List<File> attachments) throws JavaInstallationMissingComponentsException, FailedCryptException, IOException{
		List<AttachmentInfo> attachmentInfo = new ArrayList<>();
		if (attachments != null && attachments.size() > 0){
			MeasuredStream.writeMore(unencryptedOutputStream);
			for(File attachment : attachments){
				if (attachment == null){
					continue;
				}
				MeasuredStream.writeMore(unencryptedOutputStream);
				try(InputStream fis = Utils.inputStreamForFile(attachment)){
					ISecretKey secretKey = crypto.makeSecretKey();
					byte[] iv = AbstractCrypto.genIV();
					MeasuredStream.writeEncryptedOutput(secretKey, iv, privateKey, fis, unencryptedOutputStream);
					attachmentInfo.add(new AttachmentInfo(attachment.getName(), secretKey, iv));
				}
			}
		}
		MeasuredStream.writeEnd(unencryptedOutputStream);
		return attachmentInfo;
	}
	
	/**
	 * CLIENT RECEIVING unchanged data from server
	 * @throws BadMethodCallException 
	 */
	public static boolean writeAttachmentToFileAndVerifyDigest(final InputStream unencryptedIputStream, final File outputFile,  
			final IPublicKey publicKey, final AttachmentInfo attachmentInfo) throws IOException, JavaInstallationMissingComponentsException, 
			FailedCryptException, InvalidSignatureException{
		outputFile.createNewFile();
		try(OutputStream fos = Utils.outputStreamForFile(outputFile);){
			return MeasuredStream.readEncryptedInput(attachmentInfo.getSecretKey(), attachmentInfo.getIV(), publicKey, unencryptedIputStream,fos);
		} 
	}
	
	public static List<TotalListItem> receiveList(InputStream inputStream) throws IOException, JavaInstallationMissingComponentsException, FailedCryptException{
		List<TotalListItem> mailInfos = new ArrayList<>();
		int control = MeasuredStream.readControl(inputStream);
		while (control == MeasuredStream.CONTROL_MORE){
			byte[] input = MeasuredStream.readInput(inputStream);
			mailInfos.add(TotalListItem.putBytes(input));
			control = MeasuredStream.readControl(inputStream);
		}
		return mailInfos;
	}
	
	public static String getMailbox(InputStream inputStream, String mailboxName, IPublicKey serversKey) throws InvalidSignatureException, JavaInstallationMissingComponentsException, FailedCryptException, IOException{
		byte[] kAndS = MeasuredStream.readInput(inputStream);
		String[] keyAndSig = Utils.unpackIntoStringArray(kAndS);
		if (!crypto.verify(serversKey, keyAndSig[1], mailboxName, keyAndSig[0])){
			throw new InvalidSignatureException("The server returned a bad mailbox. Someone may be tampering with your communication");
		}
		return keyAndSig[0];		
	}
	
}
