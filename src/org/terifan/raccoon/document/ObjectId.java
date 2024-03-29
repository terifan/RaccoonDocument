package org.terifan.raccoon.document;

import java.io.Serializable;
import static java.lang.Integer.parseUnsignedInt;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;


/*
 *   0                   1                   2                   3
 *   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                              time                             |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                             session                           |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  |                            sequence                           |
 *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 * time      - 32 bits - time in seconds since midnight January 1, 1970 UTC
 * session   - 32 bits - random value used in all intances per JVM instance
 * sequence  - 32 bits - incrementing counter, initialized to a random value
 */
public final class ObjectId implements Serializable, Comparable<ObjectId>
{
	private final static long serialVersionUID = 1;

	public final static int LENGTH = 12;

	private final static int L1 = 9;
	private final static int L2 = 13;
	private final static int L3 = 5;
	private final static int R1 = 7;
	private final static int R2 = 25;
	private final static int R3 = 10;

	private final int mTime;
	private final int mSession;
	private final int mSequence;


	// lazy initialization
	private static class Holder
	{
		final static SecureRandom PRNG = new SecureRandom();
		final static int SESSION = PRNG.nextInt();
		final static AtomicInteger SEQUENCE = new AtomicInteger(PRNG.nextInt());
		final static char[] BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
		final static short[] PRIMES = {2,3,5,7,11,13,17,19,23,29,31,37,41,43,47,53,59,61,67,71,73,79,83,89,97,101,103,107,109,113,127,131,137,139,149,151,157,163,167,173,179,181,191,193,197,199,211,223,227,229,233,239,241,251,257,263,269,271,277,281,283,293,307,311,313,317,331,337,347,349,353,359,367,373,379,383,389,397,401,409,419,421,431,433,439,443,449,457,461,463,467,479,487,491,499,503,509,521,523,541,547,557,563,569,571,577,587,593,599,601,607,613,617,619,631,641,643,647,653,659,661,673,677,683,691,701,709,719,727,733,739,743,751,757,761,769,773,787,797,809,811,821,823,827,829,839,853,857,859,863,877,881,883,887,907,911,919,929,937,941,947,953,967,971,977,983,991,997,1009,1013,1019,1021,1031,1033,1039,1049,1051,1061,1063,1069,1087,1091,1093,1097,1103,1109,1117,1123,1129,1151,1153,1163,1171,1181,1187,1193,1201,1213,1217,1223,1229,1231,1237,1249,1259,1277,1279,1283,1289,1291,1297,1301,1303,1307,1319,1321,1327,1361,1367,1373,1381,1399,1409,1423,1427,1429,1433,1439,1447,1451,1453,1459,1471,1481,1483,1487,1489,1493,1499,1511,1523,1531,1543,1549,1553,1559,1567,1571,1579,1583,1597,1601,1607,1609,1613,1619,1621,1627,1637,1657,1663,1667,1669,1693,1697,1699,1709,1721,1723,1733,1741,1747,1753,1759,1777,1783,1787,1789,1801,1811,1823,1831,1847,1861,1867,1871,1873,1877,1879,1889,1901,1907,1913,1931,1933,1949,1951,1973,1979,1987,1993,1997,1999,2003,2011,2017,2027,2029,2039,2053,2063,2069,2081,2083,2087,2089,2099,2111,2113,2129,2131,2137,2141,2143,2153,2161,2179,2203,2207,2213,2221,2237,2239,2243,2251,2267,2269,2273,2281,2287,2293,2297,2309,2311,2333,2339,2341,2347,2351,2357,2371,2377,2381,2383,2389,2393,2399,2411,2417,2423,2437,2441,2447,2459,2467,2473,2477,2503,2521,2531,2539,2543,2549,2551,2557,2579,2591,2593,2609,2617,2621,2633,2647,2657,2659,2663,2671,2677,2683,2687,2689,2693,2699,2707,2711,2713,2719,2729,2731,2741,2749,2753,2767,2777,2789,2791,2797,2801,2803,2819,2833,2837,2843,2851,2857,2861,2879,2887,2897,2903,2909,2917,2927,2939,2953,2957,2963,2969,2971,2999,3001,3011,3019,3023,3037,3041,3049,3061,3067,3079,3083,3089,3109,3119,3121,3137,3163,3167,3169,3181,3187,3191,3203,3209,3217,3221,3229,3251,3253,3257,3259,3271,3299,3301,3307,3313,3319,3323,3329,3331,3343,3347,3359,3361,3371,3373,3389,3391,3407,3413,3433,3449,3457,3461,3463,3467,3469,3491,3499,3511,3517,3527,3529,3533,3539,3541,3547,3557,3559,3571,3581,3583,3593,3607,3613,3617,3623,3631,3637,3643,3659,3671,3673,3677,3691,3697,3701,3709,3719,3727,3733,3739,3761,3767,3769,3779,3793,3797,3803,3821,3823,3833,3847,3851,3853,3863,3877,3881,3889,3907,3911,3917,3919,3923,3929,3931,3943,3947,3967,3989,4001,4003,4007,4013,4019,4021,4027,4049,4051,4057,4073,4079,4091,4093,4099,4111,4127,4129,4133,4139,4153,4157,4159,4177,4201,4211,4217,4219,4229,4231,4241,4243,4253,4259,4261,4271,4273,4283,4289,4297,4327,4337,4339,4349,4357,4363,4373,4391,4397,4409,4421,4423,4441,4447,4451,4457,4463,4481,4483,4493,4507,4513,4517,4519,4523,4547,4549,4561,4567,4583,4591,4597,4603,4621,4637,4639,4643,4649,4651,4657,4663,4673,4679,4691,4703,4721,4723,4729,4733,4751,4759,4783,4787,4789,4793,4799,4801,4813,4817,4831,4861,4871,4877,4889,4903,4909,4919,4931,4933,4937,4943,4951,4957,4967,4969,4973,4987,4993,4999,5003,5009,5011,5021,5023,5039,5051,5059,5077,5081,5087,5099,5101,5107,5113,5119,5147,5153,5167,5171,5179,5189,5197,5209,5227,5231,5233,5237,5261,5273,5279,5281,5297,5303,5309,5323,5333,5347,5351,5381,5387,5393,5399,5407,5413,5417,5419,5431,5437,5441,5443,5449,5471,5477,5479,5483,5501,5503,5507,5519,5521,5527,5531,5557,5563,5569,5573,5581,5591,5623,5639,5641,5647,5651,5653,5657,5659,5669,5683,5689,5693,5701,5711,5717,5737,5741,5743,5749,5779,5783,5791,5801,5807,5813,5821,5827,5839,5843,5849,5851,5857,5861,5867,5869,5879,5881,5897,5903,5923,5927,5939,5953,5981,5987,6007,6011,6029,6037,6043,6047,6053,6067,6073,6079,6089,6091,6101,6113,6121,6131,6133,6143,6151,6163,6173,6197,6199,6203,6211,6217,6221,6229,6247,6257,6263,6269,6271,6277,6287,6299,6301,6311,6317,6323,6329,6337,6343,6353,6359,6361,6367,6373,6379,6389,6397,6421,6427,6449,6451,6469,6473,6481,6491,6521,6529,6547,6551,6553,6563,6569,6571,6577,6581,6599,6607,6619,6637,6653,6659,6661,6673,6679,6689,6691,6701,6703,6709,6719,6733,6737,6761,6763,6779,6781,6791,6793,6803,6823,6827,6829,6833,6841,6857,6863,6869,6871,6883,6899,6907,6911,6917,6947,6949,6959,6961,6967,6971,6977,6983,6991,6997,7001,7013,7019,7027,7039,7043,7057,7069,7079,7103,7109,7121,7127,7129,7151,7159,7177,7187,7193,7207,7211,7213,7219,7229,7237,7243,7247,7253,7283,7297,7307,7309,7321,7331,7333,7349,7351,7369,7393,7411,7417,7433,7451,7457,7459,7477,7481,7487,7489,7499,7507,7517,7523,7529,7537,7541,7547,7549,7559,7561,7573,7577,7583,7589,7591,7603,7607,7621,7639,7643,7649,7669,7673,7681,7687,7691,7699,7703,7717,7723,7727,7741,7753,7757,7759,7789,7793,7817,7823,7829,7841,7853,7867,7873,7877,7879,7883,7901,7907,7919,7927,7933,7937,7949,7951,7963,7993,8009,8011,8017,8039,8053,8059,8069,8081,8087,8089,8093,8101,8111,8117,8123,8147,8161,8167,8171,8179,8191,8209,8219,8221,8231,8233,8237,8243,8263,8269,8273,8287,8291,8293,8297,8311,8317,8329,8353,8363,8369,8377,8387,8389,8419,8423,8429,8431,8443,8447,8461,8467,8501,8513,8521,8527,8537,8539,8543,8563,8573,8581,8597,8599,8609,8623,8627,8629,8641,8647,8663,8669,8677,8681,8689,8693,8699,8707,8713,8719,8731,8737,8741,8747,8753,8761,8779,8783,8803,8807,8819,8821,8831,8837,8839,8849,8861,8863,8867,8887,8893,8923,8929,8933,8941,8951,8963,8969,8971,8999,9001,9007,9011,9013,9029,9041,9043,9049,9059,9067,9091,9103,9109,9127,9133,9137,9151,9157,9161,9173,9181,9187,9199,9203,9209,9221,9227,9239,9241,9257,9277,9281,9283,9293,9311,9319,9323,9337,9341,9343,9349,9371,9377,9391,9397,9403,9413,9419,9421,9431,9433,9437,9439,9461,9463,9467,9473,9479,9491,9497,9511,9521,9533,9539,9547,9551,9587,9601,9613,9619,9623,9629,9631,9643,9649,9661,9677,9679,9689,9697,9719,9721,9733,9739,9743,9749,9767,9769,9781,9787,9791,9803,9811,9817,9829,9833,9839,9851,9857,9859,9871,9883,9887,9901,9907,9923,9929,9931,9941,9949,9967,9973,10007,10009,10037,10039,10061,10067,10069,10079,10091,10093,10099,10103,10111,10133,10139,10141,10151,10159,10163,10169,10177,10181,10193,10211,10223,10243,10247,10253,10259,10267,10271,10273,10289,10301,10303,10313,10321,10331,10333,10337,10343,10357,10369,10391,10399,10427,10429,10433,10453,10457,10459,10463,10477,10487,10499,10501,10513,10529,10531,10559,10567,10589,10597,10601,10607,10613,10627,10631,10639,10651,10657,10663,10667,10687,10691,10709,10711,10723,10729,10733,10739,10753,10771,10781,10789,10799,10831,10837,10847,10853,10859,10861,10867,10883,10889,10891,10903,10909,10937,10939,10949,10957,10973,10979,10987,10993,11003,11027,11047,11057,11059,11069,11071,11083,11087,11093,11113,11117,11119,11131,11149,11159,11161,11171,11173,11177,11197,11213,11239,11243,11251,11257,11261,11273,11279,11287,11299,11311,11317,11321,11329,11351,11353,11369,11383,11393,11399,11411,11423,11437,11443,11447,11467,11471,11483,11489,11491,11497,11503,11519,11527,11549,11551,11579,11587,11593,11597,11617,11621,11633,11657,11677,11681,11689,11699,11701,11717,11719,11731,11743,11777,11779,11783,11789,11801,11807,11813,11821,11827,11831,11833,11839,11863,11867,11887,11897,11903,11909,11923,11927,11933,11939,11941,11953,11959,11969,11971,11981,11987,12007,12011,12037,12041,12043,12049,12071,12073,12097,12101,12107,12109,12113,12119,12143,12149,12157,12161,12163,12197,12203,12211,12227,12239,12241,12251,12253,12263,12269,12277,12281,12289,12301,12323,12329,12343,12347,12373,12377,12379,12391,12401,12409,12413,12421,12433,12437,12451,12457,12473,12479,12487,12491,12497,12503,12511,12517,12527,12539,12541,12547,12553,12569,12577,12583,12589,12601,12611,12613,12619,12637,12641,12647,12653,12659,12671,12689,12697,12703,12713,12721,12739,12743,12757,12763,12781,12791,12799,12809,12821,12823,12829,12841,12853,12889,12893,12899,12907,12911,12917,12919,12923,12941,12953,12959,12967,12973,12979,12983,13001,13003,13007,13009,13033,13037,13043,13049,13063,13093,13099,13103,13109,13121,13127,13147,13151,13159,13163,13171,13177,13183,13187,13217,13219,13229,13241,13249,13259,13267,13291,13297,13309,13313,13327,13331,13337,13339,13367,13381,13397,13399,13411,13417,13421,13441,13451,13457,13463,13469,13477,13487,13499,13513,13523,13537,13553,13567,13577,13591,13597,13613,13619,13627,13633,13649,13669,13679,13681,13687,13691,13693,13697,13709,13711,13721,13723,13729,13751,13757,13759,13763,13781,13789,13799,13807,13829,13831,13841,13859,13873,13877,13879,13883,13901,13903,13907,13913,13921,13931,13933,13963,13967,13997,13999,14009,14011,14029,14033,14051,14057,14071,14081,14083,14087,14107,14143,14149,14153,14159,14173,14177,14197,14207,14221,14243,14249,14251,14281,14293,14303,14321,14323,14327,14341,14347,14369,14387,14389,14401,14407,14411,14419,14423,14431,14437,14447,14449,14461,14479,14489,14503,14519,14533,14537,14543,14549,14551,14557,14561,14563,14591,14593,14621,14627,14629,14633,14639,14653,14657,14669,14683,14699,14713,14717,14723,14731,14737,14741,14747,14753,14759,14767,14771,14779,14783,14797,14813,14821,14827,14831,14843,14851,14867,14869,14879,14887,14891,14897,14923,14929,14939,14947,14951,14957,14969,14983,15013,15017,15031,15053,15061,15073,15077,15083,15091,15101,15107,15121,15131,15137,15139,15149,15161,15173,15187,15193,15199,15217,15227,15233,15241,15259,15263,15269,15271,15277,15287,15289,15299,15307,15313,15319,15329,15331,15349,15359,15361,15373,15377,15383,15391,15401,15413,15427,15439,15443,15451,15461,15467,15473,15493,15497,15511,15527,15541,15551,15559,15569,15581,15583,15601,15607,15619,15629,15641,15643,15647,15649,15661,15667,15671,15679,15683,15727,15731,15733,15737,15739,15749,15761,15767,15773,15787,15791,15797,15803,15809,15817,15823,15859,15877,15881,15887,15889,15901,15907,15913,15919,15923,15937,15959,15971,15973,15991,16001,16007,16033,16057,16061,16063,16067,16069,16073,16087,16091,16097,16103,16111,16127,16139,16141,16183,16187,16189,16193,16217,16223,16229,16231,16249,16253,16267,16273,16301,16319,16333,16339,16349,16361,16363,16369,16381,16411,16417,16421,16427,16433,16447,16451,16453,16477,16481,16487,16493,16519,16529,16547,16553,16561,16567,16573,16603,16607,16619,16631,16633,16649,16651,16657,16661,16673,16691,16693,16699,16703,16729,16741,16747,16759,16763,16787,16811,16823,16829,16831,16843,16871,16879,16883,16889,16901,16903,16921,16927,16931,16937,16943,16963,16979,16981,16987,16993,17011,17021,17027,17029,17033,17041,17047,17053,17077,17093,17099,17107,17117,17123,17137,17159,17167,17183,17189,17191,17203,17207,17209,17231,17239,17257,17291,17293,17299,17317,17321,17327,17333,17341,17351,17359,17377,17383,17387,17389,17393,17401,17417,17419,17431,17443,17449,17467,17471,17477,17483,17489,17491,17497,17509,17519,17539,17551,17569,17573,17579,17581,17597,17599,17609,17623,17627,17657,17659,17669,17681,17683,17707,17713,17729,17737,17747,17749,17761,17783,17789,17791,17807,17827,17837,17839,17851,17863,17881,17891,17903,17909,17911,17921,17923,17929,17939,17957,17959,17971,17977,17981,17987,17989,18013,18041,18043,18047,18049,18059,18061,18077,18089,18097,18119,18121,18127,18131,18133,18143,18149,18169,18181,18191,18199,18211,18217,18223,18229,18233,18251,18253,18257,18269,18287,18289,18301,18307,18311,18313,18329,18341,18353,18367,18371,18379,18397,18401,18413,18427,18433,18439,18443,18451,18457,18461,18481,18493,18503,18517,18521,18523,18539,18541,18553,18583,18587,18593,18617,18637,18661,18671,18679,18691,18701,18713,18719,18731,18743,18749,18757,18773,18787,18793,18797,18803,18839,18859,18869,18899,18911,18913,18917,18919,18947,18959,18973,18979,19001,19009,19013,19031,19037,19051,19069,19073,19079,19081,19087,19121,19139,19141,19157,19163,19181,19183,19207,19211,19213,19219,19231,19237,19249,19259,19267,19273,19289,19301,19309,19319,19333,19373,19379,19381,19387,19391,19403,19417,19421,19423,19427,19429,19433,19441,19447,19457,19463,19469,19471,19477,19483,19489,19501,19507,19531,19541,19543,19553,19559,19571,19577,19583,19597,19603,19609,19661,19681,19687,19697,19699,19709,19717,19727,19739,19751,19753,19759,19763,19777,19793,19801,19813,19819,19841,19843,19853,19861,19867,19889,19891,19913,19919,19927,19937,19949,19961,19963,19973,19979,19991,19993,19997};
	}


