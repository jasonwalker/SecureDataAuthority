package com.jmw.sda.crypto;

import java.io.IOException;
import java.io.InputStream;

public class VersionString {

	protected final String versionString;
	
	public VersionString(IPrivateKey privateKey){
        ClassLoader classloader = this.getClass().getClassLoader(); 
        String version;
        try(InputStream stream = classloader.getResourceAsStream("version.txt")){
			version = new String(Utils.readFully(stream), Utils.ENCODING);
		} catch (IOException e) {
			version = "not found";
		}
		String info =
				"Version\n" + //0
				version +     //1
				"\nRSA Strength\n" + //2 
		        privateKey.bitLength() + //3 
		        "\nAES Strength\n" + //4
		        AbstractCrypto.SECRET_STRENGTH + //5 
		        "\nsignature"; //6	
		String signature;
		try {
			 signature = AbstractCrypto.getCrypto().sign(privateKey, info);
		} catch (JavaInstallationMissingComponentsException
				| FailedCryptException e) {
			signature = "Signature failed.";
		}
		this.versionString = info + 
				"\n" + 
				signature;      //7
	}
	
	public String getVersion(){
		return this.versionString;
	}
	public static String getSignature(String inVersionStr){
		return inVersionStr.split("\n")[7];
	}
	public static String getSignedPart(String inVersionStr){
		 int index = inVersionStr.lastIndexOf("\n");
		 return inVersionStr.substring(0, index);
		 
	}
	public static int getAESStrength(String inVersionStr){
		return Integer.parseInt(inVersionStr.split("\n")[5]);
	}
	public static int getRSAStrength(String inVersionStr){
		return Integer.parseInt(inVersionStr.split("\n")[3]);
	}
	public static void main(String[] args) throws JavaInstallationMissingComponentsException, FailedCryptException{
		AbstractCrypto crypto = AbstractCrypto.getCrypto();
		RSAKeys keys = crypto.makeRSAKeys(1024);
		VersionString vs = new VersionString(keys.getPrivateKey());
		String versionString = vs.getVersion();
		System.out.println(VersionString.getAESStrength(versionString));
		System.out.println(VersionString.getRSAStrength(versionString));
		System.out.println(VersionString.getSignature(versionString));
		System.out.println("***");
		System.out.println(VersionString.getSignedPart(versionString));
		System.out.println("***");
		System.out.println(crypto.verify(keys.getPublicKey(), VersionString.getSignature(versionString), 
				VersionString.getSignedPart(versionString)));
	}
	



}
