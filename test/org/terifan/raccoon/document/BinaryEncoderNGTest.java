package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import test_document._Log;


public class BinaryEncoderNGTest
{
	@Test
	public void testWritingOjectsToObjectOutputStream() throws IOException, ClassNotFoundException
	{
		Document person = test_document._Person.createPerson(new Random(1));
		Array array = Array.of(1, 2, Array.of(3, 4), 5, 6, Array.of(3, 4), 7, 8, Array.of(Array.of(3, 4), Array.of(3, 4)), 9);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream dos = new ObjectOutputStream(baos))
		{
			dos.writeInt(346549864);
			dos.writeObject(person);
			dos.writeInt(346549864);
			dos.writeObject(array);
			dos.writeInt(346549864);
		}

//		_Log.hexDump(baos.toByteArray());

		ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
		assertEquals(in.readInt(), 346549864);
		assertEquals(in.readObject(), person);
		assertEquals(in.readInt(), 346549864);
		assertEquals(in.readObject(), array);
		assertEquals(in.readInt(), 346549864);
	}
}
