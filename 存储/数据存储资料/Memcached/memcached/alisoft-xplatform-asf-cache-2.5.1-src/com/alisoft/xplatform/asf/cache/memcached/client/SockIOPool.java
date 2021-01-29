/**
 * 
 */
package com.alisoft.xplatform.asf.cache.memcached.client;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Date;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.*;
import java.net.*;
import java.io.*;
import java.nio.channels.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;

/**
 * 
 * This class is a connection pool for maintaning a pool of persistent
 * connections<br/> to memcached servers.
 * 
 * The pool must be initialized prior to use. This should typically be early on<br/>
 * in the lifecycle of the JVM instance.<br/> <br/>
 * <h3>An example of initializing using defaults:</h3>
 * 
 * <pre>
 * static
 * {
 * 	String[] serverlist = { &quot;cache0.server.com:12345&quot;,
 * 			&quot;cache1.server.com:12345&quot; };
 * 
 * 	SockIOPool pool = SockIOPool.getInstance();
 * 	pool.setServers(serverlist);
 * 	pool.initialize();
 * }
 * </pre>
 * 
 * <h3>An example of initializing using defaults and providing weights for
 * servers:</h3>
 * 
 * <pre>
 * static
 * {
 * 	String[] serverlist = { &quot;cache0.server.com:12345&quot;,
 * 			&quot;cache1.server.com:12345&quot; };
 * 	Integer[] weights = { new Integer(5), new Integer(2) };
 * 
 * 	SockIOPool pool = SockIOPool.getInstance();
 * 	pool.setServers(serverlist);
 * 	pool.setWeights(weights);
 * 	pool.initialize();
 * }
 * </pre>
 * 
 * <h3>An example of initializing overriding defaults:</h3>
 * 
 * <pre>
 * static
 * {
 * 	String[] serverlist = { &quot;cache0.server.com:12345&quot;,
 * 			&quot;cache1.server.com:12345&quot; };
 * 	Integer[] weights = { new Integer(5), new Integer(2) };
 * 	int initialConnections = 10;
 * 	int minSpareConnections = 5;
 * 	int maxSpareConnections = 50;
 * 	long maxIdleTime = 1000 * 60 * 30; // 30 minutes
 * 	long maxBusyTime = 1000 * 60 * 5; // 5 minutes
 * 	long maintThreadSleep = 1000 * 5; // 5 seconds
 * 	int socketTimeOut = 1000 * 3; // 3 seconds to block on reads
 * 	int socketConnectTO = 1000 * 3; // 3 seconds to block on initial connections.  If 0, then will use blocking connect (default)
 * 	boolean failover = false; // turn off auto-failover in event of server down	
 * 	boolean nagleAlg = false; // turn off Nagle's algorithm on all sockets in pool	
 * 	boolean aliveCheck = false; // disable health check of socket on checkout
 * 
 * 	SockIOPool pool = SockIOPool.getInstance();
 * 	pool.setServers(serverlist);
 * 	pool.setWeights(weights);
 * 	pool.setInitConn(initialConnections);
 * 	pool.setMinConn(minSpareConnections);
 * 	pool.setMaxConn(maxSpareConnections);
 * 	pool.setMaxIdle(maxIdleTime);
 * 	pool.setMaxBusyTime(maxBusyTime);
 * 	pool.setMaintSleep(maintThreadSleep);
 * 	pool.setSocketTO(socketTimeOut);
 * 	pool.setNagle(nagleAlg);
 * 	pool.setHashingAlg(SockIOPool.NEW_COMPAT_HASH);
 * 	pool.setAliveCheck(true);
 * 	pool.initialize();
 * }
 * </pre>
 * 
 * The easiest manner in which to initialize the pool is to set the servers and
 * rely on defaults as in the first example.<br/> After pool is initialized, a
 * client will request a SockIO object by calling getSock with the cache key<br/>
 * The client must always close the SockIO object when finished, which will
 * return the connection back to the pool.<br/>
 * <h3>An example of retrieving a SockIO object:</h3>
 * 
 * <pre>
 * 	SockIOPool.SockIO sock = SockIOPool.getInstance().getSock( key );
 * 	try {
 * 		sock.write( &quot;version\r\n&quot; );	
 * 		sock.flush();	
 * 		System.out.println( &quot;Version: &quot; + sock.readLine() );	
 * 	}
 * 	catch (IOException ioe) { System.out.println( &quot;io exception thrown&quot; ) };	
 * 
 * 	sock.close();	
 * </pre>
 * 
 * �Ż�����Socket��Դ�أ����������Ч����
 * 
 * @author wenchu.cenwc<wenchu.cenwc@alibaba-inc.com>
 * 
 */
public class SockIOPool
{
	// logger
	private static Logger log = Logger.getLogger(SockIOPool.class.getName());

	// store instances of pools
	private static ConcurrentMap<String, SockIOPool> pools = new ConcurrentHashMap<String, SockIOPool>();

	// avoid recurring construction
	private static ThreadLocal<MessageDigest> MD5 = new ThreadLocal<MessageDigest>()
	{
		@Override
		protected MessageDigest initialValue()
		{
			try
			{
				return MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e)
			{
				log.error("++++ no md5 algorithm found");
				throw new IllegalStateException("++++ no md5 algorythm found");
			}
		}
	};

	// Constants

	/**
	 * socket��״̬����ʹ�ã�����æ
	 */
	private static final int SOCKET_STATUS_BUSY = 1;
	/**
	 * socket��״̬���Ѿ�ʧЧ
	 */
	private static final int SOCKET_STATUS_DEAD = 2;
	
	private static final int SOCKET_STATUS_ACTIVE = 3;

	public static final int NATIVE_HASH = 0; // native String.hashCode();
	public static final int OLD_COMPAT_HASH = 1; // original compatibility
													// hashing algorithm (works
													// with other clients)
	public static final int NEW_COMPAT_HASH = 2; // new CRC32 based
													// compatibility hashing
													// algorithm (works with
													// other clients)
	public static final int CONSISTENT_HASH = 3; // MD5 Based -- Stops
													// thrashing when a server
													// added or removed

	public static final long MAX_RETRY_DELAY = 10 * 60 * 1000; // max of 10
																// minute delay
																// for fall off
	
	public static final Random random = new Random();

	// Pool data
	private MaintThread maintThread;
	private boolean initialized = false;
	@SuppressWarnings("unused")
	private int maxCreate = 1; // this will be initialized by pool when the
								// pool is initialized

	// initial, min and max pool sizes
	private int poolMultiplier = 3;
	private int initConn = 1;
	private int minConn = 1;
	private int maxConn = 10;
	private long maxIdle = 1000 * 60 * 5; // max idle time for avail sockets
	private long maxBusyTime = 1000 * 30; // max idle time for avail sockets
	private long maintSleep = 1000 * 30; // maintenance thread sleep time
	private int socketTO = 1000 * 30; // default timeout of socket reads
	private int socketConnectTO = 1000 * 3; // default timeout of socket
											// connections
	@SuppressWarnings("unused")
	private static int recBufferSize = 128;//�������ݵĻ����С
	
	private boolean aliveCheck = false; // default to not check each connection
										// for being alive
	private boolean failover = true; // default to failover in event of cache
										// server dead
	private boolean failback = true; // only used if failover is also set ...
										// controls putting a dead server back
										// into rotation
	private boolean nagle = true; // enable/disable Nagle's algorithm
	private int hashingAlg = NATIVE_HASH; // default to using the native hash
											// as it is the fastest

	// locks
	private final ReentrantLock hostDeadLock = new ReentrantLock();
	private final ReentrantLock initDeadLock = new ReentrantLock();

	// list of all servers
	private String[] servers;
	private Integer[] weights;
	private Integer totalWeight = 0;

	private List<String> buckets;
	private TreeMap<Long, String> consistentBuckets;

	// dead server map
	private ConcurrentMap<String, Date> hostDead;
	private ConcurrentMap<String, Long> hostDeadDur;