	private ObjectId(int aTime, int aSession, int aSequence)
	{
		mTime = aTime;
		mSession = aSession;
		mSequence = aSequence;
	}


	public long time()
	{
		return mTime * 1000L;
	}


	public int sequence()
	{
		return mSequence;
	}


	public int session()
	{
		return mSession;
	}


	public static ObjectId randomId()
	{
		return new ObjectId((int)(System.currentTimeMillis() / 1000), Holder.SESSION, Holder.SEQUENCE.getAndIncrement());
	}


	public static ObjectId fromParts(int aTime, int aSession, int aSequence)
	{
		return new ObjectId(aTime, aSession, aSequence);
	}


	public static ObjectId fromByteArray(byte[] aBuffer)
	{
		if (aBuffer == null || aBuffer.length != LENGTH)
		{
			throw new IllegalArgumentException("data must be " + LENGTH + " bytes in length");
		}

		return new ObjectId(getInt32(aBuffer, 0), getInt32(aBuffer, 4), getInt32(aBuffer, 8));
	}


	public static ObjectId fromString(String aName)
	{
		return new ObjectId(parseUnsignedInt(aName.substring(0, 8), 16), parseUnsignedInt(aName.substring(8, 16), 16), parseUnsignedInt(aName.substring(16, 24), 16));
	}


