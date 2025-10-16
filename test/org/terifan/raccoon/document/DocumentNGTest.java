package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.DeflaterOutputStream;
import static org.testng.Assert.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class DocumentNGTest
{
	@Test(dataProvider = "decimal")
	public void testBigDecimal(String s)
	{
		Document doc = new Document().put("a", new BigDecimal(s));

//		System.out.println(doc);
		byte[] data = doc.toByteArray();

//		_Log.hexDump(data);
		Document doc2 = new Document().fromByteArray(data);

		assertEquals(doc2.getDecimal("a"), doc.getDecimal("a"));
	}


	@DataProvider
	private Object[][] decimal()
	{
		return new Object[][]
		{
			{
				"0"
			},
			{
				".1"
			},
			{
				"1."
			},
			{
				"0.1"
			},
			{
				"12"
			},
			{
				".12"
			},
			{
				"12.34"
			},
			{
				"164313131394.654613219816131"
			},
			{
				"1643131313941.654613219816131"
			},
			{
				"164313131394.6546132198161312"
			},
			{
				"1643131313943.6546132198161314"
			},
			{
				"16431313139456.654613219816131"
			},
			{
				"16431313139456.6546132198161317"
			},
			{
				"16431313139456.65461321981613178"
			},
			{
				"32196131943131.31646133219846131496311946313219841940661321981"
			},
			{
				"-1.5e+35"
			}
		};
	}


	@Test
	public void testFind()
	{
		Document doc = Document.of(
			"""
				a:{
					b:[
						{
							x:true,
							c:[
								{d:1},
								{e:2},
								{f:3},
								{g:bob, h:4}
							]
						},
						{
							x:false,
							c:[
								{d:1},
								{e:2},
								{f:3},
								{g:bob, h:-4}
							]
						}
					],
					j:[
						[1,2,3],
						[4,{k:test},6]
					]
				}
			""");

		int d = doc.findFirst("a/b/0/c/0/d");
		int e = doc.findFirst("a/b/0/c/1/e");
		int f = doc.findFirst("a/b/0/c/2/f");
		int h = doc.findFirst("a/b/[x=false]/c/[g=bob]/h");
		Integer i = doc.findFirst("a/b/[x=false]/c/[g=bob]/a");
		Document j = doc.findFirst("a/j/1/1");

		assertEquals(d, 1);
		assertEquals(e, 2);
		assertEquals(f, 3);
		assertEquals(h, -4);
		assertEquals(i, null);
		assertEquals(j, Document.of("k:test"));

//		Array arr = Array.of(Document.of("g:bob,h:5"), Document.of("g:eve,h:6"));
//		int i = arr.findFirst("[g=bob]/h");
//		assertEquals(i, 5);
	}


	@Test
	public void testFind2()
	{
		Document doc = new Document().fromJson(new InputStreamReader(DocumentNGTest.class.getResourceAsStream("trip.json")));

		String s = doc.findFirst("/event/payload/lines/0/orderLineId");

		System.out.println(s);
	}


	@Test
	public void testFindMany2()
	{
		Document doc = new Document().fromJson(new InputStreamReader(DocumentNGTest.class.getResourceAsStream("trip.json")));

		System.out.println(doc.findMany("/projections/trip/consignments/parties/shippingLocation/identifiers/identifier"));
		System.out.println("-".repeat(100));

		System.out.println(doc.findMany("/projections/trip/consignments/0/parties/shippingLocation/identifiers/0/identifier"));
		System.out.println(doc.findMany("/projections/trip/consignments/0/parties/shippingLocation/identifiers/1/identifier"));
		System.out.println(doc.findMany("/projections/trip/consignments/0/parties/shippingLocation/identifiers/2/identifier"));
		System.out.println("-".repeat(100));

		System.out.println(doc.findMany("/projections/trip/consignments/1/parties/shippingLocation/identifiers/identifier"));
		System.out.println(doc.findMany("/projections/trip/consignments/2/parties/shippingLocation/identifiers/identifier"));
		System.out.println(doc.findMany("/projections/trip/consignments/3/parties/shippingLocation/identifiers/identifier"));
		System.out.println(doc.findMany("/projections/trip/consignments/4/parties/shippingLocation/identifiers/identifier"));
		System.out.println(doc.findMany("/projections/trip/consignments/5/parties/shippingLocation/identifiers/identifier"));
		System.out.println(doc.findMany("/projections/trip/consignments/6/parties/shippingLocation/identifiers/identifier"));
		System.out.println("-".repeat(100));

		System.out.println(doc.findMany("/projections/trip/consignments/parties/*/identifiers/identifier"));
		System.out.println("-".repeat(100));

		System.out.println(doc.findMany("/projections/trip/consignments/parties/*/identifiers"));
		System.out.println("-".repeat(100));

		System.out.println(doc.findMany("/projections/trip/consignments/parties/*/identifiers[domain=party]"));
		System.out.println("-".repeat(100));

		System.out.println(doc.findMany("/projections/trip/consignments/parties/*/identifiers[domain=party]/identifier"));
		System.out.println("-".repeat(100));
	}


	@Test
	public void testFindMany()
	{
		Document doc = Document.of("people:[{gender:f,name:eve},{gender:x,name:freak},{gender:f,name:liv},{gender:m,name:bob},{gender:m,name:adam},{gender:x,name:fag},{name:adam},{gender:m},{first:bob,last:andersson},{gender:f,name:mary},{gender:[m,f],name:psycho}]");

		Array m = doc.findMany("people/[gender=m]/name");
		Array f = doc.findMany("people/[gender=f]/name");

		assertEquals(m.toJson(), "[\"bob\",\"adam\"]");
		assertEquals(f.toJson(), "[\"eve\",\"liv\",\"mary\"]");
	}


	@Test
	public void testFindManyValues()
	{
		Document doc = Document.of("people:[{gender:f,name:eve},{gender:x,name:freak},{gender:f,name:liv},{gender:m,name:bob},{gender:m,name:adam},{gender:f,name:mary},{gender:[m,f],name:psycho}]");

//		assertEquals(doc.findMany("people/gender", true).toJson(), "[\"f\",\"x\",\"f\",\"m\",\"m\",\"f\"]");
		assertEquals(doc.findMany("people/gender").toJson(), "[\"f\",\"x\",\"f\",\"m\",\"m\",\"f\",[\"m\",\"f\"]]");
	}


	@Test
	public void testFindManyArray()
	{
		Document doc = Document.of("people:[{language:[se,en]},{language:[en,fr,dk]},{language:[de,en]},{language:[fr,pl]}]");

		assertEquals(doc.findMany("people/language").toJson(), "[[\"se\",\"en\"],[\"en\",\"fr\",\"dk\"],[\"de\",\"en\"],[\"fr\",\"pl\"]]");
		assertEquals(doc.findMany("people/language/*").toJson(), "[\"se\",\"en\",\"en\",\"fr\",\"dk\",\"de\",\"en\",\"fr\",\"pl\"]");
	}


	@Test
	public void testFindMany3()
	{
		Document doc = Document.of("maps:[{a:1,b:{x:4}},{a:2,b:{x:5}},{a:3,b:{x:6}}]");

		assertEquals(doc.findMany("maps").toJson(), "[[{\"a\":1,\"b\":{\"x\":4}},{\"a\":2,\"b\":{\"x\":5}},{\"a\":3,\"b\":{\"x\":6}}]]");
		assertEquals(doc.findMany("maps/*").toJson(), "[{\"a\":1,\"b\":{\"x\":4}},{\"a\":2,\"b\":{\"x\":5}},{\"a\":3,\"b\":{\"x\":6}}]");
		assertEquals(doc.findMany("maps/a").toJson(), "[1,2,3]");
		assertEquals(doc.findMany("maps/b").toJson(), "[{\"x\":4},{\"x\":5},{\"x\":6}]");
		assertEquals(doc.findMany("maps/b/x").toJson(), "[4,5,6]");
	}


	@Test
	public void testFindMany4()
	{
		Document doc = Document.of("maps:[{a:[0,{b:4},{b:x}]},{a:[0,{b:5}]},{a:[0,{b:6}]}]");

		assertEquals(doc.findMany("maps/a/1/b").toJson(), "[4,5,6]");
	}


	@Test
	public void testOf()
	{
		Document doc = Document.of("personal/details/language/*:1,personal/firstName:1,personal/ratings/2:1");

//		System.out.println(doc);
	}


	@Test
	public void testAppend()
	{
		Document doc = new Document();
		doc.append("key", Document.of("v:1"));
		doc.append("key", Document.of("v:2"));
		doc.append("key", Document.of("v:3"));

		assertEquals(doc.toJson(), "{\"key\":[{\"v\":1},{\"v\":2},{\"v\":3}]}");
	}


	@Test
	public void testToByteArray()
	{
		Document source = new Document().put("_id", 1).put("text", "hello").put("array", Array.of(1, 2, 3));

		byte[] data = source.toByteArray();

		Document unmarshaled = new Document().fromByteArray(data);

		assertEquals(unmarshaled, source);
	}


	@Test
	public void testDateTimeTypes()
	{
		OffsetDateTime odt = OffsetDateTime.now();

		byte[] data = new Document()
			.put("offset", odt)
			.put("date", odt.toLocalDate())
			.put("time", odt.toLocalTime())
			.put("datetime", odt.toLocalDateTime())
			.toByteArray();

		Document doc = new Document().fromByteArray(data);

		assertEquals(doc.getOffsetDateTime("offset"), odt);
		assertEquals(doc.getDate("date"), odt.toLocalDate());
		assertEquals(doc.getTime("time"), odt.toLocalTime());
		assertEquals(doc.getDateTime("datetime"), odt.toLocalDateTime());
	}


	@Test
	public void testObjectId()
	{
		ObjectId id = ObjectId.randomId();

		byte[] data = new Document()
			.put("_id", id)
			.toByteArray();

		Document doc = new Document().fromByteArray(data);

		assertEquals(doc.get("_id"), id);
		assertEquals(doc.getObjectId("_id"), id);
	}


	@Test(invocationCount = 1, skipFailedInvocations = true)
	public void testAllTypes()
	{
		Byte _byte0 = Byte.MIN_VALUE;
		Byte _byte1 = Byte.MAX_VALUE;
		Short _short0 = Short.MIN_VALUE;
		Short _short1 = Short.MAX_VALUE;
		Integer _int0 = Integer.MIN_VALUE;
		Integer _int1 = Integer.MAX_VALUE;
		Long _long0 = Long.MIN_VALUE;
		Long _long1 = Long.MAX_VALUE;
		Float _float = 3.14f;
		Double _double = Math.PI;
		Boolean _bool = true;
		Object _null = null;
		String _string = "hello";
		byte[] _bytes = "world".getBytes();
		UUID _uuid = UUID.randomUUID();
		OffsetDateTime _odt = OffsetDateTime.now();
		LocalDate _ld = LocalDate.now();
		LocalTime _lt = LocalTime.now();
		LocalDateTime _ldt = LocalDateTime.now();
		Array _arr = Array.of((byte)1, (byte)2, (byte)3); // JSON decoder decodes values to smallest possible representation
		Document _doc = new Document().put("docu", "ment");
		BigDecimal _bd = new BigDecimal("32196131943131.31646133219846131496311946313219841940661321981");

		Document _allTypesDoc = new Document()
			.put("byte0", _byte0)
			.put("byte1", _byte1)
			.put("short0", _short0)
			.put("short1", _short1)
			.put("int0", _int0)
			.put("int1", _int1)
			.put("long0", _long0)
			.put("long1", _long1)
			.put("float", _float)
			.put("double", _double)
			.put("bool", _bool)
			.put("null", _null)
			.put("string", _string)
			.put("bytes", _bytes)
			.put("uuid", _uuid)
			.put("odt", _odt)
			.put("ld", _ld)
			.put("lt", _lt)
			.put("ldt", _ldt)
			.put("arr", _arr)
			.put("doc", _doc)
			.put("bd", _bd);

		Array _allTypesArr = Array.of(
			_byte0,
			_byte1,
			_short0,
			_short1,
			_int0,
			_int1,
			_long0,
			_long1,
			_float,
			_double,
			_bool,
			_null,
			_string,
			_bytes,
			_uuid,
			_odt,
			_ld,
			_lt,
			_ldt,
			_arr,
			_doc,
			_bd
		);

		Document srcDoc = new Document()
			.put("doc", _allTypesDoc)
			.put("arr", _allTypesArr);

		byte[] data = srcDoc.toByteArray();
		String json = srcDoc.toJson();
		String text = srcDoc.toString();

		Document unmarshalledBin = new Document().fromByteArray(data);
		Document dstDoc = unmarshalledBin.get("doc");
		Array dstArr = unmarshalledBin.get("arr");

		Document unmarshalledJson = new JSONDecoder(false, true).unmarshal(new StringReader(json), new Document());
		Document dstDocJson = unmarshalledJson.get("doc");
		Array dstArrJson = unmarshalledJson.get("arr");

		Document unmarshalledText = new JSONDecoder(false, true).unmarshal(new StringReader(text), new Document());
		Document dstDocText = unmarshalledText.get("doc");
		Array dstArrText = unmarshalledText.get("arr");

		System.out.println(unmarshalledJson.keySet());
		System.out.println(srcDoc.keySet());

		assertEquals(unmarshalledBin, srcDoc);
		assertEquals(unmarshalledJson, srcDoc);
		assertEquals(unmarshalledText, srcDoc);

		checkTypes(dstDoc, _byte0, _short0, _int0, _long0, _byte1, _short1, _int1, _long1, _float, _double, _bool, _null, _string, _bytes, _uuid, _odt, _ld, _lt, _ldt, _arr, _doc, _bd);
		checkTypes(dstArr, _byte0, _short0, _int0, _long0, _byte1, _short1, _int1, _long1, _float, _double, _bool, _null, _string, _bytes, _uuid, _odt, _ld, _lt, _ldt, _arr, _doc, _bd);
		checkTypes(dstDocJson, _byte0, _short0, _int0, _long0, _byte1, _short1, _int1, _long1, _float, _double, _bool, _null, _string, _bytes, _uuid, _odt, _ld, _lt, _ldt, _arr, _doc, _bd);
		checkTypes(dstArrJson, _byte0, _short0, _int0, _long0, _byte1, _short1, _int1, _long1, _float, _double, _bool, _null, _string, _bytes, _uuid, _odt, _ld, _lt, _ldt, _arr, _doc, _bd);
		checkTypes(dstDocText, _byte0, _short0, _int0, _long0, _byte1, _short1, _int1, _long1, _float, _double, _bool, _null, _string, _bytes, _uuid, _odt, _ld, _lt, _ldt, _arr, _doc, _bd);
		checkTypes(dstArrText, _byte0, _short0, _int0, _long0, _byte1, _short1, _int1, _long1, _float, _double, _bool, _null, _string, _bytes, _uuid, _odt, _ld, _lt, _ldt, _arr, _doc, _bd);
	}


	private void checkTypes(Array aDstArr, Byte a_byte0, Short a_short0, Integer a_int0, Long a_long0, Byte a_byte1, Short a_short1, Integer a_int1, Long a_long1, Float a_float, Double a_double, Boolean a_bool, Object a_null, String a_string, byte[] a_bytes, UUID a_uuid, OffsetDateTime a_odt, LocalDate a_ld, LocalTime a_lt, LocalDateTime a_ldt, Array a_arr, Document a_doc, BigDecimal a_bd)
	{
		assertEquals(aDstArr.getByte(0), a_byte0);
		assertEquals(aDstArr.getByte(1), a_byte1);
		assertEquals(aDstArr.getShort(2), a_short0);
		assertEquals(aDstArr.getShort(3), a_short1);
		assertEquals(aDstArr.getInt(4), a_int0);
		assertEquals(aDstArr.getInt(5), a_int1);
		assertEquals(aDstArr.getLong(6), a_long0);
		assertEquals(aDstArr.getLong(7), a_long1);
		assertEquals(aDstArr.getFloat(8), a_float);
		assertEquals(aDstArr.getDouble(9), a_double);
		assertEquals(aDstArr.getBoolean(10), a_bool);
		assertEquals(aDstArr.get(11), a_null);
		assertEquals(aDstArr.isNull(11), true);
		assertEquals(aDstArr.getString(12), a_string);
		assertEquals(aDstArr.getBinary(13), a_bytes);
		assertEquals(aDstArr.getUUID(14), a_uuid);
		assertEquals(aDstArr.getOffsetDateTime(15), a_odt);
		assertEquals(aDstArr.getDate(16), a_ld);
		assertEquals(aDstArr.getTime(17), a_lt);
		assertEquals(aDstArr.getDateTime(18), a_ldt);
		assertEquals(aDstArr.getArray(19), a_arr);
		assertEquals(aDstArr.getDocument(20), a_doc);
		assertEquals(aDstArr.getDecimal(21), a_bd);
	}


	private void checkTypes(Document aDstDoc, Byte a_byte0, Short a_short0, Integer a_int0, Long a_long0, Byte a_byte1, Short a_short1, Integer a_int1, Long a_long1, Float a_float, Double a_double, Boolean a_bool, Object a_null, String a_string, byte[] a_bytes, UUID a_uuid, OffsetDateTime a_odt, LocalDate a_ld, LocalTime a_lt, LocalDateTime a_ldt, Array a_arr, Document a_doc, BigDecimal a_bd)
	{
		assertEquals(aDstDoc.getByte("byte0"), a_byte0);
		assertEquals(aDstDoc.getByte("byte1"), a_byte1);
		assertEquals(aDstDoc.getShort("short0"), a_short0);
		assertEquals(aDstDoc.getShort("short1"), a_short1);
		assertEquals(aDstDoc.getInt("int0"), a_int0);
		assertEquals(aDstDoc.getInt("int1"), a_int1);
		assertEquals(aDstDoc.getLong("long0"), a_long0);
		assertEquals(aDstDoc.getLong("long1"), a_long1);
		assertEquals(aDstDoc.getFloat("float"), a_float);
		assertEquals(aDstDoc.getDouble("double"), a_double);
		assertEquals(aDstDoc.getBoolean("bool"), a_bool);
		assertEquals(aDstDoc.get("null"), a_null);
		assertEquals(aDstDoc.isNull("null"), true);
		assertEquals(aDstDoc.getString("string"), a_string);
		assertEquals(aDstDoc.getBinary("bytes"), a_bytes);
		assertEquals(aDstDoc.getUUID("uuid"), a_uuid);
		assertEquals(aDstDoc.getOffsetDateTime("odt"), a_odt);
		assertEquals(aDstDoc.getDate("ld"), a_ld);
		assertEquals(aDstDoc.getTime("lt"), a_lt);
		assertEquals(aDstDoc.getDateTime("ldt"), a_ldt);
		assertEquals(aDstDoc.getArray("arr"), a_arr);
		assertEquals(aDstDoc.getDocument("doc"), a_doc);
		assertEquals(aDstDoc.getDecimal("bd"), a_bd);
	}


	@Test
	public void testUnquotedJSON()
	{
		Document doc = Document.of("_id:[{$ge:20,$lt:30},test,{$exists:true},]");

		assertEquals(doc.toJson(), "{\"_id\":[{\"$ge\":20,\"$lt\":30},\"test\",{\"$exists\":true}]}");
	}


	@Test
	public void testObjectOutputStream() throws IOException, ClassNotFoundException
	{
		Document docOut1 = Document.of("_id:[{low:1,high:2}],name:'bob'");
		Document docOut2 = Document.of("_id:[{low:3,high:5}],name:'dan'");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(baos))
		{
			oos.writeObject(docOut1);
			oos.writeUTF("hello");
			oos.writeObject(docOut2);
		}

		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
		Object docIn1 = ois.readObject();
		String s = ois.readUTF();
		Object docIn2 = ois.readObject();

		assertEquals(docIn1, docOut1);
		assertEquals(docIn2, docOut2);
		assertEquals(s, "hello");
	}


	@Test
	public void testBinaryOutput() throws IOException, ClassNotFoundException
	{
		Document out1 = Document.of("_id:[1],name:'bob'");
		Document out2 = Document.of("_id:[2],name:'eve'");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		out1.writeTo(baos);
		out2.writeTo(baos);

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		Document in1 = new Document().readFrom(bais);
		Document in2 = new Document().readFrom(bais);

		assertEquals(in1, out1);
		assertEquals(in2, out2);
	}


	@Test(expectedExceptions = StreamException.class)
	public void testChecksumError() throws IOException, ClassNotFoundException
	{
		Document out = Document.of("_id:[1],name:'bob'");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		out.writeTo(baos);

		byte[] data = baos.toByteArray();
		data[10] ^= 4;

		new Document().readFrom(new ByteArrayInputStream(data));
	}


	@Test
	public void testChecksum() throws IOException, ClassNotFoundException
	{
		byte[] out1 = Document.of("id:[dog,77,surreptitious]").toByteArray();
		byte[] out2 = Document.of("id:[cat,77,surreptitious]").toByteArray();
		byte[] out3 = Document.of("id:[dog,67,surreptitious]").toByteArray();
		byte[] out4 = Document.of("id:[cat,67,surreptitious]").toByteArray();

//		_Log.hexDump(out1);
//		_Log.hexDump(out2);
//		_Log.hexDump(out3);
//		_Log.hexDump(out4);
	}


	@Test
	public void testCrossMarshalling() throws IOException, ClassNotFoundException
	{
		Document out1 = Document.of("_id:[1],name:'bob'");
		Document out2 = Document.of("_id:[2],name:'eve'");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(out1.toByteArray());
		baos.write('x');
		out2.writeTo(baos);

		ByteArrayInputStream input = new ByteArrayInputStream(baos.toByteArray());
		assertEquals(new Document().readFrom(input), out1);
		assertEquals(input.read(), 'x');
		assertEquals(new Document().readFrom(input), out2);
	}


