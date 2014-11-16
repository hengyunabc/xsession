package io.github.xsession;

import io.github.xsession.util.PropertyPlaceholderHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * <pre>
 * 在web.xml里配置一个XSessionFilter，这个Filter有两个配置参数：
 * propertiesFilePath          优先加载的properties文件路径
 * propertiesFilePath 支持类似${environment:dev}这样的配置。
 * 
 * 主要有下面的配置项：
 * sessionCookieName  sessionId在cookie的key名字，默认是sessionId
 * sessionCookieHttpOnly   设置session cookie为HttpOnly，默认为true
 * sessionMaxInactiveInterval   session在缓存的最大不活跃存活时间，默认是30分钟。
 * sessionCookieExpires   session cookie在浏览器的失效时间，默认是1天，即60*60*24秒
 * sessionIdLength        sessionId的长度，默认是32个随机字母
 * sessionStoreClass  存储session的类的名字，要实现{@link SessionStore}接口。默认是{@link io.github.xsession.RedisSessionStore}
 * properties配置文件里的配置会传递给{@link SessionStore#init(Properties)}函数，进行初始化。
 * 
 * </pre>
 * 
 * <pre>
 * {@code
 * 	< }{@code filter>
 * 		<filter-name>xSessionFilter</filter-name>
 * 		<filter-class>io.github.xsession.XSessionFilter</filter-class>
 * 		<init-param>
 * 			<param-name>propertiesFilePath</param-name>
 * 			<param-value>/xSessionFilter-$}{environment:dev}{@code .propertites</param-value>
 * 		</init-param>
 * 	< /filter>
 * 
 * 	<filter-mapping>
 * 		<filter-name>xSessionFilter</filter-name>
 * 		<url-pattern>/*</url-pattern>
 * 	</filter-mapping>
 * }
 * 
 * 具体的例子请参考xsession-example。
 * </pre>
 * 
 * TODO 增加从web.xml里的session config读取Session Timeout值？
 * 貌似没有直接的API，可能要通过构造一个Session，然后读取。
 * 
 * @author hengyunabc
 * 
 */
public class XSessionFilter implements Filter {
	private static final Logger logger = LoggerFactory.getLogger(XSessionFilter.class);

	public static String DEFAULT_SESSION_COOKIE = "sessionId";
	public static String DEFAULT_SESSIONSTORE_ATTRIBUTE_NAME = "__sessionStore";

	public static int DEFAULT_SESSIONCOOKIEEXPIRES = 60 * 60 * 24;
	
	public static int DEFAULT_SESSIONID_LENGTH = 32;

	// Session的最大不活跃存活时间
	int sessionMaxInactiveInterval = XSession.DEFAULT_MAXINACTIVEINTERVAL;

	boolean sessionCookieHttpOnly = true;

	int sessionCookieExpires = DEFAULT_SESSIONCOOKIEEXPIRES;

	static final PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("${", "}", ":", true);

	FilterConfig filterConfig;

	String sessionCookieName = DEFAULT_SESSION_COOKIE;

	SessionStore sessionStore;
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;

		// 读取propertiesFilePath配置，并处理有配置${env}类似表达式的情况
		String propertiesFilePath = getInitParameter("propertiesFilePath", null);

		InputStream propertieStream = null;

		if (propertiesFilePath != null) {
			logger.info("尝试从propertiesFilePath:" + propertiesFilePath + "，读取配置文件");
			propertieStream = this.getClass().getResourceAsStream(propertiesFilePath);
			if (propertieStream == null) {
				logger.error("没有配置propertiesFilePath，或者配置文件不存在");
				throw new RuntimeException("没有配置propertiesFilePath，或者配置文件不存在");
			}
		}

		Properties properties = new Properties();
		try {
			properties.load(propertieStream);
		} catch (IOException e1) {
			throw new RuntimeException("加载XSessionFilter配置文件出错！", e1);
		}

		// 把FilterConfig的配置和properties的配置合并到一起，TODO 貌似没有必要
		// properties = mergeProperties(filterConfigToProperties(filterConfig),
		// properties);

		String temp_sessionMaxInactiveInterval = (String) properties.get("sessionMaxInactiveInterval");
		if (temp_sessionMaxInactiveInterval != null) {
			this.sessionMaxInactiveInterval = Integer.parseInt(temp_sessionMaxInactiveInterval);
		}

		String temp_sessionCookieName = (String) properties.get("sessionCookieName");
		if (temp_sessionCookieName != null) {
			this.sessionCookieName = temp_sessionCookieName;
		}

		String temp_sessionCookieHttpOnly = (String) properties.get("sessionCookieHttpOnly");
		if (temp_sessionCookieHttpOnly != null) {
			this.sessionCookieHttpOnly = Boolean.parseBoolean(temp_sessionCookieHttpOnly);
		}

		String temp_sessionCookieExpires = (String) properties.get("sessionCookieExpires");
		if (temp_sessionCookieExpires != null) {
			this.sessionCookieExpires = Integer.parseInt(temp_sessionCookieExpires);
		}

		// TODO 初始化各种东东
		String sessionStoreClass = getInitParameter("sessionStoreClass", "io.github.xsession.RedisSessionStore");
		try {
			this.sessionStore = (SessionStore) Class.forName(sessionStoreClass).newInstance();
			sessionStore.init(properties);
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw new RuntimeException("初始化SessionStore出错！", e);
		}

		// 把sessionStore放到ServletContext中，这样后面就可以取出来了。
		filterConfig.getServletContext().setAttribute(DEFAULT_SESSIONSTORE_ATTRIBUTE_NAME, sessionStore);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
			ServletException {

		if ((!(request instanceof HttpServletRequest)) || (!(response instanceof HttpServletResponse))
				|| (request.getAttribute(getClass().getName()) != null)) {
			chain.doFilter(request, response);
			return;
		}

		XSessionServletRequest servletRequest = new XSessionServletRequest((HttpServletRequest) request);
		XSessionServletResponse servletResponse = new XSessionServletResponse((HttpServletResponse) response);

		servletRequest.setSessionCookieName(sessionCookieName);
		servletRequest.setSessionStore(sessionStore);
		servletRequest.setSessionMaxInactiveInterval(sessionMaxInactiveInterval);

		servletResponse.setXSessionServletRequest(servletRequest);
		servletResponse.setSessionStore(sessionStore);
		servletResponse.setSessionCookieName(sessionCookieName);
		servletResponse.setSessionCookieHttpOnly(sessionCookieHttpOnly);
		servletResponse.setSessionCookieExpires(sessionCookieExpires);

		chain.doFilter(servletRequest, servletResponse);
		return;
	}

	@Override
	public void destroy() {

	}

	/**
	 * 如果FilterConfig的配置不是null，或者空白字符串，则返回FilterConfig里的配置，并自动处理${name}这样表达式，
	 * 用System.getProperties()里的值去替换。 否则，返回默认值。
	 * 
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	String getInitParameter(String name, String defaultValue) {
		String initParameter = filterConfig.getInitParameter(name);
		if (initParameter != null) {
			String trim = initParameter.trim();
			if (trim.length() != 0) {
				return placeholderHelper.replacePlaceholders(trim, System.getProperties());
			}
		}
		return defaultValue;
	}

	/**
	 * FilterConfig 转换为 Properties
	 * 
	 * @param config
	 * @return
	 */
	Properties filterConfigToProperties(FilterConfig config) {
		Enumeration<String> initParameterNames = config.getInitParameterNames();
		Properties properties = new Properties();
		while (initParameterNames.hasMoreElements()) {
			String name = (String) initParameterNames.nextElement();
			properties.put(name, filterConfig.getInitParameter(name));
		}
		return properties;
	}

	/**
	 * 把第二个参数的Properties的数据合并到，第一个参数的Properties里。并返回合并后的结果。
	 * 
	 * @param one
	 * @param other
	 * @return
	 */
	Properties mergeProperties(Properties one, Properties other) {
		Set<Entry<Object, Object>> entrySet = other.entrySet();
		for (Entry<Object, Object> entry : entrySet) {
			one.put(entry.getKey(), entry.getValue());
		}
		return one;
	}
}
