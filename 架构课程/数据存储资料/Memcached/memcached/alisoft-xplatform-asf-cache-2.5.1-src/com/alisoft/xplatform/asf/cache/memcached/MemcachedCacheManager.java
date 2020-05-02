package com.alisoft.xplatform.asf.cache.memcached;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.stream.XMLInputFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.alisoft.xplatform.asf.cache.ICacheManager;
import com.alisoft.xplatform.asf.cache.IMemcachedCache;
import com.alisoft.xplatform.asf.cache.memcached.client.ErrorHandler;
import com.alisoft.xplatform.asf.cache.memcached.client.MemCachedClient;
import com.alisoft.xplatform.asf.cache.memcached.client.SockIOPool;

/**
 * 
 * Memcache 客户端管理类，负责读取配置文件，
 * 初始化各个Memcache客户端，
 * 同时也负责管理和销毁客户端
 * @author wenchu.cenwc<wenchu.cenwc@alibaba-inc.com>
 *
 */
public class MemcachedCacheManager implements ICacheManager<IMemcachedCache>
{
	private static final Log Logger = LogFactory.getLog(MemcachedCacheManager.class);
	
	/**
	 * 配置文件名称
	 */
	private static final String MEMCACHED_CONFIG_FILE ="memcached.xml";
	/**
	 * cache客户端池
	 */
	private ConcurrentHashMap<String,IMemcachedCache> cachepool;
	/**
	 * cache客户端配置的socketio池
	 */
	private ConcurrentHashMap<String,SockIOPool> socketpool;
	/**
	 * cache客户端集群池
	 */
	private ConcurrentHashMap<String,MemcachedClientCluster> clusterpool;
	/**
	 * cache对应的集群Map
	 */
	private ConcurrentHashMap<IMemcachedCache,MemcachedClientCluster> cache2cluster;
	
	/**
	 * Memcache客户端配置
	 */
	private List<MemcachedClientConfig> memcachedClientconfigs;
	/**
	 * Memcache SocketPool配置
	 */
	private List<MemcachedClientSocketPoolConfig> memcachedClientSocketPoolConfigs;
	/**
	 * Memcache集群配置
	 */
	private List<MemcachedClientClusterConfig> memcachedClientClusterConfigs;
	
	/**
	 * 是否支持读取所有classpath下的配置
	 */
	private boolean supportMultiConfig = false;
	
	/**
	 * memcached指定的配置文件
	 */
	private String configFile;
	
	/**
	 * 响应统计时间间隔(单位秒,默认为0,0表示不需要做响应统计)
	 */
	private int responseStatInterval = 0;


	/* (non-Javadoc)
	 * @see com.alisoft.xplatform.asf.cache.ICacheManager#start()
	 */
	public void start()
	{
		cachepool = new ConcurrentHashMap<String,IMemcachedCache>();
		socketpool = new ConcurrentHashMap<String,SockIOPool>();
		clusterpool = new ConcurrentHashMap<String,MemcachedClientCluster>();
		cache2cluster = new ConcurrentHashMap<IMemcachedCache,MemcachedClientCluster>();
		
		memcachedClientconfigs = new ArrayList<MemcachedClientConfig>();
		memcachedClientSocketPoolConfigs = new ArrayList<MemcachedClientSocketPoolConfig>();
		memcachedClientClusterConfigs = new ArrayList<MemcachedClientClusterConfig>();
		
		loadConfig(configFile);
		
		if(memcachedClientconfigs != null && memcachedClientconfigs.size() > 0
				&& memcachedClientSocketPoolConfigs != null && memcachedClientSocketPoolConfigs.size() > 0)
		{
			try
			{
				initMemCacheClientPool();
			}
			catch(Exception ex)
			{
				Logger.error("MemcachedManager init error ,please check !");
				throw new RuntimeException("MemcachedManager init error ,please check !",ex);
			}
			
		}
		else
		{
			Logger.error("no config info for MemcachedManager,please check !");
			throw new RuntimeException("no config info for MemcachedManager,please check !");
		}
		
	}
	
