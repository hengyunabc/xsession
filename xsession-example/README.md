

演示如何配置XSession Filter及效率测试的一些东东。 

### 测试环境
XSessionTestController主要用于测试不同情况下的Session的效率。

为了模拟正常的web请求，每个请求都会休眠50毫秒。

假定以 mvn tomcat7:run 启动来测试。

本地 redis-server侦听：127.0.0.1:6379

访问下面不同的url：
```
http://localhost:8080/setSleepMilliSeconds?sleepMilliSeconds=50   设置每个Request休眠的时间

http://localhost:8080/nosession   没有创建session
http://localhost:8080/tomcatsession   创建tomcat session
http://localhost:8080/xsession   创建xsession，默认保存到redis里
```

用ab进行性能压测：
```
ab -c 300 -n 100000 -r -k http://localhost:8080/nosession
ab -c 300 -n 100000 -r -k http://localhost:8080/tomcatsession
ab -c 300 -n 100000 -r -k http://localhost:8080/xsession
```
### 测试结果
之前的一些测试结果qps：
```
没有session    7000
tomcat session 6500
redis session  6000
```
### 结论
用Redis做分布式Session存储，性能下降很小。

实际上大部分web应用的qps只有100到300多，这时候分布式Session的损耗比例更小。通过把上面测试的休眠时间调大点，就可以看出来了。

