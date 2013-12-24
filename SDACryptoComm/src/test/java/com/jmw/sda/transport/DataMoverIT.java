package com.jmw.sda.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jmw.sda.Tests;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.IPublicKey;
import com.jmw.sda.crypto.InvalidSignatureException;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.Utils;
import com.jmw.sda.transport.ClientDataMover;
import com.jmw.sda.transport.ServerDataMover;
import com.jmw.sda.transport.objects.AttachmentInfo;
import com.jmw.sda.transport.objects.EncryptedListItem;
import com.jmw.sda.transport.objects.ServerMailMetaData;
import com.jmw.sda.transport.objects.TotalListItem;

public class DataMoverIT extends Tests {
	public static final int RANDOM_BYTE_AMOUNT = 65536;
	public static final int NUMBER_ATTACHMENTS = 4;
	protected static File testDir = new File("./DATAMOVERTEST");
	protected static final String toBox = "toBox";
	protected static final String fromBox = "fromBox";
	protected static final String subject = "subject";
	protected static final String note = "note";
	protected static final String listItemID = "1";
	protected static final String listItemIP = "fromIPAddress";
	protected static final String listItemTimestamp = "timestamp";
	protected static File transitSideDir;
	protected static File clientSideDir;
	protected static File serverSideDir;
	protected static File clientMailOut;
	protected static File clientAttachmentOut;
	protected static File serverMailOut;
	protected static File serverAttachmentOut;
	protected static File serverListOut;
	protected static File listTemp;
	protected static File a1,a2,a3,a4;
	protected static List<File> attachmentsList = new ArrayList<>(NUMBER_ATTACHMENTS);
	protected static List<File> serverAttachmentFiles = new ArrayList<>(NUMBER_ATTACHMENTS);
	protected static List<File> clientAttachmentFiles = new ArrayList<>(NUMBER_ATTACHMENTS);

	@BeforeClass
	public static void setUp() throws IOException{
		transitSideDir = new File(testDir, "transit");
		transitSideDir.mkdirs();
		clientSideDir = new File(testDir, "clientSide");
		clientSideDir.mkdirs();
		serverSideDir = new File(testDir, "serverSide");
		serverSideDir.mkdirs();
		clientMailOut = File.createTempFile("clientMail", ".out", transitSideDir);
		clientAttachmentOut = File.createTempFile("clientAttachment", ".out", transitSideDir);
		serverMailOut = File.createTempFile("server", ".out",serverSideDir);
		serverAttachmentOut = File.createTempFile("serverattachment", ".out", serverSideDir);
		serverListOut = File.createTempFile("serverlist", ".out", serverSideDir);
		for (int i = 0 ; i < NUMBER_ATTACHMENTS ; i++){
			File f = File.createTempFile(i +"attachment", ".in", testDir);
			addRandomDataToFile(f, RANDOM_BYTE_AMOUNT);
			attachmentsList.add(f);
		}
		for (int i = 0 ; i < NUMBER_ATTACHMENTS ; i++){
			File serverFile = File.createTempFile(i +"attachment", ".out", serverSideDir);
			addRandomDataToFile(serverFile, RANDOM_BYTE_AMOUNT);
			serverAttachmentFiles.add(serverFile);
			
			File clientFile = File.createTempFile(i +"attachment", ".out", clientSideDir);
			clientAttachmentFiles.add(clientFile);
		}
	}
	@AfterClass
	public static void cleanup() throws IOException{
		FileUtils.deleteDirectory(testDir);
	}
	
	private static List<AttachmentInfo> clientSendsAttachmentToServer() throws JavaInstallationMissingComponentsException, FailedCryptException, IOException{
		//CLIENT sends attachment to SERVER
		try(OutputStream attachmentOS = Utils.outputStreamForFile(clientAttachmentOut);){
			List<AttachmentInfo> attachmentInfo = ClientDataMover.sendAttachments(attachmentOS, myKeys.getPrivateKey(), attachmentsList);
			assertEquals(attachmentInfo.size(), attachmentsList.size());
			for (int i = 0 ; i < attachmentInfo.size() ; i++){
				attachmentInfo.get(i).setServerId(Integer.toString(i));
			}
			return attachmentInfo;
		}
	}
	
	private static void serverReceivesAttachmentFromClient() throws IOException{
		//SERVER receives attachment from CLIENT
		try(InputStream clientAttachmentIS = Utils.inputStreamForFile(clientAttachmentOut);){
			boolean hasAttachments = ServerDataMover.hasAttachments(clientAttachmentIS);
			assertTrue(hasAttachments);
			if (hasAttachments){
				int counter = 0;
				while(ServerDataMover.hasMore(clientAttachmentIS)){
					try(OutputStream serverAttachmentOS = Utils.outputStreamForFile(serverAttachmentFiles.get(counter++));){
						ServerDataMover.getAttachment(clientAttachmentIS, serverAttachmentOS);
					}
				}
				assertEquals("Sent wrong number of attachments", serverAttachmentFiles.size(), counter);
			}
		}
	}
	
