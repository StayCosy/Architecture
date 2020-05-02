package com.alisoft.xplatform.asf.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.alisoft.xplatform.asf.cache.ICacheManager;
import com.alisoft.xplatform.asf.cache.IMemcachedCache;
import com.alisoft.xplatform.asf.cache.memcached.CacheUtil;
import com.alisoft.xplatform.asf.cache.memcached.MemcachedCacheManager;

public class StressCacheTest {

	static ICacheManager<IMemcachedCache> manager;
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		manager = CacheUtil.getCacheManager(IMemcachedCache.class,
			MemcachedCacheManager.class.getName());
		manager.start();
		System.out.println("test start");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
		manager.stop();
		System.out.println("test end");
		Thread.sleep(200000000);
	}
	
	@Test
	public void test()
	{

		try
		{		
			long totaltime = System.currentTimeMillis();
			
			int threadcount = 100;
			int count = 1000;
			List<Long> result = Collections.synchronizedList(new ArrayList<Long>());
			
			CountDownLatch startSignal = new CountDownLatch(1);
		    CountDownLatch doneSignal = new CountDownLatch(threadcount);	
			
			for(int i = 0 ; i < threadcount ; i++)
			{
				new Thread(new Task(String.valueOf(i),result,count,startSignal,doneSignal)).start();
			}
			
			startSignal.countDown();
			
			doneSignal.await();
			
			if (result.size() == threadcount)
			{
				long total = 0;
				for(long l : result)
				{
					total += l;
				}
				
				System.out.println(new StringBuffer().append("cache test consume: ")
						.append(total).append(", average boundle consume: ").append(total/(long)result.size())
						.append(", average per request :").append(total/(long)result.size()/(long)count));
			}
			
			totaltime = System.currentTimeMillis() - totaltime;
			
			System.out.println("total consume: " + totaltime);
			
			Thread.sleep(100000000);
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			Assert.assertTrue(false);
		}

	}
	
	class Task implements java.lang.Runnable
	{

		String name;
		List<Long> result;
		int count;
		CountDownLatch start;
		CountDownLatch done;
		
		public Task(String n,List<Long> r,int c,CountDownLatch start,CountDownLatch done)
		{
			name = n;
			count = c;
			result = r;
			this.start = start;
			this.done = done;
		}
		
		public void run() 
		{
			
			IMemcachedCache cache = manager.getCache("mclient0");
			
			try
			{
				start.await();
			}
			catch(Exception ex)
			{
				ex.printStackTrace();
			}
			
			long time = System.currentTimeMillis();
			
			for(int i= 0 ; i < count; i++)
			{
				cache.put(String.valueOf(i), i);

				org.junit.Assert.assertEquals(cache.get(String.valueOf(i)), i);
				
				String nodename = new StringBuilder("node").append(name).append(i).toString();
				
				Node node = new Node();
				node.setName(nodename);
				
				cache.put(node.getName(), node);
				org.junit.Assert.assertEquals(((Node)cache.get(node.getName())).getName(),nodename);
				
			}
			
			time = System.currentTimeMillis() - time;
			
			result.add(time);
			done.countDown();
		}
		
	}

}
