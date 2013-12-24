package com.jmw.sda.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.apache.commons.lang3.StringUtils;

import com.jmw.sda.communication.CommunicationFailureException;
import com.jmw.sda.communication.WebServiceClient;
import com.jmw.sda.crypto.AbstractCrypto;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.IPublicKey;
import com.jmw.sda.crypto.InvalidSignatureException;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.crypto.Utils;
import com.jmw.sda.model.MailToSend;
import com.jmw.sda.model.MailboxCache;
import com.jmw.sda.utils.Configuration;
import com.jmw.sda.view.SendMailPanel;
import com.jmw.sda.view.ViewUtils;
import com.jmw.sda.view.event.StringClickListener;
import com.jmw.sda.view.event.StringEvent;

public class SendMailController {
	MailToSend model;
	SendMailPanel view;
	JFrame currentFrame;
	protected static final AbstractCrypto crypto = AbstractCrypto.getCrypto();
	public SendMailController(SendMailPanel view, MailToSend model) {
		this.view = view;
		this.model = model;
		this.view.addSendListener(new SendMailListener());
		this.currentFrame = ViewUtils.createNewWindow(view , "Send Mail");
		this.view.addAttachListener(new AttachListener());
		this.view.addDeleteAttachmentListener(new DeleteAttachmentListener());
		this.view.addRefreshToMailboxListener(new RefreshUsersListener());
		MailboxCache.loadLocalMailboxes();
		this.view.updateRecipientMailboxes(MailboxCache.getUsers());
	}
	
	class AttachListener implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent arg) {
			JFileChooser chooser = new JFileChooser();
			chooser.setMultiSelectionEnabled(true);
			int returnVal = chooser.showOpenDialog(SendMailController.this.view);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				SendMailController.this.model.addAttachments(chooser.getSelectedFiles());
				SendMailController.this.view.updateAttachments(SendMailController.this.model.getAttachmentNames());
			}				
		}		
	}
	
	class RefreshUsersListener implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				MailboxCache.loadAllMailboxesFromServer();
				SendMailController.this.view.updateRecipientMailboxes(MailboxCache.getUsers());
			} catch (IOException | JavaInstallationMissingComponentsException
					| FailedCryptException | InvalidSignatureException | CommunicationFailureException e1) {
				SendMailController.this.view.setInfo(Utils.ppStackTrace(e1));
			}
		}
		
	}
	
	class DeleteAttachmentListener implements StringClickListener{
		@Override
		public void attachmentClicked(StringEvent event) {
			int result = JOptionPane.showConfirmDialog (SendMailController.this.view, 
					String.format("Are you sure you want to remove the attachment '%s'?", event.getName()),
					"Confirm",
					JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.YES_OPTION){
				SendMailController.this.model.removeAttachment(event.getIndex());
				SendMailController.this.view.updateAttachments(SendMailController.this.model.getAttachmentNames());
			}
		}
	}
	
	class SendMailListener implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				SendMailController.this.view.setInfo("Sending message...");
				String[] toMailboxes = SendMailController.this.view.getDestinationMailboxes();
				if (toMailboxes == null || toMailboxes.length == 0){
					SendMailController.this.view.setInfo("Please supply a mailbox");
					return;
				}
				IPublicKey[] publicKeys = new IPublicKey[toMailboxes.length];
				for (int i = 0 ; i < toMailboxes.length ; i++){
					IPublicKey mailboxKey = MailboxCache.getMailbox(toMailboxes[i]);
					if (mailboxKey == null){
						SendMailController.this.view.setInfo(String.format("Mailbox '%s' does not exist on the server", toMailboxes[i]));
						return;
					}
					publicKeys[i] = mailboxKey;
				}
				String fromMailbox = Configuration.getMyMailboxName();
				String subject = SendMailController.this.view.getSubject();
				String note = SendMailController.this.view.getNote();
				List<File> attachments = SendMailController.this.model.getAttachments();
				if (fromMailbox == null){
					SendMailController.this.view.setInfo("Please create a mailbox before sending");
					return;
				}
				new SendMailWorker(publicKeys, toMailboxes, fromMailbox, subject, note, attachments).execute();

			} catch (JavaInstallationMissingComponentsException | 
					FailedCryptException | IOException | 
					InvalidSignatureException | 
					CommunicationFailureException exception) {
				SendMailController.this.view.setInfo(Utils.ppStackTrace(exception));
			}
		}
	}
	class SendMailWorker extends SwingWorker<Boolean , String>{
		protected IPublicKey[] toBoxKey;
		protected String[] toBoxes;
		protected String fromBox;
		protected String subject;
		protected String note;
		protected List<File> attachments;
		public SendMailWorker(IPublicKey[] toBoxKey, String[] toBoxes, String fromBox, String subject, String note,
				List<File> attachments){
			this.toBoxKey = toBoxKey;
			this.toBoxes = toBoxes;
			this.fromBox = fromBox;
			this.subject = subject;
			this.note = note;
			this.attachments = attachments;
		}
		@Override
		protected Boolean doInBackground()  {
			try {
				WebServiceClient.sendMail(this.toBoxKey, this.toBoxes, this.fromBox, this.subject, 
						this.note, this.attachments);
				publish(String.format("Mail successfully sent to: %s", StringUtils.join(this.toBoxes, ",")));
				return true;
			} catch (IOException | JavaInstallationMissingComponentsException | FailedCryptException | CommunicationFailureException exception) {
				publish(Utils.ppStackTrace(exception));
			} 
			return false;
		}
		@Override
		protected void done(){
			
			try{
				if(get()){
					SendMailController.this.currentFrame.dispose();
				}
			}catch(ExecutionException | InterruptedException e){
				publish(Utils.ppStackTrace(e));
			}
		}
		@Override
		protected void process(List<String> info){
			SendMailController.this.view.setInfo(info.get(info.size()-1));
		}
	}	
}