	// map to hold all available sockets
	// map to hold socket status;
	private ConcurrentMap<String, ConcurrentMap<SockIO, Integer>> socketPool;
	private Map<String,SockIO> fastPool;
	
	private static final byte[] B_VERSION = "version\r\n".getBytes();

	// empty constructor
	protected SockIOPool()
	{
	}

	/**
	 * Factory to create/retrieve new pools given a unique poolName.
	 * 
	 * @param poolName
	 *            unique name of the pool
	 * @return instance of SockIOPool
	 */
	public static SockIOPool getInstance(String poolName)
	{
		SockIOPool pool;

		if (!pools.containsKey(poolName))
		{
			pool = new SockIOPool();
			pools.putIfAbsent(poolName, pool);
		}

		return pools.get(poolName);
	}
	
	public static String getPoolUsage(String poolName)
	{
		StringBuilder result = new StringBuilder();
		
		if (pools.containsKey(poolName))
		{
			SockIOPool sockIOPool = pools.get(poolName);
			
			int total = 0;
			int busy = 0;
			int dead = 0;
			
			Iterator<ConcurrentMap<SockIO, Integer>> socketIter = sockIOPool.socketPool.values().iterator();
			
			while(socketIter.hasNext())
			{	
				ConcurrentMap<SockIO, Integer> status = socketIter.next();
				
				total += status.size();
				
				Iterator<Integer> iter = status.values().iterator();
				
				while(iter.hasNext())
				{
					int value = iter.next();
					
					if (value == SOCKET_STATUS_BUSY)
						busy++;
					
					if (value == SOCKET_STATUS_DEAD)
						dead++;
				}
			}
			
			result.append("SockIOPool ").append(poolName).append(" : ")
				.append(" total socket: ").append(total).append(" , busy socket: ")
				.append(busy).append(" , dead socket: ").append(dead);
		}
		
		return result.toString();
	}
	
	/**
	 * get new Instance form pool
	 * @param poolName
	 * @return
	 */
	public static SockIOPool getNewInstance(String poolName)
	{
		SockIOPool pool;

		if (!pools.containsKey(poolName))
		{
			pool = new SockIOPool();
			pools.putIfAbsent(poolName, pool);
		}
		else
		{
			SockIOPool newpool = new SockIOPool();
			pool = pools.get(poolName);		
			pools.put(poolName, newpool);
			
			try
			{
				pool.shutDown();
			}
			catch(Exception ex)
			{
				log.error("shutdown old pool error!",ex);
			}
			
		}

		return pools.get(poolName);
	}
	
	public static void removeInstance(String poolName)
	{
		if (pools.containsKey(poolName))
		{
			SockIOPool pool = pools.get(poolName);	
			
			try
			{
				pool.shutDown();
			}
			catch(Exception ex)
			{
				log.error("shutdown old pool error!",ex);
			}
			
			pools.remove(poolName);
		}
	}

	/**
	 * Single argument version of factory used for back compat. Simply creates a
	 * pool named "default".
	 * 
	 * @return instance of SockIOPool
	 */
	public static SockIOPool getInstance()
	{
		return getInstance("default");
	}

	/**
	 * Initializes the pool.
	 */
	public void initialize()
	{

		// check to see if already initialized
		if (initialized && (buckets != null || consistentBuckets != null)
				&& (socketPool != null))
		{
			log.error("++++ trying to initialize an already initialized pool");
			return;
		}

		initDeadLock.lock();
		try
		{
			// check to see if already initialized
			if (initialized && (buckets != null || consistentBuckets != null)
					&& (socketPool != null))
			{
				log
						.error("++++ trying to initialize an already initialized pool");
				return;
			}

			// pools
			socketPool = new ConcurrentHashMap<String, ConcurrentMap<SockIO, Integer>>(
					servers.length * initConn);
			
			fastPool = new HashMap<String,SockIO>();
			
			hostDeadDur = new ConcurrentHashMap<String, Long>();
			hostDead = new ConcurrentHashMap<String, Date>();
			maxCreate = (poolMultiplier > minConn) ? minConn : minConn
					/ poolMultiplier; // only create up to maxCreate
										// connections at once
			
			if (log.isDebugEnabled())
			{
				log.debug("++++ initializing pool with following settings:");
				log.debug("++++ initial size: " + initConn);
				log.debug("++++ min spare   : " + minConn);
				log.debug("++++ max spare   : " + maxConn);
			}

			// if servers is not set, or it empty, then
			// throw a runtime exception
			if (servers == null || servers.length <= 0)
			{
				log.error("++++ trying to initialize with no servers");
				throw new IllegalStateException(
						"++++ trying to initialize with no servers");
			}

			// initalize our internal hashing structures
			if (this.hashingAlg == CONSISTENT_HASH)
				populateConsistentBuckets();
			else
				populateBuckets();

			// mark pool as initialized
			this.initialized = true;

			// start maint thread
			if (this.maintSleep > 0)
				this.startMaintThread();

		} finally
		{
			initDeadLock.unlock();
		}

	}

	private void populateBuckets()
	{
		if (log.isDebugEnabled())
			log
					.debug("++++ initializing internal hashing structure for consistent hashing");

		// store buckets in tree map
		this.buckets = new ArrayList<String>();

		for (int i = 0; i < servers.length; i++)
		{
			if (this.weights != null && this.weights.length > i)
			{
				for (int k = 0; k < this.weights[i].intValue(); k++)
				{
					this.buckets.add(servers[i]);
					if (log.isDebugEnabled())
						log.debug("++++ added " + servers[i]
								+ " to server bucket");
				}
			} else
			{
				this.buckets.add(servers[i]);
				if (log.isDebugEnabled())
					log.debug("++++ added " + servers[i] + " to server bucket");
			}

			// create initial connections
			if (log.isDebugEnabled())
				log.debug("+++ creating initial connections (" + initConn
						+ ") for host: " + servers[i]);

			for (int j = 0; j < initConn; j++)
			{
				SockIO socket = createSocket(servers[i]);
				if (socket == null)
				{
					log.error("++++ failed to create connection to: "
							+ servers[i] + " -- only " + j + " created.");
					break;
				}

				addSocketToPool(socketPool, servers[i], socket,
						SOCKET_STATUS_ACTIVE,SOCKET_STATUS_ACTIVE, true);
				
				if (log.isDebugEnabled())
					log.debug("++++ created and added socket: "
							+ socket.toString() + " for host " + servers[i]);
			}
		}
	}

	private void populateConsistentBuckets()
	{
		if (log.isDebugEnabled())
			log
					.debug("++++ initializing internal hashing structure for consistent hashing");

		// store buckets in tree map
		this.consistentBuckets = new TreeMap<Long, String>();

		MessageDigest md5 = MD5.get();
		if (this.totalWeight <= 0 && this.weights != null)
		{
			for (int i = 0; i < this.weights.length; i++)
				this.totalWeight += (this.weights[i] == null) ? 1
						: this.weights[i];
		} else if (this.weights == null)
		{
			this.totalWeight = this.servers.length;
		}

		for (int i = 0; i < servers.length; i++)
		{
			int thisWeight = 1;
			if (this.weights != null && this.weights[i] != null)
				thisWeight = this.weights[i];

			double factor = Math
					.floor(((double) (40 * this.servers.length * thisWeight))
							/ (double) this.totalWeight);

			for (long j = 0; j < factor; j++)
			{
				byte[] d = md5.digest((servers[i] + "-" + j).getBytes());
				for (int h = 0; h < 4; h++)
				{
					Long k = ((long) (d[3 + h * 4] & 0xFF) << 24)
							| ((long) (d[2 + h * 4] & 0xFF) << 16)
							| ((long) (d[1 + h * 4] & 0xFF) << 8)
							| ((long) (d[0 + h * 4] & 0xFF));

					consistentBuckets.put(k, servers[i]);
					if (log.isDebugEnabled())
						log.debug("++++ added " + servers[i]
								+ " to server bucket");
				}
			}

			// create initial connections
			if (log.isDebugEnabled())
				log.debug("+++ creating initial connections (" + initConn
						+ ") for host: " + servers[i]);

			for (int j = 0; j < initConn; j++)
			{
				SockIO socket = createSocket(servers[i]);
				if (socket == null)
				{
					log.error("++++ failed to create connection to: "
							+ servers[i] + " -- only " + j + " created.");
					break;
				}

				addSocketToPool(socketPool, servers[i], socket
						,SOCKET_STATUS_ACTIVE,SOCKET_STATUS_ACTIVE, true);
				
				if (log.isDebugEnabled())
					log.debug("++++ created and added socket: "
							+ socket.toString() + " for host " + servers[i]);
			}
		}
	}

