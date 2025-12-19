package org.terifan.raccoon.document;


public enum BinaryType
{
	TERMINATOR,
	//
	DOCUMENT,
	ARRAY,
	//
	NULL,
	TRUE,
	FALSE,
	BYTE,
	SHORT,
	CHAR,
	INT,
	LONG,
	FLOAT,
	DOUBLE,
	STRING,
	//
	BINARY,
	BIGDECIMAL,
	BIGINTEGER,
	//
	OBJECTID,
	UUID,
	LOCALDATETIME,
	LOCALDATE,
	LOCALTIME,
	OFFSETDATETIME,
	OFFSETTIME,
	ZONEDDATETIME,
	DURATION,
	//
	INT_REF(-5, 5),
	LONG_REF(-5, 5),
	FLOAT_REF(-5, 5),
	DOUBLE_REF(-5, 5);

	private int mRangeLow;
	private int mRangeHigh;


	BinaryType()
	{
	}


	BinaryType(int aRangeLow, int aRangeHigh)
	{
		mRangeLow = aRangeLow;
		mRangeHigh = aRangeHigh;
	}
}
