package com.jmw.sda;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.jmw.sda.Utils.Configuration;
import com.jmw.sda.crypto.AbstractCrypto;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.IPrivateKey;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.RSAKeys;
import com.jmw.sda.crypto.Utils;

public class OnStartup implements ServletContextListener {
	protected static final AbstractCrypto crypto = AbstractCrypto.getCrypto();
	public OnStartup() {
	}

	protected static int getRSAStrength(){
    	File strengthFile = new File("./strength");
    	int strength = 3072;
    	String strengthString = "";
    	if (strengthFile.exists()){
    		try(FileInputStream fis = new FileInputStream(strengthFile)){
    			Properties props = new Properties();
    			props.load(fis);
    			strengthString = props.getProperty("strength", "3072");
    			strength = Integer.parseInt(strengthString);
    		} catch(NumberFormatException e){
    			System.out.println("Could not parse RSA strength value from string: " + strengthString + ". Using default of 3072.");
    		} catch (IOException e) {
    			System.out.println("Could not read \"strength\" file, using RSA default strength of 3072");
				strength = 3072;
			}
    	}else{
    		System.out.println("No RSA strength configured, using default strength of 3072");
    	}
    	return strength;

	}
	
    protected static void generateServerKeys(int strength, Configuration config) throws JavaInstallationMissingComponentsException, IOException, FailedCryptException{
    	System.out.println("Generating server " + strength + "-bit RSA key pair, please wait...");
    	//check if strength file exists
    	RSAKeys keys = crypto.makeRSAKeys(strength);
		config.setPrivateKey(keys.getPrivateKeyString());
		config.setPublicKey(keys.getPublicKeyString());	
		System.out.println("Server's " + strength + "-bit RSA key pair generated");
    }
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
        try{
        	Configuration config = new Configuration(new File("."));
        	String privateKey = config.getPrivateKey();
        	int strength = getRSAStrength();
        	if (privateKey == null || privateKey.equalsIgnoreCase("")){
        		generateServerKeys(strength, config);
        	}else{
        		IPrivateKey key = crypto.stringToPrivateKey(privateKey);
        		if (key.bitLength() != strength){
        			System.out.println("ERROR**************************************************");
        			System.out.println("**************************************************");
        			System.out.println("Configured strength of server (" + strength + ") does not match server's" + 
        					" generated private key strength (" + key.bitLength() + "). Please either adjust the " +
        					"configured strength or erase the \"securedataauthority.properties\" file and generate new " +
        					"server keys.  (Note that the second option will invalidate all existing clients)");
        			System.out.println("**************************************************");
        			System.out.println("**************************************************");
        			System.exit(1);
        		}
        	}
        }catch(IOException | JavaInstallationMissingComponentsException | FailedCryptException e){
        	Utils.ppStackTrace(e);
        }

	}
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		//nothing to do
	}
	
	public static void main(String[] args){
        try{
        	Configuration config = new Configuration(new File("."));
        	String privateKey = config.getPrivateKey();
        	if (privateKey == null || privateKey.equalsIgnoreCase("")){
        		int strength = getRSAStrength();
        		generateServerKeys(strength, config);
        	}
        }catch(IOException | JavaInstallationMissingComponentsException | FailedCryptException e){
        	Utils.ppStackTrace(e);
        }		
	}
	
}
