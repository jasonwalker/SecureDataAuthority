package com.jmw.sda.view.widgets;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class FileChooserWithOverwriteConfirm extends JFileChooser {
	private static final long serialVersionUID = -7630754475311023423L;
	
	public FileChooserWithOverwriteConfirm(){
		super(new File("."));
	}
	
	@Override
	    public void approveSelection(){
	        File fileToWrite = getSelectedFile();
	        if(fileToWrite.exists() && getDialogType() == SAVE_DIALOG){
	        	int result = JOptionPane.showConfirmDialog (this, String.format("A file named '%s' already exists.  Overwrite?", fileToWrite.getName()),"Warning",JOptionPane.YES_NO_OPTION);
	            switch(result){
	                case JOptionPane.YES_OPTION:
	                    super.approveSelection();
	                    return;
	                case JOptionPane.NO_OPTION:
	                    return;
	                case JOptionPane.CLOSED_OPTION:
	                    return;
	                case JOptionPane.CANCEL_OPTION:
	                    cancelSelection();
	                    return;
	                default:
	                	super.approveSelection();
	                	return;
	            }
	        }
	        super.approveSelection();
	    }
}
