package org.terifan.raccoon.document;


public class _Color extends _BaseColor
{
	private final static long serialVersionUID = 123;
	int red;
	int green;
	int blue;
	int overriddenValue;
	char c = 'x';
	String s = "str";


	public _Color(int aRed, int aGreen, int aBlue)
	{
		super(-aRed, -aGreen, -aBlue);
		this.red = aRed;
		this.green = aGreen;
		this.blue = aBlue;
		overriddenValue = 1;
	}


	public int getRed()
	{
		return red;
	}


	public void setRed(int aRed)
	{
		this.red = aRed;
	}


	public int getGreen()
	{
		return green;
	}


	public void setGreen(int aGreen)
	{
		this.green = aGreen;
	}


	public int getBlue()
	{
		return blue;
	}


	public void setBlue(int aBlue)
	{
		this.blue = aBlue;
	}
}
