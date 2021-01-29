/**
 * 
 */
package com.alisoft.xplatform.asf.cache;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.alisoft.xplatform.asf.cache.memcached.CacheUtil;
import com.alisoft.xplatform.asf.cache.memcached.MemcachedCacheManager;

/**
 * @author wenchu.cenwc
 *
 */
public class MemcachedClusterTest
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
		manager.setConfigFile("memcached_cluster.xml");
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
	
	@Test
	public void testActiveMode()
	{
		try
		{
			IMemcachedCache cache1 = manager.getCache("mclient1");
			IMemcachedCache cache2 = manager.getCache("mclient2");
			
			cache1.put("key1","value1");
			cache1.put("key2","value2");
			cache1.put("key3","value3");
			cache1.put("key4","value4");
			cache1.put("key5","value5");
			cache1.put("key6","value6");
			
			//模拟mclient1失效(结束服务端)，有出错日志在控制台打印
			Assert.assertEquals(cache1.get("key1"),"value1");
			Assert.assertEquals(cache1.get("key2"),"value2");
			Assert.assertEquals(cache1.get("key3"),"value3");
			Assert.assertEquals(cache1.get("key4"),"value4");
			Assert.assertEquals(cache1.get("key5"),"value5");
			Assert.assertEquals(cache1.get("key6"),"value6");
			
			Assert.assertEquals(cache2.get("key1"),"value1");
			Assert.assertEquals(cache2.get("key2"),"value2");
			Assert.assertEquals(cache2.get("key3"),"value3");
			Assert.assertEquals(cache2.get("key4"),"value4");
			Assert.assertEquals(cache2.get("key5"),"value5");
			Assert.assertEquals(cache2.get("key6"),"value6");
			
			//恢复mclient1，无出错日志在控制台打印
			Assert.assertEquals(cache1.get("key1"),"value1");
			Assert.assertEquals(cache1.get("key2"),"value2");
			Assert.assertEquals(cache1.get("key3"),"value3");
			Assert.assertEquals(cache1.get("key4"),"value4");
			Assert.assertEquals(cache1.get("key5"),"value5");
			Assert.assertEquals(cache1.get("key6"),"value6");
			
			Assert.assertEquals(cache2.get("key1"),"value1");
			Assert.assertEquals(cache2.get("key2"),"value2");
			Assert.assertEquals(cache2.get("key3"),"value3");
			Assert.assertEquals(cache2.get("key4"),"value4");
			Assert.assertEquals(cache2.get("key5"),"value5");
			Assert.assertEquals(cache2.get("key6"),"value6");
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	
	@Test
	@Ignore
	public  void testStandByMode()
	{
		IMemcachedCache cache1 = manager.getCache("mclient3");
		IMemcachedCache cache2 = manager.getCache("mclient4");
		
		cache1.put("key1","value1");
		cache1.put("key2","value2");
		cache1.put("key3","value3");
		cache1.put("key4","value4");
		cache1.put("key5","value5");
		cache1.put("key6","value6");
		
		//模拟mclient1失效(结束服务端)，有出错日志在控制台打印
		Assert.assertEquals(cache1.get("key1"),"value1");
		Assert.assertEquals(cache1.get("key2"),"value2");
		Assert.assertEquals(cache1.get("key3"),"value3");
		Assert.assertEquals(cache1.get("key4"),"value4");
		Assert.assertEquals(cache1.get("key5"),"value5");
		Assert.assertEquals(cache1.get("key6"),"value6");
		
		Assert.assertEquals(cache2.get("key1"),"value1");
		Assert.assertEquals(cache2.get("key2"),"value2");
		Assert.assertEquals(cache2.get("key3"),"value3");
		Assert.assertEquals(cache2.get("key4"),"value4");
		Assert.assertEquals(cache2.get("key5"),"value5");
		Assert.assertEquals(cache2.get("key6"),"value6");
		
		//恢复mclient1，无出错日志在控制台打印
		Assert.assertNull(cache1.get("key1"));
		Assert.assertEquals(cache1.get("key2"),"value2");
		Assert.assertNull(cache1.get("key3"));
		Assert.assertEquals(cache1.get("key4"),"value4");
		Assert.assertNull(cache1.get("key5"));
		Assert.assertEquals(cache1.get("key6"),"value6");
		
		Assert.assertNull(cache2.get("key1"));
		Assert.assertEquals(cache2.get("key2"),"value2");
		Assert.assertNull(cache2.get("key3"));
		Assert.assertEquals(cache2.get("key4"),"value4");
		Assert.assertNull(cache2.get("key5"));
		Assert.assertEquals(cache2.get("key6"),"value6");
	}
	
	@Test
	@Ignore
	public void testClusterCopy()
	{
		try
		{
			IMemcachedCache cache = manager.getCache("mclient");
			IMemcachedCache cache1 = manager.getCache("mclient1");
			IMemcachedCache cache2 = manager.getCache("mclient2");
					
			cache.put("key1","value1");
			cache.put("key2","value2");
			cache.put("key3","value3");
			cache.put("key4","value4");
			cache.put("key5","value5");
			cache.put("key6","value6");
			
			cache1.remove("key1");
			cache1.remove("key2");
			cache1.remove("key3");
			cache1.remove("key4");
			cache1.remove("key5");
			cache1.remove("key6");
			
			cache2.remove("key1");
			cache2.remove("key2");
			cache2.remove("key3");
			cache2.remove("key4");
			cache2.remove("key5");
			cache2.remove("key6");
			
			manager.clusterCopy("mclient", "cluster1");
			
			Assert.assertEquals(cache1.get("key1"),"value1");
			Assert.assertEquals(cache1.get("key2"),"value2");
			Assert.assertEquals(cache1.get("key3"),"value3");
			Assert.assertEquals(cache1.get("key4"),"value4");
			Assert.assertEquals(cache1.get("key5"),"value5");
			Assert.assertEquals(cache1.get("key6"),"value6");
			
			Assert.assertEquals(cache2.get("key1"),"value1");
			Assert.assertEquals(cache2.get("key2"),"value2");
			Assert.assertEquals(cache2.get("key3"),"value3");
			Assert.assertEquals(cache2.get("key4"),"value4");
			Assert.assertEquals(cache2.get("key5"),"value5");
			Assert.assertEquals(cache2.get("key6"),"value6");
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	
	
	@Test
	public  void testReload()
	{
		try
		{
			IMemcachedCache cache = manager.getCache("mclient");
			IMemcachedCache cache1 = manager.getCache("mclient1");
			IMemcachedCache cache2 = manager.getCache("mclient2");
			IMemcachedCache cache5 = manager.getCache("mclient5");
			IMemcachedCache cache6 = manager.getCache("mclient6");
			
			
			Assert.assertNull(cache5);
			Assert.assertNull(cache6);
			
			
			cache.clear();
			Thread.sleep(1000);
			cache1.clear();
			Thread.sleep(1000);
			cache2.clear();
			Thread.sleep(1000);
			
			cache.put("key1", "1");	
			cache1.put("key2", "2");
			
			
			Assert.assertNull(cache.get("key2"));
			Assert.assertNull(cache1.get("key1"));
			
			Thread.sleep(2000);
			
			
			//manager.reload("http://10.2.226.41/sip/memcached_cluster2.xml");
			manager.reload("memcached_cluster2.xml");
			
			Thread.sleep(2000);
			
			cache1 = manager.getCache("mclient1");
			cache2 = manager.getCache("mclient2");
			cache5 = manager.getCache("mclient5");
			cache6 = manager.getCache("mclient6");
			
			Assert.assertNull(cache1);
			Assert.assertNull(cache2);
			Assert.assertNotNull(cache5);
			Assert.assertNotNull(cache6);
			
			cache5.clear();
			Thread.sleep(1000);
			cache6.clear();
			Thread.sleep(3000);
			
			manager.reload("memcached_cluster3.xml");
			
			Thread.sleep(2000);
			
			cache = manager.getCache("mclient");
			cache1 = manager.getCache("mclient1");
			cache2 = manager.getCache("mclient2");
			cache5 = manager.getCache("mclient5");
			cache6 = manager.getCache("mclient6");

			
			Assert.assertEquals(cache.get("key2"),"2");
			Assert.assertEquals(cache1.get("key1"),"1");
			
			Assert.assertEquals(cache2.get("key2"),"2");
			Assert.assertEquals(cache5.get("key2"),"2");
			Assert.assertNull(cache2.get("key1"));
			Assert.assertNull(cache5.get("key1"));
			Assert.assertNull(cache6.get("key1"));
			Assert.assertNull(cache6.get("key2"));
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}

	}
	
}
