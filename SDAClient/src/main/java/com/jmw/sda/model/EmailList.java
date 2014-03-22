package com.jmw.sda.model;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.table.AbstractTableModel;

import com.jmw.sda.communication.CommunicationFailureException;
import com.jmw.sda.crypto.AbstractCrypto;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.IPublicKey;
import com.jmw.sda.crypto.InvalidSignatureException;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.Utils;
import com.jmw.sda.transport.objects.EncryptedListItem;
import com.jmw.sda.transport.objects.TotalListItem;
import com.jmw.sda.utils.Configuration;

public class EmailList extends AbstractTableModel {
	private static final long serialVersionUID = 1601348957105110840L;
	protected TotalListItem[] items = null;
	public static final String dateFormat = "YYY MM dd'-'hh:mm:ss";
	protected static AbstractCrypto crypto = AbstractCrypto.getCrypto();
	
	public static String dateToString(Date date){
		//simpleDateformat not thread safe
		return new SimpleDateFormat(dateFormat).format(date);
	}
	
	public EmailList(){
	}
	
	public static int getTimeColumn(){
		return 2;
	}
	
	@Override
	public String getColumnName(int col){
		switch(col){
		case 0:
			return "From";
		case 1:
			return "Subject";
		case 2:
			return "Time";
		case 3:
			return "IP";
		default:
			return "";
		}
	}
	
	public TotalListItem getItemAtRow(int row){
		if (row >= this.items.length){
			return null;
		}
		return this.items[row];
	}
	
	public String getIdAtRow(int row){
		if (row >= this.items.length){
			return null;
		}
		return this.items[row].getId();
	}
	public String getFromBoxAtRow(int row) throws FailedCryptException, JavaInstallationMissingComponentsException{
		return this.items[row].getEncryptedListItem(Configuration.getMyPrivateKey()).getFromBox();
	}
	
	public void setItems(TotalListItem[] items){
		this.items = items;
		this.fireTableDataChanged();
	}
	public void clearItems(){
		this.items = new TotalListItem[0];
		this.fireTableDataChanged();
	}
	
	@Override
	public int getColumnCount() {
		return 4;
	}

	@Override
	public int getRowCount() {
		if (this.items == null){
			return 0;
		}
		return this.items.length;
	}
	
	public boolean isRowSignatureValid(int row) throws JavaInstallationMissingComponentsException, IOException, InvalidSignatureException, CommunicationFailureException{
		if (this.items == null){
			return false;
		}
		if (row >= this.items.length){
			return false;
		}
		TotalListItem item = this.items[row];
		EncryptedListItem encItem;
		try {
			encItem = item.getEncryptedListItem(Configuration.getMyPrivateKey());
			IPublicKey sendersKey = MailboxCache.getMailbox(encItem.getFromBox());
			return encItem.verify(sendersKey);
		} catch (FailedCryptException e) {
			return false;
		}
	}

	@Override
	public Object getValueAt(int row, int col) {
		if (this.items == null){
			return "";
		}
		try{
			TotalListItem item = this.items[row];
			switch(col){
			case 0:
				return item.getEncryptedListItem(Configuration.getMyPrivateKey()).getFromBox();
			case 1:
				return item.getEncryptedListItem(Configuration.getMyPrivateKey()).getSubject();
			case 2:
				return dateToString(Utils.stringToDate(item.getEncryptedListItem(Configuration.getMyPrivateKey()).getTimestamp()));
			case 3:
				return item.getFromIPAddress();
			default: return "";
			}
			
		}catch(FailedCryptException | JavaInstallationMissingComponentsException e){
			System.out.println("threw exception" + Utils.ppStackTrace(e));
			return "";
		}
	}

}
