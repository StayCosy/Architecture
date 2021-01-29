package com.alisoft.xplatform.asf.cache.memcached;

/**
 * 
 * SocketIO Pool的配置
 * @author wenchu.cenwc<wenchu.cenwc@alibaba-inc.com>
 *
 */
public class MemcachedClientSocketPoolConfig
{
	private String name;
	private boolean failover = true;
	private int initConn = 10;
	private int minConn = 5;
	private int maxConn = 250;
	/**
	 * 这个参数很重要，检查Pool,对于连接池有维护的作用,ms作为单位
	 */
	private int maintSleep = 1000 * 3;
	private boolean nagle = false;
	/**
	 * Socket TimeOut配置
	 */
	private int socketTo = 3000;
	/**
	 * socket在处理前是否需要作心跳交验
	 */
	private boolean aliveCheck = true;
	/**
	 * max idle time in ms
	 */
	private int maxIdle = 3 * 1000;
	
	/**
	 * 分布的memcached服务器的列表字段，用逗号分割，服务器地址加端口号
	 */
	private String servers;
	/**
	 * 是否需要设置这些服务器的权重
	 */
	private String weights;
	
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public boolean isFailover()
	{
		return failover;
	}
	public void setFailover(boolean failover)
	{
		this.failover = failover;
	}
	public int getInitConn()
	{
		return initConn;
	}
	public void setInitConn(int initConn)
	{
		this.initConn = initConn;
	}
	public int getMinConn()
	{
		return minConn;
	}
	public void setMinConn(int minConn)
	{
		this.minConn = minConn;
	}
	public int getMaxConn()
	{
		return maxConn;
	}
	public void setMaxConn(int maxConn)
	{
		this.maxConn = maxConn;
	}
	public int getMaintSleep()
	{
		return maintSleep;
	}
	public void setMaintSleep(int maintSleep)
	{
		this.maintSleep = maintSleep;
	}
	public boolean isNagle()
	{
		return nagle;
	}
	public void setNagle(boolean nagle)
	{
		this.nagle = nagle;
	}
	public int getSocketTo()
	{
		return socketTo;
	}
	public void setSocketTo(int socketTo)
	{
		this.socketTo = socketTo;
	}
	public boolean isAliveCheck()
	{
		return aliveCheck;
	}
	public void setAliveCheck(boolean aliveCheck)
	{
		this.aliveCheck = aliveCheck;
	}
	public String getServers()
	{
		return servers;
	}
	public void setServers(String servers)
	{
		this.servers = servers;
	}
	public String getWeights()
	{
		return weights;
	}
	public void setWeights(String weights)
	{
		this.weights = weights;
	}
	public int getMaxIdle() {
		return maxIdle;
	}
	public void setMaxIdle(int maxIdle) {
		this.maxIdle = maxIdle;
	}
	
}