	/**
	 * Key used to encrypt an ObjectId.
	 */
	public static class Key
	{
		final char[][] encBase62 = new char[18][62];
		final int[][] decBase62 = new int[18][128];
		final int[][] tweak = new int[2][3];
		final int[] chkmix = new int[3];
		final int[][] chktrans = new int[3][256];


		/**
		 *
		 * @param aSecretKey a 256-bit random value (eight ints) used to initialize the key.
		 */
		public Key(byte[] aSecretKey)
		{
			if (aSecretKey == null || aSecretKey.length != 32)
			{
				throw new IllegalArgumentException("Bad secret key, must be 8 ints.");
			}

			Random rnd = new Random(getInt64(aSecretKey, 0));
			for (int i = 0; i < 3; i++)
			{
				chktrans[i] = randomRange(4096, 13, rnd);
			}
			for (int k = 0; k < 18; k++)
			{
				int[] order = randomRange(62, 62, rnd);
				for (int i = 0; i < 62;)
				{
					int j = order[i];
					decBase62[k][Holder.BASE62[j]] = i;
					encBase62[k][i++] = Holder.BASE62[j];
				}
			}
			for (int i = 0; i < 3; i++)
			{
				chkmix[i] = Holder.PRIMES[rnd.nextInt(Holder.PRIMES.length)];
			}
			for (int i = 0, k = 8; i < 2; i++)
			{
				for (int j = 0; j < 3; j++, k += 4)
				{
					tweak[i][j] = rnd.nextBits(32) ^ getInt32(aSecretKey, k);
				}
			}
		}
	}


