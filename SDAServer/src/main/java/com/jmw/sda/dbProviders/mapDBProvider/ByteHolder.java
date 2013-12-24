package com.jmw.sda.dbProviders.mapDBProvider;

import java.io.IOException;
import java.io.Serializable;

import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.dbProviders.Mailbox;
import com.jmw.sda.transport.objects.TotalListItem;

/**
 * Makes a byte array comparable
 * @author jwalker
 *
 */
public class ByteHolder implements Comparable<ByteHolder>, Serializable{
	private static final long serialVersionUID = -630218698636317488L;
	protected byte[] bytes;
	public ByteHolder(){
		this.bytes = null;
	}
	
	public ByteHolder(byte[] bytes){
		this.bytes = bytes;
	}
	public ByteHolder(Mailbox m) throws JavaInstallationMissingComponentsException{
		this.bytes = m.getBytes();
	}
	public ByteHolder(TotalListItem t) throws JavaInstallationMissingComponentsException{
		this.bytes = t.getBytes();
	}
	
	@Override
	public final int compareTo(ByteHolder other) {
		if (this.bytes == null){
			if (other.bytes == null){
				return 0;
			}
			return -1;
		}
		if (other.bytes == null){
			return 1;
		}
		int len = this.bytes.length > other.bytes.length ? other.bytes.length : this.bytes.length;
		for (int i = 0 ; i < len ; i++){
			if (this.bytes[i] > other.bytes[i]){
				return 1;
			}else if (this.bytes[i] < other.bytes[i]){
				return -1;
			}
		}
		if (this.bytes.length > other.bytes.length){
			return 1;
		}else if(this.bytes.length < other.bytes.length){
			return -1;
		}else{
			return 0;
		}
	}	
	 private void writeObject(java.io.ObjectOutputStream out)
		     throws IOException{
		 out.writeInt(this.bytes.length);
		 out.write(this.bytes);
	 }
	 private void readObject(java.io.ObjectInputStream in)
	     throws IOException{
		 int len = in.readInt();
		 this.bytes = new byte[len];
		 in.readFully(this.bytes);
	 }
	
	 public byte[] getBytes(){
		 return this.bytes;
	 }
}

