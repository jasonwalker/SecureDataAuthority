package com.jmw.sda.view;

import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemListener;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jmw.sda.view.widgets.ScrollingTextArea;

public class AdminPanel extends JPanel {
	private static final long serialVersionUID = 8411418536666857454L;
	
	protected JComboBox<String> hostComboBox;
	protected JButton hostDeleteButton;
	protected JTextField mailboxTextField;
	protected ScrollingTextArea publicKeyTextArea;
	protected JLabel hostKeyLabel;
	protected ScrollingTextArea hostKeyTextArea;
	protected JButton refreshButton;
	protected ScrollingTextArea infoArea;
	protected JLabel strengthLabel;
	protected JTextField bitStrengthField;
	public AdminPanel(String name) {
		super();
		this.setName(name);
		setMinimumSize(new Dimension(600, 600));
		setLayout(null);
		
		JLabel hostLabel = new JLabel("Host");
		hostLabel.setBounds(10, 134, 79, 14);
		add(hostLabel);
		
		this.hostComboBox = new JComboBox<>();
		this.hostComboBox.setBounds(99, 131, 373, 20);
		add(this.hostComboBox);
		
		this.hostDeleteButton = new JButton("Remove Mailbox From Host");
		this.hostDeleteButton.setBounds(482, 130, 197, 23);
		add(this.hostDeleteButton);
		
		JLabel mailboxLabel = new JLabel("Mailbox");
		mailboxLabel.setBounds(10, 165, 79, 14);
		add(mailboxLabel);
		
		this.mailboxTextField = new JTextField();
		this.mailboxTextField.setEditable(false);
		this.mailboxTextField.setBounds(99, 162, 310, 20);
		add(this.mailboxTextField);
		this.mailboxTextField.setColumns(10);
		
		JLabel publicKeyLabel = new JLabel("My  Public Key");
		publicKeyLabel.setBounds(10, 199, 79, 14);
		add(publicKeyLabel);
		
		this.publicKeyTextArea = new ScrollingTextArea();
		this.publicKeyTextArea.setEditable(false);
		this.publicKeyTextArea.setBounds(99, 193, 580, 153);
		this.publicKeyTextArea.setWrapStyleWord(true);
		this.publicKeyTextArea.setEnabled(false);
		this.publicKeyTextArea.setLineWrap(true);
		add(this.publicKeyTextArea);
		
		this.hostKeyLabel = new JLabel("Host Public Key");
		this.hostKeyLabel.setBounds(10, 357, 79, 14);
		add(this.hostKeyLabel);
		
		this.hostKeyTextArea = new ScrollingTextArea();
		this.hostKeyTextArea.setWrapStyleWord(true);
		this.hostKeyTextArea.setEnabled(false);
		this.hostKeyTextArea.setLineWrap(true);
		this.hostKeyTextArea.setBounds(99, 357, 580, 162);
		add(this.hostKeyTextArea);
		
		this.refreshButton = new JButton("Refresh");
		this.refreshButton.setBounds(9, 12, 101, 23);
		add(this.refreshButton);
		
		this.infoArea = new ScrollingTextArea();
		this.infoArea.setEnabled(false);
		this.infoArea.setBounds(120, 10, 559, 110);
		add(this.infoArea);
		
		this.strengthLabel = new JLabel("Host RSA Bit Strength");
		this.strengthLabel.setBounds(419, 165, 116, 14);
		add(this.strengthLabel);
		
		this.bitStrengthField = new JTextField();
		this.bitStrengthField.setEnabled(false);
		this.bitStrengthField.setEditable(false);
		this.bitStrengthField.setBounds(532, 164, 147, 20);
		add(this.bitStrengthField);
		this.bitStrengthField.setColumns(10);
		this.addComponentListener(new ResizeListener());
	}
	
	public void setInfo(String info){
		this.infoArea.setText(info);
	}
	
	public void setBitStrength(String strength){
		this.bitStrengthField.setText(strength);
	}
	
	public void addRefreshListener(ActionListener listener){
		this.refreshButton.addActionListener(listener);
	}
	
	public void removeRefreshListener(ActionListener listener){
		this.refreshButton.removeActionListener(listener);
	}
	
