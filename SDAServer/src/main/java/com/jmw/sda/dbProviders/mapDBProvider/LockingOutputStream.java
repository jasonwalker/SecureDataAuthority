package com.jmw.sda.dbProviders.mapDBProvider;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;

import com.jmw.sda.crypto.Utils;

public class LockingOutputStream extends OutputStream {
	protected OutputStream os;
	protected Semaphore lock;
	protected String keyReference;
	
	public LockingOutputStream(File file, Semaphore lock, String keyReference) throws IOException, InterruptedException {
		this.lock = lock;
		this.keyReference = keyReference;
		this.lock.acquire(Integer.MAX_VALUE);
		this.os = Utils.outputStreamForFile(file);
	}

	public void writeAllBytes(byte[] b) throws IOException{
		Utils.writeByteArray(this.os, b);
	}
	
	@Override
	public void close() throws IOException{
		try{
			this.os.close();
		}finally{
			this.lock.release(Integer.MAX_VALUE);
		}
	}

	@Override
	public void write(int b) throws IOException {
		this.os.write(b);
	}
	@Override
	public void write(byte[] b) throws IOException{
		this.os.write(b);
	}
	@Override
	public void write(byte[] b, int off, int len) throws IOException{
		this.os.write(b, off, len);
	}
	
}
