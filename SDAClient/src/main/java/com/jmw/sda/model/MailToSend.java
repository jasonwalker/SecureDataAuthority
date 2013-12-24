package com.jmw.sda.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MailToSend {
	protected String msg;
	protected List<File> attachments;
	
	public MailToSend(){
		this.attachments = new ArrayList<>();
	}
	public String getMsg() {
		return this.msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	
	public void addAttachments(File[] newAttachments){
		for (File attachment : newAttachments){
			this.attachments.add(attachment);
		}
	}
	public List<String> getAttachmentNames(){
		List<String> attachmentNames = new ArrayList<>(this.attachments.size());
		for(File file : this.attachments){
			attachmentNames.add(file.getName());
		}
		return attachmentNames;
	}
	
	public List<File> getAttachments(){
		return this.attachments;
	}
	
	public void removeAttachment(int index){
		this.attachments.remove(index);
	}
}