	public void addHostChangeListener(ItemListener listener){
		this.hostComboBox.addItemListener(listener);
	}
	
	public void removeHostChangeListener(ItemListener listener){
		this.hostComboBox.removeItemListener(listener);
	}
	
	public void addHostDeleteListener(ActionListener listener){
		this.hostDeleteButton.addActionListener(listener);
	}
	
	public void removeHostDeleteListener(ActionListener listener){
		this.hostDeleteButton.removeActionListener(listener);
	}
	
	public String getSelectedHost(){
		return (String) this.hostComboBox.getSelectedItem();
	}
	

	public void addAllHosts(Collection<String> hosts){
		String currentHost = (String) this.hostComboBox.getSelectedItem();
		this.hostComboBox.removeAllItems();
		for(String host : hosts){
			this.hostComboBox.addItem(host);
		}
		if (currentHost != null){
			this.hostComboBox.setSelectedItem(currentHost);
		}else{
			if (hosts.size() > 0){
				this.hostComboBox.setSelectedIndex(0);
			}
		}
	}
	
	public void setMyPublicKey(String key){
		this.publicKeyTextArea.setText(key);
	}
	public void setHostKey(String key){
		this.hostKeyTextArea.setText(key);
	}
	
	public void setMailboxName(String name){
		this.mailboxTextField.setText(name);
	}
	
    class ResizeListener extends ComponentAdapter{
		@Override
		public void componentResized(ComponentEvent evt) {
			Dimension dimension = evt.getComponent().getSize();
			int width = (int)dimension.getWidth();
			int height = (int)dimension.getHeight();
			int deleteButtonX = width-AdminPanel.this.hostDeleteButton.getWidth()-Styling.BORDER_WIDTH;
			AdminPanel.this.infoArea.setSize(width-(AdminPanel.this.infoArea.getX() + Styling.BORDER_WIDTH), AdminPanel.this.infoArea.getHeight());
			
			AdminPanel.this.hostComboBox.setSize(deleteButtonX-AdminPanel.this.hostComboBox.getX()-Styling.BUFFER, AdminPanel.this.hostComboBox.getHeight());
			AdminPanel.this.hostDeleteButton.setLocation(deleteButtonX, AdminPanel.this.hostDeleteButton.getY());
			
			
			AdminPanel.this.strengthLabel.setLocation(width - (Styling.BORDER_WIDTH*2 + AdminPanel.this.strengthLabel.getWidth() + 
					AdminPanel.this.bitStrengthField.getWidth()), AdminPanel.this.strengthLabel.getY());
			
			AdminPanel.this.bitStrengthField.setLocation(width - (Styling.BORDER_WIDTH + AdminPanel.this.bitStrengthField.getWidth()), 
					AdminPanel.this.bitStrengthField.getY());
			
			AdminPanel.this.mailboxTextField.setSize(width - (Styling.BORDER_WIDTH*3 + AdminPanel.this.mailboxTextField.getX() + AdminPanel.this.strengthLabel.getWidth() + 
					AdminPanel.this.bitStrengthField.getWidth()), AdminPanel.this.mailboxTextField.getHeight());
			
			
			int heightForEachKey = (height - AdminPanel.this.publicKeyTextArea.getY()) / 2;
			
			AdminPanel.this.publicKeyTextArea.setSize(width - AdminPanel.this.publicKeyTextArea.getX() - Styling.BORDER_WIDTH, 
					heightForEachKey - Styling.BORDER_WIDTH);
			
			AdminPanel.this.hostKeyLabel.setLocation(AdminPanel.this.hostKeyLabel.getX(), AdminPanel.this.publicKeyTextArea.getY() + heightForEachKey);
			AdminPanel.this.hostKeyTextArea.setLocation(AdminPanel.this.hostKeyTextArea.getX(), AdminPanel.this.publicKeyTextArea.getY() + heightForEachKey);
			AdminPanel.this.hostKeyTextArea.setSize(width-AdminPanel.this.hostKeyTextArea.getX()-Styling.BORDER_WIDTH, heightForEachKey - Styling.BORDER_WIDTH);
			
		}
    }
}
