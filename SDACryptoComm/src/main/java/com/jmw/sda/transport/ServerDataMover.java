package com.jmw.sda.transport;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.Utils;
import com.jmw.sda.transport.objects.ServerMailMetaData;
import com.jmw.sda.transport.objects.TotalListItem;
import com.jmw.sda.transport.stream.MeasuredStream;

public class ServerDataMover {
	public static ServerMailMetaData getServerData(InputStream plainInputStream) throws JavaInstallationMissingComponentsException, IOException, FailedCryptException{
		byte[] serverDataBytes = MeasuredStream.readInput(plainInputStream); 
		ServerMailMetaData serverData = new ServerMailMetaData(serverDataBytes);
		return serverData;
	}
	
	public static byte[] getListData(InputStream plainInputStream) throws IOException{
		byte[] listData = MeasuredStream.readInput(plainInputStream); 
		return listData;
	}	
	
	public static boolean hasAttachments(InputStream is) throws IOException{
		return (MeasuredStream.readControl(is) == MeasuredStream.CONTROL_MORE);
	}
	
	
	public static boolean hasMore(InputStream is) throws IOException{
		return (MeasuredStream.readControl(is) == MeasuredStream.CONTROL_MORE);
	}
	
	public static final void getAttachment(InputStream is, OutputStream os) throws IOException{
		MeasuredStream.readInputKeepMeasures(is, os);
	}
	
	public static final void sendAttachment(InputStream is, OutputStream os) throws IOException{
		Utils.writeFromInputToOutputStream(is, os);
	}	
	
	public static void sendMailList(TotalListItem[] mailInfos, OutputStream plainOutputStream) throws JavaInstallationMissingComponentsException, IOException{
		for(TotalListItem mailInfo : mailInfos){
			MeasuredStream.writeMore(plainOutputStream);
			MeasuredStream.writeOutput(mailInfo.getBytes(), plainOutputStream);
		}
		MeasuredStream.writeEnd(plainOutputStream);
	}
	
	public static void sendMailbox(String mailbox, String signature, OutputStream plainOutputStream) throws IOException, JavaInstallationMissingComponentsException{
		byte[] keyAndSig = Utils.packStringsIntoBigArray(mailbox, signature);
		MeasuredStream.writeOutput(keyAndSig, plainOutputStream);
	}
	
}
