package com.jmw.sda.communication;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.naming.CommunicationException;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import com.jmw.sda.Constants.UrlParams;
import com.jmw.sda.Constants.Urls;
import com.jmw.sda.crypto.AbstractCrypto;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.IPrivateKey;
import com.jmw.sda.crypto.IPublicKey;
import com.jmw.sda.crypto.InvalidSignatureException;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.Utils;
import com.jmw.sda.transport.ClientDataMover;
import com.jmw.sda.transport.TransportUtils;
import com.jmw.sda.transport.objects.AttachmentInfo;
import com.jmw.sda.transport.objects.TotalListItem;
import com.jmw.sda.utils.Configuration;

public class WebServiceClient {

	protected static CloseableHttpClient httpclient = HttpClients.createDefault();
	protected static IPublicKey serverPublicKey;
	protected static AbstractCrypto crypto = AbstractCrypto.getCrypto();
	
	/**
	 * @param pathString the path to post the data to
	 * @param keyValPairs an even number of strings that are key then value
	 * @return the response from the post
	 * @throws IOException
	 * @throws CommunicationException 
	 */
	private static final CloseableHttpResponse postData(String pathString, String ...keyValPairs) throws IOException, CommunicationFailureException{
		String host = Configuration.getCurrentHost();
		return postDataExplicitHost(host, pathString, keyValPairs);
	}
	@SuppressWarnings("deprecation")
	private static final CloseableHttpResponse postDataExplicitHost(String host, String pathString, String ...keyValPairs) throws IOException, CommunicationFailureException{
		if (host == null){
			throw new CommunicationFailureException("Host name is null");
		}
		HttpPost post = new HttpPost(host + pathString);
		List<NameValuePair> pairs = new ArrayList<>();
		assert keyValPairs.length % 2 == 0;
		for (int i = 0 ; i < keyValPairs.length ; i += 2){
			pairs.add(new BasicNameValuePair(keyValPairs[i], keyValPairs[i+1]));
			post.setEntity(new UrlEncodedFormEntity(pairs, HTTP.UTF_8));
		}
        return httpclient.execute(post);
	}
	
	protected static CloseableHttpResponse getData(String pathString) throws IOException, CommunicationFailureException{
		String host = Configuration.getCurrentHost();
		if (host == null){
			throw new CommunicationFailureException("Host name is null");
		}
		HttpGet get = new HttpGet(host + pathString);
        return httpclient.execute(get);
	}	
	
	protected static CloseableHttpResponse putData(String pathString, HttpEntity entity) throws IOException, CommunicationFailureException{
		String host = Configuration.getCurrentHost();
		if (host == null){
			throw new CommunicationFailureException("Host name is null");
		}
		HttpPut put = new HttpPut(host + pathString);
		put.setEntity(entity);
		return httpclient.execute(put);
	}
	
	public static String getServerId(String url) throws IOException{
		HttpGet get = new HttpGet(url + Urls.id);
		try(CloseableHttpResponse response = httpclient.execute(get);
		    InputStream is = response.getEntity().getContent();
		    ByteArrayOutputStream baos = new ByteArrayOutputStream()){
				baos.write(is);
				return baos.toString();
		}
	}
	
	public static String[] getRecipientNames() throws IOException, CommunicationFailureException{
		try(CloseableHttpResponse response = getData(Urls.names);
		    InputStream is = response.getEntity().getContent();
		    ByteArrayOutputStream baos = new ByteArrayOutputStream()){
			baos.write(is);
			return baos.toString().split(" ");
		}
	}
	
	
	public static int getServerStrength() throws IOException, CommunicationFailureException{
		try(CloseableHttpResponse response = getData(Urls.strength);
		    InputStream is = response.getEntity().getContent();
		    ByteArrayOutputStream baos = new ByteArrayOutputStream()){
				baos.write(is);
				return Integer.parseInt(baos.toString());
		}		
	}
	
