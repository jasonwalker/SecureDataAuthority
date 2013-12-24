package com.jmw.sda.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import com.jmw.sda.communication.CommunicationFailureException;
import com.jmw.sda.communication.WebServiceClient;
import com.jmw.sda.crypto.AbstractCrypto;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.InvalidSignatureException;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.Utils;
import com.jmw.sda.model.EmailList;
import com.jmw.sda.model.MailToSend;
import com.jmw.sda.model.MailboxCache;
import com.jmw.sda.transport.objects.EncryptedListItem;
import com.jmw.sda.transport.objects.TotalListItem;
import com.jmw.sda.utils.Configuration;
import com.jmw.sda.view.ListMailPanel;
import com.jmw.sda.view.ReadMailPanel;
import com.jmw.sda.view.SendMailPanel;
import com.jmw.sda.view.event.ConfigurationChangeListener;
import com.jmw.sda.view.event.ConfigurationEvent;
import com.jmw.sda.view.event.HostDeleteEvent;
import com.jmw.sda.view.event.HostDeleteListener;
import com.jmw.sda.view.event.MailboxNameChangeListener;
import com.jmw.sda.view.event.NameChangeEvent;

public class ListMailController implements ConfigurationChangeListener, HostDeleteListener{
	EmailList model;
	ListMailPanel view;
	protected static AbstractCrypto crypto = AbstractCrypto.getCrypto();
	protected List<MailboxNameChangeListener> nameChangeListeners = new ArrayList<>();
	
	public ListMailController(ListMailPanel view, EmailList model){
		this.view = view;
		this.model = model;
		this.view.setModel(this.model);
		view.addAllHosts(Configuration.getHosts());
		if (Configuration.getCurrentHost() != null){
			view.setCurrentHost(Configuration.getCurrentHost());
			(new RefreshListWorker()).execute();
		}
		view.addRefreshListener(new RefreshCalledListener());
		view.addCreateListener(new CreateMailListener());
		view.addReadListener(new ReadMailListener());
		view.addMouseListener(new DoubleClickListener());
		view.addDeleteListener(new DeleteMailListener());
		view.addHostChangeListener(new HostChangeListener());
	}
	
	@SuppressWarnings("unused")
	protected void readMail() throws FailedCryptException, JavaInstallationMissingComponentsException, IOException, IllegalStateException, CommunicationFailureException, InvalidSignatureException{
		int selectedRow = this.view.getSelectedRow();
		if (-1 == selectedRow){
			return;
		}
    	final ReadMailPanel readMailPanel = new ReadMailPanel();
    	new ReadMailController(readMailPanel, 
    			this.model.getItemAtRow(this.view.getSelectedRow()));

	}
	
	public void addMailboxNameChangeListener(MailboxNameChangeListener listener){
		this.nameChangeListeners.add(listener);
	}
	public void removeMailboxNameChangeListener(MailboxNameChangeListener listener){
		this.nameChangeListeners.remove(listener);
	}
	
	class DoubleClickListener extends MouseAdapter{

		@Override
		public void mouseClicked(MouseEvent evt) {
			if (evt.getClickCount() == 2){
				try {
					readMail();
				} catch (IllegalStateException
						| FailedCryptException
						| JavaInstallationMissingComponentsException
						| IOException
						| CommunicationFailureException 
						| InvalidSignatureException e) {
					ListMailController.this.view.setInfo(Utils.ppStackTrace(e));
				} 
			}
		}
	}
	
	class RefreshListWorker extends SwingWorker<Void , String>{

		public RefreshListWorker(){}
		@Override
		protected Void doInBackground()  {
			try {
				publish("Retrieving and decrypting mail list");
				String myMailboxName = Configuration.getMyMailboxName();
				if (myMailboxName != null){
					TotalListItem[] listItems = WebServiceClient.getMailList(myMailboxName);
					for (TotalListItem item : listItems){
						//decrypt them now, object will cache decrypted--doing it for the side effect
						item.getEncryptedListItem(Configuration.getMyPrivateKey());
					}
					ListMailController.this.model.setItems(listItems);
					publish("Mail list ready.");
					return null;
				}
				publish("No mailbox currently selected");
				ListMailController.this.model.clearItems();
				return null;
			} catch (CommunicationFailureException e) {
				publish(Utils.ppStackTrace(e));
				return null;
			}catch (FailedCryptException | JavaInstallationMissingComponentsException e) {
				publish(Utils.ppStackTrace(e));
				return null;
			}catch(RuntimeException e){
				System.out.println(Utils.ppStackTrace(e));
				return null;
			}
		}