//	@Test
//	public void testHashcode() throws IOException, ClassNotFoundException
//	{
//		assertEquals(Document.of("_id:1").hashCode(), -2019545584);
//		assertEquals(Document.of("_id:'1'").hashCode(), -1802300669);
//		assertEquals(Document.of("_id:[1]").hashCode(), -1393108735);
//		assertEquals(Document.of("_id:['1']").hashCode(), 796603583);
//	}
	@Test
	public void testInterleaved() throws IOException, ClassNotFoundException
	{
		int a = 1234;
		int b = 789;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BinaryEncoder encoder = new BinaryEncoder(baos, k -> true);
		encoder.marshal(0);
		encoder.writeInterleaved(a, b);

		BinaryDecoder decoder = new BinaryDecoder(new ByteArrayInputStream(baos.toByteArray()));
		decoder.unmarshal();
		long v = decoder.readInterleaved();

		assertEquals((int)v, a);
		assertEquals((int)(v >>> 32), b);
	}


//	@Test
//	public void testMarshall() throws IOException, ClassNotFoundException
//	{
//		String d = "$or:[{$and:[{ratings:1},{name:{$regex:'n.*'}}]},{$and:[{ratings:2},{name:{$regex:'w.*'}}]}]";
//
//		Document doc = Document.of(d);
//
//		System.out.println(doc);
//	}
	@Test(enabled = false)
	public void testMarshallCompressionRatio() throws IOException, ClassNotFoundException
	{
		System.out.println("bin" + "\t" + "zip");
		for (int i = 1; i < 10; i++)
		{
			Array array = new Array();
			for (int j = 0; j < i; j++)
			{
				array.add(_Person.createPerson(new Random(j)));
			}

			byte[] data = array.toByteArray();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (DeflaterOutputStream dos = new DeflaterOutputStream(baos))
			{
				dos.write(data);
			}

			System.out.println(data.length + "\t" + baos.size() + "\t" + (100 - baos.size() * 100 / data.length) + "%");
		}
	}


	@Test(dataProvider = "arrayLengths")
	public void testMarshallArrayNumbers(int n) throws IOException, ClassNotFoundException
	{
		Random rnd = new Random(1);

		Array out = new Array();
		for (int i = 0; i < n; i++)
		{
			out.add((byte)rnd.nextInt());
		}
		for (int i = 0; i < n; i++)
		{
			out.add((short)rnd.nextInt());
		}
		for (int i = 0; i < n; i++)
		{
			out.add(rnd.nextInt());
		}
		for (int i = 0; i < n; i++)
		{
			out.add(rnd.nextLong());
		}
		for (int i = 0; i < n; i++)
		{
			out.add(rnd.nextFloat());
		}
		for (int i = 0; i < n; i++)
		{
			out.add(rnd.nextDouble());
		}

		byte[] data = out.toByteArray();
//		System.out.println(data.length);
//		_Log.hexDump(data);

		Array in = new Array().fromByteArray(data);

		for (int i = 0; i < out.size(); i++)
		{
			assertEquals(in.getNumber(i), out.getNumber(i));
		}
	}


	@DataProvider
	private Object[][] arrayLengths()
	{
		return new Object[][]
		{
			{
				1
			},
			{
				2
			},
			{
				5
			},
			{
				10
			},
			{
				1000
			},
			{
				1000_000
			}
		};
	}


	@Test(enabled = false)
	public void testMarshallDocumentNumbers() throws IOException, ClassNotFoundException
	{
		Document doc = new Document();
		doc.put("a", (short)14);
		doc.put("b", (int)765464647);
		doc.put("c", (long)7646464147844586464L);
		doc.put("d", (float)7);
		doc.put("e", (double)7);

		byte[] data = doc.toByteArray();
//		System.out.println(data.length);
//		_Log.hexDump(data);

		Document a = new Document().fromByteArray(data);
	}


	@Test
	public void testFilter() throws IOException, ClassNotFoundException
	{
		Document out = new Document();
		out.put("a", (short)14);
		out.put("b", (int)765464647);
		out.put("c", Array.of(34, 7646464147844586464L));
		out.put("d", (float)7);
		out.put("e", (double)7);

		byte[] data = out.toByteArray(k -> k.startsWith("a") || k.startsWith(new Path("c")));

		System.out.println(new String(data));

		Document in = new Document().fromByteArray(data);

		assertEquals(in.size(), 2);
		assertEquals(in.get("a"), out.getShort("a"));
//		assertEquals(in.get("c"), out.getLong("c"));

		System.out.println(in);
	}


	@Test
	public void testReduce()
	{
		Document d = Document.of("{a:[1],b:[],c:[{}],d:[{x:1},[null],{}],e:null}");
		assertEquals(d.toJson(), "{\"a\":[1],\"b\":[],\"c\":[{}],\"d\":[{\"x\":1},[null],{}],\"e\":null}");
		assertEquals(d.reduce().toJson(), "{\"a\":[1],\"d\":[{\"x\":1}]}");

		Document e = new Document().put("a", Document.of("z:1")).put("b", Document.of("z:1"));
		assertNotSame(e.get("a"), e.get("b"));
		e.reduce();
		assertSame(e.get("a"), e.get("b"));
	}


	@Test
	public void testKeyComparator() throws IOException
	{
		TreeSet<String> set = new TreeSet<>(Document.STANDARD_COMPARATOR);
		set.add("A");
		set.add("_a");
		set.add("a");
		set.add("0");
		set.add("_id");
		assertEquals(set.toString(), "[_id, _a, 0, A, a]");
	}


	@Test
	public void testSize() throws IOException
	{
		Random rnd = new Random(1);
		Document doc = _Person.createPerson(rnd);

		System.out.println("          json: " + doc.toJson().length());
		System.out.println("    typed-json: " + doc.toTypedJson().length());
		System.out.println("           bin: " + doc.toByteArray().length);

		ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
		try (DeflaterOutputStream dos = new DeflaterOutputStream(baos2))
		{
			dos.write(doc.toJson().getBytes("utf-8"));
		}
		System.out.println("      json-zip: " + baos2.size());

		ByteArrayOutputStream baos3 = new ByteArrayOutputStream();
		try (DeflaterOutputStream dos = new DeflaterOutputStream(baos3))
		{
			dos.write(doc.toTypedJson().getBytes("utf-8"));
		}
		System.out.println("typed-json-zip: " + baos3.size());

		ByteArrayOutputStream baos4 = new ByteArrayOutputStream();
		try (DeflaterOutputStream dos = new DeflaterOutputStream(baos4))
		{
			dos.write(doc.toByteArray());
		}
		System.out.println("       bin-zip: " + baos4.size());

//		_Log.hexDump(doc.toByteArray());
	}


	@Test
	public void testLargeBlock() throws IOException
	{
		byte[] data = new byte[10 * 1024 * 1024];
		new Random().nextBytes(data);

		Document doc = new Document().put("bin", data);

		long t = System.currentTimeMillis();
		doc.toByteArray();
//		System.out.println(System.currentTimeMillis() - t);
	}


	@Test
	public void testByteBuffer() throws IOException
	{
		Random rnd = new Random(1);

		Document out = _Person.createPerson(rnd);

		byte[] data = out.toByteArray();

		ByteBuffer buf = ByteBuffer.allocate(1024);
		buf.put(data);

		Document in = new Document().fromByteArray(buf.position(0).limit(data.length));

		assertEquals(in, out);
	}


	@Test
	public void testIncrement() throws IOException
	{
		assertEquals((long)Document.of("i:" + Integer.MAX_VALUE).increment("i", 1).getLong("i"), 2147483648L);
	}


	@Test
	public void testConditionalPut()
	{
		Document doc = new Document();

		doc.putWithCondition("a", 1, x -> x == 1);
		doc.putWithCondition("b", 2, x -> x == 1);

		assertEquals(doc.get("a"), (Integer)1);
		assertTrue(!doc.containsKey("b"));
	}


	@Test
	public void testChaining()
	{
		Object gender = null;
		Stream s = Stream.of(1, 2, 3).map(i -> -i);

		Document doc = new Document()
			.put("name", "bob")
			.putIfAbsent("address", key -> new Document()
			.put("street", "Big road")
			.put("city", "Smallville")
			.put("country", "Americastan")
			)
			.put("info", Array.of("fatty")
				.addWithCondition("weirdo", key -> gender == null)
			)
			.put("number", new Array()
				.addAll(s)
			)
			.fromJson("color:red,shape:round")
			.putWithCondition("gender", gender, value -> value != null)
			.putWhenCondition("catapult", key -> gender == null, key -> Document.of("when:now!"))
			.append("name", "johnson");

//		System.out.println(doc);
		assertEquals(doc.toString(), "{\"name\":[\"bob\",\"johnson\"],\"address\":{\"street\":\"Big road\",\"city\":\"Smallville\",\"country\":\"Americastan\"},\"info\":[\"fatty\",\"weirdo\"],\"number\":[-1,-2,-3],\"color\":\"red\",\"shape\":\"round\",\"catapult\":{\"when\":\"now!\"}}");
	}


	@Test
	public void testUnmarshalId() throws IOException
	{
		Random rnd = new Random(1);
		Document out = _Person.createPerson(rnd);

		byte[] data = out.toByteArray();

		Document doc = new Document().fromByteArray(data);

//		System.out.println(doc);
//		assertEquals(in, out);
	}


	@Test
	public void testClone() throws IOException
	{
		Document doc = Document.of("a:1,b:{c:2},d:[3,4]").put("arr", Array.of(1, 2, 3));

		Document other = doc.clone();

		assertEquals(doc, other);
		assertNotSame(doc.getArray("arr"), other.getArray("arr"));
	}


	@Test
	public void testSort() throws IOException
	{
		Document doc = Document.of("a:1,c:3,b:2,_id:7,_bool:true");

		assertEquals(doc.toString(), "{\"a\":1,\"c\":3,\"b\":2,\"_id\":7,\"_bool\":true}");

		doc.sort();

		assertEquals(doc.toString(), "{\"_id\":7,\"_bool\":true,\"a\":1,\"b\":2,\"c\":3}");

		doc.sort((o1, o2) -> o1.compareTo(o2));

		assertEquals(doc.toString(), "{\"_bool\":true,\"_id\":7,\"a\":1,\"b\":2,\"c\":3}");
	}


	@Test
	public void testReference() throws IOException
	{
		Document a1 = Document.of("number:'47314631'");
		Document a2 = Document.of("number:'47314631'");
		Document a3 = Document.of("number:'47314631'");
		Document a4 = Document.of("number:'47314631'");
		Document a5 = Document.of("number:'47314631'");

//		Document a5;
//		long ex = a1.hashCode();
//		a5 = new Document();
//		for (long i = 47314631+1; ;i++)
//		{
//			a5.put("number", ""+i);
//			if (a5.hashCode()==ex) break;
//		}
//		System.out.println(a5);
//		HashMap<Document,Document> map = new HashMap<>();
//		map.putIfAbsent(a1, a1);
//		map.putIfAbsent(a2, a2);
//		map.putIfAbsent(a3, a3);
//		map.putIfAbsent(a4, a4);
//		map.putIfAbsent(a5, a5);
//		a1 = map.get(a1);
//		a2 = map.get(a2);
//		a3 = map.get(a3);
//		a4 = map.get(a4);
//		a5 = map.get(a5);
		Document out = Document.of("text:'hello world'");
		out.put("alpha", a1);
		out.put("beta", a2);
		out.put("gamma", a3);
		out.put("omega", a4);
		out.put("zeta", a5);

		out.reduce();

		System.out.println(out);
		System.out.println(new String(out.toByteArray()));

		Document in = new Document().fromByteArray(out.toByteArray());

		System.out.println(in.hashCode() == out.hashCode());
		System.out.println(in.equals(out));
	}
}
