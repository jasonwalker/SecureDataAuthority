package com.jmw.sda.dbProviders;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

public class IDAndOutputStream implements Closeable {
	protected String id;
	protected OutputStream os;
	public IDAndOutputStream(String id, OutputStream os) {
		this.id = id;
		this.os = os;
	}

	public String getId() {
		return this.id;
	}

	public OutputStream getOs() {
		return this.os;
	}
	@Override
	public void close() throws IOException {
		this.os.close();
	}
}
