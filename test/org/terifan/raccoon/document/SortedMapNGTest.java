package org.terifan.raccoon.document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.TreeMap;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;
import static org.terifan.raccoon.document._Words.wordlist;


public class SortedMapNGTest
{
	@Test
	public void testSomeMethod()
	{
		ArrayList<String> words = new ArrayList<>(Arrays.asList(wordlist));

		SortedMap<String, Integer> sortmap = new SortedMap<>();
		TreeMap<String, Integer> treemap = new TreeMap<>();
		Random rnd = new Random(1);

		for (int j = 0; j < 1000; j++)
		{
			Collections.shuffle(words, rnd);

			for (int i = 0; i < 1000; i++)
			{
				treemap.put(words.get(i), i);
				sortmap.put(words.get(i), i);
			}

			Collections.shuffle(words, rnd);

			for (int i = 0; i < 900; i++)
			{
				treemap.remove(words.get(i));
				sortmap.remove(words.get(i));
			}

			assertEquals(treemap.toString(), sortmap.toString());
		}
	}
}
