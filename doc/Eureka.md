#### Eureka的一些注意细节

###### 1.SpringCloud 服务发现注解

该注解有两个，一种为@EnableDiscoveryClient,一种为@EnableEurekaClient,用法上基本一致。

spring cloud中discovery service有许多种实现（eureka、consul、zookeeper等等）， 

@EnableDiscoveryClient基于spring-cloud-commons, @EnableEurekaClient基于spring-cloud-netflix。

其实用更简单的话来说， 就是如果选用的注册中心是eureka，那么就推荐@EnableEurekaClient， 如果是其他的注册中心，那么推荐使用@EnableDiscoveryClient。

注解@EnableEurekaClient上有@EnableDiscoveryClient注解， 可以说基本就是EnableEurekaClient有@EnableDiscoveryClient的功能， 另外上面的注释中提到，其实@EnableEurekaClient注解就是一种方便使用eureka的注解而已， 可以说使用其他的注册中心后，都可以使用@EnableDiscoveryClient注解， 但是使用@EnableEurekaClient的情景，就是在服务采用eureka作为注册中心的时候，使用场景较为单一。

###### 2.Eureka 的省略注解@EnableEurekaClient

一般我们在客户端的主启动类可以省略@EnableEurekaClient，客户端可以不用注释`@EnableEurekaClient`便可以自动注入到Eureka中，这是因为配置文件中自动配置了确认注册到Eureka

```java
eureka:
  client: # 客户端进行Eureka注册的配置
    service-url:
      defaultZone: http://localhost:7001/eureka
  instance:
    lease-renewal-interval-in-seconds: 2 # 设置心跳的时间间隔（默认是30秒）
    lease-expiration-duration-in-seconds: 5 # 如果现在超过了5秒的间隔（默认是90秒）
    instance-id: send-8801.com  # 在信息列表时显示主机名称
    prefer-ip-address: true     # 访问的路径变为IP地址
```

其中eureka-client-register-with-eureka默认为true，eureka-client-fetch-registry也默认为true，可以省略