	/**
	 * Creates a new SockIO obj for the given server.
	 * 
	 * If server fails to connect, then return null and do not try<br/> again
	 * until a duration has passed. This duration will grow<br/> by doubling
	 * after each failed attempt to connect.
	 * 
	 * @param host
	 *            host:port to connect to
	 * @return SockIO obj or null if failed to create
	 */
	protected SockIO createSocket(String host)
	{

		SockIO socket = null;

		// if host is dead, then we don't need to try again
		// until the dead status has expired
		// we do not try to put back in if failback is off
		hostDeadLock.lock();
		try
		{
			if (failover && failback && hostDead.containsKey(host)
					&& hostDeadDur.containsKey(host))
			{

				Date store = hostDead.get(host);
				long expire = hostDeadDur.get(host).longValue();

				if ((store.getTime() + expire) > System.currentTimeMillis())
					return null;
			}
		} finally
		{
			hostDeadLock.unlock();
		}

		try
		{
			socket = new SockIO(this, host, this.socketTO,
					this.socketConnectTO, this.nagle);

			if (!socket.isConnected())
			{
				log.error("++++ failed to get SockIO obj for: " + host
						+ " -- new socket is not connected");
				addSocketToPool(socketPool, host, socket
						,SOCKET_STATUS_DEAD,SOCKET_STATUS_DEAD,true);
				// socket = null;
			}
		} catch (Exception ex)
		{
			log.error("++++ failed to get SockIO obj for: " + host);
			log.error(ex.getMessage(), ex);
			socket = null;
		}

		// if we failed to get socket, then mark
		// host dead for a duration which falls off
		hostDeadLock.lock();
		try
		{
			if (socket == null)
			{
				Date now = new Date();
				hostDead.put(host, now);

				long expire = (hostDeadDur.containsKey(host)) ? (((Long) hostDeadDur
						.get(host)).longValue() * 2)
						: 1000;

				if (expire > MAX_RETRY_DELAY)
					expire = MAX_RETRY_DELAY;

				hostDeadDur.put(host, new Long(expire));
				if (log.isDebugEnabled())
					log.debug("++++ ignoring dead host: " + host + " for "
							+ expire + " ms");

				// also clear all entries for this host from availPool
				clearHostFromPool(host);
			} else
			{
				if (log.isDebugEnabled())
					log.debug("++++ created socket (" + socket.toString()
							+ ") for host: " + host);
				if (hostDead.containsKey(host) || hostDeadDur.containsKey(host))
				{
					hostDead.remove(host);
					hostDeadDur.remove(host);
				}
			}
		} finally
		{
			hostDeadLock.unlock();
		}

		return socket;
	}

	/**
	 * @param key
	 * @return
	 */
	public String getHost(String key)
	{
		return getHost(key, null);
	}

	/**
	 * Gets the host that a particular key / hashcode resides on.
	 * 
	 * @param key
	 * @param hashcode
	 * @return
	 */
	public String getHost(String key, Integer hashcode)
	{
		SockIO socket = getSock(key, hashcode);
		String host = socket.getHost();
		socket.close();
		return host;
	}

	/**
	 * Returns appropriate SockIO object given string cache key.
	 * 
	 * @param key
	 *            hashcode for cache key
	 * @return SockIO obj connected to server
	 */
	public SockIO getSock(String key)
	{
		return getSock(key, null);
	}

	/**
	 * Returns appropriate SockIO object given string cache key and optional
	 * hashcode.
	 * 
	 * Trys to get SockIO from pool. Fails over to additional pools in event of
	 * server failure.
	 * 
	 * @param key
	 *            hashcode for cache key
	 * @param hashCode
	 *            if not null, then the int hashcode to use
	 * @return SockIO obj connected to server
	 */
	public SockIO getSock(String key, Integer hashCode)
	{

		if (log.isDebugEnabled())
			log.debug("cache socket pick " + key + " " + hashCode);

		if (!this.initialized)
		{
			log.error("attempting to get SockIO from uninitialized pool!");
			return null;
		}

		// if no servers return null
		if ((this.hashingAlg == CONSISTENT_HASH && consistentBuckets.size() == 0)
				|| (buckets != null && buckets.size() == 0))
			return null;

		// if only one server, return it
		if ((this.hashingAlg == CONSISTENT_HASH && consistentBuckets.size() == 0)
				|| (buckets != null && buckets.size() == 1))
		{

			SockIO sock = (this.hashingAlg == CONSISTENT_HASH) ? getConnection(consistentBuckets
					.get(consistentBuckets.firstKey()))
					: getConnection(buckets.get(0));

			if (sock != null && sock.isConnected())
			{
				if (aliveCheck)
				{
					if (!sock.isAlive())
					{
						sock.close();
						try
						{
							if (socketPool.get(sock.getHost()) != null)
								socketPool.get(sock.getHost()).remove(sock);

							sock.trueClose();
						} catch (IOException ioe)
						{
							log.error("failed to close dead socket");
						}

						sock = null;
					}
				}
			} else
			{
				if (sock != null)
				{
					addSocketToPool(socketPool, sock.host, sock,SOCKET_STATUS_DEAD,
							SOCKET_STATUS_DEAD, true);
					// sock = null;
				}
			}

			return sock;
		}

		// from here on, we are working w/ multiple servers
		// keep trying different servers until we find one
		// making sure we only try each server one time
		Set<String> tryServers = new HashSet<String>(Arrays.asList(servers));

		// get initial bucket
		long bucket = getBucket(key, hashCode);
		String server = (this.hashingAlg == CONSISTENT_HASH) ? consistentBuckets
				.get(bucket)
				: buckets.get((int) bucket);

		while (!tryServers.isEmpty())
		{

			// try to get socket from bucket
			SockIO sock = getConnection(server);

			if (log.isDebugEnabled())
				log.debug("cache choose " + server + " for " + key);

			if (sock != null && sock.isConnected())
			{
				if (aliveCheck)
				{
					if (sock.isAlive())
					{
						return sock;
					} else
					{
						sock.close();
						try
						{

							if (socketPool.get(sock.getHost()) != null)
								socketPool.get(sock.getHost()).remove(sock);

							sock.trueClose();

						} catch (IOException ioe)
						{
							log.error("failed to close dead socket");
						}
						sock = null;
					}
				} else
				{
					return sock;
				}
			} else
			{
				if (sock != null)
				{
					addSocketToPool(socketPool, sock.host, sock,SOCKET_STATUS_DEAD,
							SOCKET_STATUS_DEAD, true);
					// sock = null;
				}
			}

			// if we do not want to failover, then bail here
			if (!failover)
				return null;

			// log that we tried
			tryServers.remove(server);

			if (tryServers.isEmpty())
				break;

			// if we failed to get a socket from this server
			// then we try again by adding an incrementer to the
			// current key and then rehashing
			int rehashTries = 0;
			while (!tryServers.contains(server))
			{

				String newKey = new StringBuilder().append(rehashTries).append(
						key).toString();

				// String.format( "%s%s", rehashTries, key );
				if (log.isDebugEnabled())
					log.debug("rehashing with: " + newKey);

				bucket = getBucket(newKey, null);
				server = (this.hashingAlg == CONSISTENT_HASH) ? consistentBuckets
						.get(bucket)
						: buckets.get((int) bucket);

				rehashTries++;
			}
		}

		return null;
	}

