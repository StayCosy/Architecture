package com.alisoft.xplatform.asf.cache.memcached;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alisoft.xplatform.asf.cache.memcached.client.MemCachedClient;

/**
 * 为了提高性能，对于Cluster内的数据作异步同步
 * @author wenchu.cenwc
 *
 */
public class ClusterProcessor extends java.lang.Thread
{
	private static final Log Logger = LogFactory.getLog(ClusterProcessor.class);
	
	LinkedBlockingQueue<Object[]> queue;
	MemCachedClientHelper helper;
	
	boolean isRunning = true;
	
	/**
	 * 执行异步同步Cluster的线程池
	 */
	private ExecutorService clusterProcessorPool;
	
	public ClusterProcessor(LinkedBlockingQueue<Object[]> queue,MemCachedClientHelper helper)
	{
		this.queue = queue;
		this.helper = helper;
		clusterProcessorPool = Executors.newFixedThreadPool(30);
	}
	
	
	public void run()
	{
		while(isRunning)
		{
			process();
		}
	}
	
	public void stopProcess()
	{
		isRunning = false;
		
		try
		{
			if (clusterProcessorPool != null)
				clusterProcessorPool.shutdown();
			
			clusterProcessorPool = null;
			
			interrupt();
		}
		catch(Exception ex)
		{
			Logger.error(ex);
		}
	}
	
	void process()
	{
		Object[] commands = null;
		
		try
		{
			commands = queue.take();
			
			if (commands != null && clusterProcessorPool != null)
				clusterProcessorPool.execute(new ClusterUpdateJob(commands));
		}
		catch(InterruptedException e)
		{
			Logger.warn("cluster Process stoped!");
		}
		catch(Exception ex)
		{
			Logger.error("cluster Process error!",ex);
		}
	}
	
	class ClusterUpdateJob implements java.lang.Runnable
	{
		Object[] commands;
		
		public ClusterUpdateJob(Object[] commands)
		{
			this.commands = commands;
		}
		
		public void run()
		{
			if (commands != null)
			{
			
				if (ansyCommandProcess(commands) && !helper.hasCluster())
					return;
				
				commandProcess(commands);
				
			}
		}
		
	}
	
	/**
	 * 异步存储请求处理
	 * @param commands
	 * @return
	 */
	public boolean ansyCommandProcess(Object[] commands)
	{
		boolean result = false;
		
		MemCachedClient innerCache = helper.getInnerCacheClient();
		
		//对Ansy请求的处理
		switch ((CacheCommand)commands[0])
		{
			case ANSYPUT:
				innerCache.set(commands[1].toString(),commands[2]);
				result = true;
				break;
				
			case ANSYSTORECOUNTER:
				innerCache.storeCounter(commands[1].toString(),(Long)commands[2]);
				result = true;
				break;
				
			case ANSYADDORDECR:
				innerCache.addOrDecr(commands[1].toString(),(Long)commands[2]);
				result = true;
				break;
				
			case ANSYADDORINCR:
				innerCache.addOrIncr(commands[1].toString(),(Long)commands[2]);
				result = true;
				break;
				
			case ANSYDECR:
				innerCache.decr(commands[1].toString(),(Long)(commands[2]));
				result = true;
				break;
			
			case ANSYINCR:
				innerCache.incr(commands[1].toString(),(Long)(commands[2]));
				result = true;
				break;
				
		}
		
		return result;
	}
	
	/**
	 * 队列请求命令处理
	 * @param commands
	 */
	public void commandProcess(Object[] commands)
	{
		List<MemCachedClient> caches = helper.getClusterCache();
		
		for(MemCachedClient cache : caches)
		{
			if (commands[0] == CacheCommand.RECOVER
					|| commands[0] == CacheCommand.RECOVERCOUNTER)
			{
				if (!helper.getCacheClient(commands[1].toString()).equals(cache))
					continue;
				else
				{
					if (commands[0] == CacheCommand.RECOVER)
						cache.set(commands[1].toString(),commands[2]);
					else
						cache.storeCounter(commands[1].toString(),(Long)commands[2]);
					
					break;
				}
			}
			
			if (helper.getCacheClient(commands[1].toString()).equals(cache))
				continue;
			
			try
			{
				switch ((CacheCommand)commands[0])
				{
					case PUT:
					case ANSYPUT:
						if(commands.length > 3)
							cache.set(commands[1].toString(),commands[2],(Date)commands[3]);
						else
							cache.set(commands[1].toString(),commands[2]);
						break;
						
					
					case ADD:
						if(commands.length > 3)
							cache.add(commands[1].toString(),commands[2],(Date)commands[3]);
						else
							cache.add(commands[1].toString(),commands[2]);
						break;
						
					case REPLACE:
						if(commands.length > 3)
							cache.replace(commands[1].toString(),commands[2],(Date)commands[3]);
						else
							cache.replace(commands[1].toString(),commands[2]);
						break;
						
					case STORECOUNTER:
					case ANSYSTORECOUNTER:
						cache.storeCounter(commands[1].toString(),(Long)commands[2]);
						break;
						
					case ADDORDECR:
					case ANSYADDORDECR:
						cache.addOrDecr(commands[1].toString(),(Long)commands[2]);
						break;
						
					case ADDORINCR:
					case ANSYADDORINCR:
						cache.addOrIncr(commands[1].toString(),(Long)commands[2]);
						break;
						
					case INCR:
					case ANSYINCR:
						cache.incr(commands[1].toString(),(Long)(commands[2]));
						break;
						
					case DECR:
					case ANSYDECR:
						cache.decr(commands[1].toString(),(Long)(commands[2]));
						break;
						
				}

			}
			catch(Exception ex)
			{
				Logger.error(new StringBuilder(helper.getCacheName())
							.append(" cluster process error"),ex);
			}
		}
	}
	
	
	
}
