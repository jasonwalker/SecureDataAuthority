package com.jmw.sda.view.widgets;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ScrollingTextArea extends JScrollPane {
	private static final long serialVersionUID = 6434203871526965187L;
	JTextArea textArea;
	public ScrollingTextArea(){
		this.textArea = new JTextArea();
		this.setViewportView(this.textArea);
	}
	
	public void setWrapStyleWord(boolean val){
		this.textArea.setWrapStyleWord(val);
	}
	
	public void setEditable(boolean val){
		this.textArea.setEditable(val);
	}
	
	public void setLineWrap(boolean val){
		this.textArea.setLineWrap(val);
	}
	public String getText(){
		return this.textArea.getText();
	}
	public void setText(String text){
		this.textArea.setText(text);
	}
	@Override
	public void setEnabled(boolean val){
		this.textArea.setEnabled(val);
	}

}
