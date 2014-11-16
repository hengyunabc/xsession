
### 项目说明
一个分布式Java Web Session。基于filter机制。

### 优点
* 支持多种序列化方式，默认使用Java自带的Serializable方式（兼容性最好）；
* 支持多种Session存储方案，默认是redis；
* 合并session的写操作，一个request里的多次session操作，只会写一次缓存；
* 支持Tomcat，netty等Web服务器，理论上j2ee的Web服务器都支持；
* 支持标准的session api，可以和其它组件无缝对接；

### 注意事项
假定是类似下面的部署结构：
```
nginx -> tomcat1
      -> tomcat2
      -> tomcat3
```
则nginx最好配置为sticky session。推荐淘宝的tengine 的session sticky模块：
http://tengine.taobao.org/document_cn/http_upstream_session_sticky_cn.html

或者这个项目：
https://bitbucket.org/nginx-goodies/nginx-sticky-module-ng/overview

因为后端的Session共享存储并不能锁住sessionId对应的key，所以为了防止多个Tomcat同时操作一个Session，同一个SessionId的请求要转发给同一个Tomcat去处理。

#### 设计思路
首先一个典型的网站结构可能是这样的：
```
nginx1 -----------tomcat1
            |
nginx2------|-----tomcat2
            |
nginx3------------tomcat3
```
前面还会有LVS等，就不画出来了。

nginx会把请求分发给一个Tomcat。

如果nginx配置的是ip_hash，则效果可能不是很理想。比如某些地方有个集中的出口IP，会造成单台的Tomcat负载过高。
或者用户在移动中上网，可以等下IP地址改变，那么请求会发到另外的Tomcat。