	private static class Random
	{
		private long seed;
		public Random(long aSeed)
		{
	        seed = aSeed;
		}
		protected int nextBits(int aBits)
		{
            seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
			return (int)(seed >>> (48 - aBits));
		}
		public int nextInt(int aBound)
		{
			int r = nextBits(31);
	        int m = aBound - 1;
            for (int u = r; u - (r = u % aBound) + m < 0; u = nextBits(31)) {}
			return r;
		}
	}


	private static int[] randomRange(int aLength, int aLimit, Random aRandom)
	{
		int[] order = new int[aLength];
		for (int i = 0; i < aLength; i++)
		{
			order[i] = i % aLimit;
		}
		for (int i = 0; i < aLength; i++)
		{
			int j = aRandom.nextInt(aLength);
			int t = order[j];
			order[j] = order[i];
			order[i] = t;
		}
		return order;
	}


	/**
	 * Return an encrypted String representation of this ObjectId
	 *
	 * @param aKey the Key used to encrypt the ObjectId
	 * @return the ObjectId as an encrypted 18 character Base62 encoded String. The encoded String also contains a checksum used for
	 * validation when decoding.
	 */
	public String toArmouredString(Key aKey)
	{
		int a = mTime;
		int b = mSession;
		int c = mSequence;

		a ^= aKey.tweak[0][0];
		b ^= aKey.tweak[0][1];
		c ^= aKey.tweak[0][2];
		for (int i = 0; i < 3; i++)
		{
			c -= Integer.rotateLeft(a, L1) ^ Integer.rotateRight(b, R1);
			b -= Integer.rotateLeft(c, L2) ^ Integer.rotateRight(a, R2);
			a -= Integer.rotateLeft(b, L3) ^ Integer.rotateRight(c, R3);
		}
		a ^= aKey.tweak[1][0];
		b ^= aKey.tweak[1][1];
		c ^= aKey.tweak[1][2];

		int chk = a * aKey.chkmix[0] + b * aKey.chkmix[1] + c * aKey.chkmix[2];
		int c0 = aKey.chktrans[0][(chk       ) & 0xfff];
		int c1 = aKey.chktrans[1][(chk >>> 11) & 0xfff];
		int c2 = aKey.chktrans[2][(chk >>> 20) & 0xfff];

		long A = (a & 0xffffffffL) + ((long)c0 << 32);
		long B = (b & 0xffffffffL) + ((long)c1 << 32);
		long C = (c & 0xffffffffL) + ((long)c2 << 32);

		char[] buf = new char[18];
		encodeBase62(aKey, buf, A, 0);
		encodeBase62(aKey, buf, B, 1);
		encodeBase62(aKey, buf, C, 2);
		return new String(buf);
	}


