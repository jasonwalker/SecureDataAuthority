package com.jmw.sda.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Collection;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jmw.sda.view.event.StringClickListener;
import com.jmw.sda.view.event.StringEvent;
import com.jmw.sda.view.widgets.ScrollingTextArea;
import com.jmw.sda.view.widgets.StringHolder;

public class SendMailPanel extends JPanel {
	private static final long serialVersionUID = -9040490470282617418L;
	protected JButton sendButton;
	protected StringHolder destinationMailbox;
	protected JTextField subjectField;
	protected ScrollingTextArea noteArea;
	protected ScrollingTextArea infoArea;
	protected JButton attachButton;
	protected StringHolder attachmentHolder;
	protected JComboBox<String> emailBox;
	protected JButton toLabelRefresh;
	protected EmailAddListener emailToListener = new EmailAddListener();
	
	public SendMailPanel() {
		setLayout(null);
		this.addComponentListener(new ResizeListener());

		this.sendButton = new JButton("Send Message");
		this.sendButton.setBounds(10, 11, 128, 56);
		add(this.sendButton);
		this.infoArea = new ScrollingTextArea();
		this.infoArea.setEnabled(true);
		this.infoArea.setBounds(150, 11, 520, 56);
		add(this.infoArea);
		
		this.destinationMailbox = new StringHolder();
		this.destinationMailbox.setBackground(Color.white);
		this.destinationMailbox.setSize(428, 65);
		this.destinationMailbox.setLocation(242, 78);
		this.destinationMailbox.addStringClickListener(new EmailRemoveListener());
		add(this.destinationMailbox);
		
		JLabel subjectLabel = new JLabel("Subject");
		subjectLabel.setBounds(10, 149, 58, 23);
		add(subjectLabel);		
		
		this.subjectField = new JTextField();
		this.subjectField.setBounds(73, 149, 597, 23);
		add(this.subjectField);
		this.attachButton = new JButton("Attachment");

		this.attachButton.setBounds(10, 190, 109, 23);
		add(this.attachButton);
		
		this.attachmentHolder = new StringHolder();
		this.attachmentHolder.setBounds(128, 190, 542, 65);
		
		add(this.attachmentHolder);
		
		JLabel lblNote = new JLabel("Note");
		lblNote.setBounds(10, 255, 50, 23);
		add(lblNote);
		this.noteArea = new ScrollingTextArea();
		this.noteArea.setWrapStyleWord(true);
		this.noteArea.setLineWrap(true);
		this.noteArea.setBounds(10, 280, 660, 242);
		add(this.noteArea);
		
		this.emailBox = new JComboBox<>();
		this.emailBox.setBounds(73, 95, 159, 30);
		add(this.emailBox);
		this.emailBox.addActionListener(this.emailToListener);
		
		this.toLabelRefresh = new JButton("To");
		this.toLabelRefresh.setToolTipText("Click to refresh recipient list");
		this.toLabelRefresh.setBounds(10, 95, 58, 31);
		add(this.toLabelRefresh);
	}
	
	public void addRefreshToMailboxListener(ActionListener listener){
		this.toLabelRefresh.addActionListener(listener);
	}
	public void removeRefreshToMailboxListener(ActionListener listener){
		this.toLabelRefresh.removeActionListener(listener);
	}
	
	public void addToDestinationMailbox(String toMailbox){
        if (toMailbox.trim().equalsIgnoreCase("")){
        	return;
        }
        String[] recipients = SendMailPanel.this.destinationMailbox.getStrings();
        for (String recipient : recipients){
        	if (recipient == null){
        		continue;
        	}
        	if (recipient.equals(toMailbox)){
        		return;
        	}
        }
        this.destinationMailbox.addString(toMailbox);		
	}
	
	class EmailAddListener implements ActionListener{
	    @Override
		public void actionPerformed(ActionEvent e) {
	        String toMailbox = (String)SendMailPanel.this.emailBox.getSelectedItem();
	        SendMailPanel.this.addToDestinationMailbox(toMailbox);
	    }		
	}
	
	class EmailRemoveListener implements StringClickListener{
		@Override
		public void attachmentClicked(StringEvent event) {
			int result = JOptionPane.showConfirmDialog (SendMailPanel.this, 
					String.format("Are you sure you want to remove the recipient '%s'?", event.getName()),
					"Confirm",
					JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.YES_OPTION){
				SendMailPanel.this.destinationMailbox.removeString(event.getName());
			}
		}
	}
	
    class ResizeListener extends ComponentAdapter{
		@Override
		public void componentResized(ComponentEvent evt) {
			Dimension dimension = evt.getComponent().getSize();
			int width = (int)dimension.getWidth();
			int height = (int)dimension.getHeight();
			int currentWidth = width-Styling.BORDER_WIDTH*2;
			SendMailPanel.this.destinationMailbox.setSize(width-SendMailPanel.this.destinationMailbox.getX()-Styling.BORDER_WIDTH, SendMailPanel.this.destinationMailbox.getHeight());
			SendMailPanel.this.subjectField.setSize(width-SendMailPanel.this.subjectField.getX()-Styling.BORDER_WIDTH, SendMailPanel.this.subjectField.getHeight());
			SendMailPanel.this.infoArea.setSize(width - SendMailPanel.this.infoArea.getX()-Styling.BORDER_WIDTH, SendMailPanel.this.infoArea.getHeight());
			SendMailPanel.this.noteArea.setSize(currentWidth, height - (SendMailPanel.this.noteArea.getY()+Styling.BORDER_WIDTH));
			SendMailPanel.this.attachmentHolder.setSize(width-(SendMailPanel.this.attachmentHolder.getX()+Styling.BORDER_WIDTH), SendMailPanel.this.attachmentHolder.getHeight());
		}
    }	
    
    public void updateAttachments(List<String> attachments){
    	this.attachmentHolder.setStrings(attachments);
    }
    
    public void updateRecipientMailboxes(Collection<String> names){
    	this.emailBox.removeActionListener(this.emailToListener);
    	this.emailBox.removeAllItems();
    	this.emailBox.addItem("");
    	for (String name : names){
    		this.emailBox.addItem(name);
    	}
    	this.emailBox.addActionListener(this.emailToListener);
    }
    
	public void addDeleteRecipientListener(StringClickListener listener){
		this.destinationMailbox.addStringClickListener(listener);
	}
	
	public void removeDeleteRecipientListener(StringClickListener listener){
		this.destinationMailbox.removeStringClickListener(listener);
	}
	
    public void addDeleteAttachmentListener(StringClickListener listener){
    	this.attachmentHolder.addStringClickListener(listener);
    }
    
    public void removeDeleteAttachmentListener(StringClickListener listener){
    	this.attachmentHolder.removeStringClickListener(listener);
    }
    
    public void addSendListener(ActionListener listener){
    	this.sendButton.addActionListener(listener);
    }
    
    public void removeSendListener(ActionListener listener){
    	this.sendButton.removeActionListener(listener);
    }
    
    public void addAttachListener(ActionListener listener){
    	this.attachButton.addActionListener(listener);
    }
    
    public void removeAttachListener(ActionListener listener){
    	this.attachButton.removeActionListener(listener);
    }
	
	public String[] getDestinationMailboxes() {
		return this.destinationMailbox.getStrings();
	}

	public String getSubject(){
		return this.subjectField.getText();
	}
	public void setSubject(String s){
		this.subjectField.setText(s);
	}
	
	public String getNote() {
		return this.noteArea.getText();
	}
	
	public void setNote(String s){
		this.noteArea.setText(s);
	}

	public void setInfo(String info) {
		this.infoArea.setText(info);
	}
}
