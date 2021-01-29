package com.alisoft.xplatform.asf.cache.memcached;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.alisoft.xplatform.asf.cache.ICache;
import com.alisoft.xplatform.asf.cache.ICacheManager;
import com.alisoft.xplatform.asf.cache.impl.DefaultCacheImpl;

/**
 * Cache工具类
 * @author wenchu.cenwc<wenchu.cenwc@alibaba-inc.com>
 *
 */
public class CacheUtil
{
	private static final Log Logger = LogFactory.getLog(CacheUtil.class);
	
	private static ICache<String,Object> localCache = new DefaultCacheImpl();
	
	
	/**
	 * 获取CacheManager的接口类，首先去查找META-INF/services/下是否有定义，如果没有载入defaultImplClass的定义
	 * @param CacheManager管理的ｃａｃｈｅ的接口类型
	 * @param 接口类
	 * @param 默认实现类名，需要全名
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <B extends ICache<?,?>> ICacheManager<B> 
						getCacheManager(Class<B> cache,String defaultImplClass)
	{
		ICacheManager<B> cacheManager = null;
		
		cacheManager =  (ICacheManager<B>)getInstanceByInterface(ICacheManager.class
					,Thread.currentThread().getContextClassLoader(),defaultImplClass,true);
		
		return cacheManager;
	}
	
	/**
	 * 根据传入的类型按照策略去搜索classpath下的实现，动态装载实现类并且返回
	 * @param <I>
	 * @param 接口描述
	 * @param classLoader
	 * @param 默认的实现类
	 * @param 是否需要缓存，如果为否则将不缓存
	 * @return 返回的接口实现实例
	 */
	public static <I> I getInstanceByInterface(Class<I> interfaceDefinition
			,ClassLoader classLoader,String defaultImplClass,boolean needCache)
	{
		I result = null;
		
		//获取缓存的情况，或者移除缓存
		if (needCache)
		{
			String className = (String)localCache.get(interfaceDefinition.getName());
			
			if (className != null && !className.equals(""))
				return newInstance(interfaceDefinition,className, classLoader);
		}
		else
		{
			localCache.remove(interfaceDefinition.getName());
		}
		
		
		String errorStr = new StringBuilder(interfaceDefinition.getName())
							.append(" Instance load error!").toString();
		
		try
		{
			String systemProp =
                System.getProperty(interfaceDefinition.getName());
            if( systemProp!=null) 
            {
            	Logger.info("found system property" + systemProp);
            	
            	if (needCache)
            		localCache.put(interfaceDefinition.getName(), systemProp);
            	
                return newInstance(interfaceDefinition,systemProp, classLoader);
            }
		}
		catch(Exception ex)
		{
			Logger.error(errorStr,ex);
			result = null;
		}
		
		String serviceId = new StringBuilder("META-INF/services/").
								append(interfaceDefinition.getName()).toString();

		InputStream in = null;
		
		try 
		{
            if (classLoader == null)
	            in = ClassLoader.getSystemResourceAsStream(serviceId);
            else
            	in = classLoader.getResourceAsStream(serviceId);
            
        
            if( in != null) 
            {
                BufferedReader rd =
                    new BufferedReader(new InputStreamReader(in, "UTF-8"));
        
                String className = rd.readLine();
                rd.close();

                if (className != null &&
                    ! "".equals(className)) 
                {
                	Logger.info("loaded from services: " + className);
                	
                	if (needCache)
                		localCache.put(interfaceDefinition.getName(), className);
                	
                    return newInstance(interfaceDefinition,className, classLoader);
                }
            }
            else
            {
            	if (defaultImplClass != null && !defaultImplClass.equals(""))
            	{
            		Logger.info("loaded from services: " + defaultImplClass);
            		
            		if (needCache)
                		localCache.put(interfaceDefinition.getName(), defaultImplClass);
            		
                    return newInstance(interfaceDefinition,defaultImplClass, classLoader);
            	}
            }
            
        } 
		catch( Exception ex ) 
		{
			Logger.error(errorStr,ex);
			result = null;
        }
		finally
		{
			try
			{
				if (in != null)
					in.close();
				
				in = null;
			}
			catch(Exception ex)
			{
				Logger.error(errorStr,ex);
				result = null;
			}
		}		
		
		
		if (result == null)
			throw new java.lang.RuntimeException(errorStr);
		
		return result;
	}
	
