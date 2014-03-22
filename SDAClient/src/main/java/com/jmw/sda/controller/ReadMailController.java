package com.jmw.sda.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.SwingWorker;

import com.jmw.sda.communication.CommunicationFailureException;
import com.jmw.sda.communication.WebServiceClient;
import com.jmw.sda.crypto.AbstractCrypto;
import com.jmw.sda.crypto.FailedCryptException;
import com.jmw.sda.crypto.IPrivateKey;
import com.jmw.sda.crypto.IPublicKey;
import com.jmw.sda.crypto.InvalidSignatureException;
import com.jmw.sda.crypto.JavaInstallationMissingComponentsException;
import com.jmw.sda.model.MailToForward;
import com.jmw.sda.model.MailboxCache;
import com.jmw.sda.transport.objects.AttachmentInfo;
import com.jmw.sda.transport.objects.EncryptedListItem;
import com.jmw.sda.transport.objects.TotalListItem;
import com.jmw.sda.utils.Configuration;
import com.jmw.sda.view.ReadMailPanel;
import com.jmw.sda.view.SendMailPanel;
import com.jmw.sda.view.ViewUtils;
import com.jmw.sda.view.event.StringClickListener;
import com.jmw.sda.view.event.StringEvent;
import com.jmw.sda.view.widgets.FileChooserWithOverwriteConfirm;

public class ReadMailController {
	protected TotalListItem model;
	protected ReadMailPanel view;
	protected WebServiceClient webServiceClient;
	protected EncryptedListItem encryptedItem;
	protected static AbstractCrypto crypto = AbstractCrypto.getCrypto();
	protected IPrivateKey privateKey;
	
	public ReadMailController(ReadMailPanel view, TotalListItem model) throws CommunicationFailureException, IOException, IllegalStateException, JavaInstallationMissingComponentsException, FailedCryptException, InvalidSignatureException{
		this.view = view;
		this.model = model;
		this.view.addForwardListener(new GetForwardListener());
		this.view.addReplyListener(new GetReplyListener());
		this.view.addDownloadRequestListener(new DownloadListener());
		this.webServiceClient = new WebServiceClient();
		ViewUtils.createNewWindow(view , "View Mail");	
		this.privateKey = Configuration.getMyPrivateKey();
		this.encryptedItem = model.getEncryptedListItem(this.privateKey);
		this.view.setFromMailbox(this.encryptedItem.getFromBox());
		this.view.setSubjectField(this.encryptedItem.getSubject());
		this.view.setNote(this.encryptedItem.getNote());
		IPublicKey sendersKey = MailboxCache.getMailbox(this.encryptedItem.getFromBox());
		if (!this.encryptedItem.verify(sendersKey)){
			view.setInfo("Do not trust this message.  The signature failed to verify.");
		}
		if (this.encryptedItem.getAttachments() != null){
			List<String> attachmentNames = new ArrayList<>();
			for (AttachmentInfo info : this.encryptedItem.getAttachments()){
				attachmentNames.add(info.getName());
			}
			if (this.encryptedItem.getAttachments() != null){
				this.view.setAttachments(attachmentNames);
			}
		}
	}
	
	class GetForwardListener implements ActionListener{
		@SuppressWarnings("unused")
		@Override
		public void actionPerformed(ActionEvent event) {
			try{
		    	new ForwardMailController(new SendMailPanel(), new MailToForward(),
		    			ReadMailController.this.model.getEncryptedListItem(null), false);
			}catch(JavaInstallationMissingComponentsException | FailedCryptException e){
					//should have failed already
			}
		}
	}
	
	class GetReplyListener implements ActionListener{
		@SuppressWarnings("unused")
		@Override
		public void actionPerformed(ActionEvent event) {
			try{
		    	new ForwardMailController(new SendMailPanel(), new MailToForward(),
		    			ReadMailController.this.model.getEncryptedListItem(null), true);
			}catch(JavaInstallationMissingComponentsException | FailedCryptException e){
					//should have failed already
			}
		}
	}
	
	class DownloadListener implements StringClickListener{
		@Override
		public void attachmentClicked(StringEvent event) {
			try{
				FileChooserWithOverwriteConfirm fileChooser = new FileChooserWithOverwriteConfirm();
				fileChooser.setSelectedFile(new File(event.getName()));
			    int returnVal = fileChooser.showSaveDialog(ReadMailController.this.view);
			    if(returnVal == JFileChooser.APPROVE_OPTION) {
			    	new DownloadAttachmentWorker(ReadMailController.this.model.getEncryptedListItem(ReadMailController.this.privateKey).getAttachments().get(event.getIndex()), ReadMailController.this.encryptedItem.getFromBox(), 
			    			fileChooser.getSelectedFile()).execute();
			    }	
			}catch(JavaInstallationMissingComponentsException | FailedCryptException e){
				//should have failed already
			}
		}
	}
	class DownloadAttachmentWorker extends SwingWorker<Void , String>{
		protected String fromBox;
		protected File fileToWrite;
		protected AttachmentInfo attachmentInfo;
		public DownloadAttachmentWorker(AttachmentInfo attachmentInfo, String fromBox, File fileToWrite){
			this.fromBox = fromBox;
			this.fileToWrite = fileToWrite;
			this.attachmentInfo = attachmentInfo;
		}
		@Override
		protected Void doInBackground()  {
		    try {
				boolean verified = WebServiceClient.getAttachment(this.attachmentInfo, this.fromBox, this.fileToWrite);
				if (!verified){
					publish("The server failed to send a correct signature.  Someone may be tampering with your connection.  Do not trust this communication");
				}else{
					publish(String.format("Attachment downloaded, decrypted and saved to file: %s.", this.fileToWrite.getAbsolutePath()));
				}
			} catch (IllegalStateException | IOException |  CommunicationFailureException |
					JavaInstallationMissingComponentsException | FailedCryptException e1) {
				publish(e1.getMessage());
			} catch (InvalidSignatureException e) {
				publish("The server failed to send a correct signature.  Someone may be tampering with your connection.  Do not trust this communication");
			}
		    return null;
		}
		@Override
		protected void done(){
			//nothing				
		}
		@Override
		protected void process(List<String> info){
			ReadMailController.this.view.setInfo(info.get(info.size()-1));
		}
	}	
}
