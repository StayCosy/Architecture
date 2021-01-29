/**
 * 
 */
package com.alisoft.xplatform.asf.cache.memcached;

/**
 * Cluster的配置
 * @author wenchu.cenwc<wenchu.cenwc@alibaba-inc.com>
 *
 */
public class MemcachedClientClusterConfig
{
	private String name;
	private String[] memCachedClients;
	/**
	 * 集群模式：active/standby/fullcopy，
	 * 1.多节点负载均衡
	 * 2.数据冗余（异步）
	 * 3.节点出错切换
	 * 4.节点回复后内容Lazy复制
	 * 5.数据递归查找
	 * 
	 * active：1,2,3,4,5;  standby:1,2,3;
	 * 
	 * 默认是active
	 * 
	 */
	private String mode = CLUSTER_MODE_ACTIVE;
	
	public final static String CLUSTER_MODE_ACTIVE = "active";
	public final static String CLUSTER_MODE_STANDBY = "standby";
	public final static String CLUSTER_MODE_NONE = "none";

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String[] getMemCachedClients()
	{
		return memCachedClients;
	}

	public void setMemCachedClients(String[] memCachedClients)
	{
		this.memCachedClients = memCachedClients;
	}

	public String getMode()
	{
		return mode;
	}

	public void setMode(String mode)
	{
		this.mode = mode;
	}
}
