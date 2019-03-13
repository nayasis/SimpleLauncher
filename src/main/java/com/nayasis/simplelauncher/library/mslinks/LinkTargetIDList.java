package com.nayasis.simplelauncher.library.mslinks;

import com.nayasis.simplelauncher.library.mslinks.data.ItemID;
import com.nayasis.simplelauncher.library.mslinks.exception.ShellLinkException;
import com.nayasis.simplelauncher.library.mslinks.io.ByteReader;
import com.nayasis.simplelauncher.library.mslinks.io.ByteWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;

@SuppressWarnings( "serial" )
public class LinkTargetIDList extends LinkedList<ItemID> implements Serializable {

	public LinkTargetIDList() {}

	public LinkTargetIDList( ByteReader reader ) throws IOException, ShellLinkException {

		int idListSize = (int)reader.read2bytes();

		int pos = reader.getPosition();

		int itemIdSize = (int)reader.read2bytes();

		while (itemIdSize != 0) {
			itemIdSize -= 2;

			byte[] data = new byte[itemIdSize];

			for (int i=0; i<itemIdSize; i++)

				data[i] = (byte) reader.read();

			add(new ItemID(data));

//			System.out.printf( "\t - itemIdSize : %d, pure data size : %d, data : %s\n", itemIdSize + 2, itemIdSize, new String(data)  );

			itemIdSize = (int) reader.read2bytes();
		}

		pos = reader.getPosition() - pos;

		if (pos != idListSize) throw new ShellLinkException();
	}

	public void serialize(ByteWriter bw) throws IOException {
		int size = 2;
		byte[][] b = new byte[size()][];
		int i = 0;
		for (ItemID j : this) {
			ByteArrayOutputStream ba = new ByteArrayOutputStream();
			ByteWriter w = new ByteWriter(ba);

			j.serialize(w);
			b[i++] = ba.toByteArray();
		}
		for (byte[] j : b)
			size += j.length + 2;

		bw.write2bytes(size);
		for (byte[] j : b) {
			bw.write2bytes(j.length + 2);
			bw.writeBytes(j);
		}
		bw.write2bytes(0);
	}

	public boolean isCorrect() {
		for (ItemID i : this)
			if (i.getType() == ItemID.TYPE_UNKNOWN)
				return false;
		return true;
	}
}