	public static String createMailbox(String host, String mailboxName, String publicKey, String serversPublicKey) throws IOException, JavaInstallationMissingComponentsException, FailedCryptException, CommunicationFailureException{
		IPublicKey serverKey = null;
			serverKey = crypto.stringToPublicKey(serversPublicKey);
		String encryptedMailboxName = crypto.encrypt(serverKey, mailboxName);
		try(CloseableHttpResponse response = postDataExplicitHost(host, Urls.mailbox,
    			UrlParams.mailboxName, encryptedMailboxName, UrlParams.publicKey, publicKey, UrlParams.action, UrlParams.create)){
	        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED){
	        	return response.getStatusLine().getReasonPhrase();
	        }
	        return null;
		}
	}
	
	public static String deleteMailbox(String host) throws JavaInstallationMissingComponentsException, FailedCryptException, CommunicationFailureException{
    	String signature = crypto.sign(Configuration.getMyPrivateKey(host), Configuration.getMyPublicKeyString(host), Configuration.getMyMailboxName(host));
    	try(CloseableHttpResponse response = postDataExplicitHost(host, Urls.mailbox,
    			UrlParams.mailboxName, Configuration.getMyMailboxName(host), UrlParams.action, UrlParams.delete, UrlParams.signature, signature)){
	        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
	        	return response.getStatusLine().getReasonPhrase();
	        }
	        return null;
		} catch (IOException e) {
			return Utils.ppStackTrace(e);
		} 
	}
	
	public static String getMailbox(String mailboxName) throws IOException, JavaInstallationMissingComponentsException, FailedCryptException, InvalidSignatureException, CommunicationFailureException{
		try(CloseableHttpResponse response = getData(String.format("%s/%s", Urls.mailbox,mailboxName))){
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
				return null;
			}
			try(InputStream is = response.getEntity().getContent()){
				String mailboxKeyString = ClientDataMover.getMailbox(is, mailboxName, Configuration.getHostPublicKey());
				return mailboxKeyString;
			}
		}
	}
	
	private static final List<AttachmentInfo> sendAttachments(String[] toMailboxes, String fromMailbox, List<File> attachments) throws JavaInstallationMissingComponentsException, FailedCryptException, IOException, CommunicationFailureException{
		List<AttachmentInfo> attachInfos = null;
		IPrivateKey privateKey = Configuration.getMyPrivateKey();
		EncryptedAttachmentEntity attachmentEntity = new EncryptedAttachmentEntity(privateKey, fromMailbox, attachments);
		try(CloseableHttpResponse response1 = putData(Urls.attachment, attachmentEntity);){
			attachInfos = attachmentEntity.getAttachmentInfo();
			if (attachInfos == null){
				throw new IOException("Could not retrieve attachment metadata");
			}
			String returnString = TransportUtils.inputStreamToString(response1.getEntity().getContent());
       	 	String[] attachmentIds = returnString.length() > 0 ? returnString.split("\\s") : new String[]{} ;
       	 	if (attachmentIds.length != attachInfos.size()){
       	 		throw new IOException("Server failed to return correct number of attachment ids");
       	 	}
			for(int i = 0 ; i < attachmentIds.length ; i++){
				attachInfos.get(i).setServerId(attachmentIds[i]);
			}
		}
		for(String mailboxName : toMailboxes){
			for(AttachmentInfo info : attachInfos){
				String attachSignature = TransportUtils.getStringForActionMailboxNameAndId(UrlParams.addRecipient, mailboxName, info.getServerId());
				String signature = crypto.sign(privateKey, attachSignature);
				try(CloseableHttpResponse response = postData(String.format("%s/%s/%s", Urls.attachment, mailboxName, info.getServerId()), UrlParams.signature, signature, UrlParams.action, UrlParams.addRecipient, UrlParams.mailboxName, fromMailbox);){
			        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
			        	throw new IOException(response.getStatusLine().getReasonPhrase());
			        }
				}
			}
		}
		return attachInfos;
	}
	
	public static void sendMail(IPublicKey[] toBoxPublicKey, String[] toMailboxes, String fromMailbox, String subject, String note, List<File> attachments) throws 
	JavaInstallationMissingComponentsException, IOException, FailedCryptException, CommunicationFailureException{
		List<AttachmentInfo> attachInfos = sendAttachments(toMailboxes, fromMailbox, attachments);
		EncryptedOutputEntity entity = new EncryptedOutputEntity(Configuration.getMyPrivateKey(), toBoxPublicKey, toMailboxes,
				fromMailbox, subject, note, attachInfos);
		try(CloseableHttpResponse response = putData(Urls.letter, entity);){
	        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED){
	        	throw new IOException(response.getStatusLine().getReasonPhrase());
	        }
		}
	}
	
	public static void assignAttachmentToNewMailbox(String mailboxName, String attachmentId) throws JavaInstallationMissingComponentsException, FailedCryptException, IOException, CommunicationFailureException{
		String attachToSign = TransportUtils.getStringForActionMailboxNameAndId(UrlParams.addRecipient,mailboxName, attachmentId);
		String attachSignature = crypto.sign(Configuration.getMyPrivateKey(), attachToSign);
		try(CloseableHttpResponse response = postData(String.format("%s/%s/%s", Urls.attachment, mailboxName, attachmentId), UrlParams.signature, attachSignature, UrlParams.action, UrlParams.addRecipient);){
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
				throw new CommunicationFailureException(response.getStatusLine().getReasonPhrase());
			}
		}
	}
	
	public static void forwardMail(IPublicKey[] toBoxPublicKey, String[] toMailboxes, String fromMailbox, String subject, String note, List<File> attachments,
			List<AttachmentInfo> attachmentInfos) throws 
	JavaInstallationMissingComponentsException, IOException, FailedCryptException, CommunicationFailureException{
		List<AttachmentInfo> attachInfos = sendAttachments(toMailboxes, fromMailbox, attachments);
		for(AttachmentInfo newAttachment : attachmentInfos){
			attachInfos.add(newAttachment);
		}
		IPrivateKey privateKey = Configuration.getMyPrivateKey();
		for(String mailboxName : toMailboxes){
			for(AttachmentInfo info : attachInfos){
				String attachSignature = TransportUtils.getStringForActionMailboxNameAndId(UrlParams.addRecipient, mailboxName, info.getServerId());
				String signature = crypto.sign(privateKey, attachSignature);
				try(CloseableHttpResponse response2 = postData(String.format("%s/%s/%s", Urls.attachment, mailboxName, info.getServerId()), UrlParams.signature, signature, UrlParams.action, UrlParams.addRecipient, UrlParams.mailboxName, fromMailbox);){
			        if (response2.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
			        	throw new IOException(response2.getStatusLine().getReasonPhrase());
			        }
				}
			}
		}

		EncryptedOutputEntity entity = new EncryptedOutputEntity(privateKey, toBoxPublicKey, toMailboxes,
				fromMailbox, subject, note, attachInfos);
		try(CloseableHttpResponse response = putData(Urls.letter, entity);){
	        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED){
	        	throw new IOException(response.getStatusLine().getReasonPhrase());
	        }
		}
	}
	
	
	public static TotalListItem[] getMailList(String mailboxName) throws CommunicationFailureException, FailedCryptException{
        try (CloseableHttpResponse response = getData(String.format("%s/%s", Urls.letter, mailboxName));){
	        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
	        	throw new CommunicationFailureException(response.getStatusLine().getReasonPhrase());
	        }
        	try(InputStream is = response.getEntity().getContent();){
	        	List<TotalListItem> mailInfo = ClientDataMover.receiveList(is);
	        	return mailInfo.toArray(new TotalListItem[mailInfo.size()]);
        	}
		} catch (IOException | JavaInstallationMissingComponentsException e) {
			throw new CommunicationFailureException(e);
		} 
	}
	
	public static void deleteMessage(String mailId, List<AttachmentInfo> attachments) throws JavaInstallationMissingComponentsException, IOException, FailedCryptException, CommunicationFailureException{
		IPrivateKey privateKey = Configuration.getMyPrivateKey();
		if(attachments != null){
			for (AttachmentInfo attachment : attachments){
				String attachToSign = TransportUtils.getStringForActionMailboxNameAndId(UrlParams.delete,Configuration.getMyMailboxName(), attachment.getServerId());
				String attachSignature = crypto.sign(privateKey, attachToSign);
				try(CloseableHttpResponse response = postData(String.format("%s/%s/%s", Urls.attachment, Configuration.getMyMailboxName(), attachment.getServerId()), 
						UrlParams.signature, attachSignature, UrlParams.action, UrlParams.delete, UrlParams.mailboxName, Configuration.getMyMailboxName());){
					if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
						throw new CommunicationFailureException(response.getStatusLine().getReasonPhrase());
					}
				}
			}
		}
		String toSign = TransportUtils.getStringForActionMailboxNameAndId(UrlParams.delete, Configuration.getMyMailboxName(), mailId);
		String signature = crypto.sign(privateKey, toSign);
		try(CloseableHttpResponse response = postData(String.format("%s/%s/%s", Urls.letter, Configuration.getMyMailboxName(), mailId), UrlParams.signature, signature);){
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
				throw new CommunicationFailureException(response.getStatusLine().getReasonPhrase());
			}
		}
	}
	
	public static boolean getAttachment(AttachmentInfo attachmentInfo, String fromBox, File outFile) throws IOException, JavaInstallationMissingComponentsException, FailedCryptException, InvalidSignatureException,  CommunicationFailureException{
		try(CloseableHttpResponse response = getData(String.format("%s/%s", Urls.attachment, attachmentInfo.getServerId()));){
			IPublicKey fromKey = crypto.stringToPublicKey(getMailbox(fromBox));
			boolean verified = ClientDataMover.writeAttachmentToFileAndVerifyDigest(response.getEntity().getContent(), 
					outFile, fromKey, attachmentInfo);
			return verified;
		}
	}
}


