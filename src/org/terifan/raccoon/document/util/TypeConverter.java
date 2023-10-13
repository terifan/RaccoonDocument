package org.terifan.raccoon.document.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import org.terifan.raccoon.document.Array;
import org.terifan.raccoon.document.Document;


public class TypeConverter
{
	public static void put(Document aDocument, String aKey, Point aValue)
	{
		aDocument.append(aKey, "$pt", toArray(aValue));
	}


	public static Array toArray(Point aValue)
	{
		return Array.of(aValue.x, aValue.y);
	}


	public static Document toDocument(Point aValue)
	{
		return new Document().put("x", aValue.x).put("y", aValue.y);
	}


	public static void put(Document aDocument, String aKey, Color aValue)
	{
		aDocument.append(aKey, "$co", toArray(aValue));
	}


	public static Array toArray(Color aValue)
	{
		if (aValue.getAlpha() != 255)
		{
			return Array.of(aValue.getRed(), aValue.getGreen(), aValue.getBlue(), aValue.getAlpha());
		}
		return Array.of(aValue.getRed(), aValue.getGreen(), aValue.getBlue());
	}


	public static Document toDocument(Color aValue)
	{
		if (aValue.getAlpha() != 255)
		{
			return new Document().put("r", aValue.getRed()).put("g", aValue.getGreen()).put("b", aValue.getBlue()).put("a", aValue.getAlpha());
		}
		return new Document().put("r", aValue.getRed()).put("g", aValue.getGreen()).put("b", aValue.getBlue());
	}


	public static void put(Document aDocument, String aKey, Font aValue)
	{
		aDocument.append(aKey, "$ft", toArray(aValue));
	}


	public static Array toArray(Font aValue)
	{
		String style =
		switch(aValue.getStyle())
		{
			case Font.PLAIN -> style = "Plain";
			case Font.BOLD -> style = "Bold";
			case Font.ITALIC -> style = "Italic";
			case Font.BOLD | Font.ITALIC -> style = "BoldItalic";
			default -> style = Integer.toString(aValue.getStyle());
		};
		return Array.of(aValue.getName(), style, aValue.getSize());
	}


	public static Document toDocument(Font aValue)
	{
		String style =
		switch(aValue.getStyle())
		{
			case Font.PLAIN -> style = "Plain";
			case Font.BOLD -> style = "Bold";
			case Font.ITALIC -> style = "Italic";
			case Font.BOLD | Font.ITALIC -> style = "BoldItalic";
			default -> style = Integer.toString(aValue.getStyle());
		};
		return new Document().put("name", aValue.getName()).put("style", style).put("size", aValue.getSize());
	}


	public static void put(Document aDocument, String aKey, Dimension aValue)
	{
		aDocument.append(aKey, "$di", toArray(aValue));
	}


	public static Array toArray(Dimension aValue)
	{
		return Array.of(aValue.width, aValue.height);
	}


	public static Document toDocument(Dimension aValue)
	{
		return new Document().put("width", aValue.width).put("height", aValue.height);
	}


	public static void put(Document aDocument, String aKey, Rectangle aValue)
	{
		aDocument.append(aKey, "$rt", toArray(aValue));
	}


	public static Array toArray(Rectangle aValue)
	{
		return Array.of(aValue.x, aValue.y, aValue.width, aValue.height);
	}


	public static Document toDocument(Rectangle aValue)
	{
		return new Document().put("x", aValue.x).put("y", aValue.y).put("width", aValue.width).put("height", aValue.height);
	}


	public static <T> T get(Document aDocument, String aKey)
	{
		Array arr = aDocument.getArray(aKey);
		switch (arr.getString(0))
		{
			case "point": return (T)new Point(arr.getInt(1), arr.getInt(2));
			case "color": return (T)new Color(arr.getInt(1), arr.getInt(2), arr.getInt(3), arr.size()==4?0:arr.getInt(4));
			case "font": return (T)new Font(arr.getString(1), arr.getString(2).equalsIgnoreCase("Bold")?Font.BOLD:arr.getString(2).equalsIgnoreCase("Italic")?Font.ITALIC:arr.getString(2).equalsIgnoreCase("BoldItalic")?Font.BOLD|Font.ITALIC:Font.PLAIN, arr.getInt(3));
			case "dim": return (T)new Dimension(arr.getInt(1), arr.getInt(2));
			case "rect": return (T)new Rectangle(arr.getInt(1), arr.getInt(2), arr.getInt(3), arr.getInt(4));
		}
		return null;
	}


	public static void main(String ... args)
	{
		try
		{
			Document doc = new Document();

			put(doc, "triangle", new Point(1,2));
			put(doc, "triangle", new Point(3,4));
			put(doc, "triangle", new Point(5,6));
			put(doc, "triangle", new Color(0,127,255));
			put(doc, "triangle", new Color(255,255,255));
			put(doc, "triangle", new Font("arial",Font.BOLD|Font.ITALIC,48));
			put(doc, "triangle", new Dimension(1,2));
			put(doc, "triangle", new Rectangle(1,2,3,4));

			doc.append("triangle2", "$pt", toArray(new Point(1,2)));
			doc.append("triangle2", "$pt", toArray(new Point(3,4)));
			doc.append("triangle2", "$pt", toArray(new Point(5,6)));
			doc.append("triangle2", "$co", toArray(new Color(0,127,255)));
			doc.append("triangle2", "$co", toArray(new Color(255,255,255)));
			doc.append("triangle2", "$ft", toArray(new Font("arial",Font.BOLD|Font.ITALIC,48)));
			doc.append("triangle2", "$di", toArray(new Dimension(1,2)));
			doc.append("triangle2", "$re", toArray(new Rectangle(1,2,3,4)));

			doc.append("triangle3", "$pt", toDocument(new Point(1,2)));
			doc.append("triangle3", "$pt", toDocument(new Point(3,4)));
			doc.append("triangle3", "$pt", toDocument(new Point(5,6)));
			doc.append("triangle3", "$co", toDocument(new Color(0,127,255)));
			doc.append("triangle3", "$co", toDocument(new Color(255,255,255)));
			doc.append("triangle3", "$ft", toDocument(new Font("arial",Font.BOLD|Font.ITALIC,48)));
			doc.append("triangle3", "$di", toDocument(new Dimension(1,2)));
			doc.append("triangle3", "$re", toDocument(new Rectangle(1,2,3,4)));

			put(doc, "point", new Point(1,2));
			put(doc, "color", new Color(0,127,255));
			put(doc, "font", new Font("arial",Font.BOLD|Font.ITALIC,48));
			put(doc, "dim", new Dimension(1,2));
			put(doc, "rect", new Rectangle(1,2,3,4));

			doc.append("a", 2);

			System.out.println(doc);
//
//			Point pt = get(doc, "point");
//			Color col = get(doc, "color");
//			Font font = get(doc, "font");
//			Dimension dim = get(doc, "dim");
//			Rectangle rect = get(doc, "rect");
//
//			System.out.println(pt);
//			System.out.println(col);
//			System.out.println(font);
//			System.out.println(dim);
//			System.out.println(rect);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