	/**
	 * Return an ObjectId from an encrypted String representation
	 *
	 * @param aKey the Key used to encrypt the ObjectId
	 * @param aName the encrypted String representation of an ObjectId
	 * @return the decoded ObjectId or null if the String provided is invalid
	 */
	public static ObjectId fromArmouredString(Key aKey, String aName)
	{
		long A = decodeBase62(aKey, aName, 0);
		long B = decodeBase62(aKey, aName, 1);
		long C = decodeBase62(aKey, aName, 2);

		int a = (int)A;
		int b = (int)B;
		int c = (int)C;

		int chk = a * aKey.chkmix[0] + b * aKey.chkmix[1] + c * aKey.chkmix[2];
		int c0 = aKey.chktrans[0][(chk       ) & 0xfff];
		int c1 = aKey.chktrans[1][(chk >>> 11) & 0xfff];
		int c2 = aKey.chktrans[2][(chk >>> 20) & 0xfff];
		if ((A >> 32) != c0 || (B >> 32) != c1 || (C >> 32) != c2)
		{
			return null;
		}

		a ^= aKey.tweak[1][0];
		b ^= aKey.tweak[1][1];
		c ^= aKey.tweak[1][2];
		for (int i = 0; i < 3; i++)
		{
			a += Integer.rotateLeft(b, L3) ^ Integer.rotateRight(c, R3);
			b += Integer.rotateLeft(c, L2) ^ Integer.rotateRight(a, R2);
			c += Integer.rotateLeft(a, L1) ^ Integer.rotateRight(b, R1);
		}
		a ^= aKey.tweak[0][0];
		b ^= aKey.tweak[0][1];
		c ^= aKey.tweak[0][2];

		return new ObjectId(a, b, c);
	}


