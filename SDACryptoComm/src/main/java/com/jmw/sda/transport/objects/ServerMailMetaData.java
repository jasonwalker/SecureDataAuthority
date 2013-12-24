package com.jmw.sda.transport.objects;

import java.io.UnsupportedEncodingException;

import com.jmw.sda.crypto.AbstractCrypto;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.IPrivateKey;
import com.jmw.sda.crypto.IPublicKey;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.Utils;

/**
 * This data is sent unencrypted to server
 * @author jasonw
 *
 */
public class ServerMailMetaData {
	protected String timestamp; 
	protected String[] toMailbox; 
	protected String fromMailbox; 
	protected String signature; 
	protected static AbstractCrypto crypto = AbstractCrypto.getCrypto();
	public ServerMailMetaData(byte[] bytes) throws JavaInstallationMissingComponentsException, FailedCryptException{
		initFromBytes(bytes);	
	}
	
	public ServerMailMetaData(IPrivateKey privateKey, String timestamp, String[] toMailbox, String fromMailbox) throws JavaInstallationMissingComponentsException, FailedCryptException{
		this.timestamp = timestamp;
		this.toMailbox = toMailbox;
		this.fromMailbox = fromMailbox;
		this.signature = crypto.sign(privateKey, timestamp, Utils.join(toMailbox), fromMailbox);
	}
	
	public boolean verify(IPublicKey publicKey) throws JavaInstallationMissingComponentsException, FailedCryptException{
		return crypto.verify(publicKey, this.signature, this.timestamp, Utils.join(this.toMailbox), this.fromMailbox);
	}
	
	public byte[] getBytes() throws JavaInstallationMissingComponentsException{
		try {
			return Utils.packIntoBigArray(this.timestamp.getBytes(Utils.ENCODING), Utils.packStringsIntoBigArray(this.toMailbox), this.fromMailbox.getBytes(Utils.ENCODING), this.signature.getBytes(Utils.ENCODING));
		} catch (UnsupportedEncodingException e) {
			throw new JavaInstallationMissingComponentsException(e);
		}
	}
	
	public void initFromBytes(byte[] bytes) throws JavaInstallationMissingComponentsException, FailedCryptException{
		byte[][] data = Utils.unpackBigArray(bytes);

		this.timestamp = Utils.byteArrayToString(data[0]); 
		this.toMailbox = Utils.unpackIntoStringArray(data[1]);
		this.fromMailbox = Utils.byteArrayToString(data[2]); 
		this.signature = Utils.byteArrayToString(data[3]); 	
	}
	
	
	public String getTimestamp() {
		return this.timestamp;
	}

	public String[] getToMailbox() {
		return this.toMailbox;
	}

	public String getFromMailbox() {
		return this.fromMailbox;
	}
	public void setFromMailbox(String fromMailbox) {
		this.fromMailbox = fromMailbox;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder("ServerData--");
		sb.append("To: ");
		sb.append(this.toMailbox);
		sb.append(", from: ");
		sb.append(this.fromMailbox);
		sb.append(", timestamp: ");
		sb.append(this.timestamp);
		return sb.toString();
	}
}
