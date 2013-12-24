package com.jmw.sda.view;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jmw.sda.view.event.StringClickListener;
import com.jmw.sda.view.widgets.MailboxNameTextField;
import com.jmw.sda.view.widgets.ScrollingTextArea;
import com.jmw.sda.view.widgets.StringHolder;

public class ReadMailPanel extends JPanel {
	private static final long serialVersionUID = -9040490470282617418L;
	protected JButton forwardButton;
	protected JTextField fromMailbox;
	protected JTextField subjectField;
	protected ScrollingTextArea noteArea;
	protected ScrollingTextArea infoArea;
	protected StringHolder attachmentHolder;
	protected File selectedDirectory;
	
	public ReadMailPanel() {
		setLayout(null);
		this.addComponentListener(new ResizeListener());

		this.forwardButton = new JButton("Forward Message");
		this.forwardButton.setBounds(10, 11, 128, 56);
		add(this.forwardButton);
		this.infoArea = new ScrollingTextArea();
		this.infoArea.setEnabled(false);
		this.infoArea.setBounds(148, 11, 522, 56);
		this.infoArea.setWrapStyleWord(true);
		add(this.infoArea);
		
		JLabel fromLabel = new JLabel("From");
		fromLabel.setBounds(10, 78, 50, 23);
		add(fromLabel);
		
		this.fromMailbox = new MailboxNameTextField(Styling.BORDER_WIDTH, 45);
		this.fromMailbox.setSize(561, 23);
		this.fromMailbox.setLocation(109, 78);
		this.fromMailbox.setEditable(false);
		add(this.fromMailbox);
		
		JLabel subjectLabel = new JLabel("Subject");
		subjectLabel.setBounds(10, 112, 80, 23);
		add(subjectLabel);		
		
		this.subjectField = new JTextField();
		this.subjectField.setBounds(109, 112, 561, 23);
		this.subjectField.setEditable(false);
		add(this.subjectField);
		
		this.attachmentHolder = new StringHolder();
		this.attachmentHolder.setBounds(109, 149, 562, 45);
		add(this.attachmentHolder);
		
		JLabel lblNote = new JLabel("Note");
		lblNote.setBounds(10, 200, 50, 23);
		add(lblNote);
		this.noteArea = new ScrollingTextArea();
		this.noteArea.setWrapStyleWord(true);
		this.noteArea.setLineWrap(true);
		this.noteArea.setBounds(10, 235, 660, 336);
		this.noteArea.setEditable(false);
		add(this.noteArea);
		
		JLabel lblAttachmentsl = new JLabel("Attachments");
		lblAttachmentsl.setBounds(10, 163, 101, 14);
		add(lblAttachmentsl);

	}
	
    class ResizeListener extends ComponentAdapter{
		@Override
		public void componentResized(ComponentEvent evt) {
			Dimension dimension = evt.getComponent().getSize();
			int width = (int)dimension.getWidth();
			int height = (int)dimension.getHeight();
			int currentWidth = width-Styling.BORDER_WIDTH*2;
			ReadMailPanel.this.fromMailbox.setSize(width-ReadMailPanel.this.fromMailbox.getX()-Styling.BORDER_WIDTH, ReadMailPanel.this.fromMailbox.getHeight());
			ReadMailPanel.this.subjectField.setSize(width-ReadMailPanel.this.subjectField.getX()-Styling.BORDER_WIDTH, ReadMailPanel.this.subjectField.getHeight());
			ReadMailPanel.this.infoArea.setSize(width - ReadMailPanel.this.infoArea.getX()-Styling.BORDER_WIDTH, ReadMailPanel.this.infoArea.getHeight());
			ReadMailPanel.this.noteArea.setSize(currentWidth, height - (ReadMailPanel.this.noteArea.getY()+Styling.BORDER_WIDTH));
			ReadMailPanel.this.attachmentHolder.setSize(width-(ReadMailPanel.this.attachmentHolder.getX()+Styling.BORDER_WIDTH), ReadMailPanel.this.attachmentHolder.getHeight());
		}
    }	
    
    public void addDownloadRequestListener(StringClickListener listener){
    	this.attachmentHolder.addStringClickListener(listener);
    }
    public void removeDownloadRequestListener(StringClickListener listener){
    	this.attachmentHolder.removeStringClickListener(listener);
    }
    
    public void setFromMailbox(String mailboxName){
    	this.fromMailbox.setText(mailboxName);
    }
    public void setSubjectField(String subject){
    	this.subjectField.setText(subject);
    }
    public void setNote(String text){
    	this.noteArea.setText(text);
    }
   
    public void setAttachments(List<String> attachments){
    	this.attachmentHolder.setStrings(attachments);
    }
    
    public void addForwardListener(ActionListener listener){
    	this.forwardButton.addActionListener(listener);
    }
    public void removeForwardListener(ActionListener listener){
    	this.forwardButton.removeActionListener(listener);
    }

	public void setInfo(String info) {
		this.infoArea.setText(info);
	}
	public File getSelectedDirectory(){
		return this.selectedDirectory;
	}
}