	private void encodeBase62(Key aKey, char[] aOutput, long aValue, int aIndex)
	{
		for (int start = 6 * aIndex, j = start + 6; --j >= start;)
		{
			int symbol = (int)(aValue % 62);
			aOutput[j] = aKey.encBase62[j][symbol];
			aValue /= 62;
		}
	}


	private static long decodeBase62(Key aKey, String aName, int aIndex)
	{
		long value = 0;
		for (int i = 6 * aIndex, end = i + 6; i < end; i++)
		{
			int symbol = aKey.decBase62[i][aName.charAt(i) & 0x7f];
			value *= 62;
			value += symbol;
		}
		return value;
	}


	public byte[] toByteArray()
	{
		byte[] buffer = new byte[LENGTH];
		putInt32(buffer, 0, mTime);
		putInt32(buffer, 4, mSession);
		putInt32(buffer, 8, mSequence);
		return buffer;
	}


	@Override
	public String toString()
	{
		return String.format("%08x%08x%08x", mTime, mSession, mSequence);
	}


	@Override
	public int hashCode()
	{
		return mTime ^ mSession ^ mSequence;
	}


	@Override
	public boolean equals(Object aOther)
	{
		if (aOther instanceof ObjectId v)
		{
			return (mTime == v.mTime && mSession == v.mSession && mSequence == v.mSequence);
		}
		return false;
	}


