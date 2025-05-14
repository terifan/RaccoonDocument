package org.terifan.raccoon.serializer;


public interface TypeDecoder<T, U>
{
	T read(U aValue) throws Exception;
}