	/**
	 * Returns a SockIO object from the pool for the passed in host.
	 * 
	 * Meant to be called from a more intelligent method<br/> which handles
	 * choosing appropriate server<br/> and failover.
	 * 
	 * @param host
	 *            host from which to retrieve object
	 * @return SockIO object or null if fail to retrieve one
	 */
	public SockIO getConnection(String host)
	{

		if (!this.initialized)
		{
			log.error("attempting to get SockIO from uninitialized pool!");
			return null;
		}

		if (host == null)
			return null;

		// if we have items in the pool
		// then we can return it
		if (socketPool != null && !socketPool.isEmpty())
		{
			// take first connected socket
			Map<SockIO, Integer> aSockets = socketPool.get(host);
			
			//fast check
			SockIO socket = fastPool.get(host);
			if (socket != null)
			{
				if (isFreeSocket(socket,aSockets))
					return socket;
			}
			
			if (aSockets != null && !aSockets.isEmpty())
			{
				int start = (random.nextInt() % aSockets.size());
				
				if (start < 0)
					start*= -1;
				
				int count = 0;
				
				for (Iterator<SockIO> i = aSockets.keySet().iterator(); 
					i.hasNext();)
				{
					if (count < start)
					{
						i.next();
						count++;
						continue;
					}
					
					socket = i.next();
					
					if (isFreeSocket(socket,aSockets))
						return socket;
				}
				
				for (Iterator<SockIO> i = aSockets.keySet().iterator(); 
					i.hasNext();)
				{
					if (count > 0)
					{
						socket = i.next();
						if (isFreeSocket(socket,aSockets))
							return socket;
						
						count--;
					}
					else
						break;
					
				}
				
			}
		}

		// create one socket -- let the maint thread take care of creating more
		SockIO socket = createSocket(host);
		if (socket != null)
		{
			addSocketToPool(socketPool, host, socket,
					SOCKET_STATUS_BUSY,SOCKET_STATUS_BUSY, true);
		}

		return socket;
	}
	
	private boolean isFreeSocket(SockIO socket,Map<SockIO, Integer> socketMap)
	{	
		if (socket.isConnected())
		{
			if (socketMap.get(socket).intValue() == SOCKET_STATUS_ACTIVE)
			{

				if (!addSocketToPool(socketPool, socket.getHost(), socket
						,SOCKET_STATUS_ACTIVE,SOCKET_STATUS_BUSY, false))
					return false;

				if (log.isDebugEnabled())
					log.debug("++++ moving socket for host ("
							+ socket.getHost() + ") to busy pool ... socket: "
							+ socket);

				// return socket
				return true;

			}

		} else
		{
			// add to deadpool for later reaping
			addSocketToPool(socketPool, socket.getHost(), socket,SOCKET_STATUS_DEAD,
					SOCKET_STATUS_DEAD, true);
		}
		
		
		return false;
	}

	/**
	 * Adds a socket and value to a given pool for the given host.
	 * 
	 * @param pool
	 *            pool to add to
	 * @param host
	 *            host this socket is connected to
	 * @param socket
	 *            socket to add
	 */
	protected <T> boolean addSocketToPool(
			ConcurrentMap<String, ConcurrentMap<SockIO, T>> pool, String host,
			SockIO socket, T oldValue,T newValue, boolean needReplace)
	{
		boolean result = false;

		ConcurrentMap<SockIO, T> sockets;

		if (!pool.containsKey(host))
		{
			sockets = new ConcurrentHashMap<SockIO, T>();
			pool.putIfAbsent(host, sockets);
		}

		sockets = pool.get(host);

		if (sockets != null)
		{
			if (needReplace)
			{
				sockets.put(socket, newValue);
				result = true;
			} 
			else
			{
				return sockets.replace(socket, oldValue, newValue);
			}
		}

		return result;
	}

	/**
	 * @param host
	 * @param socket
	 * @param oldStatus
	 * @param newStatus
	 */
	protected void updateStatusPool(String host, SockIO socket, int newStatus)
	{
		if (socketPool.containsKey(host))
			socketPool.get(host).replace(socket, newStatus);
	}

	/**
	 * Closes and removes all sockets from specified pool for host. THIS METHOD
	 * IS NOT THREADSAFE, SO BE CAREFUL WHEN USING!
	 * 
	 * Internal utility method.
	 * 
	 * @param pool
	 *            pool to clear
	 * @param host
	 *            host to clear
	 */
	protected void clearHostFromPool(String host)
	{
		Map<SockIO, Integer> sockets = socketPool.remove(host);

		if (sockets != null)
		{

			if (sockets.size() > 0)
			{
				for (SockIO socket : sockets.keySet())
				{
					sockets.remove(socket);

					try
					{
						socket.trueClose();
					} catch (IOException ioe)
					{
						log.error("++++ failed to close socket: "
								+ ioe.getMessage());
					}

					socket = null;
				}
			}

		}
	}

	/**
	 * Checks a SockIO object in with the pool.
	 * 
	 * This will remove SocketIO from busy pool, and optionally<br/> add to
	 * avail pool.
	 * 
	 * @param socket
	 *            socket to return
	 * @param addToAvail
	 *            add to avail pool if true
	 */
	private void checkIn(SockIO socket, boolean addToAvail)
	{

		String host = socket.getHost();
		if (log.isDebugEnabled())
			log.debug("++++ calling check-in on socket: " + socket.toString()
					+ " for host: " + host);

		// remove from the busy pool
		if (log.isDebugEnabled())
			log.debug("++++ removing socket (" + socket.toString()
					+ ") from busy pool for host: " + host);

		if (socketPool.containsKey(host)
				&& socketPool.get(host).containsKey(socket)
				&& socketPool.get(host).get(socket) == SOCKET_STATUS_BUSY)
		{
			addSocketToPool(socketPool, host, socket,
					SOCKET_STATUS_ACTIVE,SOCKET_STATUS_ACTIVE,true);
			
			fastPool.put(host, socket);
		}

		if (socket.isConnected() && addToAvail)
		{
			// add to avail pool
			if (log.isDebugEnabled())
				log.debug("++++ returning socket (" + socket.toString()
						+ " to avail pool for host: " + host);
		} else
		{
			addSocketToPool(socketPool, host, socket
					,SOCKET_STATUS_DEAD,SOCKET_STATUS_DEAD, true);
			// socket = null;
		}

	}

	/**
	 * Returns a socket to the avail pool.
	 * 
	 * This is called from SockIO.close(). Calling this method<br/> directly
	 * without closing the SockIO object first<br/> will cause an IOException
	 * to be thrown.
	 * 
	 * @param socket
	 *            socket to return
	 */
	private void checkIn(SockIO socket)
	{
		checkIn(socket, true);
	}

	/**
	 * Closes all sockets in the passed in pool.
	 * 
	 * Internal utility method.
	 * 
	 * @param pool
	 *            pool to close
	 */
	protected void closeSocketPool()
	{
		for (Iterator<String> i = socketPool.keySet().iterator(); i.hasNext();)
		{
			String host = i.next();
			Map<SockIO, Integer> sockets = socketPool.get(host);

			for (SockIO socket : sockets.keySet())
			{
				sockets.remove(socket);

				try
				{
					socket.trueClose(false);
				} catch (IOException ioe)
				{
					log.error("++++ failed to trueClose socket: "
							+ socket.toString() + " for host: " + host);
				}

				socket = null;
			}
		}
	}

	/**
	 * Shuts down the pool.
	 * 
	 * Cleanly closes all sockets.<br/> Stops the maint thread.<br/> Nulls out
	 * all internal maps<br/>
	 */
	public void shutDown()
	{
		if (log.isDebugEnabled())
			log.debug("++++ SockIOPool shutting down...");

		if (maintThread != null && maintThread.isRunning())
		{
			// stop the main thread
			stopMaintThread();

			// wait for the thread to finish
			while (maintThread.isRunning())
			{
				if (log.isDebugEnabled())
					log.debug("++++ waiting for main thread to finish run +++");
				try
				{
					Thread.sleep(500);
				} catch (Exception ex)
				{
				}
			}
		}

		if (log.isDebugEnabled())
			log.debug("++++ closing all internal pools.");
		closeSocketPool();

		socketPool.clear();
		fastPool.clear();
		socketPool = null;
		fastPool = null;
		buckets = null;
		consistentBuckets = null;
		hostDeadDur = null;
		hostDead = null;
		maintThread = null;
		initialized = false;
		if (log.isDebugEnabled())
			log.debug("++++ SockIOPool finished shutting down.");

	}

