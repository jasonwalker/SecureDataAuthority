package com.jmw.sda.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentNavigableMap;

import org.mapdb.BTreeKeySerializer;
import org.mapdb.Bind;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;

import com.jmw.sda.crypto.AbstractCrypto;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.IPrivateKey;
import com.jmw.sda.crypto.IPublicKey;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.view.event.ConfigurationChangeListener;
import com.jmw.sda.view.event.ConfigurationEvent;


public class Configuration {
	protected static final AbstractCrypto crypto = AbstractCrypto.getCrypto();
	protected static final DB db;
	protected static final NavigableSet<Fun.Tuple2<String,String>> userList;
	protected static final NavigableSet<Fun.Tuple2<String,String>> hostList;
	protected static final ConcurrentNavigableMap<String,String> data;
	protected static final List<ConfigurationChangeListener> configListeners = new ArrayList<>();
	
	protected static IPrivateKey currentPrivateKey;
	protected static IPublicKey currentPublicKey;
	protected static IPublicKey currentHostKey;
	

	protected static String currentHost;
	static {
		db = DBMaker.newFileDB(new File("./db"))
                .closeOnJvmShutdown()
                .make();
		
        if (db.exists("userList")){
        	userList = db.getTreeSet("userList");
        }else{
        	userList = db.createTreeSet("userList")
                    .serializer(BTreeKeySerializer.TUPLE2)
                    .make();
        }
        if (db.exists("hostList")){
        	hostList = db.getTreeSet("hostList");
        }else{
        	hostList = db.createTreeSet("hostList")
                    .serializer(BTreeKeySerializer.TUPLE2)
                    .make();
        }
        data = db.getTreeMap("data");
        List<String> hosts = getHosts();
        if (getCurrentHost() == null || getCurrentHost().equalsIgnoreCase("")){
	        if (hosts.size() > 0){
	        	setCurrentHost(hosts.get(0));
	        }
        }
	}
	
	public static final void addConfigurationChangeListener(ConfigurationChangeListener listener){
		configListeners.add(listener);
	}
	
	public static final void removeConfigurationChangeListener(ConfigurationChangeListener listener){
		configListeners.remove(listener);
	}
	
	
	protected static final void addToDB(String key, String value){
		data.put(key, value);
		db.commit();
	}
	
	protected static final void deleteFromDB(String key){
		data.remove(key);
		db.commit();
	}
	
	protected static final List<String> searchDB(final NavigableSet<Fun.Tuple2<String,String>> set, final String name){
		List<String> retVal = new ArrayList<>();
		if (name != null){
			for (String value : Bind.findVals2(set, name)){
				retVal.add(value);
			}
		}
		return retVal;		
	}
	
	public static final void setCurrentHost(String host){
		//remove cached values for keys
		currentPrivateKey = null;
		currentPublicKey = null;
		currentHostKey = null;
		currentHost = host;
		addToDB("currentHost", host);
		ConfigurationEvent event = new ConfigurationEvent(host);
		for (ConfigurationChangeListener listener : configListeners){
			listener.hostAdded(event);
		}
	}
	
	public static final void clearCurrentHost(){
		currentPrivateKey = null;
		currentPublicKey = null;
		currentHostKey = null;
		currentHost = null;
		deleteFromDB("currentHost");	
	}
	
	public static final String getCurrentHost(){
		if (currentHost == null){
			currentHost = data.get("currentHost");
		}
		return currentHost;
	}
	
	public static final void addHost(String url, String publicKey){
		hostList.add(Fun.t2("host",  url));
		addToDB(String.format("hostKey_%s", url), publicKey);
	}
	
