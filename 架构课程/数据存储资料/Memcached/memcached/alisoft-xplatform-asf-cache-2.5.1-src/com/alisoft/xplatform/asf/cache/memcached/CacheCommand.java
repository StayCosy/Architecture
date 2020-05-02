package com.alisoft.xplatform.asf.cache.memcached;

/**
 * @author wenchu.cenwc
 *
 */
public enum CacheCommand 
{
	PUT("put"),
	RECOVER("recover"),
	STORECOUNTER("storeCounter"),
	RECOVERCOUNTER("recoverCounter"),
	ADDORDECR("addOrDecr"),
	ADDORINCR("addOrIncr"),
	DECR("decr"),
	INCR("incr"),
	ADD("add"),
	REPLACE("replace"),
	ANSYPUT("ansyPut"),
	ANSYSTORECOUNTER("ansystoreCounter"),
	ANSYADDORDECR("ansyAddOrDecr"),
	ANSYADDORINCR("ansyAddOrIncr"),
	ANSYDECR("ansyDecr"),
	ANSYINCR("ansyIncr"),
	ANSYADD("ansyAdd"),
	ANSYREPLACE("ansyReplace");
	
	private String v;
	
	CacheCommand(String value)
	{
		v = value;
	}
	
	@Override
	public String toString() {
		return v;
	}
	
}