	/**
	 * 载入配置信息
	 */
	protected void loadConfig(String configFile)
	{
		try
		{
			if (supportMultiConfig)
			{
				Enumeration<URL> urls = null;
		    	ClassLoader loader = Thread.currentThread().getContextClassLoader();
		    	
		    	if(configFile != null && !configFile.equals(""))
		    		urls = loader.getResources(configFile);
		    	else
		    		urls = loader.getResources(MEMCACHED_CONFIG_FILE);
		    	
		    	XMLInputFactory factory = XMLInputFactory.newInstance();
		    	
		    	if (urls == null || !urls.hasMoreElements())
		    	{
		    		Logger.error("no memcached config find! please put memcached.xml in your classpath");
		    		throw new java.lang.RuntimeException("no memcached config find! please put memcached.xml in your classpath");
		    	}
		    	
		    	while(urls.hasMoreElements())
		    	{
		    		URL url = urls.nextElement();
		    		CacheUtil.loadMemcachedConfigFromURL(url,factory,
		    				memcachedClientconfigs,memcachedClientSocketPoolConfigs,memcachedClientClusterConfigs);
		    		
		    		Logger.info(new StringBuilder().append("load config from :").append(url.getFile()));
		    	}
			}
			else
			{
				URL url = null;
				ClassLoader loader = Thread.currentThread().getContextClassLoader();
				
				if(configFile != null && !configFile.equals(""))
				{
					if (configFile.startsWith("http"))
						url = new URL(configFile);
					else
						url = loader.getResource(configFile);
				}
				else
					url = loader.getResource(MEMCACHED_CONFIG_FILE);
				
				XMLInputFactory factory = XMLInputFactory.newInstance();
				
				if (url == null)
		    	{
		    		Logger.error("no memcached config find! please put memcached.xml in your classpath");
		    		throw new java.lang.RuntimeException("no memcached config find! please put memcached.xml in your classpath");
		    	}
				
				CacheUtil.loadMemcachedConfigFromURL(url,factory,
	    				memcachedClientconfigs,memcachedClientSocketPoolConfigs,memcachedClientClusterConfigs);
	    		
	    		Logger.info(new StringBuilder().append("load config from :").append(url.getFile()));
			}

		}
		catch(Exception ex)
		{
			Logger.error("MemcachedManager loadConfig error !");
			throw new RuntimeException("MemcachedManager loadConfig error !",ex);
		}
	}			
	
	/**
	 * 初始化各个资源池
	 */
	protected void initMemCacheClientPool()
	{
		//初始化socket pool
		for(MemcachedClientSocketPoolConfig socketPool : memcachedClientSocketPoolConfigs)
		{
			if (socketPool != null && 
					socketPool.getServers() != null && !socketPool.getServers().equals(""))
			{
				SockIOPool pool = SockIOPool.getNewInstance(socketPool.getName());
				
				String[] servers = socketPool.getServers().split(",");
				String[] weights = null;
				
				if (socketPool.getWeights() != null && !socketPool.getWeights().equals("") )
					weights = socketPool.getWeights().split(",");
				
				pool.setServers(servers);
				
				if (weights != null && weights.length > 0 
						&& weights.length == servers.length)
				{
					Integer[] weightsarr = new Integer[weights.length];
					
					for(int i = 0 ; i < weights.length; i++)
						weightsarr[i] =  new Integer(weights[i]);
					
					pool.setWeights( weightsarr );
				}
			
				pool.setInitConn(socketPool.getInitConn());
				pool.setMinConn(socketPool.getMinConn());
				pool.setMaxConn(socketPool.getMaxConn());
				pool.setMaintSleep(socketPool.getMaintSleep());
				pool.setSocketTO(socketPool.getSocketTo() );
				pool.setNagle(socketPool.isNagle() );	
				pool.setFailover(socketPool.isFailover());
				pool.setAliveCheck(socketPool.isAliveCheck() );
				pool.setMaxIdle(socketPool.getMaxIdle());
				pool.initialize();
				
				if (socketpool.get(socketPool.getName())!= null)
					Logger.error(new StringBuilder("socketpool define duplicate! socketpool name:").append(socketPool.getName()));
				
				socketpool.put(socketPool.getName(), pool);
				Logger.info(new StringBuilder().append(" add socketpool :").append(socketPool.getName()));
				
			} 
			else
			{
				Logger.error("MemcachedClientSocketPool config error !");
				throw new RuntimeException("MemcachedClientSocketPool config error !");
			}
		}
		
		
		for(MemcachedClientConfig node : memcachedClientconfigs)
		{

			//初始化Cache Client。
			MemCachedClientHelper helper = new MemCachedClientHelper();
			IMemcachedCache cache = new MemcachedCache(helper,responseStatInterval);
			MemCachedClient client = new MemCachedClient(node.getSocketPool());
			
			client.setCompressEnable(node.isCompressEnable());
			client.setDefaultEncoding(node.getDefaultEncoding());
			
			try
			{
				if (node.getErrorHandler() != null && !node.getErrorHandler().equals(""))
					client.setErrorHandler((ErrorHandler)Class.forName(node.getErrorHandler()).newInstance());
				
			}
			catch(Exception ex)
			{
				Logger.error(new StringBuilder().append("Not find class name:")
						.append(node.getErrorHandler())
						.append("please check space char or tab char"));
			}
			
			helper.setCacheName(node.getName());
			helper.setCacheClient(client);
			helper.setCacheManager(this);
			helper.setMemcachedCache(cache);
			
			if (cachepool.get(node.getName())!= null)
				Logger.error(new StringBuilder("cache define duplicate! cache name :").append(node.getName()));
			
			cachepool.put(node.getName(), cache);
			Logger.info(new StringBuilder().append(" add memcachedClient :").append(node.getName()));
		}
		
		for(MemcachedClientClusterConfig node : memcachedClientClusterConfigs)
		{
			String[] clients = node.getMemCachedClients();
			
			if (clients != null && clients.length > 0)
			{
				MemcachedClientCluster cluster = new MemcachedClientCluster();
				cluster.setName(node.getName());
				cluster.setMode(node.getMode());
				cluster.setCaches(new ArrayList<IMemcachedCache>());
				
				for(String client : clients)
				{
					IMemcachedCache cache = cachepool.get(client);
					
					if (cache != null)
					{
						cluster.getCaches().add(cache);
						cache2cluster.put(cache, cluster);
					}
					
				}
				
				if (clusterpool.get(cluster.getName())!= null)
					Logger.error(new StringBuilder("cluster define duplicate! cluster name :").append(cluster.getName()));
				
				clusterpool.put(cluster.getName(),cluster);
			}
			
		}
		
	}
	
