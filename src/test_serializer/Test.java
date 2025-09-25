package test_serializer;

import java.awt.Color;
import java.awt.Point;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Currency;
import org.terifan.raccoon.serializer.Marshaller;
import org.terifan.raccoon.document.Array;
import org.terifan.raccoon.document.Document;
import org.terifan.raccoon.document.JSONEncoder;
import test_serializer.model.Category;
import test_serializer.model.Customer;
import test_serializer.model.Customer.Address;
import test_serializer.model.Employee;
import test_serializer.model.Order;
import test_serializer.model.Price;
import test_serializer.model.Product;
import test_serializer.model.Size;
import test_serializer.model.Weight;


public class Test
{
	public static void xmain(String ... args)
	{
		try
		{
			{
			Document child = Document.of("c:'xxxxxxxxx'");
			Document parent = new Document();
			parent.put("a", child);
			System.out.println(new String(parent.toByteArray()));
			System.out.println(parent.toJson());
			}
			System.out.println();
			{
			Document child = Document.of("c:'xxxxxxxxx'");
			Document parent = new Document();
			parent.put("a", child);
			parent.put("b", child);
			System.out.println(new String(parent.toByteArray()));
			System.out.println(parent.toJson());
			}
			System.out.println();
			{
			Document child1 = Document.of("c:'xxxxxxxxx'");
			Document child2 = Document.of("c:'xxxxxxxxx'");
			Document parent = new Document();
			parent.put("a", child1);
			parent.put("b", child2);
			System.out.println(new String(parent.toByteArray()));
			System.out.println(parent.toJson());
			}
			System.out.println();
			{
			Document child1 = Document.of("c:'xxxxxxxxx'");
			Document child2 = Document.of("c:'yyyyyyyyy'");
			Document parent = new Document();
			parent.put("a", child1);
			parent.put("b", child2);
			System.out.println(new String(parent.toByteArray()));
			System.out.println(parent.toJson());
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}

	public static void main(String... args)
	{
		try
		{
//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
//			ObjectOutputStream oos = new ObjectOutputStream(baos);
//			oos.writeObject(Color.RED);
//			oos.close();
//			System.out.println(new String(baos.toString()));

			Product product1 = new Product().setName("apple").setPrice(new Price().setValue(3.25).setCurrency(Currency.getInstance("SEK"))).addFeature(new Weight().setValue(32), new Size().setValue(7)).setCategory(Category.FRUIT).setProperty("kcal", 152);
			Address address = new Address(Document.of("street:'Main street'"));
			Customer customer = new Customer().setCustomerNumber(3165).setDeliveryAddress(address).setInvoiceAddress(address).setDiscountPercent(5).setFirstName("Bob").setLastName("Kendell").setEmail("bob.kendell@gloggle.com").setAvatarId(21).setBackgroundColor(Color.BLACK).setForegroundColor(Color.WHITE);
			Employee employee = new Employee().setEmployeeNumber(7).setFirstName("greta");
			Order order = new Order().setCustomer(customer).setSalesPerson(employee).addProduct(product1);
			customer.getOrders().add(order);
			product1.setProperty(new Point(1,2), "a").setProperty(new Point(2,3), "b");


			Marshaller marshaller = new Marshaller()
				.registerType(Color.class, aColor -> aColor == null ? null : Array.of(aColor.getRed(), aColor.getGreen(), aColor.getBlue()), aArray -> new Color(aArray.getInt(0), aArray.getInt(1), aArray.getInt(2)))
//				.registerType(Point.class, aPoint -> aPoint == null ? null : Document.of("x:" + aPoint.x + ",y:" + aPoint.y), aDocument -> new Point(aDocument.getInt("x"), aDocument.getInt("y")))
				.registerType(Point.class, aPoint -> aPoint == null ? null : Array.of(aPoint.x, aPoint.y), aDocument -> new Point(aDocument.getInt(0), aDocument.getInt(1)))
				.registerType(Currency.class, aCurrency -> aCurrency == null ? null : aCurrency.getCurrencyCode(), Currency::getInstance)
				.registerType(Address.class, aAddress -> aAddress == null ? null : aAddress.mAddress, Address::new)
//				.setIncludeOverriddenFields(true)
//				.setIncludeClassTypes(true)
//				.setEnableComplexMapKeySerialization(true)
				.setExcludeFieldsWithModifiers()
				.setExcludeFieldsWithNullValue(true)
				.bind("org.terifan.marshaller.model.Price", 1).to(document ->
			{
				return new Price();
			});

			Document doc = marshaller.marshall(order);
//			Document doc = marshaller.marshall(product1);
			System.out.println(doc.toJson(false));
			byte[] toByteArray = doc.toByteArray();
			System.out.println(new String(toByteArray));
			System.out.println(toByteArray.length);


//			doc.reduce();
//
//			Class<?> cls = Class.forName("test.model.Weight");
//			Object instance = cls.getConstructor().newInstance();
//			Field field0 = cls.getDeclaredField("mUnit");
//			field0.setAccessible(true);
//			field0.set(instance, "kg");
//			Field field1 = cls.getSuperclass().getDeclaredField("mName");
//			field1.setAccessible(true);
//			field1.set(instance, "test");
//			Field field2 = cls.getSuperclass().getSuperclass().getDeclaredField("mValue");
//			field2.setAccessible(true);
//			field2.set(instance, 44);
//			System.out.println(instance);


//			Document d = Document.of("xoxoxoxoxoxoxo:11111111111");
//			Document e = new Document().put("z", Document.of("ioioioioioioio:22222222222"));
//			Document doc = new Document().put("arr", Array.of(d,e,d,1,d));
//
//			System.out.println(doc);
//
//			System.out.println(new String(doc.toByteArray()));
//
//			Document fromByteArray = new Document().fromByteArray(doc.toByteArray());
//			System.out.println(fromByteArray);
//			System.out.println("-".repeat(200));
//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
//			ObjectOutputStream oos = new ObjectOutputStream(baos);
//			oos.writeObject(order);
//			oos.close();
//			System.out.println(new String(baos.toString()));
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