/**
 * Class used to send data using EE method with Apache's HTTP library
 * @author jwalker
 *
 */
class EncryptedAttachmentEntity extends AbstractHttpEntity {
	protected IPrivateKey privateKey; 
	protected String fromMailbox;
	protected String subject;
	protected String note;
	protected List<File> attachments;
	protected List<AttachmentInfo> attachmentInfo;
	
	public EncryptedAttachmentEntity(IPrivateKey privateKey, String fromMailbox, List<File> attachments){
		this.privateKey = privateKey;
		this.fromMailbox = fromMailbox;
		this.attachments = attachments;
	}

    @Override
	public boolean isRepeatable() {
        return false;
    }

    @Override
	public long getContentLength() {
        return -1;
    }

    @Override
	public boolean isStreaming() {
        return false;
    }

    @Override
	public InputStream getContent() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void writeTo(final OutputStream outstream) throws IOException {
    	try {
			this.attachmentInfo = ClientDataMover.sendAttachments(outstream, this.privateKey, this.attachments);
			outstream.close();
		} catch (JavaInstallationMissingComponentsException| FailedCryptException e) {
			throw new IOException(e);
		}
    }
    
    public List<AttachmentInfo> getAttachmentInfo(){
    	return this.attachmentInfo;
    }
}

/**
 * Class used to send data using EE method with Apache's HTTP library
 * @author jwalker
 *
 */