	public static final void deleteHost(String url){
		hostList.remove(Fun.t2("host", url));
		deleteFromDB(String.format("hostKey_%s", url));
	}
	public static final List<String> getHosts(){
		return searchDB(hostList, "host");
	}
	public static final boolean hasHost(String host){
		return data.containsKey(String.format("hostKey_%s", host));
	}
	public static final String getHostKeyString(String host){
		return data.get(String.format("hostKey_%s", host));
	}
	public static final void addUser(String name, String publicKey){
		String dbKey = String.format("user_%s", getCurrentHost());
		userList.add(Fun.t2(dbKey, name));
		addToDB(String.format("%s_%s", getCurrentHost(), name), publicKey);
	}
	public static final void deleteUser(String name, String host){
		String dbKey = String.format("user_%s", host);
		userList.remove(Fun.t2(dbKey, name));
		deleteFromDB(String.format("%s_%s", host, name));
	}
	public static final boolean hasMailbox(String name){
		return data.containsKey(String.format("%s_%s", getCurrentHost(), name));
	}
	public static final List<String> getMailboxUsers(String host){
		return searchDB(userList, String.format("user_%s", host));
	}
	public static final List<String> getMailboxUsers(){
		return getMailboxUsers(getCurrentHost());
	}
	public static final String getMailboxPublicKey(String name){
		return data.get(String.format("%s_%s", getCurrentHost(), name));
	}
	public static final void putMyPrivateKey(String host, String privateKey){
		addToDB(String.format("privateKey_%s", host), privateKey);
	}
	public static final void deleteMyPrivateKey(String host){
		deleteFromDB(String.format("privateKey_%s", host));
	}

	public static final void putMyPublicKey(String host, String publicKey){
		addToDB(String.format("publicKey_%s", host), publicKey);
	}
	public static final void deleteMyPublicKey(String host){
		deleteFromDB(String.format("publicKey_%s", host));
	}

	public static final void putMyMailboxName(String host, String name){
		addToDB(String.format("mailbox_%s", host), name);
	}
	public static final void deleteMyMailboxName(String host){
		deleteFromDB(String.format("mailbox_%s", host));
	}
	public static final String getMyMailboxName(String host){
		return data.get(String.format("mailbox_%s", host));
	}
	public static final String getMyMailboxName(){
		return getMyMailboxName(getCurrentHost());
	}
	public static String getMyPublicKeyString(String host){
		return data.get(String.format("publicKey_%s", host));
	}
	public static String getMyPublicKeyString(){
		return getMyPublicKeyString(getCurrentHost());
	}
	public static String getMyPrivateKeyString(String host){
		return data.get(String.format("privateKey_%s", host));
	}
	public static String getMyPrivateKeyString(){
		return getMyPrivateKeyString(getCurrentHost());
	}
	public static IPrivateKey getMyPrivateKey(String host) throws JavaInstallationMissingComponentsException, FailedCryptException{
		String privateString = getMyPrivateKeyString(host);
		if (privateString != null){
			return crypto.stringToPrivateKey(privateString);
		}
		return null;
	}
	public static IPrivateKey getMyPrivateKey() throws JavaInstallationMissingComponentsException, FailedCryptException{
		if (currentPrivateKey == null){
			String privateString = getMyPrivateKeyString();
			if (privateString != null){
				currentPrivateKey = crypto.stringToPrivateKey(privateString);
			}else{
				return null;
			}
		}
		return currentPrivateKey;
	}
	public static IPublicKey getMyPublicKey() throws JavaInstallationMissingComponentsException, FailedCryptException{
		if (currentPublicKey == null){
			String publicString = getMyPublicKeyString();
			if (publicString != null){
				currentPublicKey = crypto.stringToPublicKey(publicString);
			}else{
				return null;
			}
		}
		return currentPublicKey;
	}
	public static IPublicKey getHostPublicKey(String host) throws JavaInstallationMissingComponentsException, FailedCryptException{
		String hostPublicString = getHostKeyString(host);
		if (hostPublicString != null){
			return crypto.stringToPublicKey(hostPublicString);
		}
		return null;
	}
	public static IPublicKey getHostPublicKey() throws JavaInstallationMissingComponentsException, FailedCryptException{
		if (currentHostKey == null){
			currentHostKey = getHostPublicKey(Configuration.getCurrentHost());
		}
		return currentHostKey;
	}
	
	private static void printCollection(String label, Collection<String> coll){
		System.out.println(label);
		for(String s : coll){
			System.out.println(s);
		}
	}
	public static void main(String[] args){
		addHost("host1", "hostkey");
		setCurrentHost("host1");
		addUser("j1", "publicKey1");
		addUser("j2", "publicKey1");
		printCollection("Users", getMailboxUsers("host1"));
		deleteUser("j2", "host1");
		printCollection("Users", getMailboxUsers());
	}

}