	/**
	 * Starts the maintenance thread.
	 * 
	 * This thread will manage the size of the active pool<br/> as well as move
	 * any closed, but not checked in sockets<br/> back to the available pool.
	 */
	protected void startMaintThread()
	{

		if (maintThread != null)
		{

			if (maintThread.isRunning())
			{
				log.error("main thread already running");
			} else
			{
				maintThread.start();
			}
		} else
		{
			maintThread = new MaintThread(this);
			maintThread.setInterval(this.maintSleep);
			maintThread.start();
		}
	}

	/**
	 * Stops the maintenance thread.
	 */
	protected void stopMaintThread()
	{
		if (maintThread != null && maintThread.isRunning())
			maintThread.stopThread();
	}

	/**
	 * Runs self maintenance on all internal pools.
	 * 
	 * This is typically called by the maintenance thread to manage pool size.
	 */
	protected void selfMaint()
	{
		if (log.isDebugEnabled())
			log.debug("++++ Starting self maintenance....");

		// go through avail sockets and create sockets
		// as needed to maintain pool settings
		Map<String, Integer> needSockets = new HashMap<String, Integer>();

		// find out how many to create
		for (Iterator<String> i = socketPool.keySet().iterator(); i.hasNext();)
		{
			String host = i.next();
			ConcurrentMap<SockIO, Integer> sockets = socketPool.get(host);

			int usedcount = 0;
			
			if (sockets == null)
			{
				sockets = new ConcurrentHashMap<SockIO, Integer>();
				socketPool.putIfAbsent(host, sockets); 
				sockets = socketPool.get(host);
			}

			if (log.isDebugEnabled())
				log.debug("++++ Size of avail pool for host (" + host + ") = "
						+ sockets.size());

			// if pool is too small (n < minSpare)
			if (sockets != null && sockets.size() < minConn)
			{
				// need to create new sockets
				int need = minConn - sockets.size();
				needSockets.put(host, need);
			}
		}

		// now create
		for (String host : needSockets.keySet())
		{
			Integer need = needSockets.get(host);

			if (log.isDebugEnabled())
				log.debug("++++ Need to create " + need
						+ " new sockets for pool for host: " + host);

			for (int j = 0; j < need; j++)
			{
				SockIO socket = createSocket(host);

				if (socket == null)
					break;

				addSocketToPool(socketPool, host, socket
						,SOCKET_STATUS_ACTIVE,SOCKET_STATUS_ACTIVE, true);
			}

		}

		for (Iterator<String> i = socketPool.keySet().iterator(); i.hasNext();)
		{
			String host = i.next();
			ConcurrentMap<SockIO, Integer> sockets = socketPool.get(host);

			if (log.isDebugEnabled())
				log.debug("++++ Size of avail pool for host (" + host + ") = "
						+ sockets.size());

			int active = 0;
			
			if (sockets == null)
			{
				sockets = new ConcurrentHashMap<SockIO, Integer>();
				socketPool.putIfAbsent(host, sockets); 
				sockets = socketPool.get(host);
			}
			
			Iterator<Integer> iter =  sockets.values().iterator();
			
			while(iter.hasNext())
			{
				if(iter.next() == SOCKET_STATUS_ACTIVE)
					active++;
			}

			if (sockets != null && (active > maxConn))
			{
				// need to close down some sockets
				int diff = active - maxConn;
				int needToClose = (diff <= poolMultiplier) ? diff : (diff)
						/ poolMultiplier;

				if (log.isDebugEnabled())
					log.debug("++++ need to remove " + needToClose
							+ " spare sockets for pool for host: " + host);

				for (Iterator<SockIO> j = sockets.keySet().iterator(); j
						.hasNext();)
				{
					if (needToClose <= 0)
						break;

					// remove stale entries
					SockIO socket = j.next();

					// remove from the availPool
					if (sockets.get(socket) == SOCKET_STATUS_ACTIVE)
					{
						if (addSocketToPool(socketPool, host, socket
								,SOCKET_STATUS_ACTIVE,SOCKET_STATUS_DEAD, false))
							needToClose--;
					}
					
				}
			}
		}

		// finally clean out the deadPool
		for (Iterator<String> i = socketPool.keySet().iterator(); i.hasNext();)
		{
			String host = i.next();
			ConcurrentMap<SockIO, Integer> sockets = socketPool.get(host);

			// loop through all connections and check to see if we have any hung
			// connections
			
			for (Iterator<SockIO> j = sockets.keySet().iterator(); j.hasNext();)
			{
				// remove stale entries
				SockIO socket = j.next();
				
				try
				{
					Integer status = null;
					if (sockets != null && socket != null)
						status = sockets.get(socket);				
					
					if (status != null && status.intValue() == SOCKET_STATUS_DEAD)
					{
						
						if (socketPool.containsKey(host))
							socketPool.get(host).remove(socket);

						socket.trueClose(false);
						
						socket = null;
					}
				} catch (Exception ex)
				{
					log.error("++++ failed to close SockIO obj from deadPool");
					log.error(ex.getMessage(), ex);
				}
			}
		}

		if (log.isDebugEnabled())
			log.debug("+++ ending self maintenance.");

	}

	/**
	 * Returns state of pool.
	 * 
	 * @return <CODE>true</CODE> if initialized.
	 */
	public boolean isInitialized()
	{
		return initialized;
	}

	/**
	 * Sets the list of all cache servers.
	 * 
	 * @param servers
	 *            String array of servers [host:port]
	 */
	public void setServers(String[] servers)
	{
		this.servers = servers;
	}

	/**
	 * Returns the current list of all cache servers.
	 * 
	 * @return String array of servers [host:port]
	 */
	public String[] getServers()
	{
		return this.servers;
	}

	/**
	 * Sets the list of weights to apply to the server list.
	 * 
	 * This is an int array with each element corresponding to an element<br/>
	 * in the same position in the server String array.
	 * 
	 * @param weights
	 *            Integer array of weights
	 */
	public void setWeights(Integer[] weights)
	{
		this.weights = weights;
	}

	/**
	 * Returns the current list of weights.
	 * 
	 * @return int array of weights
	 */
	public Integer[] getWeights()
	{
		return this.weights;
	}

	/**
	 * Sets the initial number of connections per server in the available pool.
	 * 
	 * @param initConn
	 *            int number of connections
	 */
	public void setInitConn(int initConn)
	{
		this.initConn = initConn;
	}

	/**
	 * Returns the current setting for the initial number of connections per
	 * server in the available pool.
	 * 
	 * @return number of connections
	 */
	public int getInitConn()
	{
		return this.initConn;
	}

	/**
	 * Sets the minimum number of spare connections to maintain in our available
	 * pool.
	 * 
	 * @param minConn
	 *            number of connections
	 */
	public void setMinConn(int minConn)
	{
		this.minConn = minConn;
	}

	/**
	 * Returns the minimum number of spare connections in available pool.
	 * 
	 * @return number of connections
	 */
	public int getMinConn()
	{
		return this.minConn;
	}

	/**
	 * Sets the maximum number of spare connections allowed in our available
	 * pool.
	 * 
	 * @param maxConn
	 *            number of connections
	 */
	public void setMaxConn(int maxConn)
	{
		this.maxConn = maxConn;
	}

	/**
	 * Returns the maximum number of spare connections allowed in available
	 * pool.
	 * 
	 * @return number of connections
	 */
	public int getMaxConn()
	{
		return this.maxConn;
	}

