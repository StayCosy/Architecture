/**
 * 
 */
package com.alisoft.xplatform.asf.cache.memcached;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Slab统计结果的结构
 * @author wenchu.cenwc<wenchu.cenwc@alibaba-inc.com>
 *
 */
public class MemcacheStatsSlab
{
	private String serverHost;
	private Map<String,Slab> slabs;
	
	public MemcacheStatsSlab()
	{
		slabs = new TreeMap<String,Slab>(new SlabKeyComparator());
		
		Slab node = new Slab();
		node.setSlabNum("0");
		slabs.put("0", node);
	}
	
	public class Slab
	{
		private String slabNum;
		private Map<String,String> slabInfo;
		
		public Slab()
		{
			slabInfo = new TreeMap<String,String>();
		}
		
		public String getSlabNum()
		{
			return slabNum;
		}
		public void setSlabNum(String slabNum)
		{
			this.slabNum = slabNum;
		}
		public Map<String,String> getSlabInfo()
		{
			return slabInfo;
		}
		public void setSlabInfo(Map<String,String> slabInfo)
		{
			this.slabInfo = slabInfo;
		}
		
		public String toString()
		{
			if (slabNum.equals("0") )
				return new StringBuilder().append("Total Slab Info : ").append(slabInfo.toString()).toString();
			else
				return new StringBuilder().append("slabNum:  ").append(slabNum)
					.append(",slabInfo:  ").append(slabInfo.toString()).toString();
		}
	}
	
	
	class SlabKeyComparator implements Comparator<String>
	{
		public int compare(String o1, String o2)
		{
			int result = 0;
			
			try
			{
				int i1 = Integer.parseInt(o1);
				
				int i2 = Integer.parseInt(o2);
				
				result = i1 - i2;
			}
			catch(Exception ex)
			{
				System.out.println(ex);
			}
			
			return result;
		}
		
	}
	
	public void addSlab(String slabNum,String slabInfo)
	{
		if (slabNum.indexOf(":") < 0)
		{
			slabs.get("0").getSlabInfo().put(slabNum, slabInfo);
			return;
		}
		
		String num = slabNum.substring(0,slabNum.indexOf(":"));
		
		Slab node = slabs.get(num);
		
		if (node == null)
		{
			node = new Slab();
			node.setSlabNum(num);
		}
		
		node.getSlabInfo().put(slabNum.substring(slabNum.indexOf(":") +1), slabInfo);
		
		slabs.put(num, node);
	}

	public String getServerHost()
	{
		return serverHost;
	}

	public void setServerHost(String serverHost)
	{
		this.serverHost = serverHost;
	}

	public Map<String,Slab> getSlabs()
	{
		return slabs;
	}

	public void setSlabs(Map<String,Slab> slabs)
	{
		this.slabs = slabs;
	}
	
}
