package test_serializer.model;

import java.awt.Color;
import java.io.Serializable;


public class Person<T extends Person> implements Serializable
{
	private final static long serialVersionUID = 1L;

	private String mFirstName;
	private String mLastName;
	private String mEmail;
	private int mAvatarId;
	private Color mBackgroundColor;
	private Color mForegroundColor;


	public Person()
	{
	}


	public String getFirstName()
	{
		return mFirstName;
	}


	public T setFirstName(String aFirstName)
	{
		mFirstName = aFirstName;
		return (T)this;
	}


	public String getLastName()
	{
		return mLastName;
	}


	public T setLastName(String aLastName)
	{
		mLastName = aLastName;
		return (T)this;
	}


	public String getEmail()
	{
		return mEmail;
	}


	public T setEmail(String aEmail)
	{
		mEmail = aEmail;
		return (T)this;
	}


	public int getAvatarId()
	{
		return mAvatarId;
	}


	public T setAvatarId(int aAvatarId)
	{
		mAvatarId = aAvatarId;
		return (T)this;
	}


	public Color getBackgroundColor()
	{
		return mBackgroundColor;
	}


	public T setBackgroundColor(Color aBackgroundColor)
	{
		mBackgroundColor = aBackgroundColor;
		return (T)this;
	}


	public Color getForegroundColor()
	{
		return mForegroundColor;
	}


	public T setForegroundColor(Color aForegroundColor)
	{
		mForegroundColor = aForegroundColor;
		return (T)this;
	}
}
