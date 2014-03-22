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
import com.jmw.sda.model.MailToForward;
import com.jmw.sda.model.MailboxCache;
import com.jmw.sda.transport.objects.AttachmentInfo;
import com.jmw.sda.transport.objects.EncryptedListItem;
import com.jmw.sda.utils.Configuration;
import com.jmw.sda.view.SendMailPanel;
import com.jmw.sda.view.ViewUtils;
import com.jmw.sda.view.event.StringClickListener;
import com.jmw.sda.view.event.StringEvent;

public class ForwardMailController {

	MailToForward model;
	SendMailPanel view;
	JFrame currentFrame;
	protected static final AbstractCrypto crypto = AbstractCrypto.getCrypto();
	// reply is a specific type of forwarding
	public ForwardMailController(SendMailPanel view, MailToForward model, EncryptedListItem item, boolean reply){
		this.view = view;
		this.model = model;
		for (AttachmentInfo info : item.getAttachments()){
			model.addAttachment(info);
		}
		view.updateAttachments(model.getAttachmentNames());
		this.view.setNote(item.getNote());
		this.view.setSubject(item.getSubject());
		this.view.addSendListener(new ForwardMailListener());
		String title = reply ? "Reply To Mail" : "Forward Mail";
		this.currentFrame = ViewUtils.createNewWindow(view , title);
		this.view.addAttachListener(new AttachListener());
		this.view.addDeleteAttachmentListener(new DeleteAttachmentListener());
		this.view.updateRecipientMailboxes(MailboxCache.getUsers());
		if (reply){
			this.view.addToDestinationMailbox(item.getFromBox());
			for (String toBox : item.getToBoxes()){
				this.view.addToDestinationMailbox(toBox);
			}
		}
	}
	
	class AttachListener implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent arg) {
			JFileChooser chooser = new JFileChooser();
			int returnVal = chooser.showOpenDialog(ForwardMailController.this.view);
			if(returnVal == JFileChooser.APPROVE_OPTION) {
				ForwardMailController.this.model.addAttachment(chooser.getSelectedFile());
				ForwardMailController.this.view.updateAttachments(ForwardMailController.this.model.getAttachmentNames());
			}				
		}		
	}
	
	class DeleteAttachmentListener implements StringClickListener{
		@Override
		public void attachmentClicked(StringEvent event) {
			int result = JOptionPane.showConfirmDialog (ForwardMailController.this.view, 
					String.format("Are you sure you want to remove the attachment '%s'?", event.getName()),
					"Confirm",
					JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.YES_OPTION){
				ForwardMailController.this.model.removeAttachment(event.getIndex());
				ForwardMailController.this.view.updateAttachments(ForwardMailController.this.model.getAttachmentNames());
			}
		}
	}
	
	class ForwardMailListener implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				ForwardMailController.this.view.setInfo("Sending message...");
				String[] toMailboxes = ForwardMailController.this.view.getDestinationMailboxes();
				if (toMailboxes == null || toMailboxes.length == 0){
					ForwardMailController.this.view.setInfo("Please supply a mailbox");
					return;
				}
				IPublicKey[] publicKeys = new IPublicKey[toMailboxes.length];
				for (int i = 0 ; i < toMailboxes.length ; i++){
					IPublicKey mailboxKey = MailboxCache.getMailbox(toMailboxes[i]);
					if (mailboxKey == null){
						ForwardMailController.this.view.setInfo(String.format("Mailbox '%s' does not exist on the server", toMailboxes[i]));
						return;
					}
					publicKeys[i] = mailboxKey;
				}
				String fromMailbox = Configuration.getMyMailboxName();
				String subject = ForwardMailController.this.view.getSubject();
				String note = ForwardMailController.this.view.getNote();
				List<File> attachments = ForwardMailController.this.model.getFiles();
				if (fromMailbox == null){
					ForwardMailController.this.view.setInfo("Please create a mailbox before sending");
					return;
				}
				new ForwardMailWorker(publicKeys, toMailboxes, fromMailbox, subject, note, attachments, ForwardMailController.this.model.getAttachmentInfo()).execute();

			} catch (JavaInstallationMissingComponentsException | FailedCryptException | IOException | InvalidSignatureException | CommunicationFailureException exception) {
				ForwardMailController.this.view.setInfo(Utils.ppStackTrace(exception));
			}
		}
	}
	class ForwardMailWorker extends SwingWorker<Boolean , String>{
		protected IPublicKey[] toBoxKey;
		protected String[] toBoxes;
		protected String fromBox;
		protected String subject;
		protected String note;
		protected List<File> attachments;
		protected List<AttachmentInfo> attachmentInfo;
		public ForwardMailWorker(IPublicKey[] toBoxKey, String[] toBoxes, String fromBox, String subject, String note,
				List<File> attachments, List<AttachmentInfo> attachmentInfo){
			this.toBoxKey = toBoxKey;
			this.toBoxes = toBoxes;
			this.fromBox = fromBox;
			this.subject = subject;
			this.note = note;
			this.attachments = attachments;
			this.attachmentInfo = attachmentInfo;
		}
		@Override
		protected Boolean doInBackground()  {
			try {
				WebServiceClient.forwardMail(this.toBoxKey, this.toBoxes, this.fromBox, this.subject, 
						this.note, this.attachments, this.attachmentInfo);
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
					ForwardMailController.this.currentFrame.dispose();
				}
			}catch(ExecutionException | InterruptedException e){
				publish(Utils.ppStackTrace(e));
			}
		}
		@Override
		protected void process(List<String> info){
			ForwardMailController.this.view.setInfo(info.get(info.size()-1));
		}
	}	
}

