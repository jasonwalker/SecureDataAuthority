package com.jmw.sda.view.widgets;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.jmw.sda.view.event.StringClickListener;
import com.jmw.sda.view.event.StringEvent;

public class StringHolder extends JScrollPane {
	private static final long serialVersionUID = -7624603103390860069L;
	protected JPanel panel;
	protected List<StringClickListener> attachmentListeners;
	public StringHolder() {
		super();
		this.attachmentListeners = new ArrayList<>();
		this.panel = new JPanel();
		this.setViewportView(this.panel);
		this.panel.setLayout(new FlowLayout(FlowLayout.LEFT));
	}
	
	@Override
	public void setBackground(Color bg){
		super.setBackground(bg);
		if (this.panel != null){
			this.panel.setBackground(bg);
		}
	}
	
	public void setStrings(List<String> strings){
		this.panel.removeAll();
		for (String string : strings){
			addNewButton(string);
		}
		this.panel.revalidate();
		this.panel.repaint();
	}
	
	private final void addNewButton(String string){
		JButton button = new JButton(string);
		int index = this.panel.getComponentCount();
		button.addActionListener(new StringButtonListener(button, index));
		this.panel.add(button);		
	}
	
	public void addString(String s){
		addNewButton(s);
		this.panel.revalidate();
		this.panel.repaint();		
	}
	
	public void removeString(String string){
		Component[] comps = this.panel.getComponents();
		for (int i = 0 ; i < comps.length ; i++){
			if (comps[i] instanceof JButton){
				JButton button = (JButton) comps[i];
				if (string.equalsIgnoreCase(button.getText())){
					this.panel.remove(i);
					this.panel.revalidate();
					this.panel.repaint();	
					return;
				}
			}
		}
	}
	
	public String[] getStrings(){
		ArrayList<String> retList = new ArrayList<>();
		for (Component comp : this.panel.getComponents()){
			if (comp instanceof JButton){
				JButton button = (JButton) comp;
				retList.add(button.getText());
			}
		}		
		return retList.toArray(new String[retList.size()]);
	}
	
	class StringButtonListener implements ActionListener{
		protected JButton button;
		protected int index;
		public StringButtonListener(JButton button, int index){
			this.button = button;
			this.index = index;
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			StringEvent attach = new StringEvent(this.button.getText(), this.index);
			for(StringClickListener listener : StringHolder.this.attachmentListeners){
				listener.attachmentClicked(attach);
			}
		}		
	}
	
	public void addStringClickListener(StringClickListener listener){
		this.attachmentListeners.add(listener);
	}
	public void removeStringClickListener(StringClickListener listener){
		this.attachmentListeners.remove(listener);
	}


}
