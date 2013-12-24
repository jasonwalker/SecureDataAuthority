package com.jmw.sda.view;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jmw.sda.view.widgets.MailboxNameTextField;
import com.jmw.sda.view.widgets.ScrollingTextArea;

public class CreateMailboxPanel extends JPanel {
	private static final long serialVersionUID = -5143965773636124727L;
	
	protected JButton createButton;
	protected MailboxNameTextField nameField;
	protected ScrollingTextArea infoArea;
	protected JLabel serverPublicKeyLabel;
	protected ScrollingTextArea serversPublicKeyArea;
	protected JTextField serversUriField;
	
	public CreateMailboxPanel(String name){
		super();
		this.setName(name);
		setMinimumSize(new Dimension(600, 600));
		setLayout(null);
		this.addComponentListener(new ResizeListener());
		this.createButton = new JButton("Create Mailbox");
		this.createButton.setBounds(10, 11, 142, 88);
		this.add(this.createButton);
		JLabel lblPleaseSupplyThe = new JLabel("Please supply the server's URL");
		lblPleaseSupplyThe.setBounds(10, 125, 499, 14);
		add(lblPleaseSupplyThe);
		
		this.serversUriField = new JTextField();
		this.serversUriField.setSize(663, 23);
		this.serversUriField.setBounds(10, 150, 663, 34);
		add(this.serversUriField);		
		this.serverPublicKeyLabel = new JLabel("Please paste the server's public key in the space below.  It is very important that this key is correct.");
		this.serverPublicKeyLabel.setBounds(10, 181, 663, 40);
		add(this.serverPublicKeyLabel);
		this.serversPublicKeyArea = new ScrollingTextArea();
		this.serversPublicKeyArea.setWrapStyleWord(true);
		this.serversPublicKeyArea.setLineWrap(true);
		this.serversPublicKeyArea.setBounds(10, 215, 663, 132);
		add(this.serversPublicKeyArea);
		JLabel label = new JLabel("Please supply a mailbox name");
		label.setBounds(10, 358, 222, 14);
		this.add(label);
		this.nameField = new MailboxNameTextField(10,70);
		this.nameField.setSize(663, 34);
		this.nameField.setLocation(10, 383);
		this.nameField.setToolTipText("Enter the name you would like for your mailbox here.");
		this.add(this.nameField);
		this.infoArea = new ScrollingTextArea();
		this.infoArea.setBounds(162, 10, 511, 88);
		this.infoArea.setEnabled(false);
		this.add(this.infoArea);
		
	}
	public void addCreateListener(ActionListener listener){
		this.createButton.addActionListener(listener);
	}
	public void removeCreateListener(ActionListener listener){
		this.createButton.removeActionListener(listener);
	}
	public String getMailboxName(){
		return this.nameField.getText();
	}

	public String getServersKey(){
		return this.serversPublicKeyArea.getText().trim();
	}

	public String getServersAddress(){
		return this.serversUriField.getText();
	}

	public void setInfo(String info){
		this.infoArea.setText(info);
	}
    class ResizeListener extends ComponentAdapter{
		@Override
		public void componentResized(ComponentEvent evt) {
			Dimension dimension = evt.getComponent().getSize();
			int width = (int)dimension.getWidth();
			CreateMailboxPanel.this.infoArea.setSize(width - CreateMailboxPanel.this.infoArea.getX() - Styling.BORDER_WIDTH , CreateMailboxPanel.this.infoArea.getHeight());
			CreateMailboxPanel.this.nameField.setSize(width - Styling.BORDER_WIDTH*2 , CreateMailboxPanel.this.nameField.getHeight());
			CreateMailboxPanel.this.serversPublicKeyArea.setSize(width - Styling.BORDER_WIDTH*2, CreateMailboxPanel.this.serversPublicKeyArea.getHeight());
			CreateMailboxPanel.this.serversUriField.setSize(width - Styling.BORDER_WIDTH*2, CreateMailboxPanel.this.serversUriField.getHeight());
		}
    }

}
