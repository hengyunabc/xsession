

### 环境要求
用到servlet3里的javax.servlet.http.Cookie.setHttpOnly方，所以pom.xml里的javax.servlet-api依赖要是3.0以上的。

运行环境要求是tomcat7或者jetty8以上版本。
```xml
		<dependency>
			<groupId>javax.servlet</groupId>
			<artifactId>javax.servlet-api</artifactId>
			<version>3.1.0</version>
			<scope>provided</scope>
		</dependency>
```
否则可能报这个错误：
```java
java.lang.NoSuchMethodError: javax.servlet.http.Cookie.setHttpOnly(Z)V
```

### 配置项

在web.xml里配置一个XSessionFilter，这个Filter有一个配置参数：
```
 propertiesFilePath 加载的properties文件路径，支持类似${environment:dev}这样的配置。
```
可以通过启动时的JVM参数来在不同环境下加载不同的配置文件。比如 -Denvironment=product。
 
主要有下面的配置项：
```
 sessionCookieName  sessionId在cookie的key名字，默认是sessionId
 sessionCookieHttpOnly   设置session cookie为HttpOnly，默认为true
 sessionMaxInactiveInterval   session在缓存的最大不活跃存活时间，默认是30分钟。
 sessionCookieExpires   session cookie在浏览器的失效时间，默认是1天，即60*60*24秒
 sessionIdLength        sessionId的长度，默认是32个随机字母
 sessionStoreClass  存储session的类的名字，要实现SessionStore接口。默认是io.github.xsession.RedisSessionStore
 properties配置文件里的配置会传递给SessionStore.init(Properties)函数，进行初始化。
```

在web.xml里配置：
```xml
	<filter>
		<filter-name>xSessionFilter</filter-name>
		<filter-class>io.github.xsession.XSessionFilter</filter-class>
		<init-param>
			<param-name>propertiesFilePath</param-name>
			<param-value>/xSessionFilter-${environment:dev}.properties</param-value>
		</init-param>
	</filter>

	<filter-mapping>
		<filter-name>xSessionFilter</filter-name>
		<url-pattern>/*</url-pattern>
	</filter-mapping>
```

配置src/main/resources/xSessionFilter-dev.properties
```
#redisSessionStore.jedis.address=10.0.0.1:6379,10.0.0.2:6379,10.0.0.3:6379
redisSessionStore.jedis.address=127.0.0.1:6379

sessionMaxInactiveInterval=1800
```
具体的例子请参考xsession-example。
