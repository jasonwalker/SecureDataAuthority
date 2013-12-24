package com.jmw.sda.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import com.jmw.sda.communication.CommunicationFailureException;
import com.jmw.sda.communication.WebServiceClient;
import com.jmw.sda.crypto.AbstractCrypto;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.IPublicKey;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.Utils;
import com.jmw.sda.crypto.VersionString;
import com.jmw.sda.model.SecretKeyHolder;
import com.jmw.sda.utils.Configuration;
import com.jmw.sda.view.CreateMailboxPanel;
import com.jmw.sda.view.MainPanel;
import com.jmw.sda.view.event.ConfigurationChangeListener;
import com.jmw.sda.view.event.ConfigurationEvent;
import com.jmw.sda.view.event.MailboxNameChangeListener;
import com.jmw.sda.view.event.NameChangeEvent;

public class CreateMailboxController {
	SecretKeyHolder model;
	CreateMailboxPanel view;
	MainPanel mainPanel;
	protected static AbstractCrypto crypto = AbstractCrypto.getCrypto();
	protected List<MailboxNameChangeListener> nameChangeListeners = new ArrayList<>();
	protected List<ConfigurationChangeListener> hostAddedListener = new ArrayList<>();

	public CreateMailboxController(CreateMailboxPanel view, SecretKeyHolder model) {
		this.view = view;
		this.model = model;
		view.addCreateListener(new KeyMadeListener());
	}
	
	class KeyMadeListener implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent arg0) {
			String serverAddress = CreateMailboxController.this.view.getServersAddress().trim();
			if (Configuration.getHosts().contains(serverAddress)){
				CreateMailboxController.this.view.setInfo(
						String.format("You have already created a user for the server \"%s\" under the name \"%s\"", serverAddress, Configuration.getMyMailboxName(serverAddress)));
				return;				
			}
			
			
			String serversPublicKey = CreateMailboxController.this.view.getServersKey();
			if (serversPublicKey == null){ 
				CreateMailboxController.this.view.setInfo("Please provide a public key for the server");
				return;
			}
			serversPublicKey = serversPublicKey.trim();
			String mailboxName = CreateMailboxController.this.view.getMailboxName();
			if (mailboxName == null){
				CreateMailboxController.this.view.setInfo("Please provide a mailbox name");
				return;
			}
			mailboxName = mailboxName.trim();
			if (mailboxName.equalsIgnoreCase("")){
				CreateMailboxController.this.view.setInfo("Please provide a mailbox name");
				return;
			}
			new CreateMailboxWorker(mailboxName, serversPublicKey).execute();
		}
	}
	public void addMailboxNameChangeListener(MailboxNameChangeListener listener){
		this.nameChangeListeners.add(listener);
	}
	public void removeMailboxNameChangeListener(MailboxNameChangeListener listener){
		this.nameChangeListeners.remove(listener);
	}
	public void addConfigurationChangeListener(ConfigurationChangeListener listener){
		this.hostAddedListener.add(listener);
	}
	public void removeConfigurationChangeListener(ConfigurationChangeListener listener){
		this.hostAddedListener.remove(listener);
	}
	class CreateMailboxWorker extends SwingWorker<String[], String>{
		protected String mailboxName;
		protected String serversPublicKey;
		protected boolean error;
		public CreateMailboxWorker(String mailboxName, String serversPublicKey){
			this.mailboxName = mailboxName;
			this.serversPublicKey = serversPublicKey;
		}
		@Override
		protected String[] doInBackground()  {
			try{
				String url = CreateMailboxController.this.view.getServersAddress();
				publish(String.format("Verifying server exists at: %s", url));
				//verify server is up
				String serverId = WebServiceClient.getServerId(url);
				publish("Server id is: " + serverId);
				int rsaStrength = VersionString.getRSAStrength(serverId);
				String signature = VersionString.getSignature(serverId);
				IPublicKey serverKey = crypto.stringToPublicKey(this.serversPublicKey);
				boolean verified = crypto.verify(serverKey, signature, VersionString.getSignedPart(serverId));
				if (!verified){
					this.error = true;
					publish("The server failed to verify.  Is the server's public key correct?");
					return null;
				}
				String[] valsList = new String[4];
				valsList[0] = url;
				valsList[1] = this.mailboxName;
				String privateKey;
				String publicKey;
				if (Configuration.hasHost(url)){
					publish(String.format("Sending previously-generated %d bit key pair...", rsaStrength));
					privateKey = Configuration.getMyPrivateKeyString();
					publicKey = Configuration.getMyPublicKeyString();
				}else{
					publish(String.format("Generating %d bit key pair...", rsaStrength));
					CreateMailboxController.this.model.setRsaKeys(crypto.makeRSAKeys(rsaStrength));
					publish("Key pair generated. Contacting server...");
					privateKey = CreateMailboxController.this.model.getRsaKeys().getPrivateKeyString();
					publicKey = CreateMailboxController.this.model.getRsaKeys().getPublicKeyString();					
				}
				valsList[2] = privateKey;
				valsList[3] = publicKey;
				String ret = WebServiceClient.createMailbox(url, this.mailboxName,  publicKey, this.serversPublicKey);
				if (ret != null){
					publish(ret);
					this.error = true;
				}else{
					this.error = false;
				}
				return valsList;
			}catch(CommunicationFailureException e){
				System.out.println(Utils.ppStackTrace(e));
				this.error = true;
				publish(Utils.ppStackTrace(e));
			} catch(FailedCryptException | JavaInstallationMissingComponentsException e){
				System.out.println(Utils.ppStackTrace(e));
				this.error = true;
				publish("The server's public key failed to encrypt.  Is it correct?");
			}catch(IOException e){
				System.out.println(Utils.ppStackTrace(e));
				this.error = true;
				publish("Could not contact server. " + e.getMessage());
			}
			return null;
		}
		@Override
		protected void done(){
			try{
				if (!this.error){
					String[] vals = get();
					String newHost = vals[0];
					Configuration.addHost(newHost, this.serversPublicKey);
					Configuration.putMyMailboxName(newHost, vals[1]);
					Configuration.putMyPrivateKey(newHost, vals[2]);
					Configuration.putMyPublicKey(newHost, vals[3]);
					for (MailboxNameChangeListener listener : CreateMailboxController.this.nameChangeListeners){
						listener.nameChanged(new NameChangeEvent(this.mailboxName));
					}
					publish(String.format("Mailbox '%s' successfully created.", this.mailboxName));
					for (ConfigurationChangeListener listener : CreateMailboxController.this.hostAddedListener){
						listener.hostAdded(new ConfigurationEvent(newHost));
					}
				}
			}catch(ExecutionException | 
					InterruptedException e){
				publish(Utils.ppStackTrace(e));
			}
		}
		@Override
		protected void process(List<String> info){
			CreateMailboxController.this.view.setInfo(info.get(info.size()-1));
		}
	}
}
