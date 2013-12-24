package com.jmw.sda.model;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

import com.jmw.sda.communication.CommunicationFailureException;
import com.jmw.sda.communication.WebServiceClient;
import com.jmw.sda.crypto.AbstractCrypto;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.IPublicKey;
import com.jmw.sda.crypto.InvalidSignatureException;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.utils.Configuration;

public class MailboxCache {
	protected static HashMap<String, IPublicKey> keyMap;
	protected static final AbstractCrypto crypto = AbstractCrypto.getCrypto();
	static{
		keyMap = new HashMap<>();
	}

	public static Collection<String> getUsers(){
		return Configuration.getMailboxUsers();
	}
	
	public static IPublicKey getMailbox(String mailbox) throws IOException, JavaInstallationMissingComponentsException, FailedCryptException, InvalidSignatureException, CommunicationFailureException{
		String name = String.format("%s+%s", Configuration.getCurrentHost(), mailbox);
		if (keyMap.get(name) == null){
			String publicKeyString = Configuration.getMailboxPublicKey(mailbox);
			if (publicKeyString == null || publicKeyString.equalsIgnoreCase("")){
				publicKeyString = WebServiceClient.getMailbox(mailbox);
				if (publicKeyString == null || publicKeyString.equalsIgnoreCase("")){
					return null;
				}
				Configuration.addUser(mailbox, publicKeyString);
			}
			IPublicKey key = crypto.stringToPublicKey(publicKeyString);
			keyMap.put(name, key);
		}
		return keyMap.get(name);
	}
	
	public static void loadLocalMailboxes(){
		String host = Configuration.getCurrentHost();
		for (String user : getUsers()){
			String name = String.format("%s+%s", host, user);
			keyMap.put(name, null);
		}
	}
	
	public static void loadAllMailboxesFromServer() throws IOException, JavaInstallationMissingComponentsException, FailedCryptException, InvalidSignatureException, CommunicationFailureException{
		String[] mailboxes = WebServiceClient.getRecipientNames();
		for (String mailbox : mailboxes){
			if (!Configuration.hasMailbox(mailbox)){
				getMailbox(mailbox);			
			}
		}
	}

}
