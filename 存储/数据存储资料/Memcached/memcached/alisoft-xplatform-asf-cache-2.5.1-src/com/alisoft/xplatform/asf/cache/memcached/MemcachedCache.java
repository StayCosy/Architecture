package com.alisoft.xplatform.asf.cache.memcached;

import java.net.URLDecoder;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alisoft.xplatform.asf.cache.ICache;
import com.alisoft.xplatform.asf.cache.IMemcachedCache;
import com.alisoft.xplatform.asf.cache.impl.DefaultCacheImpl;
import com.alisoft.xplatform.asf.cache.memcached.client.MemCachedClient;

public class MemcachedCache implements IMemcachedCache
{
	private static final Log Logger = LogFactory.getLog(MemcachedCache.class);
	private MemCachedClientHelper helper;
	private ICache<String,Object> localCache;
	private ClusterProcessor processor;
	private StatisticsTask task;
	private long statisticsInterval = 5 * 60;//单位秒
	
	static final String CACHE_STATUS_RESPONSE = "cacheStatusResponse";

	/**
	 * 数据队列
	 */
	private LinkedBlockingQueue<Object[]> dataQueue;	
	
	public MemcachedCache(MemCachedClientHelper helper,int statisticsInterval)
	{
		this.helper = helper;
		
		dataQueue = new LinkedBlockingQueue<Object[]>();
		localCache = new DefaultCacheImpl();
		
		if (statisticsInterval > 0)
		{
			this.statisticsInterval = statisticsInterval;
			task = new StatisticsTask();
			task.setDaemon(true);
			task.start();
		}
		
		
		processor = new ClusterProcessor(dataQueue,helper);
		processor.setDaemon(true);
		processor.start();
	}
	
	public boolean clear()
	{
		boolean result = false;
		
		if (helper.hasCluster())
		{
			List<MemCachedClient> caches = helper.getClusterCache();
			
			for(MemCachedClient cache : caches)
			{
				try
				{
					result = cache.flushAll(null);
				}
				catch(Exception ex)
				{
					Logger.error(new StringBuilder(helper.getCacheName())
								.append(" cluster clear error"),ex);
					result = false;
				}
			}
			
			return result;
			
		}
		else
			return helper.getInnerCacheClient().flushAll( null );
	}

	public Map<String, Object> getMulti(String[] keys)
	{
		if (keys == null || keys.length <= 0)
			return null;
			
			
		Map<String, Object> result = getCacheClient(keys[0]).getMulti(keys);
		
		if (result != null)
			return result;
		
		if (helper.hasCluster())
		{
			List<MemCachedClient> caches = helper.getClusterCache();
			
			for(MemCachedClient cache : caches)
			{
				if (getCacheClient(keys[0]).equals(cache))
					continue;
				
				try
				{
					result = cache.getMulti(keys);
				}
				catch(Exception ex)
				{
					Logger.error(new StringBuilder(helper.getCacheName())
								.append(" cluster getMulti error"),ex);
				}
				
				if (result != null)
					break;
			}
		}
		
		
		return result;
	}

	public Object[] getMultiArray(String[] keys)
	{
		if (keys == null || keys.length <= 0)
			return null;
		
		Object[] result = getCacheClient(keys[0]).getMultiArray(keys);
		
		if (result != null)
			return result;
		
		if (helper.hasCluster())
		{
			List<MemCachedClient> caches = helper.getClusterCache();
			
			for(MemCachedClient cache : caches)
			{
				if (getCacheClient(keys[0]).equals(cache))
					continue;
				
				try
				{
					result = cache.getMultiArray(keys);
				}
				catch(Exception ex)
				{
					Logger.error(new StringBuilder(helper.getCacheName())
								.append(" cluster getMultiArray error"),ex);
				}
				
				if (result != null)
					break;
			}
		}
		
		
		return result;
	}

	public Object put(String key, Object value, Date expiry)
	{

		boolean result = getCacheClient(key).set(key,value,expiry);
		
		//移除本地缓存的内容
		if (result)
			localCache.remove(key);
		
		if (helper.hasCluster())
		{
			Object[] commands = new Object[]{CacheCommand.PUT,key,value,expiry};
			
			addCommandToQueue(commands);
		}
		else
			if (!result)
				throw new java.lang.RuntimeException
					(new StringBuilder().append("put key :").append(key).append(" error!").toString());
		
		return value;
	}
	