	/**
	 * Sets the max idle time for threads in the available pool.
	 * 
	 * @param maxIdle
	 *            idle time in ms
	 */
	public void setMaxIdle(long maxIdle)
	{
		this.maxIdle = maxIdle;
	}

	/**
	 * Returns the current max idle setting.
	 * 
	 * @return max idle setting in ms
	 */
	public long getMaxIdle()
	{
		return this.maxIdle;
	}

	/**
	 * Sets the max busy time for threads in the busy pool.
	 * 
	 * @param maxBusyTime
	 *            idle time in ms
	 */
	public void setMaxBusyTime(long maxBusyTime)
	{
		this.maxBusyTime = maxBusyTime;
	}

	/**
	 * Returns the current max busy setting.
	 * 
	 * @return max busy setting in ms
	 */
	public long getMaxBusy()
	{
		return this.maxBusyTime;
	}

	/**
	 * Set the sleep time between runs of the pool maintenance thread. If set to
	 * 0, then the maint thread will not be started.
	 * 
	 * @param maintSleep
	 *            sleep time in ms
	 */
	public void setMaintSleep(long maintSleep)
	{
		this.maintSleep = maintSleep;
	}

	/**
	 * Returns the current maint thread sleep time.
	 * 
	 * @return sleep time in ms
	 */
	public long getMaintSleep()
	{
		return this.maintSleep;
	}

	/**
	 * Sets the socket timeout for reads.
	 * 
	 * @param socketTO
	 *            timeout in ms
	 */
	public void setSocketTO(int socketTO)
	{
		this.socketTO = socketTO;
	}

	/**
	 * Returns the socket timeout for reads.
	 * 
	 * @return timeout in ms
	 */
	public int getSocketTO()
	{
		return this.socketTO;
	}

	/**
	 * Sets the socket timeout for connect.
	 * 
	 * @param socketConnectTO
	 *            timeout in ms
	 */
	public void setSocketConnectTO(int socketConnectTO)
	{
		this.socketConnectTO = socketConnectTO;
	}

	/**
	 * Returns the socket timeout for connect.
	 * 
	 * @return timeout in ms
	 */
	public int getSocketConnectTO()
	{
		return this.socketConnectTO;
	}

	/**
	 * Sets the failover flag for the pool.
	 * 
	 * If this flag is set to true, and a socket fails to connect,<br/> the
	 * pool will attempt to return a socket from another server<br/> if one
	 * exists. If set to false, then getting a socket<br/> will return null if
	 * it fails to connect to the requested server.
	 * 
	 * @param failover
	 *            true/false
	 */
	public void setFailover(boolean failover)
	{
		this.failover = failover;
	}

	/**
	 * Returns current state of failover flag.
	 * 
	 * @return true/false
	 */
	public boolean getFailover()
	{
		return this.failover;
	}

	/**
	 * Sets the failback flag for the pool.
	 * 
	 * If this is true and we have marked a host as dead, will try to bring it
	 * back. If it is false, we will never try to resurrect a dead host.
	 * 
	 * @param failback
	 *            true/false
	 */
	public void setFailback(boolean failback)
	{
		this.failback = failback;
	}

	/**
	 * Returns current state of failover flag.
	 * 
	 * @return true/false
	 */
	public boolean getFailback()
	{
		return this.failback;
	}

	/**
	 * Sets the aliveCheck flag for the pool.
	 * 
	 * When true, this will attempt to talk to the server on every connection
	 * checkout to make sure the connection is still valid. This adds extra
	 * network chatter and thus is defaulted off. May be useful if you want to
	 * ensure you do not have any problems talking to the server on a dead
	 * connection.
	 * 
	 * @param aliveCheck
	 *            true/false
	 */
	public void setAliveCheck(boolean aliveCheck)
	{
		this.aliveCheck = aliveCheck;
	}

	/**
	 * Returns the current status of the aliveCheck flag.
	 * 
	 * @return true / false
	 */
	public boolean getAliveCheck()
	{
		return this.aliveCheck;
	}

	/**
	 * Sets the Nagle alg flag for the pool.
	 * 
	 * If false, will turn off Nagle's algorithm on all sockets created.
	 * 
	 * @param nagle
	 *            true/false
	 */
	public void setNagle(boolean nagle)
	{
		this.nagle = nagle;
	}

	/**
	 * Returns current status of nagle flag
	 * 
	 * @return true/false
	 */
	public boolean getNagle()
	{
		return this.nagle;
	}

	/**
	 * Sets the hashing algorithm we will use.
	 * 
	 * The types are as follows.
	 * 
	 * SockIOPool.NATIVE_HASH (0) - native String.hashCode() - fast (cached) but
	 * not compatible with other clients SockIOPool.OLD_COMPAT_HASH (1) -
	 * original compatibility hashing alg (works with other clients)
	 * SockIOPool.NEW_COMPAT_HASH (2) - new CRC32 based compatibility hashing
	 * algorithm (fast and works with other clients)
	 * 
	 * @param alg
	 *            int value representing hashing algorithm
	 */
	public void setHashingAlg(int alg)
	{
		this.hashingAlg = alg;
	}

	/**
	 * Returns current status of customHash flag
	 * 
	 * @return true/false
	 */
	public int getHashingAlg()
	{
		return this.hashingAlg;
	}

	/**
	 * Internal private hashing method.
	 * 
	 * This is the original hashing algorithm from other clients. Found to be
	 * slow and have poor distribution.
	 * 
	 * @param key
	 *            String to hash
	 * @return hashCode for this string using our own hashing algorithm
	 */
	private static long origCompatHashingAlg(String key)
	{
		long hash = 0;
		char[] cArr = key.toCharArray();

		for (int i = 0; i < cArr.length; ++i)
		{
			hash = (hash * 33) + cArr[i];
		}

		return hash;
	}

	/**
	 * Internal private hashing method.
	 * 
	 * This is the new hashing algorithm from other clients. Found to be fast
	 * and have very good distribution.
	 * 
	 * UPDATE: This is dog slow under java
	 * 
	 * @param key
	 * @return
	 */
	private static long newCompatHashingAlg(String key)
	{
		CRC32 checksum = new CRC32();
		checksum.update(key.getBytes());
		long crc = checksum.getValue();
		return (crc >> 16) & 0x7fff;
	}

	/**
	 * Internal private hashing method.
	 * 
	 * MD5 based hash algorithm for use in the consistent hashing approach.
	 * 
	 * @param key
	 * @return
	 */
	private static long md5HashingAlg(String key)
	{
		MessageDigest md5 = MD5.get();
		md5.reset();
		md5.update(key.getBytes());
		byte[] bKey = md5.digest();
		long res = ((long) (bKey[3] & 0xFF) << 24)
				| ((long) (bKey[2] & 0xFF) << 16)
				| ((long) (bKey[1] & 0xFF) << 8) | (long) (bKey[0] & 0xFF);
		return res;
	}

	/**
	 * Returns a bucket to check for a given key.
	 * 
	 * @param key
	 *            String key cache is stored under
	 * @return int bucket
	 */
	private long getHash(String key, Integer hashCode)
	{

		if (hashCode != null)
		{
			if (hashingAlg == CONSISTENT_HASH)
				return hashCode.longValue() & 0xffffffffL;
			else
				return hashCode.longValue();
		} else
		{
			switch (hashingAlg)
			{
			case NATIVE_HASH:
				return (long) key.hashCode();
			case OLD_COMPAT_HASH:
				return origCompatHashingAlg(key);
			case NEW_COMPAT_HASH:
				return newCompatHashingAlg(key);
			case CONSISTENT_HASH:
				return md5HashingAlg(key);
			default:
				// use the native hash as a default
				hashingAlg = NATIVE_HASH;
				return (long) key.hashCode();
			}
		}
	}

	private long getBucket(String key, Integer hashCode)
	{
		long hc = getHash(key, hashCode);

		if (this.hashingAlg == CONSISTENT_HASH)
		{
			return findPointFor(hc);
		} else
		{
			long bucket = hc % buckets.size();
			if (bucket < 0)
				bucket *= -1;
			return bucket;
		}
	}

