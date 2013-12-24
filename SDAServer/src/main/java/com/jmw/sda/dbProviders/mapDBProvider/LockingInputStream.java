package com.jmw.sda.dbProviders.mapDBProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Semaphore;

import com.jmw.sda.crypto.Utils;

public class LockingInputStream extends InputStream {
	protected InputStream is;
	Semaphore lock;
	String keyReference;
	public LockingInputStream(File file, Semaphore lock, String keyReference) throws IOException, InterruptedException {
		this.lock = lock;
		this.keyReference = keyReference;
		this.lock.acquire(1);
		this.is = Utils.inputStreamForFile(file);
	}

	public byte[] getAllBytes() throws IOException{
		return Utils.readByteArray(this.is);
	}

	@Override
	public void close() throws IOException{
		try{
			this.is.close();
		}finally{
			this.lock.release(1);
		}
	}

	@Override
	public int read() throws IOException {
		return this.is.read();
	}
	@Override
	public int read(byte[] b) throws IOException {
		return this.is.read(b);
	}
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return this.is.read(b, off, len);
	}

}
