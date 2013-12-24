package com.jmw.sda.dbProviders;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.internal.ArrayComparisonFailure;

import com.jmw.sda.Tests;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.Utils;
import com.jmw.sda.dbProviders.CurrentProvider;
import com.jmw.sda.dbProviders.DBException;
import com.jmw.sda.dbProviders.IDAndOutputStream;
import com.jmw.sda.dbProviders.IDatabase;
import com.jmw.sda.dbProviders.Mailbox;
import com.jmw.sda.transport.objects.TotalListItem;

public class DBProviderTest extends Tests {
	static IDatabase database = CurrentProvider.get();
	protected static final int TESTSIZE = 10;
	protected static final int NUMBER_THREADS = 50;

	public static void failThread(String msg){
		fail(msg);
	}
	
	@SuppressWarnings("static-method")
	@Test
	public void mailboxTest() throws DBException, ArrayComparisonFailure, JavaInstallationMissingComponentsException{
		List<Mailbox> newMailboxes = new ArrayList<>();
		for (int i = 0 ; i < TESTSIZE ; i++){
			Mailbox mailbox = new Mailbox("test" + i, "PublicKey" + i, "serverSignature" + i);
			database.setMailbox(mailbox);
			newMailboxes.add(mailbox);
		}
		String[] users = database.listUsers();
		List<String> userList = Arrays.asList(users);
		for (int i = 0 ; i < TESTSIZE ; i++){
			String name = "test" + i;
			Mailbox mailbox = database.getMailbox(name);
			assertTrue("database does not correctly show existing mailbox", userList.contains(name));
			assertArrayEquals("Mailbox was not retrieved correctly from db", mailbox.getBytes(), newMailboxes.get(i).getBytes());
		}
		for (int i = 0 ; i < TESTSIZE ; i++){	
			String name = "test" + i;
			database.deleteMailbox(name);
		}	
		String[] usersDeleted = database.listUsers();
		List<String> usersDeletedList = Arrays.asList(usersDeleted);
		for (int i = 0 ; i < TESTSIZE ; i++){
			String name = "test" + i;
			Mailbox mailbox = database.getMailbox(name);
			assertNull("database still retrieves deleted mailbox", mailbox);
			assertFalse("database did not correctly delete mailbox", usersDeletedList.contains(name));
		}		
	}
	
	@SuppressWarnings("static-method")
	@Test 
	public void mailTest() throws DBException, UnsupportedEncodingException{
		Mailbox mailbox = new Mailbox("test", "PublicKey", "serverSignature");
		database.setMailbox(mailbox);	
		for (int i = 0 ; i < TESTSIZE ; i++){
			database.putMail("test", "timestamp", "fromIpAddress", ("encryptedMailListData").getBytes(Utils.ENCODING));
		}
		TotalListItem[] mailItems = database.getList("test");
		assertEquals("db returned wrong number of mail items", TESTSIZE, mailItems.length);
		for (TotalListItem mailItem : mailItems){
			assertEquals("Timestamp incorrect from db", mailItem.getTimestamp(), "timestamp");
			assertEquals("IP address incorrect from db", mailItem.getFromIPAddress(), "fromIpAddress");
		}
		int numberToDelete = mailItems.length/2;
		for (int i = 0 ; i < numberToDelete ; i++){
			database.deleteMail("test", mailItems[i].getId());
		}
		TotalListItem[] remainingMailItems = database.getList("test");
		assertEquals("mail items not deleted", mailItems.length-numberToDelete, remainingMailItems.length);
		database.deleteMailbox("test");
		TotalListItem[] deletedMailItems = database.getList("test");
		assertEquals("mail items not deleted with mailbox deletion", 0, deletedMailItems.length);
	}
	
