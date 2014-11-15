package io.github.xsession;

import java.util.Properties;

/**
 * 
 *	TODO 序列化时先判断是不是适合Java序列化，如果不是，则改用其它的序列化方式？
 *	TODO 把序列化方式同时写到缓存上？这样反序列化就可以选择对应的方式了。
 */
public interface SessionStore {

	public void init(Properties properties);
	
	public XSession loadSession(String id);
	
	public void commit(XSession session);
	
	public void invalidate(XSession session);
	
	public long ttl(XSession session);
	
	//TODO 这个函数是否必要的？
//	public void expire(XSession session, int seconds);
}
