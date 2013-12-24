package com.jmw.sda.dbProviders.mapDBProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.Semaphore;

import org.mapdb.Atomic;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.Bind;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;

import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.dbProviders.DBException;
import com.jmw.sda.dbProviders.IDAndOutputStream;
import com.jmw.sda.dbProviders.IDatabase;
import com.jmw.sda.dbProviders.Mailbox;
import com.jmw.sda.transport.objects.TotalListItem;

public class MapDBProvider implements IDatabase {
	WeakHashMap<String, Semaphore> lockHolder = new WeakHashMap<>();
	protected DB db;
	ConcurrentNavigableMap<String,ByteHolder> mailboxMap;
	//we don't mailboxes created with same names as deleted ones
	NavigableSet<String> allMailboxSet;
	NavigableSet<Fun.Tuple2<String,String>> letterInfo;
	ConcurrentNavigableMap<String,String> attachmentInfo;
	ConcurrentNavigableMap<String,ByteHolder> letters;

	public static final File DATADIRECTORY = new File("./data");
	protected Atomic.Long keyinc;
	static {
		DATADIRECTORY.mkdirs();
	}
	
	public MapDBProvider() {
		this.db = DBMaker.newFileDB(new File("db"))
                .closeOnJvmShutdown()
                .make();
		
        if (this.db.exists("letterInfo")){
        	this.letterInfo = this.db.getTreeSet("letterInfo");
        }else{
    		this.letterInfo = this.db.createTreeSet("letterInfo")
                    .serializer(BTreeKeySerializer.TUPLE2)
                    .make();
        }	
 
        this.attachmentInfo = this.db.getTreeMap("attachmentInfo");
		this.mailboxMap = this.db.getTreeMap("mailboxes");
        if (this.db.exists("deletedMailboxes")){
        	this.allMailboxSet = this.db.getTreeSet("deletedMailboxes");
        }else{
    		this.allMailboxSet = this.db.createTreeSet("deletedMailboxes")
                    .make();
        }			
		this.letters = this.db.getTreeMap("letters");
		this.keyinc = this.db.getAtomicLong("map_keyinc");
	}

	@Override
	public Mailbox getMailbox(String mailboxName) throws DBException {
		try {
			ByteHolder mailboxBytes = this.mailboxMap.get(mailboxName);
			if (mailboxBytes == null){
				return null;
			}
			return Mailbox.putBytes(mailboxBytes.bytes);
		} catch (JavaInstallationMissingComponentsException | FailedCryptException e) {
			throw new DBException(e);
		}
	}

	@Override
	public void setMailbox(Mailbox mailbox) throws DBException {
		try {
			if (allMailboxSet.contains(mailbox.getName())){
				throw new DBException("Cannot create mailbox with same name as previously-deleted mailbox");
			}
			this.mailboxMap.put(mailbox.getName(), new ByteHolder(mailbox));
			this.allMailboxSet.add(mailbox.getName());
			this.db.commit();
		} catch (JavaInstallationMissingComponentsException e) {
			throw new DBException(e);
		}
	}

	@Override
	public void deleteMailbox(String mailbox) throws DBException {
		TotalListItem[] listItems = getList(mailbox);
		for (TotalListItem item : listItems){
			deleteMail(mailbox, item.getId());
		}
		this.mailboxMap.remove(mailbox);
		this.db.commit();
	}

	@Override
	public String putMail(String toMailbox, String timestamp,  String fromIPAddress, byte[] listData) throws DBException {
		try {
			String key = UUID.randomUUID().toString();
			TotalListItem mailInfo = new TotalListItem(key, timestamp, fromIPAddress, listData);
			this.letterInfo.add(Fun.t2(toMailbox, key));
			this.letters.put(key, new ByteHolder(mailInfo));
			this.db.commit();
			return key;
		} catch (JavaInstallationMissingComponentsException e) {
			throw new DBException(e);
		}
	}
	
