package com.jmw.sda.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import com.jmw.sda.crypto.Utils;

public class Configuration {
	protected final File config;
	private static final String PRIVATEKEY = "privatekey";
	private static final String PUBLICKEY = "publickey";
	private static final String STRENGTH = "strength";

	public Configuration(File directory) throws IOException{
		this.config = new File(directory, "/securedataauthority.properties");
		this.config.createNewFile();	
	}
	
	private synchronized final void saveProperty(String key, String value) throws IOException{
		Properties props = new Properties();
		try(InputStream fis = Utils.inputStreamForFile(this.config)){
			props.load(fis);
			props.put(key, value);
		}
		try(OutputStream fos = Utils.outputStreamForFile(this.config)){
			props.store(fos, "");
		}

	}
	private final String getProperty(String key) throws IOException{
		try(InputStream fis = Utils.inputStreamForFile(this.config);){
			Properties props = new Properties();
			props.load(fis);
			return props.getProperty(key);
		}
	}
	
	public final void setPrivateKey(String privateKey) throws IOException{
		saveProperty(PRIVATEKEY, privateKey);
	}
	public final String getPrivateKey() throws IOException{
		return getProperty(PRIVATEKEY);
	}
	public final void setPublicKey(String publicKey) throws IOException{
		saveProperty(PUBLICKEY, publicKey);
	}
	public final String getPublicKey() throws IOException{
		return getProperty(PUBLICKEY);
	}
	public final void setStrength(String strength) throws IOException{
		saveProperty(STRENGTH, strength);
	}
	public final String getStrength() throws IOException{
		return getProperty(STRENGTH);
	}

}