	/**
	 * Gets the first available key equal or above the given one, if none found,
	 * returns the first k in the bucket
	 * 
	 * @param k
	 *            key
	 * @return
	 */
	private Long findPointFor(Long hv)
	{
		// this works in java 6, but still want to release support for java5
		// Long k = this.consistentBuckets.ceilingKey( hv );
		// return ( k == null ) ? this.consistentBuckets.firstKey() : k;

		SortedMap<Long, String> tmap = this.consistentBuckets.tailMap(hv);

		return (tmap.isEmpty()) ? this.consistentBuckets.firstKey() : tmap
				.firstKey();
	}
	

	/**
	 * Class which extends thread and handles maintenance of the pool.
	 * 
	 * @author greg whalin <greg@meetup.com>
	 * @version 1.5
	 */
	protected static class MaintThread extends Thread
	{

		// logger
		private static Logger log = Logger.getLogger(MaintThread.class
				.getName());

		private SockIOPool pool;
		private long interval = 1000 * 3; // every 3 seconds
		private boolean stopThread = false;
		private boolean running;

		protected MaintThread(SockIOPool pool)
		{
			this.pool = pool;
			this.setDaemon(true);
			this.setName("MaintThread");
		}

		public void setInterval(long interval)
		{
			this.interval = interval;
		}

		public boolean isRunning()
		{
			return this.running;
		}

		/**
		 * sets stop variable and interupts any wait
		 */
		public void stopThread()
		{
			this.stopThread = true;
			this.interrupt();
		}

		/**
		 * Start the thread.
		 */
		public void run()
		{
			this.running = true;

			while (!this.stopThread)
			{
				try
				{
					Thread.sleep(interval);

					// if pool is initialized, then
					// run the maintenance method on itself
					if (pool.isInitialized())
						pool.selfMaint();

				} catch (Exception e)
				{
					if (e instanceof java.lang.InterruptedException)
						log.info("MaintThread stop !");
					else
						log.error("MaintThread error !", e);
					break;
				}
			}

			this.running = false;
		}
	}

	/**
	 * MemCached Java client, utility class for Socket IO.
	 * 
	 * This class is a wrapper around a Socket and its streams.
	 * 
	 * @author greg whalin <greg@meetup.com>
	 * @author Richard 'toast' Russo <russor@msoe.edu>
	 * @version 1.5
	 */
	public static class SockIO
	{

		// logger
		private static Logger log = Logger.getLogger(SockIO.class.getName());

		// pool
		private SockIOPool pool;

		// data
		private String host;
		private Socket sock;

		private DataInputStream in;
		private BufferedOutputStream out;
		
		private byte[] recBuf;
		
		private int recBufSize = 1028;
		private int recIndex = 0;
		//�ж��Ƿ���Ҫ������Ӵ��ڿ���״̬
		private long aliveTimeStamp = 0;

		/**
		 * creates a new SockIO object wrapping a socket connection to
		 * host:port, and its input and output streams
		 * 
		 * @param pool
		 *            Pool this object is tied to
		 * @param host
		 *            host to connect to
		 * @param port
		 *            port to connect to
		 * @param timeout
		 *            int ms to block on data for read
		 * @param connectTimeout
		 *            timeout (in ms) for initial connection
		 * @param noDelay
		 *            TCP NODELAY option?
		 * @throws IOException
		 *             if an io error occurrs when creating socket
		 * @throws UnknownHostException
		 *             if hostname is invalid
		 */
		public SockIO(SockIOPool pool, String host, int port, int timeout,
				int connectTimeout, boolean noDelay) throws IOException,
				UnknownHostException
		{

			this.pool = pool;
			
			recBuf = new byte[recBufSize];

			// get a socket channel
			sock = getSocket(host, port, connectTimeout);

			if (timeout >= 0)
				sock.setSoTimeout(timeout);

			// testing only
			sock.setTcpNoDelay(noDelay);

			// wrap streams
			in = new DataInputStream(sock.getInputStream());
			out = new BufferedOutputStream(sock.getOutputStream());

			this.host = host + ":" + port;
		}

		/**
		 * creates a new SockIO object wrapping a socket connection to
		 * host:port, and its input and output streams
		 * 
		 * @param host
		 *            hostname:port
		 * @param timeout
		 *            read timeout value for connected socket
		 * @param connectTimeout
		 *            timeout for initial connections
		 * @param noDelay
		 *            TCP NODELAY option?
		 * @throws IOException
		 *             if an io error occurrs when creating socket
		 * @throws UnknownHostException
		 *             if hostname is invalid
		 */
		public SockIO(SockIOPool pool, String host, int timeout,
				int connectTimeout, boolean noDelay) throws IOException,
				UnknownHostException
		{

			this.pool = pool;

			recBuf = new byte[recBufSize];
			
			//String[] ip = host.split(":");
			int index = host.indexOf(":");
			
			if(index <= 0)
				throw new RuntimeException(new StringBuilder().append("host :")
						.append(host).append(" is error,check config file!").toString());
			
			// get socket: default is to use non-blocking connect
			sock = getSocket(host.substring(0,index), Integer.parseInt(host.substring(index+1)), connectTimeout);

			if (timeout >= 0)
				this.sock.setSoTimeout(timeout);

			// testing only
			sock.setTcpNoDelay(noDelay);

			// wrap streams
			in = new DataInputStream(sock.getInputStream());
			out = new BufferedOutputStream(sock.getOutputStream());

			this.host = host;
		}

		/**
		 * Method which gets a connection from SocketChannel.
		 * 
		 * @param host
		 *            host to establish connection to
		 * @param port
		 *            port on that host
		 * @param timeout
		 *            connection timeout in ms
		 * 
		 * @return connected socket
		 * @throws IOException
		 *             if errors connecting or if connection times out
		 */
		protected static Socket getSocket(String host, int port, int timeout)
				throws IOException
		{
			SocketChannel sock = SocketChannel.open();
			sock.socket().connect(new InetSocketAddress(host, port), timeout);
			return sock.socket();
		}

		/**
		 * Lets caller get access to underlying channel.
		 * 
		 * @return the backing SocketChannel
		 */
		public SocketChannel getChannel()
		{
			return sock.getChannel();
		}

		/**
		 * returns the host this socket is connected to
		 * 
		 * @return String representation of host (hostname:port)
		 */
		public String getHost()
		{
			return this.host;
		}
		
		

		/**
		 * closes socket and all streams connected to it
		 * 
		 * @throws IOException
		 *             if fails to close streams or socket
		 */
		public void trueClose() throws IOException
		{
			trueClose(true);
		}

		/**
		 * closes socket and all streams connected to it
		 * 
		 * @throws IOException
		 *             if fails to close streams or socket
		 */
		public void trueClose(boolean addToDeadPool) throws IOException
		{
			if (log.isDebugEnabled())
				log.debug("++++ Closing socket for real: " + toString());
			
			//alive state clear
			aliveTimeStamp = 0;
			
			recBuf = new byte[recBufSize];
			recIndex = 0;

			boolean err = false;
			StringBuilder errMsg = new StringBuilder();

			if (in == null || out == null || sock == null)
			{
				err = true;
				errMsg
						.append("++++ socket or its streams already null in trueClose call");
			}

			if (in != null)
			{
				try
				{
					in.close();
				} catch (IOException ioe)
				{
					log.error("++++ error closing input stream for socket: "
							+ toString() + " for host: " + getHost());
					log.error(ioe.getMessage(), ioe);
					errMsg
							.append("++++ error closing input stream for socket: "
									+ toString()
									+ " for host: "
									+ getHost()
									+ "\n");
					errMsg.append(ioe.getMessage());
					err = true;
				}
			}

			if (out != null)
			{
				try
				{
					out.close();
				} catch (IOException ioe)
				{
					log.error("++++ error closing output stream for socket: "
							+ toString() + " for host: " + getHost());
					log.error(ioe.getMessage(), ioe);
					errMsg
							.append("++++ error closing output stream for socket: "
									+ toString()
									+ " for host: "
									+ getHost()
									+ "\n");
					errMsg.append(ioe.getMessage());
					err = true;
				}
			}

			if (sock != null)
			{
				try
				{
					sock.close();
				} catch (IOException ioe)
				{
					log.error("++++ error closing socket: " + toString()
							+ " for host: " + getHost());
					log.error(ioe.getMessage(), ioe);
					errMsg.append("++++ error closing socket: " + toString()
							+ " for host: " + getHost() + "\n");
					errMsg.append(ioe.getMessage());
					err = true;
				}
			}

			// check in to pool
			if (addToDeadPool && sock != null)
				pool.checkIn(this, false);

			in = null;
			out = null;
			sock = null;

			if (err)
				throw new IOException(errMsg.toString());
		}

