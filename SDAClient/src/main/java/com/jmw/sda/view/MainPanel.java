package com.jmw.sda.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;

import com.jmw.sda.view.event.MailboxNameChangeListener;
import com.jmw.sda.view.event.NameChangeEvent;

public class MainPanel extends JPanel implements MailboxNameChangeListener {
	private static final long serialVersionUID = -2122294705425238532L;
	protected JPanel[] panels;
	protected JTabbedPane tabbedPane;
	protected JLabel mailboxNameLabel;
	public MainPanel(String mailboxName, JPanel ...panels) {
		setLayout(null);
        this.addComponentListener(new ResizeListener());
        this.mailboxNameLabel = new JLabel();
        this.mailboxNameLabel.setFont(Styling.MailboxName);
        this.mailboxNameLabel.setBounds(10, 0, 430, 27);
        add(this.mailboxNameLabel);
        setMailboxName(mailboxName);
        this.tabbedPane = new JTabbedPane();
        this.tabbedPane.setBounds(1, 27, 450, 273);
        this.panels = panels;
        for (JPanel panel : panels){
        	this.tabbedPane.addTab(panel.getName(), panel);
        }
        add(this.tabbedPane);
	}
    class ResizeListener extends ComponentAdapter{
		@Override
		public void componentResized(ComponentEvent evt) {
			Dimension dimension = evt.getComponent().getSize();
			int width = (int)dimension.getWidth();
			int height = (int)dimension.getHeight() - MainPanel.this.tabbedPane.getY();
			MainPanel.this.tabbedPane.setSize(width , height);
			int panelHeight = height-10;
			for (JPanel panel : MainPanel.this.panels){
				panel.setSize(width, panelHeight);
			}
		}
    }
    
    public void setMailboxName(String name){
    	this.mailboxNameLabel.setText("Mailbox name: " + (name == null ? "" : name));
    }
    
    public void setSelectedTab(Component tabComponent){
    	this.tabbedPane.setSelectedComponent(tabComponent);
    }

	@Override
	public void nameChanged(NameChangeEvent event) {
		setMailboxName(event.getName());
	}
	
	public void addTabbedListener(ChangeListener listener){
		this.tabbedPane.addChangeListener(listener);
	}
	public void removeTabbedListener(ChangeListener listener){
		this.tabbedPane.removeChangeListener(listener);
	}
}
