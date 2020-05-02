/**
 * 
 */
package com.alisoft.xplatform.asf.cache;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import com.alisoft.xplatform.asf.cache.memcached.CacheUtil;
import com.alisoft.xplatform.asf.cache.memcached.MemcacheStats;
import com.alisoft.xplatform.asf.cache.memcached.MemcacheStatsSlab;
import com.alisoft.xplatform.asf.cache.memcached.MemcachedCacheManager;
import com.alisoft.xplatform.asf.cache.memcached.MemcachedResponse;
import com.alisoft.xplatform.asf.cache.memcached.MemcacheStatsSlab.Slab;

/**
 * @author wenchu.cenwc
 *
 */
public class IMemcachedCacheTest
{
	
	static ICacheManager<IMemcachedCache> manager;
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		manager = CacheUtil.getCacheManager(IMemcachedCache.class,
			MemcachedCacheManager.class.getName());
		manager.setConfigFile("memcached1.xml");
		manager.setResponseStatInterval(5*1000);
		manager.start();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
		manager.stop();
	}
	
	/**
	 * Test method for {@link com.alisoft.xplatform.asf.cache.ICache#get(java.lang.Object)}.
	 */
	@Test
	public void testGet()
	{
		try
		{
			IMemcachedCache cache = manager.getCache("mclient0");
			
			cache.remove("key1");
			cache.remove("key2你好");
			
			cache.put("key1", "1");
			cache.put("key2你好", "你好123");
			
			Assert.assertEquals(cache.get("key1"),"1");
			Assert.assertEquals(cache.get("key2你好"),"你好123");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
		
	}
	

	
	/**
	 * Test method for {@link com.alisoft.xplatform.asf.cache.ICache#remove(java.lang.Object)}.
	 */
	@Test
	public void testRemove()
	{
		try
		{
			IMemcachedCache cache = manager.getCache("mclient0");
			
			cache.remove("key1");	
			cache.put("key1", "value1");
			
			Assert.assertEquals(cache.get("key1"),"value1");
			
			cache.remove("key1");
			
			Assert.assertNull(cache.get("key1"));
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}	
	
	@Test
	public void testKeysetValues()
	{
		try
		{
			IMemcachedCache cache = manager.getCache("mclient0");
			
			cache.clear();
			Thread.sleep(2000);
			
			cache.put("key1", "value1");
			cache.put("key2", "value2");
			
			Set<String> keys = cache.keySet(false);
			
			String[] c = new String[2];
			
			keys.toArray(c);
			
			if ((c[0].equals("key1")&&c[1].equals("key2"))
					||(c[0].equals("key2")&&c[1].equals("key1")))
				Assert.assertTrue(true);
			else
				Assert.assertTrue(false);
			
			cache.values().toArray(c);
			
			if ((c[0].equals("value1")&&c[1].equals("value2"))
					||(c[0].equals("value1")&&c[1].equals("value2")))
				Assert.assertTrue(true);
			else
				Assert.assertTrue(false);
			
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	
	
	@Test
	public void testcontainsKey()
	{
		try
		{
			IMemcachedCache cache = manager.getCache("mclient0");
			
			cache.remove("key1");
			cache.put("key1", "value1");
			
			Assert.assertTrue(cache.containsKey("key1"));

		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	
	/**
	 * Test method for {@link com.alisoft.xplatform.asf.cache.IMemcachedCache#get(java.lang.String, int)}.
	 */
	@Test
	public void testGetStringInt()
	{
		try
		{
			IMemcachedCache cache1 = manager.getCache("mclient1");
			IMemcachedCache cache2 = manager.getCache("mclient2");
			
			cache1.remove("key1");
			cache1.remove("key2");
			cache2.remove("key1");
			cache2.remove("key2");
			
			cache1.put("key1", "value1");
			cache1.put("key2", "value2");
			
			Assert.assertEquals(cache1.get("key1",30),"value1");
			Assert.assertEquals(cache1.get("key2"),"value2");
			
			Assert.assertEquals(cache2.get("key1",30),"value1");
			Assert.assertEquals(cache2.get("key2"),"value2");
			
			cache1.put("key1", "value3");
			
			Assert.assertEquals(cache1.get("key1",30),"value3");
			Assert.assertEquals(cache2.get("key1",30),"value1");
			Assert.assertEquals(cache2.get("key1"),"value3");
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
		
	}
	
	
	@Test
	public void testgetMulti()
	{
		try
		{
			IMemcachedCache cache = manager.getCache("mclient0");
			
			cache.remove("key1");
			cache.remove("key2");
			
			cache.put("key1", "value1");
			cache.put("key2", "value2");
			
			Map<String,Object> result = cache.getMulti(new String[]{"key1","key2"});
			
			Assert.assertEquals(result.get("key1"), "value1"); 
			Assert.assertEquals(result.get("key2"), "value2");
			
			
			Object[] values = cache.getMultiArray(new String[]{"key1","key2"});
			
			if ((values[0].equals("value1") && values[1].equals("value2"))
					||(values[0].equals("value1") && values[1].equals("value2")))
				Assert.assertTrue(true);
			else
				Assert.assertTrue(false);
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
		
		
	}
	
	/**
	 * Test method for {@link com.alisoft.xplatform.asf.cache.IMemcachedCache#incr(java.lang.String, long)}.
	 */
	@Test
	public void testIncr()
	{
		try
		{
			IMemcachedCache cache = manager.getCache("mclient0");
			
			cache.remove("counter");
			
			cache.storeCounter("counter", 20);
			
			Assert.assertEquals(cache.incr("counter", 11),31);
			
			cache.remove("counter");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	
	/**
	 * Test method for {@link com.alisoft.xplatform.asf.cache.IMemcachedCache#decr(java.lang.String, long)}.
	 */
	@Test
	public void testDecr()
	{
		try
		{
			IMemcachedCache cache = manager.getCache("mclient0");
			
			cache.remove("counter");
			
			cache.storeCounter("counter", 20);
			
			Assert.assertEquals(cache.decr("counter", 11),9);
			
			cache.remove("counter");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	
	/**
	 * Test method for {@link com.alisoft.xplatform.asf.cache.IMemcachedCache#addOrIncr(java.lang.String, long)}.
	 */
	@Test
	public void testAddOrIncr()
	{
		try
		{
			IMemcachedCache cache = manager.getCache("mclient0");
			
			cache.remove("counter");
			
			Assert.assertEquals(cache.addOrIncr("counter", 20),20);
			Assert.assertEquals(cache.addOrIncr("counter", 20),40);
			
			cache.remove("counter");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}

	/**
	 * Test method for {@link com.alisoft.xplatform.asf.cache.IMemcachedCache#addOrDecr(java.lang.String, long)}.
	 */
	@Test
	public void testAddOrDecr()
	{
		try
		{
			IMemcachedCache cache = manager.getCache("mclient0");
			
			cache.remove("counter");
			
			Assert.assertEquals(cache.addOrDecr("counter", 20),20);
			Assert.assertEquals(cache.addOrDecr("counter", 10),10);
			
			cache.remove("counter");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	
	/**
	 * Test method for {@link com.alisoft.xplatform.asf.cache.IMemcachedCache#statsSlabs()}.
	 */
	@Test
	public void testStatsSlabs()
	{
		IMemcachedCache cache = manager.getCache("mclient0");
		
		cache.remove("key1");
		cache.remove("key2");
		cache.remove("key3");
		cache.remove("key4");
		cache.remove("key5");
		cache.remove("key6");
		
		
		cache.put("key1", "value1");
		cache.put("key2", "value2");
		cache.put("key3", "value3");
		cache.put("key4", "value4");
		cache.put("key5", "value5");
		cache.put("key6", "value6");
		
		
		MemcacheStatsSlab[] result = cache.statsSlabs();
		
		for(int i = 0 ; i < result.length; i++)
		{
			MemcacheStatsSlab node = result[i];
			
			System.out.println("--------Stat Slabs---------------------");
			System.out.println(new StringBuffer()
				.append("key:").append(node.getServerHost()).toString());
			
			Map<String,Slab> slabs = node.getSlabs();
			
			Iterator<String> n = slabs.keySet().iterator();
			
			while(n.hasNext())
			{
				System.out.println(slabs.get(n.next()));
			}
			 
			System.out.println("-----------------------------");
		}
		
		cache.remove("key1");
		cache.remove("key2");
		cache.remove("key3");
		cache.remove("key4");
		cache.remove("key5");
		cache.remove("key6");
	}

	/**
	 * Test method for {@link com.alisoft.xplatform.asf.cache.IMemcachedCache#stats()}.
	 */
	@Test
	public void testStats()
	{
		IMemcachedCache cache = manager.getCache("mclient0");
		
		cache.remove("key1");
		cache.remove("key2");
		cache.remove("key3");
		cache.remove("key4");
		cache.remove("key5");
		cache.remove("key6");
		
		
		cache.put("key1", "value1");
		cache.put("key2", "value2");
		cache.put("key3", "value3");
		cache.put("key4", "value4");
		cache.put("key5", "value5");
		cache.put("key6", "value6");
		
		
		MemcacheStats[] result = cache.stats();
		
		for(int i = 0 ; i < result.length; i++)
		{
			MemcacheStats node = result[i];
			
			System.out.println(new StringBuffer()
				.append("key:").append(node.getServerHost())
				.append(",value:").append(node.getStatInfo()));
		}
		
		cache.remove("key1");
		cache.remove("key2");
		cache.remove("key3");
		cache.remove("key4");
		cache.remove("key5");
		cache.remove("key6");
	}
	
	@Ignore
	@Test
	public void testStatCacheResponse()
	{
		IMemcachedCache cache = manager.getCache("mclient0");
		cache.setStatisticsInterval(30);
		SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		
		try
		{
			Thread.sleep(5*30*1000);
			
			MemcachedResponse response = cache.statCacheResponse();
			
			Assert.assertEquals(response.getCacheName(), "mclient0");
			
			String cacheName = response.getCacheName();
			String startTime = formater.format(response.getStartTime());
			String endTime = formater.format(response.getEndTime());
			
			System.out.println(cacheName);
			System.out.println(startTime);
			System.out.println(endTime);
			
			for(long c :response.getResponses())
				System.out.println(c);
			
			
		} catch (InterruptedException e)
		{
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		
	}
	
	@Test
	public void testAdd()
	{
		try
		{
			IMemcachedCache cache = manager.getCache("mclient0");
			
			cache.remove("key1");
			
			Assert.assertTrue(cache.add("key1", "value1"));
			Assert.assertFalse(cache.add("key1", "value1"));
	
			Assert.assertEquals(cache.get("key1"),"value1");
			
			cache.remove("key1");
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	
	@Test
	public void testReplace()
	{
		try
		{
			IMemcachedCache cache = manager.getCache("mclient0");
			
			cache.remove("key1");
					
			Assert.assertFalse(cache.replace("key1", "value1"));
			cache.put("key1", "value1");			
			Assert.assertTrue(cache.replace("key1", "value2"));	
			Assert.assertEquals(cache.get("key1"),"value2");
			
			cache.remove("key1");
	
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}	
	
	/**
	 * Test method for {@link com.alisoft.xplatform.asf.cache.ICache#clear()}.
	 */
	@Test
	public void testClear()
	{
		try
		{
			IMemcachedCache cache = manager.getCache("mclient0");
			
			cache.remove("key1");
			cache.put("key1", "value1");
			
			Assert.assertEquals(cache.get("key1"),"value1");
			
			cache.clear();
			
			Assert.assertNull(cache.get("key1"));
			
			Thread.sleep(2000);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}		
	
	
	@Test
	public void testAnsyPut()
	{
		try
		{
			IMemcachedCache cache = manager.getCache("mclient0");
			
			cache.remove("key1");
			cache.asynPut("key1", "value1");
			
			Thread.sleep(10);
			
			Assert.assertEquals(cache.get("key1"),"value1");
			
			IMemcachedCache cache1 = manager.getCache("mclient1");
			IMemcachedCache cache2 = manager.getCache("mclient2");
			
			cache1.remove("key1");
			cache2.remove("key1");
			cache1.asynPut("key1", "value1");
			
			Thread.sleep(10);
			
			Assert.assertEquals(cache1.get("key1"),"value1");
			Assert.assertEquals(cache2.get("key1"),"value1");
			
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	
	@Test
	public void testAnsyStoreCounter()
	{
		try
		{
			IMemcachedCache cache = manager.getCache("mclient0");
			
			cache.remove("key1");
			cache.asynStoreCounter("key1", 100);
			
			Thread.sleep(10);
			
			Assert.assertEquals(cache.getCounter("key1"),100);
			
			IMemcachedCache cache1 = manager.getCache("mclient1");
			IMemcachedCache cache2 = manager.getCache("mclient2");
			
			cache1.remove("key1");
			cache2.remove("key1");
			cache1.asynStoreCounter("key1", 1000);
			
			Thread.sleep(10);
			
			Assert.assertEquals(cache1.getCounter("key1"),1000);
			Assert.assertEquals(cache2.getCounter("key1"),1000);
			
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	
	@Test
	public void testAnsyAddOrIncr()
	{
		try
		{
			IMemcachedCache cache = manager.getCache("mclient0");
			
			cache.remove("key1");
			cache.asynAddOrIncr("key1", 100);
			
			Thread.sleep(10);
			Assert.assertEquals(cache.getCounter("key1"),100);
			
			IMemcachedCache cache1 = manager.getCache("mclient1");
			IMemcachedCache cache2 = manager.getCache("mclient2");
			
			cache1.remove("key1");
			cache2.remove("key1");
			cache1.asynAddOrIncr("key1", 1000);
			
			Thread.sleep(10);
			
			Assert.assertEquals(cache1.getCounter("key1"),1000);
			Assert.assertEquals(cache2.getCounter("key1"),1000);
			
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	
	@Test
	public void testAnsyAddOrDecr()
	{
		try
		{
			IMemcachedCache cache = manager.getCache("mclient0");
			
			cache.remove("key1");
			cache.storeCounter("key1", 100);
			cache.asynAddOrDecr("key1", 90);
			
			Thread.sleep(10);
			Assert.assertEquals(cache.getCounter("key1"),10);
			
			IMemcachedCache cache1 = manager.getCache("mclient1");
			IMemcachedCache cache2 = manager.getCache("mclient2");
			
			cache1.remove("key1");
			cache2.remove("key1");
			cache1.storeCounter("key1", 100);
			cache1.asynAddOrDecr("key1", 10);
			
			Thread.sleep(10);
			
			Assert.assertEquals(cache1.getCounter("key1"),90);
			Assert.assertEquals(cache2.getCounter("key1"),90);
			
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	
	@Test
	public void testAnsyIncr()
	{
		try
		{
			IMemcachedCache cache = manager.getCache("mclient0");
			
			cache.remove("key1");
			cache.storeCounter("key1", 100);
			cache.asynIncr("key1", 100);
			
			Thread.sleep(10);
			Assert.assertEquals(cache.getCounter("key1"),200);
			
			IMemcachedCache cache1 = manager.getCache("mclient1");
			IMemcachedCache cache2 = manager.getCache("mclient2");
			
			cache1.remove("key1");
			cache2.remove("key1");
			cache1.storeCounter("key1", 100);
			cache1.asynIncr("key1", 1000);
			
			Thread.sleep(10);
			
			Assert.assertEquals(cache1.getCounter("key1"),1100);
			Assert.assertEquals(cache2.getCounter("key1"),1100);
			
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	
	@Test
	public void testAnsyDecr()
	{
		try
		{
			IMemcachedCache cache = manager.getCache("mclient0");
			
			cache.remove("key1");
			cache.storeCounter("key1", 100);
			cache.asynDecr("key1", 90);
			
			Thread.sleep(10);
			Assert.assertEquals(cache.getCounter("key1"),10);
			
			IMemcachedCache cache1 = manager.getCache("mclient1");
			IMemcachedCache cache2 = manager.getCache("mclient2");
			
			cache1.remove("key1");
			cache2.remove("key1");
			cache1.storeCounter("key1", 100);
			cache1.asynDecr("key1", 10);
			
			Thread.sleep(10);
			
			Assert.assertEquals(cache1.getCounter("key1"),90);
			Assert.assertEquals(cache2.getCounter("key1"),90);
			
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	
	/**
	 * Test method for {@link com.alisoft.xplatform.asf.cache.ICache#put(java.lang.Object, java.lang.Object, java.util.Date)}.
	 */
	@Test
	public void testPutKVDate()
	{
		try
		{
			
			IMemcachedCache cache = manager.getCache("mclient0");
			
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.SECOND, 30);
			
			cache.remove("key1");
			cache.put("key1", "value1",calendar.getTime());
			
			Assert.assertEquals(cache.get("key1"),"value1");
			
			Thread.sleep(30 * 1000 + 10);
			Assert.assertNull(cache.get("key1"));
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}	
	
}