	/**
	 * 创建实例
	 * @param <I>
	 * @param interfaceDefinition
	 * @param className
	 * @param classLoader
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static <I> I newInstance(Class<I> interfaceDefinition,String className,
            ClassLoader classLoader)
	{
		try
		{
			Class<I> spiClass;
			
            if (classLoader == null) 
            {
                spiClass = (Class<I>) Class.forName(className);
            } 
            else 
            {
                spiClass = (Class<I>) classLoader.loadClass(className);
            }

            return spiClass.newInstance();
		}
		catch(ClassNotFoundException x)
		{
			throw new java.lang.RuntimeException(
	                "Provider " + className + " not found", x);
		}
		catch(Exception ex)
		{
			throw new java.lang.RuntimeException(
	                "Provider " + className + " could not be instantiated: " + ex,
	                ex);
		}
	}
	
	/**
	 * 从URL中载入Memcached的配置信息
	 * @param url
	 * @param factory
	 * @param memcachedClientconfigs
	 * @param memcachedClientSocketPoolConfigs
	 */
	public static void loadMemcachedConfigFromURL(URL url,XMLInputFactory factory
										,List<MemcachedClientConfig> memcachedClientconfigs
										,List<MemcachedClientSocketPoolConfig> memcachedClientSocketPoolConfigs
										,List<MemcachedClientClusterConfig> memcachedClientClusterConfig)
	{
		MemcachedClientConfig node = null;
		MemcachedClientSocketPoolConfig socketnode = null;
		MemcachedClientClusterConfig clusternode = null;
		
		InputStream in = null;
		XMLEventReader r = null;
		
		try
		{
			in = url.openStream();
			r = factory.createXMLEventReader(in);
			
			String servers = null;
			String weights = null;
			
			while(r.hasNext())
			{
				XMLEvent event = r.nextEvent();
				
				if (event.isStartElement())
				{
					StartElement start = event.asStartElement();
					
					String tag = start.getName().getLocalPart();
					
					if (tag.equalsIgnoreCase("client"))
					{
						node = new MemcachedClientConfig();

						if (start.getAttributeByName(new QName("","name")) != null)
							node.setName(start.getAttributeByName(new QName("","name")).getValue());
						else
							throw new RuntimeException("memcached client name can't not be null!");
						
						if (start.getAttributeByName(new QName("","socketpool")) != null)
							node.setSocketPool(start.getAttributeByName(new QName("","socketpool")).getValue());
						else
							throw new RuntimeException("memcached client socketpool can't not be null!");
						
						if (start.getAttributeByName(new QName("","compressEnable")) != null)
							node.setCompressEnable(Boolean.parseBoolean(
									start.getAttributeByName(new QName("","compressEnable")).getValue()));
						else
							node.setCompressEnable(true);
						
						if (start.getAttributeByName(new QName("","defaultEncoding")) != null)
							node.setDefaultEncoding(start.getAttributeByName(new QName("","defaultEncoding")).getValue());
						else
							node.setDefaultEncoding("UTF-8");
					
						continue;
					}
					
					if (tag.equalsIgnoreCase("errorHandler") && node!=null)
					{
						event = r.peek();
						
						if (event.isCharacters())
						{
							node.setErrorHandler(event.asCharacters().getData());
							r.nextEvent();
						}
						
						continue;
					}
					
					if (tag.equalsIgnoreCase("socketpool"))
					{
						socketnode = new MemcachedClientSocketPoolConfig();
						
						servers = null;
						weights = null;
						
						if (start.getAttributeByName(new QName("","name")) != null)
							socketnode.setName(start.getAttributeByName(new QName("","name")).getValue());
						else
							throw new RuntimeException("memcached client socketpool name can't not be null!");
						
						if (start.getAttributeByName(new QName("","failover")) != null)
							socketnode.setFailover(Boolean.parseBoolean(
									start.getAttributeByName(new QName("","failover")).getValue()));
						
						if (start.getAttributeByName(new QName("","initConn")) != null)
							socketnode.setInitConn(Integer.parseInt(
									start.getAttributeByName(new QName("","initConn")).getValue()));
						
						if (start.getAttributeByName(new QName("","minConn")) != null)
							socketnode.setMinConn(Integer.parseInt(
									start.getAttributeByName(new QName("","minConn")).getValue()));
						
						if (start.getAttributeByName(new QName("","maxConn")) != null)
							socketnode.setMaxConn(Integer.parseInt(
									start.getAttributeByName(new QName("","maxConn")).getValue()));
						
						if (start.getAttributeByName(new QName("","maintSleep")) != null)
							socketnode.setMaintSleep(Integer.parseInt(
									start.getAttributeByName(new QName("","maintSleep")).getValue()));
						
						if (start.getAttributeByName(new QName("","nagle")) != null)
							socketnode.setNagle(Boolean.parseBoolean(
									start.getAttributeByName(new QName("","nagle")).getValue()));
						
						if (start.getAttributeByName(new QName("","socketTO")) != null)
							socketnode.setSocketTo(Integer.parseInt(
									start.getAttributeByName(new QName("","socketTO")).getValue()));
						
						if (start.getAttributeByName(new QName("","maxIdle")) != null)
							socketnode.setMaxIdle(Integer.parseInt(
									start.getAttributeByName(new QName("","maxIdle")).getValue()));
						
						if (start.getAttributeByName(new QName("","aliveCheck")) != null)
							socketnode.setAliveCheck(Boolean.parseBoolean(
									start.getAttributeByName(new QName("","aliveCheck")).getValue()));
						
						continue;
					}
					
					
					if (tag.equalsIgnoreCase("servers")&& socketnode!=null)
					{
						event = r.peek();
						
						if (event.isCharacters())
						{
							servers = event.asCharacters().getData();
							socketnode.setServers(servers);
							r.nextEvent();
						}
						
						continue;
					}
					
					if (tag.equalsIgnoreCase("weights")&& socketnode!=null)
					{
						event = r.peek();
						
						if (event.isCharacters())
						{
							weights = event.asCharacters().getData();
							socketnode.setWeights(weights);
							r.nextEvent();
						}
						
						continue;
					}
					
					if (tag.equalsIgnoreCase("cluster"))
					{
						clusternode = new MemcachedClientClusterConfig();
						
						if (start.getAttributeByName(new QName("","name")) != null)
							clusternode.setName(start.getAttributeByName(new QName("","name")).getValue());
						else
							throw new RuntimeException("memcached cluster name can't not be null!");
						
						if (start.getAttributeByName(new QName("","mode")) != null)
							clusternode.setMode(start.getAttributeByName(new QName("","mode")).getValue());
						
						continue;
					}
					
					if(tag.equalsIgnoreCase("memCachedClients")&& clusternode != null)
					{
						event = r.peek();
						
						if (event.isCharacters())
						{
							String clients = event.asCharacters().getData();
							
							if (clients != null && !clients.equals(""))
							{
								clusternode.setMemCachedClients(clients.split(","));
							}
							r.nextEvent();
						}
						
						continue;
					}
					
				}
				
				if (event.isEndElement())
				{
					EndElement end = event.asEndElement();
					
					if (node != null 
							&& end.getName().getLocalPart().equalsIgnoreCase("client"))
					{
						memcachedClientconfigs.add(node);
						Logger.info(new StringBuilder().append(" add memcachedClient config :").append(node.getName()));
						
						continue;
					}
					
					if (socketnode != null 
							&& end.getName().getLocalPart().equalsIgnoreCase("socketpool"))
					{
						memcachedClientSocketPoolConfigs.add(socketnode);
						Logger.info(new StringBuilder().append(" add socketpool config :").append(socketnode.getName()));
						
						continue;
					}
					
					if (clusternode != null 
							&& end.getName().getLocalPart().equalsIgnoreCase("cluster"))
					{
						memcachedClientClusterConfig.add(clusternode);
						Logger.info(new StringBuilder().append(" add cluster config :").append(clusternode.getName()));
						
						continue;
					}
				}
				
			}
			
		}
		catch(Exception e)
		{
			Logger.error(new StringBuilder("MemcachedManager loadConfig error !")
					.append(" config url :").append(url.getFile()).toString());
			node = null;
		}
		finally
		{
			
			try
			{
				if (r != null)
					r.close();
				
				if (in != null)
					in.close();
				
				r = null;
				in = null;
			}
			catch(Exception ex)
			{
				throw new RuntimeException("processConfigURL error !",ex);
			}
		}

	}	
	
}
