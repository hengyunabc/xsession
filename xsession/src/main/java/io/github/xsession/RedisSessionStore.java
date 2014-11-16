package io.github.xsession;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * 支持以下的配置项：
 * 
 * <pre>
 * redisSessionStore.largeValueDetectSize    session存储的内容如果太太，会打印警告信息，默认是1M
 * redisSessionStore.sessionKeyPrefix        sessionId存储到redis上的key的前缀，默认是 __s|
 * redisSessionStore.jedis.address           redis集群的地址，如：127.0.0.1:6379,192.168.66.66:6379
 * </pre>
 * 
 * TODO 增加其它的一些配置？比如超时等
 * 
 * @author hengyunabc
 * 
 */
public class RedisSessionStore implements SessionStore {
	static final Logger logger = LoggerFactory.getLogger(RedisSessionStore.class);

	static final String DEFAULT_SESSIONKEY_PREFIX = "__s|";

	ShardedJedisPool jedisPool;

	Serializer serializer = new JavaSerializer();

	Charset utf8 = Charset.forName("utf8");

	String sessionKeyPrefix = DEFAULT_SESSIONKEY_PREFIX;

	/**
	 * 如果value的体积太大，则打印警告日志
	 */
	long largeValueDetectSize = 1024 * 1024;

	int sessionIdLength = XSessionFilter.DEFAULT_SESSIONID_LENGTH;

	@Override
	public void init(Properties properties) {
		String temp_sessionIdLength = (String) properties.get("sessionIdLength");
		if (temp_sessionIdLength != null) {
			this.sessionIdLength = Integer.parseInt(temp_sessionIdLength);
		}

		String detectSizeString = (String) properties.get("redisSessionStore.largeValueDetectSize");
		if (detectSizeString != null) {
			largeValueDetectSize = Long.parseLong(detectSizeString);
		}

		String temp_sessionKeyPrefix = (String) properties.get("redisSessionStore.sessionKeyPrefix");
		if (temp_sessionKeyPrefix != null) {
			this.sessionKeyPrefix = temp_sessionKeyPrefix;
		}

		String address = (String) properties.get("redisSessionStore.jedis.address");
		List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();

		if (address != null) {
			// 解析redis host/port的配置，如 127.0.0.1:6379,192.168.66.66:6379
			String[] split = address.split(",");
			if (split != null) {
				for (String oneAddress : split) {
					String[] hostAndPort = oneAddress.split(":");
					if (hostAndPort == null) {
						continue;
					}
					String host = null;
					int port = 6379;
					if (hostAndPort.length == 1) {
						host = hostAndPort[0];
					} else if (hostAndPort.length == 2) {
						host = hostAndPort[0];
						port = Integer.parseInt(hostAndPort[1]);
					}
					JedisShardInfo jedisShardInfo = new JedisShardInfo(host, port);
					shards.add(jedisShardInfo);
				}
			}
		}

		if (shards.isEmpty()) {
			throw new RuntimeException("RedisSessionStore 没有配置address，无法完成初始化！");
		}

		jedisPool = new ShardedJedisPool(new JedisPoolConfig(), shards);
	}

	@Override
	public void commit(XSession session) {
		ShardedJedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			byte[] key = getSessionCacheKey(session);
			// 如果session里的内容有被修改，才把新内容set到redis上
			if (session.isChanged() || session.isNew()) {
				// 只保存session的attributeMap
				byte[] data = serializer.toData(session.getAttributeMap());
				jedis.set(key, data);
				session.setChanged(false);
				// TODO 这里的逻辑要改进，commit之后，response再commit？
				// session.setNew(false);
			}
			// 更新缓存的失效时间，TODO，是否需要合成一个请求里完成？
			jedis.expire(key, session.getMaxInactiveInterval());

		} catch (JedisConnectionException e) {
			if (jedis != null) {
				jedisPool.returnBrokenResource(jedis);
			}
		} catch (Throwable e) {
			logger.error("set session to redis error! sessionId:" + session.getId(), e);
			// TODO 是抛出RuntimeException 还是只打印日志？或者统计错误的次数？
			throw new RuntimeException(e);
		} finally {
			if (jedis != null) {
				jedisPool.returnResource(jedis);
			}
		}
	}

	@Override
	public void invalidate(XSession session) {
		ShardedJedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			jedis.del(getSessionCacheKey(session));
		} catch (JedisConnectionException e) {
			if (jedis != null) {
				jedisPool.returnBrokenResource(jedis);
			}
		} catch (Throwable e) {
			logger.error("del session from redis error! sessionId:" + session.getId(), e);
		} finally {
			if (jedis != null) {
				jedisPool.returnResource(jedis);
			}
		}
	}

	@Override
	public XSession loadSession(String id) {
		ShardedJedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			// 加载attributeMap，并设置到Session中
			byte[] data = jedis.get(getSessionCacheKey(id));
			if (data == null) {
				return null;
			}
			@SuppressWarnings("unchecked")
			Map<String, Object> attributeMap = (Map<String, Object>) serializer.fromData(data);
			// 从缓存加载的session不是new的
			XSession xSession = new XSession(false, sessionIdLength);
			xSession.setId(id);
			xSession.setAttributeMap(attributeMap);
			return xSession;
		} catch (JedisConnectionException e) {
			if (jedis != null) {
				jedisPool.returnBrokenResource(jedis);
			}
		} catch (Throwable e) {
			logger.error("get session from redis error! sessionId:" + id, e);
		} finally {
			if (jedis != null) {
				jedisPool.returnResource(jedis);
			}
		}
		return null;
	}

	@Override
	public long ttl(XSession session) {
		ShardedJedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			return jedis.ttl(getSessionCacheKey(session));
		} catch (JedisConnectionException e) {
			if (jedis != null) {
				jedisPool.returnBrokenResource(jedis);
			}
		} catch (Throwable e) {
			logger.error("ttl session from redis error! sessionId:" + session.getId(), e);
		} finally {
			if (jedis != null) {
				jedisPool.returnResource(jedis);
			}
		}
		// TODO 在没有取到值的情况下，到底返回什么值合适？
		return -1;
	}

	public byte[] getSessionCacheKey(String id) {
		return (sessionKeyPrefix + id).getBytes(utf8);
	}

	public byte[] getSessionCacheKey(XSession session) {
		return getSessionCacheKey(session.getId());
	}

	@Override
	public XSession createSession() {
		return new XSession(true, sessionIdLength);
	}

}