	private Semaphore getLockForFilename(String filename){
		synchronized(this.lockHolder){
			Semaphore lock = this.lockHolder.get(filename);
			if (lock == null){
				lock = new Semaphore(Integer.MAX_VALUE);
				this.lockHolder.put(filename, lock);
			}
			return lock;
		}		
	}

	@Override
	public InputStream getAttachment(final String id) throws DBException {
		File data = new File(DATADIRECTORY, id);
		if (!data.exists()){
			throw new DBException("There is no data with the provided id");
		}
		try{
			//interning filename to make sure same filename string object is maintained in WeakHashMap for the duration of Input stream's use
			//(LockingInputStream maintains a strong reference to it)
			return new LockingInputStream(data, getLockForFilename(id), id.intern());
		}catch(IOException | InterruptedException e){
			throw new DBException(e);
		}
	}	
	
	@SuppressWarnings("resource")
	@Override
	public IDAndOutputStream putAttachment() throws DBException {
		//interning filename to make sure same filename string object is maintained in WeakHashMap for the duration of Input stream's use
		//(LockingOutputStream maintains a strong reference to it)
		String attachmentId = UUID.randomUUID().toString().intern();
		try {
			File dataHolder = new File(DATADIRECTORY, attachmentId);
			dataHolder.createNewFile();
			return new IDAndOutputStream(attachmentId, new LockingOutputStream(dataHolder, getLockForFilename(attachmentId), attachmentId));
		} catch (IOException | InterruptedException e) {
			throw new DBException(e);
		}
	}
	
	@Override
	public void addMailboxToAttachment(String mailboxName, String attachmentId) throws DBException{
		final String mailboxes = this.attachmentInfo.get(attachmentId);
		String newBoxes;
		if (mailboxes == null){
			newBoxes = mailboxName + "\n";
		}else{
			newBoxes = mailboxes + mailboxName + "\n";
		}
		this.attachmentInfo.put(attachmentId, newBoxes);
		this.db.commit();
	}
	
	@Override
	public final void deleteAttachment(String mailboxName, String attachmentId) throws DBException {
		boolean delete = false;
		final String mailboxes = this.attachmentInfo.get(attachmentId);
		if (mailboxes == null || mailboxes.length() == 0){
			delete = true;
		}else{
			String newString = mailboxes.replaceAll(mailboxName + "\n", "");
			if (newString.length() == 0){
				delete = true;
				this.attachmentInfo.remove(attachmentId);
			}else{
				this.attachmentInfo.put(attachmentId, newString);
			}
			this.db.commit();
		}
		try{
			if (delete){
				//interning filename and maintaining reference to it to make sure filename string object is 
				//maintained in WeakHashMap for the duration of this delete
				String fileName = attachmentId.intern();
				File file = new File(DATADIRECTORY, fileName);
				if (file.exists()){
					Semaphore lock = getLockForFilename(fileName);
					lock.acquire(Integer.MAX_VALUE);
					file.delete();
					lock.release(Integer.MAX_VALUE);
				} 
			}
		}catch(InterruptedException e){
			throw new DBException(e);
		}
	}

	@Override
	public void deleteMail(final String mailboxName, final String mailId) throws DBException {
		this.letterInfo.remove(Fun.t2(mailboxName, mailId));
		this.letters.remove(mailId);
		this.db.commit();	
	}

	@Override
	public TotalListItem[] getList(String mailboxName) throws DBException {
		try {
			List<TotalListItem> items = new ArrayList<>();
			for (String id : Bind.findVals2(this.letterInfo, mailboxName)){
				ByteHolder holder = this.letters.get(id);
				items.add(TotalListItem.putBytes(holder.bytes));
			}
			return items.toArray(new TotalListItem[items.size()]);
		} catch (JavaInstallationMissingComponentsException | FailedCryptException e) {
			throw new DBException(e);
		}
	}

	@Override
	public String[] listUsers() throws DBException {
		NavigableSet<String> keys = this.mailboxMap.keySet();
		return keys.toArray(new String[0]);
	}

}
