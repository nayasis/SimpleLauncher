package com.nayasis.simplelauncher.library.mslinks;

import nayasis.simpleLauncher.library.mslinks.io.ByteWriter;

import java.io.IOException;

public interface Serializable {
	void serialize( ByteWriter bw ) throws IOException;
}
