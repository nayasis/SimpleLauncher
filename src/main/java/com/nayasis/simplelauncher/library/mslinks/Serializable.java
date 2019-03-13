package com.nayasis.simplelauncher.library.mslinks;

import com.nayasis.simplelauncher.library.mslinks.io.ByteWriter;

import java.io.IOException;

public interface Serializable {
	void serialize( ByteWriter bw ) throws IOException;
}
