/**
 * 
 */
package com.alisoft.xplatform.asf.cache.memcached;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * memcached响应统计结构
 * @author wenchu.cenwc
 *
 */
@SuppressWarnings("serial")
public class MemcachedResponse implements java.io.Serializable
{
	private Date startTime;
	private String cacheName;
	private List<Long> responses;
	private Date endTime;
	
	public MemcachedResponse()
	{
		responses = new ArrayList<Long>();
		ini(); 
	}
	
	public void ini()
	{
		Calendar calendar = Calendar.getInstance();
		startTime = calendar.getTime();
		
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		endTime = calendar.getTime();
		
		responses.clear();
	}
	
	
	public Date getStartTime()
	{
		return startTime;
	}
	public void setStartTime(Date startTime)
	{
		this.startTime = startTime;
	}
	public String getCacheName()
	{
		return cacheName;
	}
	public void setCacheName(String cacheName)
	{
		this.cacheName = cacheName;
	}
	public List<Long> getResponses()
	{
		return responses;
	}
	public void setResponses(List<Long> responses)
	{
		this.responses = responses;
	}

	public Date getEndTime()
	{
		return endTime;
	}

	public void setEndTime(Date endTime)
	{
		this.endTime = endTime;
	}

	@Override
	public String toString()
	{
		StringBuilder content = new StringBuilder();
		SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		content.append("cacheName:").append(cacheName);
		content.append(",startTime:").append(formater.format(startTime));
		content.append(",endTime:").append(formater.format(endTime));
		content.append(",responseRecords:");
		
		for(long c : responses)
		{
			content.append(c).append(",");
		}
		
		return content.toString();
	}
	
	
}
