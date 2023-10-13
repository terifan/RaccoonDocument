package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.IOException;


public interface Marshallable
{
	<T extends DocumentEntity> T marshal();

	void unmarshal(DocumentEntity aDocumentEntity);

	default byte[] marshalByteArray()
	{
		return ((KeyValueContainer)marshal()).toByteArray();
	}

	default void unmarshalFromByteArray(byte[] aBuffer) throws IOException
	{
		unmarshal((DocumentEntity)new BinaryDecoder(new ByteArrayInputStream(aBuffer)).unmarshal());
	}
}
