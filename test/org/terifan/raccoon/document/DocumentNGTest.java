package org.terifan.raccoon.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;
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
		return new Object[][]{
			{"0"},
			{".1"},{"1."},{"0.1"},
			{"12"},{".12"},{"12.34"},
			{"164313131394.654613219816131"},
			{"1643131313941.654613219816131"},
			{"164313131394.6546132198161312"},
			{"1643131313943.6546132198161314"},
			{"16431313139456.654613219816131"},
			{"16431313139456.6546132198161317"},
			{"16431313139456.65461321981613178"},
			{"32196131943131.31646133219846131496311946313219841940661321981"}
		};
	}


	@Test
	public void testFind()
	{
		Document doc = Document.of("a:{b:{c:[{d:1},{e:2},{f:3}]}}");

		int d = doc.find("a/b/c/0/d");
		int e = doc.find("a/b/c/1/e");
		int f = doc.find("a/b/c/2/f");

		assertEquals(d, 1);
		assertEquals(e, 2);
		assertEquals(f, 3);
	}


	@Test
	public void testInsert()
	{
		Document doc = new Document();

		doc.append("key", Document.of("value:1"));
		doc.append("key", Document.of("value:2"));
		doc.append("key", Document.of("value:3"));

		assertEquals(doc.toString().length(), 86);
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


	@Test(invocationCount = 1000, skipFailedInvocations = true)
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
			.put("bd", _bd)
			;

		Array _allTypesArr = Array.of(_allTypesDoc.values());

		Document srcDoc = new Document()
			.put("doc", _allTypesDoc)
			.put("arr", _allTypesArr)
			;

		byte[] data = srcDoc.toByteArray();
		String json = srcDoc.toJson();
		String text = srcDoc.toString();

		Document unmarshalledBin = new Document().fromByteArray(data);
		Document dstDoc = unmarshalledBin.get("doc");
		Array dstArr = unmarshalledBin.get("arr");

		Document unmarshalledJson = new Document().fromJson(json, true);
		Document dstDocJson = unmarshalledJson.get("doc");
		Array dstArrJson = unmarshalledJson.get("arr");

		Document unmarshalledText = new Document().fromJson(text, true);
		Document dstDocText = unmarshalledText.get("doc");
		Array dstArrText = unmarshalledText.get("arr");

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
		data[10] ^= 2; // "name" => "ncme"

		new Document().readFrom(new ByteArrayInputStream(data));
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


	@Test
	public void testHashcode() throws IOException, ClassNotFoundException
	{
		assertEquals(Document.of("_id:1").hashCode(), -2019545584);
		assertEquals(Document.of("_id:'1'").hashCode(), -1802300669);
		assertEquals(Document.of("_id:[1]").hashCode(), -1393108735);
		assertEquals(Document.of("_id:['1']").hashCode(), 796603583);
	}


	@Test
	public void testInterleaved() throws IOException, ClassNotFoundException
	{
		int a = 1234;
		int b = 789;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BinaryEncoder encoder = new BinaryEncoder(baos);
		encoder.marshal(0);
		encoder.writeInterleaved(a, b);

		BinaryDecoder decoder = new BinaryDecoder(new ByteArrayInputStream(baos.toByteArray()));
		decoder.unmarshal();
		long v = decoder.readInterleaved();

		assertEquals((int)v, a);
		assertEquals((int)(v >>> 32), b);
	}


	@Test
	public void testMarshall() throws IOException, ClassNotFoundException
	{
		byte[] data = _Person.createPerson(new Random(1)).toByteArray();
		System.out.println(data.length);

		_Log.hexDump(data);

		Document d = new Document().fromByteArray(data);
	}


	@Test
	public void testMarshall2() throws IOException, ClassNotFoundException
	{
		byte[] data = Document.of("a:1,b:{c:2},d:[3,{e:4}]").toByteArray();

		System.out.println(data.length);

		_Log.hexDump(data);

		Document d = new Document().fromByteArray(data);

		System.out.println(d);

	}


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
		return new Object[][]{
			{1},
			{2},
			{5},
			{10},
			{1000},
			{1000_000}
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
	public void testReduce()
	{
		Document d = Document.of("{a:[1],b:[],c:[{}],d:[{x:1},[null],{}],e:null}");
		assertEquals(d.toJson(), "{\"a\":[1],\"b\":[],\"c\":[{}],\"d\":[{\"x\":1},[null],{}],\"e\":null}");
		assertEquals(d.reduce().toJson(), "{\"a\":[1],\"d\":[{\"x\":1}]}");
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

		_Log.hexDump(doc.toByteArray());
	}
}