	@Override
	public int compareTo(ObjectId aOther)
	{
		return mTime < aOther.mTime ? -1
			: mTime > aOther.mTime ? 1
				: mSession < aOther.mSession ? -1
					: mSession > aOther.mSession ? 1
						: mSequence < aOther.mSequence ? -1
							: mSequence > aOther.mSequence ? 1 : 0;
	}


	private static int getInt32(byte[] aBuffer, int aPosition)
	{
		return ((aBuffer[aPosition] & 0xFF) << 24)
			+ ((aBuffer[aPosition + 1] & 0xFF) << 16)
			+ ((aBuffer[aPosition + 2] & 0xFF) << 8)
			+ ((aBuffer[aPosition + 3] & 0xFF));
	}


	private static long getInt64(byte[] aBuffer, int aPosition)
	{
		return ((0xffffffffL & getInt32(aBuffer, aPosition)) << 32) + (0xffffffffL & getInt32(aBuffer, aPosition + 4));
	}


	private static void putInt32(byte[] aBuffer, int aPosition, int aValue)
	{
		aBuffer[aPosition] = (byte)(aValue >>> 24);
		aBuffer[aPosition + 1] = (byte)(aValue >> 16);
		aBuffer[aPosition + 2] = (byte)(aValue >> 8);
		aBuffer[aPosition + 3] = (byte)(aValue);
	}


	public static int compare(byte[] aBuffer1, int aOffset1, byte[] aBuffer2, int aOffset2)
	{
		return getInt32(aBuffer1, aOffset1) < getInt32(aBuffer2, aOffset2) ? -1
			: getInt32(aBuffer1, aOffset1) > getInt32(aBuffer2, aOffset2) ? 1
			: getInt32(aBuffer1, aOffset1 + 4) < getInt32(aBuffer2, aOffset2 + 4) ? -1
			: getInt32(aBuffer1, aOffset1 + 4) > getInt32(aBuffer2, aOffset2 + 4) ? 1
			: getInt32(aBuffer1, aOffset1 + 8) < getInt32(aBuffer2, aOffset2 + 8) ? -1
			: getInt32(aBuffer1, aOffset1 + 8) > getInt32(aBuffer2, aOffset2 + 8) ? 1
			: 0;
	}


//	public static void main(String... args)
//	{
//		try
//		{
//			byte[] kd = new byte[32];
//			Key key = new Key(kd);
//
//			HashMap<Character, Integer> cc = new HashMap<>();
//			HashMap<Integer, HashMap<Character, Integer>> ci = new HashMap<>();
//
//			for (int i = 0; i < 10_000_000; i++)
////			for (int i = 0; i < 10; i++)
//			{
//				ObjectId in = ObjectId.randomId();
//				String encoded = in.toArmouredString(key);
//				ObjectId out = ObjectId.fromArmouredString(key, encoded);
////				System.out.printf("%s  %s  %s  %s%n", encoded, in, out == null ? "-".repeat(24) : out, in.equals(out));
//				if (!in.equals(out))
//				{
//					throw new IllegalStateException();
//				}
//
//				for (int j = 0; j < encoded.length(); j++)
//				{
//					cc.put(encoded.charAt(j), cc.getOrDefault(encoded.charAt(j), 0) + 1);
//					HashMap<Character, Integer> m = ci.computeIfAbsent(j, k -> new HashMap<>());
//					m.put(encoded.charAt(j), m.getOrDefault(encoded.charAt(j), 0) + 1);
//					ci.put(j, m);
//				}
//			}
//
//			System.out.println(cc);
//			for (Entry e : ci.entrySet())
//			{
//				System.out.println(e.getKey() + " " + e.getValue());
//			}
//
//			System.out.println("-".repeat(100));
//			long t = System.currentTimeMillis();
//			for (int i = 0; i < 10_000_000; i++)
//			{
//				ObjectId.fromArmouredString(key, ObjectId.randomId().toArmouredString(key));
//			}
//			System.out.println(System.currentTimeMillis() - t);
//		}
//		catch (Throwable e)
//		{
//			e.printStackTrace(System.out);
//		}
//	}
}
