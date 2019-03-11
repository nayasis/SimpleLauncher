package com.nayasis.simplelauncher.library.mslinks.data.copy;

import nayasis.simpleLauncher.library.mslinks.Serializable;
import nayasis.simpleLauncher.library.mslinks.io.ByteReader;
import nayasis.simpleLauncher.library.mslinks.io.ByteWriter;

import java.io.IOException;

public class BitSet32 implements Serializable {
	private int d;
	
	public BitSet32(int n) {
		d = n;
	}
	
	public BitSet32(ByteReader data) throws IOException {
		d = (int)data.read4bytes();
	}
	
	protected boolean get(int i) {
		return (d & (1 << i)) != 0;
	}
	
	protected void set(int i) {
		d = (d & ~(1 << i)) | (1 << i);
	}
	
	protected void clear(int i) {
		d = d & ~(1 << i);
	}

	public void serialize(ByteWriter bw) throws IOException {
		bw.write4bytes(d);		
	}
}
