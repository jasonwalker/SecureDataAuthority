package com.jmw.sda.view;

import java.awt.Component;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;

import com.jmw.sda.view.event.MailboxNameChangeListener;

public class MainFrame extends JFrame {
	private static final long serialVersionUID = -3524260393606985246L;
	protected MainPanel mainPanel;

    public MainFrame(String mailboxName, JPanel ...panels) {
        super("Secure Data Authority");
        this.mainPanel = new MainPanel(mailboxName, panels);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().add(this.mainPanel);
        pack();
        setSize(940, 534);
        setVisible(true);
    }
    
    public void addTabListener(ChangeListener listener){
    	this.mainPanel.addTabbedListener(listener);
    }
    public void removeTabListener(ChangeListener listener){
    	this.mainPanel.removeTabbedListener(listener);
    }
    
    public MailboxNameChangeListener getMailboxNameChangeListener(){
    	return this.mainPanel;
    }
    
    public void setSelectedTab(Component tabComponent){
    	this.mainPanel.setSelectedTab(tabComponent);
    }

}