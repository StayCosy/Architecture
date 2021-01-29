/**
 * 
 */
package com.alisoft.xplatform.asf.cache;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import com.alisoft.xplatform.asf.cache.memcached.MemcacheStats;
import com.alisoft.xplatform.asf.cache.memcached.MemcacheStatsSlab;
import com.alisoft.xplatform.asf.cache.memcached.MemcachedResponse;

/**
 * Memcached Cache的接口定义
 * @author wenchu.cenwc<wenchu.cenwc@alibaba-inc.com>
 *
 */
public interface IMemcachedCache extends ICache<String,Object>
{

	/**
	 * 降低memcache的交互频繁造成的性能损失，因此采用本地cache结合memcache的方式
	 * @param key
	 * @param 本地缓存失效时间单位秒
	 * @return
	 */
	public Object get(String key,int localTTL);
	
	/**
	 * 获取多个keys对应的值
	 * @param keys
	 * @return
	 */
	public Object[] getMultiArray(String[] keys);
	/**
	 * 获取多个keys对应的key&value Entrys
	 * @param keys
	 * @return
	 */
	public Map<String,Object> getMulti(String[] keys);
	
	
	/**
	 * key所对应的是一个计数器，实现增加inc的数量
	 * @param key
	 * @param inc
	 * @return
	 */
	public long incr(String key,long inc);
	
	/**
	 * key所对应的是一个计数器，实现减少decr的数量
	 * @param key
	 * @param decr
	 * @return
	 */
	public long decr(String key,long decr);
	
	/**
	 * key所对应的是一个计数器，实现增加inc的数量
	 * @param key
	 * @param inc
	 * @return
	 */
	public long addOrIncr(String key,long inc);
	
	/**
	 * key所对应的是一个计数器，实现减少decr的数量
	 * @param key
	 * @param decr
	 * @return
	 */
	public long addOrDecr(String key,long decr);
	
	/**
	 * 存储计数器
	 * @param key
	 * @param count
	 */
	public void storeCounter(String key,long count); 
	
	/**
	 * 获取寄存器，-1表示不存在
	 * @param key
	 */
	public long getCounter(String key);
	
	
	/**
	 * 这个接口返回的Key如果采用fast模式，
	 * 那么返回的key可能已经被清除或者失效，但是在内存中还有痕迹，如果是非fast模式，那么就会精确返回，但是效率较低
	 * @param 是否需要去交验key是否存在
	 * @return
	 */
	public Set<String> keySet(boolean fast);
	
	
	/**
	 * 统计服务器的Slab的情况
	 * @return
	 */
	public MemcacheStatsSlab[] statsSlabs();
	
	
	/**
	 * 统计Memcache使用的情况
	 * @return
	 */
	public MemcacheStats[] stats();
	
	/**
	 * 统计Items的存储情况
	 * @param servers
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Map statsItems();
	
	/**
	 * 统计Cache的响应时间
	 * @return
	 */
	public MemcachedResponse statCacheResponse();
	
	/**
	 * 设置统计时间，单位为秒
	 * @param checkInterval
	 */
	public void setStatisticsInterval(long checkInterval);
	
	
	/**
	 * 保存数据,前提是key不存在于memcache中，否则保存不成功
	 * @param key
	 * @param value
	 * @return
	 */
	public boolean add(String key,Object value);	
	
	/**
	 * 保存有有效期的数据，前提是key不存在于memcache中，否则保存不成功
	 * @param key
	 * @param value
	 * @param 有效期
	 * @return
	 */
	public boolean add(String key,Object value, Date expiry);
	
	
	/**
	 * 保存数据,前提是key必须存在于memcache中，否则保存不成功
	 * @param key
	 * @param value
	 * @return
	 */
	public boolean replace(String key,Object value);	
	
	/**
	 * 保存有有效期的数据，前提是key必须存在于memcache中，否则保存不成功
	 * @param key
	 * @param value
	 * @param 有效期
	 * @return
	 */
	public boolean replace(String key,Object value, Date expiry);	
	
	/**
	 * 异步存入数据，当前立即返回，稍后存入数据
	 * @param key
	 * @param value
	 */
	public void asynPut(String key,Object value);
	
	
	/**
	 * 异步累减计数器，不保证累减成功
	 * @param key
	 * @param decr
	 */
	public void asynAddOrDecr(String key,long decr);
	
	/**
	 * 异步累加计数器，不保证累加成功
	 * @param key
	 * @param incr
	 */
	public void asynAddOrIncr(String key,long incr);
	
	/**
	 * 异步累减计数器，不保证累减成功
	 * @param key
	 * @param decr
	 */
	public void asynDecr(String key,long decr);
	
	/**
	 * 异步累加计数器，不保证累加成功
	 * @param key
	 * @param incr
	 */
	public void asynIncr(String key,long incr);
	
	/**
	 * 异步存储计数器,不保证保存成功
	 * @param key
	 * @param count
	 */
	public void asynStoreCounter(String key,long count); 
	
	
}