class EncryptedOutputEntity extends AbstractHttpEntity {
	protected IPrivateKey privateKey; 
	protected IPublicKey[] publicKey;
	protected String[] toMailbox;
	protected String fromMailbox;
	protected String subject;
	protected String note;
	protected List<AttachmentInfo> attachments;
	
	public EncryptedOutputEntity(IPrivateKey privateKey, IPublicKey[] publicKey, String[] toMailbox,
			String fromMailbox, String subject, String note, List<AttachmentInfo> attachments){
		this.privateKey = privateKey;
		this.publicKey = publicKey;
		this.toMailbox = toMailbox;
		this.fromMailbox = fromMailbox;
		this.subject = subject;
		this.note = note;
		this.attachments = attachments;
	}

    @Override
	public boolean isRepeatable() {
        return false;
    }

    @Override
	public long getContentLength() {
        return -1;
    }

    @Override
	public boolean isStreaming() {
        return false;
    }

    @Override
	public InputStream getContent() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void writeTo(final OutputStream outstream) throws IOException {
    	try {
			ClientDataMover.sendToServer(this.privateKey, this.publicKey, this.toMailbox, this.fromMailbox, this.subject, this.note, outstream, this.attachments);
			outstream.close();
		} catch (JavaInstallationMissingComponentsException| FailedCryptException e) {
			throw new IOException(e);
		}
    }
}
