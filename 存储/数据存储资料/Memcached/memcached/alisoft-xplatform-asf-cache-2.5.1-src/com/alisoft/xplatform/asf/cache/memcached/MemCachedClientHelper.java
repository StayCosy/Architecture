/**
 * 
 */
package com.alisoft.xplatform.asf.cache.memcached;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alisoft.xplatform.asf.cache.IMemcachedCache;
import com.alisoft.xplatform.asf.cache.memcached.client.MemCachedClient;

/**
 * 为封装的MemCache提供实际处理的帮助类
 * @author wenchu.cenwc<wenchu.cenwc@alibaba-inc.com>
 *
 */
public class MemCachedClientHelper
{
	private static final Log Logger = LogFactory.getLog(MemCachedClientHelper.class);
	private MemCachedClient cacheClient;
	private MemcachedCacheManager cacheManager;
	private IMemcachedCache memcachedCache;
	private String cacheName;
	
	
	public List<MemCachedClient> getClusterCache()
	{
		List<MemCachedClient> result = new ArrayList<MemCachedClient>();
		
		if (hasCluster())
		{
			MemcachedClientCluster cluster = cacheManager.getCache2cluster().get(memcachedCache);
			
			for(IMemcachedCache node : cluster.getCaches())
			{
				if (node instanceof MemcachedCache)
				{
					result.add(((MemcachedCache)node).getHelper().getInnerCacheClient());
				}
			}
		}
		
		return result;
	}
	
	protected boolean hasCluster()
	{
		boolean result = false;
		
		if (memcachedCache != null && cacheManager != null)
		{
			MemcachedClientCluster cluster = cacheManager.getCache2cluster().get(memcachedCache);
			
			if (cluster != null && cluster.getCaches() != null 
					&& cluster.getCaches().size() > 0)
				result = true;
		}
		
		return result;
	}
	
	protected String getClusterMode()
	{
		String mode = MemcachedClientClusterConfig.CLUSTER_MODE_NONE;
		
		if (memcachedCache != null && cacheManager != null)
		{
			MemcachedClientCluster cluster = cacheManager.getCache2cluster().get(memcachedCache);
			
			if (cluster != null && cluster.getCaches() != null 
					&& cluster.getCaches().size() > 0)
				if (cluster.getMode().equals(MemcachedClientClusterConfig.CLUSTER_MODE_ACTIVE) 
						||cluster.getMode().equals(MemcachedClientClusterConfig.CLUSTER_MODE_STANDBY))
					mode = cluster.getMode();
		}
		
		
		return mode;
	}
	
	public MemCachedClient getInnerCacheClient()
	{
		if (cacheClient == null)
		{
			Logger.error("cacheClient can't be injected into MemcachedCacheHelper");
			throw new java.lang.RuntimeException("cacheClient can't be injected into MemcachedCacheHelper");
		}
		
		return cacheClient;
	}
	
	public MemCachedClient getCacheClient(String key)
	{
		if (cacheClient == null)
		{
			Logger.error("cacheClient can't be injected into MemcachedCacheHelper");
			throw new java.lang.RuntimeException("cacheClient can't be injected into MemcachedCacheHelper");
		}
		
		//根据算法获取集群中的某一台节点服务器
		if (hasCluster())
		{
			List<MemCachedClient> clusters = getClusterCache();
			
			long keyhash = key.hashCode();
			
			int index = (int)keyhash % clusters.size();
			
			if (index < 0 )
				index *= -1;
			
			return clusters.get(index);
			
		}
		else
			return cacheClient;
	}

	public void setCacheClient(MemCachedClient cacheClient)
	{
		this.cacheClient = cacheClient;
	}

	public MemcachedCacheManager getCacheManager()
	{
		return cacheManager;
	}

	public void setCacheManager(MemcachedCacheManager cacheManager)
	{
		this.cacheManager = cacheManager;
	}

	public IMemcachedCache getMemcachedCache()
	{
		return memcachedCache;
	}

	public void setMemcachedCache(IMemcachedCache memcachedCache)
	{
		this.memcachedCache = memcachedCache;
	}

	public String getCacheName()
	{
		return cacheName;
	}

	public void setCacheName(String cacheName)
	{
		this.cacheName = cacheName;
	}


}
