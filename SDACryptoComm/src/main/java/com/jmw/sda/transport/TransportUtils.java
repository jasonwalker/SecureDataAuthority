package com.jmw.sda.transport;

import java.io.IOException;
import java.io.InputStream;

public class TransportUtils {

	public static final String getStringForActionMailboxNameAndId(String action, String mailboxName, String letterId){
		return String.format("%s:%s:%s", action, mailboxName, letterId);
	}
	public static final String inputStreamToString(InputStream is) throws IOException{
    	StringBuilder sb = new StringBuilder();
    	byte[] buf = new byte[8192];
    	int len = is.read(buf);
    	while(len > 0){
    		sb.append(new String(buf, 0, len, "UTF-8"));
    		len = is.read(buf);
    	}
    	return sb.toString();
	}
}
