package io.github.xsession;

import io.github.xsession.util.SessionIdGenerator;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

@SuppressWarnings("deprecation")
public class XSession implements HttpSession {

	static final public String DEFAULT_MAXINACTIVEINTERVAL_ATTRIBUTE_NAME = "__maxInactiveInterval";
	static final public String DEFAULT_CREATIONTIME_ATTRIBUTE_NAME = "__creationTime";
	
	//默认session最长存活时间是30分钟
	static final public int DEFAULT_MAXINACTIVEINTERVAL = 30 * 60;

	String id;

	/**
	 * maxInactiveInterval 和 creationTime都放进这里了，因为这样可以直接保存到缓存服务器上
	 */
	Map<String, Object> attributeMap = new HashMap<>(4);

	ServletContext servletContext;

	boolean isNew = true;

	// 标记这个Session的attributeMap 数据是否更新过
	boolean changed = false;
	
	public XSession(boolean isNew, int sessionIdLength) {
		if (isNew) {
			// 如果不是从缓存中加载Session，则要设置创建时间，并生成id
			long creationTime = System.currentTimeMillis();
			// 把创建时间放到attributeMap里
			this.attributeMap.put(DEFAULT_CREATIONTIME_ATTRIBUTE_NAME, creationTime);
			this.attributeMap.put(DEFAULT_MAXINACTIVEINTERVAL_ATTRIBUTE_NAME, DEFAULT_MAXINACTIVEINTERVAL);
			this.id = SessionIdGenerator.generateSessionId(sessionIdLength);

			this.changed = true;
			this.isNew = true;
		} else {
			this.changed = false;
			this.isNew = false;
		}
	}

	public boolean isChanged() {
		return changed;
	}

	public void setChanged(boolean changed) {
		this.changed = changed;
	}

	public Map<String, Object> getAttributeMap() {
		return attributeMap;
	}

	public void setAttributeMap(Map<String, Object> attributeMap) {
		this.attributeMap = attributeMap;
	}

	@Override
	public long getCreationTime() {
		Long creationTime = (Long) attributeMap.get("DEFAULT_CREATIONTIME_ATTRIBUTE_NAME");
		if (creationTime != null) {
			return creationTime;
		}
		return -1;
	}

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public long getLastAccessedTime() {
		// TODO 这个貌似意义不大？直接返回创建时间？
		return this.getCreationTime();
	}

	@Override
	public ServletContext getServletContext() {
		return servletContext;
	}

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public void setMaxInactiveInterval(int interval) {
		// TODO 这里只是设置了，等这个Request结束时，再提交到Redis上
		attributeMap.put(DEFAULT_MAXINACTIVEINTERVAL_ATTRIBUTE_NAME, interval);
	}

	@Override
	public int getMaxInactiveInterval() {
		Integer interval = (Integer) attributeMap.get(DEFAULT_MAXINACTIVEINTERVAL_ATTRIBUTE_NAME);
		if (interval == null) {
			// TODO 这里到底返回什么值比较合理？
			return -1;
		}
		return interval;
	}

	@Override
	public HttpSessionContext getSessionContext() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getAttribute(String name) {
		return attributeMap.get(name);
	}

	@Override
	public Object getValue(String name) {
		return getAttribute(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return null;
	}

	@Override
	public String[] getValueNames() {
		Set<String> keySet = attributeMap.keySet();
		return keySet.toArray(new String[keySet.size()]);
	}

	@Override
	public void setAttribute(String name, Object value) {
		attributeMap.put(name, value);
		changed = true;
	}

	/**
	 * 设置属性值的同时，强制刷新session到缓存上
	 * 
	 * @param name
	 * @param value
	 * @param force
	 */
	public void setAttribute(String name, Object value, boolean force) {
		setAttribute(name, value);
		if (force) {
			SessionStore sessionStore = (SessionStore) servletContext
					.getAttribute(XSessionFilter.DEFAULT_SESSIONSTORE_ATTRIBUTE_NAME);
			sessionStore.commit(this);
			// TODO 这里是否必要？
			changed = false;
		}
	}

	@Override
	public void putValue(String name, Object value) {
		setAttribute(name, value);
	}

	@Override
	public void removeAttribute(String name) {
		changed = true;
		attributeMap.remove(name);
	}

	@Override
	public void removeValue(String name) {
		removeAttribute(name);
	}

	@Override
	public void invalidate() {
		SessionStore sessionStore = (SessionStore) getServletContext().getAttribute(
				XSessionFilter.DEFAULT_SESSIONSTORE_ATTRIBUTE_NAME);
		if (sessionStore != null) {
			sessionStore.invalidate(this);
		}
	}

	@Override
	public boolean isNew() {
		return isNew;
	}

	public void setNew(boolean isNew) {
		this.isNew = isNew;
	}

}
