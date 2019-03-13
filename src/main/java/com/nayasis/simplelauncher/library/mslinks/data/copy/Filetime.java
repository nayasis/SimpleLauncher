package com.nayasis.simplelauncher.library.mslinks.data.copy;

import com.nayasis.simplelauncher.library.mslinks.Serializable;
import com.nayasis.simplelauncher.library.mslinks.io.ByteReader;
import com.nayasis.simplelauncher.library.mslinks.io.ByteWriter;

import java.io.IOException;
import java.util.GregorianCalendar;

public class Filetime extends GregorianCalendar implements Serializable {
    
	private static final long serialVersionUID = -4759844108824307831L;

    private long residue;
	
	public Filetime() {
		super();
	}
	
	public Filetime(ByteReader data) throws IOException {
		this(data.read8bytes());
	}
	
	public Filetime(long time) {
		long t = time / 10000;
		residue = time - t;
		setTimeInMillis(t);
		add(GregorianCalendar.YEAR, -369);
	}
	
	public long toLong() {
		GregorianCalendar tmp = (GregorianCalendar)clone();
		tmp.add(GregorianCalendar.YEAR, 369);
		return tmp.getTimeInMillis() + residue;		
	}

	public void serialize(ByteWriter bw) throws IOException {
		bw.write8bytes(toLong());		
	}
	
	public String toString() {
		return String.format("%d:%d:%d %d.%d.%d", 
				get(GregorianCalendar.HOUR_OF_DAY), get(GregorianCalendar.MINUTE), get(GregorianCalendar.SECOND),
				get(GregorianCalendar.DAY_OF_MONTH), get(GregorianCalendar.MONTH) + 1, get(GregorianCalendar.YEAR));
	}
}
