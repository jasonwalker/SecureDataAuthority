package com.jmw.sda.view.widgets;

import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.jmw.sda.view.Styling;

public class MailboxNameTextField extends JTextField {
	private static final long serialVersionUID = 1501447632292809090L;
	
	public MailboxNameTextField(int x, int y){
		super();
		setFont(Styling.MailboxName);
		setHorizontalAlignment(SwingConstants.LEFT);
		setLocation(x,y);
	}

}