	public IMemcachedCache getCache(String name)
	{
		return getCachepool().get(name);
	}
	
	/* (non-Javadoc)
	 * @see com.alisoft.xplatform.asf.cache.ICacheManager#stop()
	 */
	public void stop()
	{
		
		try
		{
			for(IMemcachedCache node :getCachepool().values())
			{
				if (node != null)
					node.destroy();
			}
			
			if (socketpool != null && socketpool.size() > 0)
			{
				Enumeration<String> keys = socketpool.keys();
				
				while(keys.hasMoreElements())
				{
					String node = keys.nextElement();
					
					SockIOPool.removeInstance(node);
				}
				
				socketpool.clear();
				
			}
		}
		catch(Exception ex)
		{
			Logger.error("Cache Manager Stop Error!",ex);
		}
		finally
		{
			getCachepool().clear();
			
			if (clusterpool != null)
				clusterpool.clear();
			
			if (cache2cluster != null)
				cache2cluster.clear();
			
			if (memcachedClientconfigs != null)
				memcachedClientconfigs.clear();
			
			if (memcachedClientSocketPoolConfigs != null)
				memcachedClientSocketPoolConfigs.clear();
			
			if (memcachedClientClusterConfigs != null)
				memcachedClientClusterConfigs.clear();
		}

	}

	public ConcurrentHashMap<String,IMemcachedCache> getCachepool()
	{
		if (cachepool == null)
			throw new java.lang.RuntimeException("cachepool is null!");
		
		return cachepool;
	}

	public ConcurrentHashMap<String, SockIOPool> getSocketpool()
	{
		return socketpool;
	}

	public void setSocketpool(ConcurrentHashMap<String, SockIOPool> socketpool)
	{
		this.socketpool = socketpool;
	}


	public boolean isSupportMultiConfig()
	{
		return supportMultiConfig;
	}

	public void setSupportMultiConfig(boolean supportMultiConfig)
	{
		this.supportMultiConfig = supportMultiConfig;
	}

	public ConcurrentHashMap<IMemcachedCache, MemcachedClientCluster> getCache2cluster()
	{
		return cache2cluster;
	}

	public String getConfigFile()
	{
		return configFile;
	}

	public void setConfigFile(String configFile)
	{
		this.configFile = configFile;
	}

	@Override
	public void reload(String configFile)
	{
		if (configFile != null 
				&& !configFile.equals(""))
			this.configFile = configFile;
		
		stop();
		start();
	}

	@Override
	public void clusterCopy(String fromCache,String cluster)
	{
		IMemcachedCache fcache = getCachepool().get(fromCache);
		MemcachedClientCluster t_cluster = clusterpool.get(cluster);
		
		if (fcache != null && t_cluster != null)
		{
			Set<String> keys = fcache.keySet(false);
			
			for(IMemcachedCache cache : t_cluster.getCaches())
			{
				if (cache == fcache)
					continue;
				
				for(String key : keys)
				{
					cache.put(key, fcache.get(key));
				}
			}
			
		}
		
	}

	@Override
	public void setResponseStatInterval(int seconds) {
		this.responseStatInterval = seconds;
	}

}
