package com.jmw.sda.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import com.jmw.sda.communication.CommunicationFailureException;
import com.jmw.sda.communication.WebServiceClient;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.IPublicKey;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.Utils;
import com.jmw.sda.utils.Configuration;
import com.jmw.sda.view.AdminPanel;
import com.jmw.sda.view.event.HostDeleteEvent;
import com.jmw.sda.view.event.HostDeleteListener;

public class AdminController {
	protected AdminPanel view;
	protected List<HostDeleteListener> hostDeleteListeners = new ArrayList<>();
	public AdminController(AdminPanel panel) {
		this.view = panel;
		this.view.addHostChangeListener(new HostChangeListener());
		this.view.addAllHosts(Configuration.getHosts());
		this.view.addRefreshListener(new RefreshListener());
		this.view.addHostDeleteListener(new HostDeleteClickListener());
	}
	
	public void addHostDeleteListener(HostDeleteListener listener){
		this.hostDeleteListeners.add(listener);
	}
	
	public void removeHostDeleteListener(HostDeleteListener listener){
		this.hostDeleteListeners.remove(listener);
	}
	
	class RefreshListener implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent e) {
			AdminController.this.view.addAllHosts(Configuration.getHosts());
		}
	}
	
	class HostChangeListener implements ItemListener{

		@Override
		public void itemStateChanged(ItemEvent e) {
			String newHost = (String)e.getItem();
			String hostKey = "";
			String myPublicKey = "";
			String mailboxName = "";
			if (newHost != null){
				hostKey = Configuration.getHostKeyString(newHost);
				myPublicKey = Configuration.getMyPublicKeyString(newHost);
				mailboxName = Configuration.getMyMailboxName(newHost);
			}
			AdminController.this.view.setHostKey(hostKey);
			AdminController.this.view.setMyPublicKey(myPublicKey);
			AdminController.this.view.setMailboxName(mailboxName);
			IPublicKey publicKey;
			try {
				publicKey = Configuration.getHostPublicKey(newHost);
				if (publicKey != null){
					AdminController.this.view.setBitStrength(Integer.toString(publicKey.bitLength()));
				}
			} catch (JavaInstallationMissingComponentsException
					| FailedCryptException e1) {
				AdminController.this.view.setInfo(Utils.ppStackTrace(e1));
			}
		}
	}
	
	class HostDeleteClickListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {			
			String host = AdminController.this.view.getSelectedHost();

			if (host != null){
				int result = JOptionPane.showConfirmDialog (AdminController.this.view, 
						String.format("Are you sure you want to delete this host?"),
						"Confirm",
						JOptionPane.YES_NO_OPTION);
				if (result == JOptionPane.YES_OPTION){	
					try {
						WebServiceClient.deleteMailbox(host);
						List<String> users = Configuration.getMailboxUsers(host);
						for (String user : users){
							Configuration.deleteUser(user, host);
						}
						Configuration.deleteMyMailboxName(host);
						Configuration.deleteMyPrivateKey(host);
						Configuration.deleteMyPublicKey(host);
						Configuration.deleteHost(host);
						if (Configuration.getCurrentHost().equals(host)){
							Configuration.clearCurrentHost();
						}
						AdminController.this.view.addAllHosts(Configuration.getHosts());
						HostDeleteEvent event = new HostDeleteEvent(host);
						for(HostDeleteListener hostDeleteListener : AdminController.this.hostDeleteListeners){
							hostDeleteListener.hostDeleted(event);
						}
					} catch (JavaInstallationMissingComponentsException
							| FailedCryptException | CommunicationFailureException e1) {
						AdminController.this.view.setInfo(Utils.ppStackTrace(e1));
					}
				}
			}
			
		}
	}


}
