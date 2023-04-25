package org.terifan.raccoon.document;


public class _BaseColor
{
	int bgred;
	int bggreen;
	int bgblue;
	int overriddenValue;


	public _BaseColor(int aBgred, int aBggreen, int aBgblue)
	{
		this.bgred = aBgred;
		this.bggreen = aBggreen;
		this.bgblue = aBgblue;
		overriddenValue = 5;
	}


	public int getBgred()
	{
		return bgred;
	}


	public void setBgred(int aBgred)
	{
		this.bgred = aBgred;
	}


	public int getBggreen()
	{
		return bggreen;
	}


	public void setBggreen(int aBggreen)
	{
		this.bggreen = aBggreen;
	}


	public int getBgblue()
	{
		return bgblue;
	}


	public void setBgblue(int aBgblue)
	{
		this.bgblue = aBgblue;
	}
}
