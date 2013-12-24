package com.jmw.sda.dbProviders;

import java.io.IOException;
import java.io.InputStream;

import com.jmw.sda.transport.objects.TotalListItem;

public interface IDatabase {

	/**
	 * Gets mailbox object associated with mailbox name
	 * @param mailboxName
	 * @return Mailbox object associated with name
	 * @throws DBException
	 */
	Mailbox getMailbox(final String mailboxName) throws DBException;
	/**
	 * Sets this mailbox with mailbox.getName as it's key
	 * @param mailbox
	 * @throws DBException
	 */
	void setMailbox(final Mailbox mailbox) throws DBException;
	/**
	 * removes mailbox from server and all associated mail and attachments
	 * @param mailbox
	 * @throws DBException
	 */
	void deleteMailbox(final String mailbox) throws DBException;

	/**
	 * returns TotalListItem that contains data placed with putMail command and DB-generated ID
	 * @param mailboxName
	 * @return
	 * @throws IOException
	 */
	TotalListItem[] getList(final String mailboxName) throws DBException;
	
	/**
	 * returns GUID associated with mail
	 * @param serverData
	 * @param fromIPAddress
	 * @param listData
	 * @return
	 * @throws DBException
	 */
	String putMail(String toMailboxes, String timestamp,  String fromIpAddress, byte[] encryptedMailListData) throws DBException;
	/**
	 * this also deletes all attachments associated with the mail
	 * @param id
	 * @param mailboxName
	 * @throws DBException
	 */
	void deleteMail(final String mailboxName, final String id) throws DBException;
	
	/**
	 * returns an inputStream of the attachment mapped to the id
	 * @param id
	 * @return
	 * @throws DBException
	 */
	InputStream getAttachment(String id) throws DBException;
	
	/**
	 * returns an id assigned to the attachment and an outputstream to write the attachment to
	 * @return
	 * @throws DBException
	 */
	IDAndOutputStream putAttachment() throws DBException;
	
	/**
	 * map a mailbox name to an attachment id.  This must support have more than one of the same mailbox name associated with an attachment 
	 * @param mailbox
	 * @param attachmentId
	 * @throws DBException
	 */
	public void addMailboxToAttachment(final String mailbox, final String attachmentId) throws DBException;
	
	
	/**
	 * removes the mapping of the mailbox name to the attachment id, if no mailboxes left, delete the attachment
	 * @param mailboxName
	 * @param attachmentId
	 * @throws DBException
	 */
	void deleteAttachment(String mailboxName, String attachmentId) throws DBException;


	/**
	 * returns a string array of mailbox names
	 * @return
	 * @throws DBException
	 */
	String[] listUsers() throws DBException;
	
}
