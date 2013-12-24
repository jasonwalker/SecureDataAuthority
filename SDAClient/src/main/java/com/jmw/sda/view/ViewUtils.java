package com.jmw.sda.view;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public class ViewUtils {
    public static JFrame createNewWindow(JPanel panel, String title) {
		JFrame  frame= new JFrame(title); 
		frame.add(panel);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setSize(new Dimension(800,500));
		frame.setVisible(true);
		return frame;
    }
}