		/**
		 * sets closed flag and checks in to connection pool but does not close
		 * connections
		 */
		void close()
		{
			// check in to pool
			if (log.isDebugEnabled())
				log.debug("++++ marking socket (" + this.toString()
						+ ") as closed and available to return to avail pool");
			
			recBuf = new byte[recBufSize];
			recIndex = 0;
			
			pool.checkIn(this);
		}

		/**
		 * checks if the connection is open
		 * 
		 * @return true if connected
		 */
		boolean isConnected()
		{
			return (sock != null && sock.isConnected());
		}

		/*
		 * checks to see that the connection is still working
		 * 
		 * @return true if still alive
		 */
		boolean isAlive()
		{

			if (!isConnected())
			{
				//alive state clear
				aliveTimeStamp = 0;			
				return false;
			}
				

			// try to talk to the server w/ a dumb query to ask its version
			boolean needcheck = true;
			
			if (aliveTimeStamp > 0)
			{
				long interval = System.currentTimeMillis() - aliveTimeStamp;
				
				if (interval < 100)
					needcheck = false;
			}
				
			
			if (needcheck)
			{
				try
				{
					this.write(B_VERSION);
					this.flush();
					this.readLine();
					
					//update alive state
					aliveTimeStamp = System.currentTimeMillis();
					
				} catch (IOException ex)
				{
					return false;
				}
			}
			

			return true;
		}
		
		
		public byte[] readBytes(int length) throws IOException
		{
			if (sock == null || !sock.isConnected())
			{
				log.error("++++ attempting to read from closed socket");
				throw new IOException(
						"++++ attempting to read from closed socket");
			}

			byte[] result = null;
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			
			if (recIndex >= length)
			{
				bos.write(recBuf,0,length);

				byte[] newBuf = new byte[recBufSize];
				
				if (recIndex > length)
					System.arraycopy(recBuf,length,newBuf,0,recIndex - length);
				
				recBuf = newBuf;
				recIndex = recIndex - length;
			}
			else
			{
				int totalread = length;
				
				if (recIndex > 0)
				{
					totalread = totalread - recIndex;
					bos.write(recBuf,0,recIndex);
					
					recBuf = new byte[recBufSize];
					recIndex = 0;
				}
				
				int readCount = 0;
				
				while(totalread > 0)
				{
					if ((readCount = in.read(recBuf)) > 0)
					{
						if (totalread > readCount)
						{
							bos.write(recBuf,0,readCount);
							recBuf = new byte[recBufSize];
							recIndex = 0;
						}
						else
						{
							bos.write(recBuf,0,totalread);
							byte[] newBuf = new byte[recBufSize];
							System.arraycopy(recBuf,totalread,newBuf,0,readCount - totalread);
							
							recBuf = newBuf;
							recIndex = readCount - totalread;
						}
						
						totalread = totalread - readCount;
					}
				}
				
			}
			
			result = bos.toByteArray();
			
			if (result == null || (result != null && result.length <= 0 && recIndex <= 0))
			{
				throw new IOException(
						"++++ Stream appears to be dead, so closing it down");
			}
			
			//update alive state
			aliveTimeStamp = System.currentTimeMillis();

			return result;
		}

		/**
		 * reads a line intentionally not using the deprecated readLine method
		 * from DataInputStream
		 * 
		 * @return String that was read in
		 * @throws IOException
		 *             if io problems during read
		 */
		public String readLine() throws IOException
		{
			if (sock == null || !sock.isConnected())
			{
				log.error("++++ attempting to read from closed socket");
				throw new IOException(
						"++++ attempting to read from closed socket");
			}

			String result = null;
			//ByteArrayOutputStream bos = new ByteArrayOutputStream();
			StringBuilder content = new StringBuilder();
			int readCount = 0;
			
			// some recbuf releave
			if (recIndex > 0 && read(content))
			{
				return content.toString();
			}
			
			while((readCount = in.read(recBuf,recIndex,recBuf.length - recIndex)) > 0)
			{
				recIndex = recIndex + readCount;
				
				if (read(content))
					break;		
			}
					
			result = content.toString();
			
			if (result == null || (result != null && result.length() <= 0 && recIndex <= 0))
			{
				throw new IOException(
						"++++ Stream appears to be dead, so closing it down");
			}
			
			//update alive state
			aliveTimeStamp = System.currentTimeMillis();

			return result;

		}
		
		private boolean read(StringBuilder strBuilder)
		{
			boolean result = false;
			int index = -1;
			
			for(int i = 0 ; i < recIndex-1; i++)
			{
				if(recBuf[i] == 13 && recBuf[i+1] == 10)
				{
					index = i;
					break;
				}
			}
			
			if (index >= 0)
			{
				strBuilder.append(new String(recBuf,0,index));
				//bos.write(recBuf,0,index);

				byte[] newBuf = new byte[recBufSize];
				
				if (recIndex > index+2)
					System.arraycopy(recBuf,index+2,newBuf,0,recIndex-index-2);
				
				recBuf = newBuf;
				recIndex = recIndex-index-2;

				result = true;
			}
			else
			{
				//�߽����
				if (recBuf[recIndex-1] == 13)
				{
					strBuilder.append(new String(recBuf,0,recIndex-1));
					//bos.write(recBuf,0,recIndex-1);
					recBuf = new byte[recBufSize];
					recBuf[0] = 13;
					recIndex = 1;
				}
				else
				{
					strBuilder.append(new String(recBuf,0,recIndex));
					//bos.write(recBuf,0,recIndex);
					recBuf = new byte[recBufSize];
					recIndex = 0;
				}
				
			}
			
			return result;
		}
		

		/**
		 * flushes output stream
		 * 
		 * @throws IOException
		 *             if io problems during read
		 */
		void flush() throws IOException
		{
			if (sock == null || !sock.isConnected())
			{
				log.error("++++ attempting to write to closed socket");
				throw new IOException(
						"++++ attempting to write to closed socket");
			}
			out.flush();
		}

		/**
		 * writes a byte array to the output stream
		 * 
		 * @param b
		 *            byte array to write
		 * @throws IOException
		 *             if an io error happens
		 */
		void write(byte[] b) throws IOException
		{
			if (sock == null || !sock.isConnected())
			{
				log.error("++++ attempting to write to closed socket");
				throw new IOException(
						"++++ attempting to write to closed socket");
			}
			out.write(b);
		}

		/**
		 * use the sockets hashcode for this object so we can key off of SockIOs
		 * 
		 * @return int hashcode
		 */
		public int hashCode()
		{
			return (sock == null) ? 0 : sock.hashCode();
		}

		/**
		 * returns the string representation of this socket
		 * 
		 * @return string
		 */
		public String toString()
		{
			return (sock == null) ? "" : sock.toString();
		}

		/**
		 * Hack to reap any leaking children.
		 */
		protected void finalize() throws Throwable
		{
			try
			{
				if (sock != null)
				{
					log
							.error("++++ closing potentially leaked socket in finalize");
					sock.close();
					sock = null;
				}
			} catch (Throwable t)
			{
				log.error(t.getMessage(), t);

			} finally
			{
				super.finalize();
			}
		}
		
	}

}
