package com.jmw.sda.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.jmw.sda.transport.objects.AttachmentInfo;

public class MailToForward {

	protected String msg;
	protected List<FileOrAttachmentInfo> attachments;
	
	public MailToForward(){
		this.attachments = new ArrayList<>();
	}
	public String getMsg() {
		return this.msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	
	public void addAttachment(File attachment){
		this.attachments.add(new FileOrAttachmentInfo(attachment));
	}
	public void addAttachment(AttachmentInfo attachment){
		this.attachments.add(new FileOrAttachmentInfo(attachment));
	}
	public List<String> getAttachmentNames(){
		List<String> attachmentNames = new ArrayList<>(this.attachments.size());
		for(FileOrAttachmentInfo attachment : this.attachments){
			attachmentNames.add(attachment.getName());
		}
		return attachmentNames;
	}
	
	public List<File> getFiles(){
		List<File> files = new ArrayList<>();
		for(FileOrAttachmentInfo attachment : this.attachments){
			if (attachment.isFile){
				files.add(attachment.file);
			}
		}
		return files;
	}
	public List<AttachmentInfo> getAttachmentInfo(){
		List<AttachmentInfo> infos = new ArrayList<>();
		for(FileOrAttachmentInfo attachment : this.attachments){
			if (!attachment.isFile){
				infos.add(attachment.info);
			}
		}
		return infos;		
	}
	
	public void removeAttachment(int index){
		this.attachments.remove(index);
	}

}
class FileOrAttachmentInfo{
	protected boolean isFile;
	protected File file;
	protected AttachmentInfo info;
	public FileOrAttachmentInfo(File f){
		this.file = f;
		this.isFile = true;
	}
	public FileOrAttachmentInfo(AttachmentInfo ai){
		this.info = ai;
		this.isFile = false;
	}
	public String getName(){
		if (this.isFile){
			return this.file.getName();
		}
		return this.info.getName();
	}
}