	public Object put(String key, Object value, int TTL)
	{
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, TTL);
		
		put(key,value,calendar.getTime());
		
		return value;
	}

	public boolean containsKey(String key)
	{	
		boolean result = false;
		boolean isError = false;
		
		try
		{
			result = getCacheClient(key).keyExists(key);
		}
		catch(MemcachedException ex)
		{
			Logger.error(new StringBuilder(helper.getCacheName())
				.append(" cluster containsKey error"),ex);
			isError = true;
		}
		
		
		if (!result && helper.hasCluster())
			if (isError || helper.getClusterMode().equals
				(MemcachedClientClusterConfig.CLUSTER_MODE_ACTIVE))
		{
			List<MemCachedClient> caches = helper.getClusterCache();
			
			for(MemCachedClient cache : caches)
			{
				if (getCacheClient(key).equals(cache))
					continue;
				
				try
				{
					try
					{
						result = cache.keyExists(key);
					}
					catch(MemcachedException ex)
					{
						Logger.error(new StringBuilder(helper.getCacheName())
							.append(" cluster containsKey error"),ex);
						
						continue;
					}
					
					//仅仅判断另一台备份机器，不多次判断，防止效率低下,异步修复
					if (helper.getClusterMode()
							.equals(MemcachedClientClusterConfig.CLUSTER_MODE_ACTIVE) && result)
					{
						Object[] commands = new Object[]{CacheCommand.RECOVER,key,cache.get(key)};
						
						addCommandToQueue(commands);
					}
					
					break;
					
				}
				catch(Exception e)
				{
					Logger.error(new StringBuilder(helper.getCacheName())
								.append(" cluster get error"),e);
				}

			}
		}
		
		return result;
	}

	public Object get(String key)
	{
		Object result = null;
		boolean isError = false;
		
		try
		{
			result = getCacheClient(key).get(key);	
		}
		catch(MemcachedException ex)
		{
			Logger.error(new StringBuilder(helper.getCacheName())
				.append(" cluster get error"),ex);
			
			isError = true;
		}
		
		
		if (result == null && helper.hasCluster())
			if (isError || helper.getClusterMode().equals
					(MemcachedClientClusterConfig.CLUSTER_MODE_ACTIVE))
		{
			List<MemCachedClient> caches = helper.getClusterCache();
			
			for(MemCachedClient cache : caches)
			{
				if (getCacheClient(key).equals(cache))
					continue;
				
				try
				{
					try
					{
						result = cache.get(key);
					}
					catch(MemcachedException ex)
					{
						Logger.error(new StringBuilder(helper.getCacheName())
							.append(" cluster get error"),ex);
						
						continue;
					}
					
					//仅仅判断另一台备份机器，不多次判断，防止效率低下
					if (helper.getClusterMode()
							.equals(MemcachedClientClusterConfig.CLUSTER_MODE_ACTIVE) && result != null)
					{
						Object[] commands = new Object[]{CacheCommand.RECOVER,key,result};
						
						addCommandToQueue(commands);
					}
					
					break;
					
				}
				catch(Exception e)
				{
					Logger.error(new StringBuilder(helper.getCacheName())
								.append(" cluster get error"),e);
				}

			}
		}
		

		return result;
	}

	public Object put(String key, Object value)
	{
		boolean result = getCacheClient(key).set(key, value);
		
		//移除本地缓存的内容
		if (result)
			localCache.remove(key);
		
		if (helper.hasCluster())
		{
			Object[] commands = new Object[]{CacheCommand.PUT,key,value};
			addCommandToQueue(commands);
		}
		else
			if (!result)
				throw new java.lang.RuntimeException
					(new StringBuilder().append("put key :").append(key).append(" error!").toString());

		return value;
	}
	
	public void storeCounter(String key, long count)
	{
		boolean result = getCacheClient(key).storeCounter(key,count);

		if (helper.hasCluster())
		{
			Object[] commands = new Object[]{CacheCommand.STORECOUNTER,key,count};

			addCommandToQueue(commands);	
		}
		else
			if (!result)
				throw new java.lang.RuntimeException
					(new StringBuilder().append("storeCounter key :").append(key).append(" error!").toString());

	}
	
	public long getCounter(String key)
	{
		long result = -1;
		boolean isError = false;
		
		try
		{
			result = getCacheClient(key).getCounter(key);
		}
		catch(MemcachedException ex)
		{
			Logger.error(new StringBuilder(helper.getCacheName())
				.append(" cluster getCounter error"),ex);
			
			isError = true;
		}
		
		
		if (result == -1 && helper.hasCluster())
			if (isError || helper.getClusterMode().equals
				(MemcachedClientClusterConfig.CLUSTER_MODE_ACTIVE))
		{
			List<MemCachedClient> caches = helper.getClusterCache();
			
			for(MemCachedClient cache : caches)
			{
				if (getCacheClient(key).equals(cache))
					continue;
				
				try
				{
					try
					{
						result = cache.getCounter(key);
					}
					catch(MemcachedException ex)
					{
						Logger.error(new StringBuilder(helper.getCacheName())
							.append(" cluster getCounter error"),ex);
						
						continue;
					}
					
					if (result != -1 && helper.getClusterMode().equals
							(MemcachedClientClusterConfig.CLUSTER_MODE_ACTIVE))
					{
						Object[] commands = new Object[]{CacheCommand.RECOVERCOUNTER,key,result};
						
						addCommandToQueue(commands);
					}
					
					break;
				}
				catch(Exception e)
				{
					Logger.error(new StringBuilder(helper.getCacheName())
								.append(" cluster getCounter error"),e);
				}
			}
		}
		
		
		return result;
	}
	
	public long addOrDecr(String key, long decr)
	{
		long result = getCacheClient(key).addOrDecr(key,decr);
		
		if (helper.hasCluster())
		{
			Object[] commands = new Object[]{CacheCommand.ADDORDECR,key,decr};
			
			addCommandToQueue(commands);	
		}
		
		return result;
	}

	public long addOrIncr(String key, long inc)
	{
		long result = getCacheClient(key).addOrIncr(key,inc);
		
		if (helper.hasCluster())
		{
			Object[] commands = new Object[]{CacheCommand.ADDORINCR,key,inc};

			addCommandToQueue(commands);	
		}
		
		return result;
	}
	
	public long decr(String key, long decr)
	{
		long result = getCacheClient(key).decr(key,decr);
		
		if (helper.hasCluster())
		{
			Object[] commands = new Object[]{CacheCommand.DECR,key,decr};

			addCommandToQueue(commands);	
			
		}
		
		return result;
	}

	public long incr(String key, long inc)
	{
		long result = getCacheClient(key).incr(key,inc);
		
		if (helper.hasCluster())
		{
			Object[] commands = new Object[]{CacheCommand.INCR,key,inc};
			
			addCommandToQueue(commands);	
		}
		
		return result;
	}	

	public Object remove(String key)
	{
		Object result = getCacheClient(key).delete(key);
		
		//异步删除由于集群会导致无法被删除，因此需要一次性全部清除
		if (helper.hasCluster())
		{
			
			List<MemCachedClient> caches = helper.getClusterCache();
			
			for(MemCachedClient cache : caches)
			{
				if (getCacheClient(key).equals(cache))
					continue;
				
				try
				{
					cache.delete(key);
				}
				catch(Exception ex)
				{
					Logger.error(new StringBuilder(helper.getCacheName())
								.append(" cluster remove error"),ex);
				}
			}

		}
		
		return result;
	}

	@Deprecated
	public int size()
	{
		throw new java.lang.UnsupportedOperationException("Memcached not support size method!");
	}

	@SuppressWarnings("unchecked")
	public Collection<Object> values()
	{
		Set<Object> values = new HashSet<Object>();
		Map<String,Integer> dumps = new HashMap<String,Integer>();
			 
		Map slabs = helper.getInnerCacheClient().statsItems();
		
		if (slabs != null && slabs.keySet() != null)
		{
			Iterator itemsItr = slabs.keySet().iterator();
			
			while(itemsItr.hasNext())
			{
				String server = itemsItr.next().toString();
				Map itemNames = (Map) slabs.get(server);
				Iterator itemNameItr = itemNames.keySet().iterator();
				
				while(itemNameItr.hasNext())
				{
					String itemName = itemNameItr.next().toString();
					
					// itemAtt[0] = itemname
			        // itemAtt[1] = number
			        // itemAtt[2] = field
			        String[] itemAtt = itemName.split(":");
			        
			        if (itemAtt[2].startsWith("number")) 
			        	dumps.put(itemAtt[1], Integer.parseInt(itemAtt[1]));
				}
			}
			
			if (!dumps.values().isEmpty())
			{
				Iterator<Integer> dumpIter = dumps.values().iterator();
				
				while(dumpIter.hasNext())
				{
					int dump = dumpIter.next();
					
					Map cacheDump = helper.getInnerCacheClient().statsCacheDump(dump,50000);
					
					Iterator entryIter = cacheDump.values().iterator();
					
					while (entryIter.hasNext()) 
		            {
		            	Map items = (Map)entryIter.next();
		            	
		            	Iterator ks = items.keySet().iterator();
		            	

		            	while(ks.hasNext())
		            	{
		            		String k = (String)ks.next();
		            		
		            		try
		            		{
		            			k = URLDecoder.decode(k,"UTF-8");
		            		}
		            		catch(Exception ex)
		            		{
		            			Logger.error(ex);
		            		}

		            		if (k != null && !k.trim().equals(""))
		            		{
		            			Object value = get(k);
		            			
		            			if (value != null)
		            				values.add(value);
		            		}
		            	}
		            }
					
				}
			}
		}
		
		return values;		
	}
	
	
	@SuppressWarnings("unchecked")
	public Set<String> keySet(boolean fast)
	{
		Set<String> keys = new HashSet<String>();
		Map<String,Integer> dumps = new HashMap<String,Integer>();
			 
		Map slabs = helper.getInnerCacheClient().statsItems();
		
		if (slabs != null && slabs.keySet() != null)
		{
			Iterator itemsItr = slabs.keySet().iterator();
			
			while(itemsItr.hasNext())
			{
				String server = itemsItr.next().toString();
				Map itemNames = (Map) slabs.get(server);
				Iterator itemNameItr = itemNames.keySet().iterator();
				
				while(itemNameItr.hasNext())
				{
					String itemName = itemNameItr.next().toString();
					
					// itemAtt[0] = itemname
			        // itemAtt[1] = number
			        // itemAtt[2] = field
			        String[] itemAtt = itemName.split(":");
			        
			        if (itemAtt[2].startsWith("number")) 
			        	dumps.put(itemAtt[1], Integer.parseInt(itemAtt[1]));
				}
			}
			
			if (!dumps.values().isEmpty())
			{
				Iterator<Integer> dumpIter = dumps.values().iterator();
				
				while(dumpIter.hasNext())
				{
					int dump = dumpIter.next();
					
					Map cacheDump = helper.getInnerCacheClient().statsCacheDump(dump,0);
					
					Iterator entryIter = cacheDump.values().iterator();
					
					while (entryIter.hasNext()) 
		            {
		            	Map items = (Map)entryIter.next();
		            	
		            	Iterator ks = items.keySet().iterator();
		            	

		            	while(ks.hasNext())
		            	{
		            		String k = (String)ks.next();
		            		
		            		try
		            		{
		            			k = URLDecoder.decode(k,"UTF-8");
		            		}
		            		catch(Exception ex)
		            		{
		            			Logger.error(ex);
		            		}

		            		if (k != null && !k.trim().equals(""))
		            		{
		            			if (fast)
		            				keys.add(k);
		            			else
		            				if (containsKey(k))
		            					keys.add(k);
		            		}
		            	}
		            }
					
				}
			}
		}
		
		return keys;

	}	
	
	public MemCachedClient getCacheClient(String key)
	{
		if (helper == null)
		{
			Logger.error("MemcachedCache helper is null!");
			throw new java.lang.RuntimeException("MemcachedCache helper is null!");
		}
		
		return helper.getCacheClient(key);
	}

	public MemCachedClientHelper getHelper()
	{
		return helper;
	}

	public void setHelper(MemCachedClientHelper helper)
	{
		this.helper = helper;
	}

	public Set<String> keySet()
	{
		return keySet(false);
	}

	public Object get(String key, int localTTL)
	{
		Object result = null;
		
		result = localCache.get(key);
		
		if (result == null)
		{
			result = get(key);
			
			if (result != null)
			{
				Calendar calendar = Calendar.getInstance();
				calendar.add(Calendar.SECOND, localTTL);
				localCache.put(key, result,calendar.getTime());
			}
		}
		
		
		return result;
	}

	@SuppressWarnings("unchecked")
	public MemcacheStats[] stats()
	{
		MemcacheStats[] result = null;
		
		Map<String,Map<String,String>> statMap = helper.getInnerCacheClient().stats();
		
		if (statMap != null && !statMap.isEmpty())
		{
			result = new MemcacheStats[statMap.size()];
			
			Iterator<String> iter = statMap.keySet().iterator();
			
			int i = 0;
			
			while(iter.hasNext())
			{
				result[i] = new MemcacheStats();
				result[i].setServerHost(iter.next());
				result[i].setStatInfo(statMap.get(result[i].getServerHost()).toString());
				i += 1;
			}
		}
		
		return result;
	}

	@SuppressWarnings("unchecked")
	public MemcacheStatsSlab[] statsSlabs()
	{
		MemcacheStatsSlab[] result = null;
		
		Map<String,Map<String,Object>> statMap = helper.getInnerCacheClient().statsSlabs();
		
		if (statMap != null && !statMap.isEmpty())
		{
			result = new MemcacheStatsSlab[statMap.size()];
			
			Iterator<String> iter = statMap.keySet().iterator();
			
			int i = 0;
			
			while(iter.hasNext())
			{
				result[i] = new MemcacheStatsSlab();
				result[i].setServerHost(iter.next());
				
				Map<String,Object> node = statMap.get(result[i].getServerHost());
				
				Iterator<String> nodeIter = node.keySet().iterator();
				
				while(nodeIter.hasNext())
				{
					String key = nodeIter.next();
					result[i].addSlab(key,node.get(key).toString());
				}

				i += 1;
			}
		}	
		
		return result;
	}

	@SuppressWarnings("unchecked")
	public Map statsItems()
	{
		Map items = helper.getInnerCacheClient().statsItems();
		return items;
	}
	
	/**
	 * 将需要异步处理的内容放到Queue中
	 * @param command
	 */
	public void addCommandToQueue(Object[] command)
	{
		dataQueue.add(command);
	}

	public void destroy() 
	{
		try
		{
			if (localCache != null)
				localCache.destroy();
			
			if (processor != null)
				processor.stopProcess();
			
			if (task != null)
			{
				task.stopTask();
			}
				
		}
		catch(Exception ex)
		{
			Logger.error(ex);
		}
	}
	

	public MemcachedResponse statCacheResponse()
	{
		if (localCache.get(CACHE_STATUS_RESPONSE)== null)
		{
			MemcachedResponse response = new MemcachedResponse();
			response.setCacheName(helper.getCacheName());
			localCache.put(CACHE_STATUS_RESPONSE, response);
		}
			
		
		return (MemcachedResponse)localCache.get(CACHE_STATUS_RESPONSE);
	}
	
	public long getStatisticsInterval()
	{
		return statisticsInterval;
	}

	public void setStatisticsInterval(long statisticsInterval)
	{
		this.statisticsInterval = statisticsInterval;
	}

	@Override
	public boolean add(String key, Object value)
	{
		boolean result = getCacheClient(key).add(key,value);
		
		if (helper.hasCluster())
		{
			Object[] commands = new Object[]{CacheCommand.ADD,key,value};
			
			addCommandToQueue(commands);
		}
		
		return result;
	}

	@Override
	public boolean add(String key, Object value, Date expiry)
	{
		boolean result = getCacheClient(key).add(key,value,expiry);
		
		if (helper.hasCluster())
		{
			Object[] commands = new Object[]{CacheCommand.ADD,key,value,expiry};
			
			addCommandToQueue(commands);
		}
		
		return result;
	}


	@Override
	public boolean replace(String key, Object value)
	{
		boolean result = getCacheClient(key).replace(key,value);
		
		if (helper.hasCluster())
		{
			Object[] commands = new Object[]{CacheCommand.REPLACE,key,value};
			
			addCommandToQueue(commands);
		}
		
		return result;
	}

	@Override
	public boolean replace(String key, Object value, Date expiry)
	{
		boolean result = getCacheClient(key).replace(key,value,expiry);
		
		if (helper.hasCluster())
		{
			Object[] commands = new Object[]{CacheCommand.REPLACE,key,value,expiry};
			
			addCommandToQueue(commands);
		}
		
		return result;
	}

	
	/**
	 * 统计响应时间等信息的后台线程
	 * @author wenchu.cenwc
	 *
	 */
	class StatisticsTask extends java.lang.Thread
	{
		private boolean flag = true;

		
		@Override
		public void run()
		{
			while(flag)
			{
				long consume = 0;
				
				try
				{
					Thread.sleep(statisticsInterval * 1000);
					
					consume = checkResponse();		
				}
				catch(InterruptedException e)
				{
					Logger.warn("StatisticsTask stoped!");
				}
				catch(Exception ex)
				{
					Logger.error("StatisticsTask execute error",ex);
					consume = -1;
				}
				
				if (localCache != null)
				{
					MemcachedResponse response = (MemcachedResponse)localCache.get(CACHE_STATUS_RESPONSE);
					
					if (response != null && response.getResponses() != null)
						response.getResponses().add(consume);
				}
			}
			
		}
		
		/**
		 * 发送请求
		 * @return
		 */
		private long checkResponse()
		{
			if (localCache.get(CACHE_STATUS_RESPONSE)== null)
			{
				MemcachedResponse response = new MemcachedResponse();
				response.setCacheName(helper.getCacheName());
				localCache.put(CACHE_STATUS_RESPONSE, response);
			}
			else if (((MemcachedResponse)localCache.get(CACHE_STATUS_RESPONSE))
						.getEndTime().before(new Date()))
			{
				((MemcachedResponse)localCache.get(CACHE_STATUS_RESPONSE)).ini();
			}
			
			long consume = System.currentTimeMillis();
			
			put(CACHE_STATUS_RESPONSE,CACHE_STATUS_RESPONSE);
			get(CACHE_STATUS_RESPONSE);
			
			consume = System.currentTimeMillis() - consume;
			
			return consume;
		}
		
		public void stopTask()
		{
			flag = false;
			interrupt();
		}


		public boolean isFlag()
		{
			return flag;
		}


		public void setFlag(boolean flag)
		{
			this.flag = flag;
		}
		
	}


	@Override
	public void asynPut(String key, Object value)
	{
		Object[] commands = new Object[]{CacheCommand.ANSYPUT,key,value};
		
		addCommandToQueue(commands);
	}

	@Override
	public void asynAddOrDecr(String key, long decr)
	{
		Object[] commands = new Object[]{CacheCommand.ANSYADDORDECR,key,decr};
		
		addCommandToQueue(commands);
	}

	@Override
	public void asynAddOrIncr(String key, long incr)
	{
		Object[] commands = new Object[]{CacheCommand.ANSYADDORINCR,key,incr};
		
		addCommandToQueue(commands);
	}

	@Override
	public void asynDecr(String key, long decr)
	{
		Object[] commands = new Object[]{CacheCommand.ANSYDECR,key,decr};
		
		addCommandToQueue(commands);
		
	}

	@Override
	public void asynIncr(String key, long incr)
	{
		Object[] commands = new Object[]{CacheCommand.ANSYINCR,key,incr};
		
		addCommandToQueue(commands);
	}

	@Override
	public void asynStoreCounter(String key, long count)
	{
		Object[] commands = new Object[]{CacheCommand.ANSYSTORECOUNTER,key,count};
		
		addCommandToQueue(commands);
	}

}
