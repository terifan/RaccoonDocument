package org.terifan.raccoon.serializer;


public interface TypeEncoder<T, U>
{
	U write(T aValue) throws Exception;
}