		@Override
		protected void process(List<String> info){
			ListMailController.this.view.setInfo(info.get(info.size()-1));
		}
	}	
	
	class CreateMailListener implements ActionListener{
		@SuppressWarnings("unused")
		@Override
		public void actionPerformed(ActionEvent evt) {
	    	final SendMailPanel sendMailPanel = new SendMailPanel();
	    	final MailToSend mailToSend = new MailToSend();
			new SendMailController(sendMailPanel, mailToSend);
		}
	}
	class ReadMailListener implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				readMail();
			} catch (IOException 
					| FailedCryptException 
					| JavaInstallationMissingComponentsException 
					| IllegalStateException 
					| CommunicationFailureException 
					| InvalidSignatureException e1) {
				ListMailController.this.view.setInfo(Utils.ppStackTrace(e1));
			}
		}
	}
	class DeleteMailListener implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				int selectedRow = ListMailController.this.view.getSelectedRow();
				if (-1 == selectedRow){
					return;
				}
				int result = JOptionPane.showConfirmDialog (ListMailController.this.view, 
						String.format("Are you sure you want to delete this mail?"),
						"Confirm",
						JOptionPane.YES_NO_OPTION);
				if (result == JOptionPane.YES_OPTION){
					TotalListItem item = ListMailController.this.model.getItemAtRow(selectedRow);
					EncryptedListItem decryptedItem = item.getEncryptedListItem(null);//was already decrypted and cached, don't need private key
					WebServiceClient.deleteMessage(item.getId(), decryptedItem.getAttachments());
					new RefreshListWorker().execute();
				}		    	
			} catch (IOException | FailedCryptException | JavaInstallationMissingComponentsException  | IllegalStateException | CommunicationFailureException e1) {
				ListMailController.this.view.setInfo(Utils.ppStackTrace(e1));
			}
		}
	}
	
	class RefreshCalledListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			(new RefreshListWorker()).execute();
		}
	}	
	
	class HostChangeListener implements ItemListener{

		@Override
		public void itemStateChanged(ItemEvent e) {	
			if (ListMailController.this.view.getCurrentHost() != null && !ListMailController.this.view.getCurrentHost().equalsIgnoreCase(Configuration.getCurrentHost())){
				if (ListMailController.this.view.getCurrentHost() != null){
					Configuration.setCurrentHost(ListMailController.this.view.getCurrentHost());
				}
				
				for (MailboxNameChangeListener listener : ListMailController.this.nameChangeListeners){
					listener.nameChanged(new NameChangeEvent(Configuration.getMyMailboxName()));
				}
				(new RefreshListWorker()).execute();	
				try {
					MailboxCache.loadAllMailboxesFromServer();
				} catch (IOException
						| JavaInstallationMissingComponentsException
						| FailedCryptException | InvalidSignatureException
						| CommunicationFailureException e1) {
					ListMailController.this.view.setInfo(Utils.ppStackTrace(e1));
				}
			}
		}
	}

	@Override
	public void hostAdded(ConfigurationEvent event) {
		if (!event.getHostName().equals(this.view.getCurrentHost())){
			this.view.addHost(event.getHostName());
			this.view.setCurrentHost(event.getHostName());
		}
	}

	@Override
	public void hostDeleted(HostDeleteEvent event) {
		ListMailController.this.view.addAllHosts(Configuration.getHosts());
		if (Configuration.getCurrentHost() != null){
			ListMailController.this.view.setCurrentHost(Configuration.getCurrentHost());
		}
		(new RefreshListWorker()).execute();
	}
}