	@SuppressWarnings("static-method")
	@Test
	public void attachmentsTest() throws DBException, IOException{
		List<byte[]> randomData = new ArrayList<>(TESTSIZE);
		List<String> ids = new ArrayList<>(TESTSIZE);
		for(int i = 0 ; i < TESTSIZE ; i++){
			byte[] data = randomBytes(700);
			randomData.add(data);
			try(IDAndOutputStream idOS = database.putAttachment();){
				idOS.getOs().write(data);
				ids.add(idOS.getId());
				database.addMailboxToAttachment("test", idOS.getId());
			}
		}
		for(int i = 0 ; i < TESTSIZE ; i++){
			String id = ids.get(i);
			try(InputStream is = database.getAttachment(id);){
				byte[] attachment = Utils.readFully(is);
				assertArrayEquals("db returned wrong attachment data", randomData.get(i), attachment);
			}
		}
		for(int i = 0 ; i < TESTSIZE ; i++){
			String id = ids.get(i);
			database.deleteAttachment("test", id);
		}
		for(int i = 0 ; i < TESTSIZE ; i++){
			try{
				String id = ids.get(i);
				database.getAttachment(id);
				fail("db did not delete attachment");
			}catch(DBException e){
				//expected
			}
			
		}
		
	}
	
	@SuppressWarnings("static-method")
	@Test
	public void concurrentAccessTest() throws DBException, InterruptedException, IOException{
		String id = null;
		ArrayList<ReadDataThread> threadHolder = new ArrayList<>();
		try(IDAndOutputStream idOs = database.putAttachment();){
			byte[] data = message.getBytes(ENCODING);
			idOs.getOs().write(data);
			id = idOs.getId();
			
			
			for (int i = 0 ; i < NUMBER_THREADS ; i++){
				threadHolder.add(new ReadDataThread(id, database));
			}
			for(ReadDataThread test : threadHolder){
				test.start();
			}
		} 
		for(ReadDataThread thread : threadHolder){
			thread.join();
			if (thread.getError() != null){
				fail(thread.getError());
			}
		}
		database.deleteAttachment(null, id);
	}
	
	@SuppressWarnings("static-method")
	@Test
	public void concurentAccessDeletedTest() throws IOException, DBException, InterruptedException{
		String id = null;
		try(IDAndOutputStream idOs = database.putAttachment();){
			byte[] data = message.getBytes(ENCODING);
			idOs.getOs().write(data);
			id = idOs.getId();
		}
		database.deleteAttachment(null, id);
		ArrayList<ReadDataExceptionThread> threadHolder = new ArrayList<>();
		for (int i = 0 ; i < NUMBER_THREADS ; i++){
			threadHolder.add(new ReadDataExceptionThread(id, database));
		}
		for (ReadDataExceptionThread thread : threadHolder){
			thread.start();
		}	
		for(ReadDataExceptionThread thread : threadHolder){
			thread.join();
			if (thread.getError() != null){
				fail(thread.getError());
			}
		}
	}
	
}

class ReadDataThread extends Thread{
	private String id;
	private IDatabase database;
	private String error = null;
	public ReadDataThread(String id, IDatabase database){
		this.id = id;
		this.database = database;
	}
	
	@Override
	public void run(){
		try (InputStream is = this.database.getAttachment(this.id);) {
			byte[] data = Utils.readFully(is);
			if (!Tests.message.equals(new String(data, Tests.ENCODING))){
				this.error = "Thread Failed to retrieve attachment";
			}
		} catch (DBException | IOException e) {
			this.error = e.getMessage();
		}
	}
	
	public String getError(){
		return this.error;
	}
	
	
}
class ReadDataExceptionThread extends Thread{
	private String id;
	private IDatabase database;
	private String error = null;
	public ReadDataExceptionThread(String id, IDatabase database){
		this.id = id;
		this.database = database;
	}
	
	@Override
	public void run(){
		try (InputStream is = this.database.getAttachment(this.id);) {
			this.error = "Retrieved a deleted attachment";
		} catch (DBException e){
			//expected
		} catch (IOException e) {
			this.error = e.getMessage();
		}
	}
	
	public String getError(){
		return this.error;
	}
}