	private static void clientSendsMailToServer(List<AttachmentInfo> attachmentInfo) throws JavaInstallationMissingComponentsException, FailedCryptException, IOException{
		//CLIENT sends mail to SERVER
		try(OutputStream clientMailOS = Utils.outputStreamForFile(clientMailOut);){
			ClientDataMover.sendToServer(myKeys.getPrivateKey(), new IPublicKey[]{othersKeys.getPublicKey()}, new String[]{toBox}, fromBox, subject, note, clientMailOS, attachmentInfo);
		}
	}
	
	private static byte[] serverReceivesMailFromClient() throws JavaInstallationMissingComponentsException, IOException, FailedCryptException{
		try(InputStream serverMailIS = Utils.inputStreamForFile(clientMailOut);){
			ServerMailMetaData serverData = ServerDataMover.getServerData(serverMailIS);
			assertEquals("From box name incorrect", fromBox, serverData.getFromMailbox());
			assertEquals("To box name incorrect", toBox, serverData.getToMailbox()[0]);
			int counter = 0;
			byte[] encryptedListData = null;
			while (ServerDataMover.hasMore(serverMailIS)){
				encryptedListData = ServerDataMover.getListData(serverMailIS);
				counter++;
			}
			assertEquals("Server received incorrect amount of list data", 1, counter);
			return encryptedListData;
		}
	}
	
	
	private static void serverSendsMailListToClient(byte[] encryptedListData) throws JavaInstallationMissingComponentsException, IOException{
		try(OutputStream os = Utils.outputStreamForFile(serverListOut);){
			TotalListItem[] items = new TotalListItem[1];
			items[0] = new TotalListItem(listItemID, listItemTimestamp, listItemIP, encryptedListData);
			ServerDataMover.sendMailList(items, os);
		}
	}
	
	private static EncryptedListItem clientReceivesMailList() throws IOException, JavaInstallationMissingComponentsException, FailedCryptException{
		//Client receives from server
		try(InputStream serverFIS = Utils.inputStreamForFile(serverListOut);){
			List<TotalListItem> listItems = ClientDataMover.receiveList(serverFIS);
			TotalListItem item = listItems.get(0);
			assertEquals("List item has incorrect ID", listItemID, item.getId());
			assertEquals("List item has incorrect from IP address", listItemIP, item.getFromIPAddress());
			assertEquals("List item has incorrect timestamp", listItemTimestamp, item.getTimestamp());
			EncryptedListItem encItem = item.getEncryptedListItem(othersKeys.getPrivateKey());
			assertEquals("List item has incorrect subject", subject, encItem.getSubject());
			assertEquals("List item has incorrect note", note, encItem.getNote());
			assertEquals("List item has incorrect from box", fromBox, encItem.getFromBox());
			return encItem;
		}
	}
	
	private static void clientReceivesAttachments(EncryptedListItem encListItem) throws IOException, JavaInstallationMissingComponentsException, FailedCryptException, InvalidSignatureException{
		//Client receives from server
		int counter = 0;
		for (File attachmentFile : serverAttachmentFiles){
			try(InputStream serverFIS = Utils.inputStreamForFile(attachmentFile);){
				File outputFile = clientAttachmentFiles.get(counter);
				ClientDataMover.writeAttachmentToFileAndVerifyDigest(serverFIS, outputFile, othersKeys.getPublicKey(), encListItem.getAttachments().get(counter++));
			}
		}
		for (int i = 0 ; i < NUMBER_ATTACHMENTS ; i++){
			assertTrue("Client received attachment does not match sent attachment", compareTwoFiles(attachmentsList.get(i), clientAttachmentFiles.get(i)));
		}
		
	}
	
	@SuppressWarnings("static-method")
	@Test
	public void uploadSaveDownload() throws JavaInstallationMissingComponentsException, FailedCryptException, IOException, InvalidSignatureException{
		List<AttachmentInfo> attachmentInfo = clientSendsAttachmentToServer();
		serverReceivesAttachmentFromClient();
		clientSendsMailToServer(attachmentInfo);
		byte[] encryptedListData = serverReceivesMailFromClient();
		serverSendsMailListToClient(encryptedListData);
		EncryptedListItem encListItem = clientReceivesMailList();
		clientReceivesAttachments(encListItem);
	}
}
