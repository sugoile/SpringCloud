#### Springboot集成consul

###### 1.Consul说明

`consul`跟`zookeeper`与`eureka`都可以作为服务注册中心，而`consul`和`eureka`都有自己的可视化界面，默认端口号为 8500 

- 服务发现（Service Discovery）：`Consul`提供了通过DNS或者HTTP接口的方式来注册服务和发现服务。一些外部的服务通过`Consul`很容易的找到它所依赖的服务。
- 健康检查（Health Checking）：`Consul`的Client可以提供任意数量的健康检查，既可以与给定的服务相关联(“webserver是否返回200 OK”)，也可以与本地节点相关联(“内存利用率是否低于90%”)。操作员可以使用这些信息来监视集群的健康状况，服务发现组件可以使用这些信息将流量从不健康的主机路由出去。
- Key/Value存储：应用程序可以根据自己的需要使用Consul提供的Key/Value存储。 `Consul`提供了简单易用的HTTP接口，结合其他工具可以实现动态配置、功能标记、领袖选举等等功能。
- 安全服务通信：`Consul`可以为服务生成和分发TLS证书，以建立相互的TLS连接。意图可用于定义允许哪些服务通信。服务分割可以很容易地进行管理，其目的是可以实时更改的，而不是使用复杂的网络拓扑和静态防火墙规则。
- 多数据中心：`Consul`支持开箱即用的多数据中心. 这意味着用户不需要担心需要建立额外的抽象层让业务扩展到多个区域。

###### 2.搭建模块，引入pom依赖

```java
<!--SpringCloud consul-server -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-consul-discovery</artifactId>
</dependency>
```



###### 3.application.yml

```java
###consul服务端口号
server:
  port: 8004

spring:
  application:
    name: consul-provider-payment
####consul注册中心地址
  cloud:
    consul:
      host: localhost
      port: 8500
      discovery:
        #hostname: 127.0.0.1
        service-name: ${spring.application.name}
```

###### 

###### 4.主类与运行类

```java
@SpringBootApplication
@EnableDiscoveryClient
public class PaymentMain8004 {
    public static void main(String[] args) {
        SpringApplication.run(PaymentMain8004.class, args);
    }
}
```

```java
@RestController
@Slf4j
public class PaymentController
{
    @Value("${server.port}")
    private String serverPort;

    @RequestMapping(value = "/payment/consul")
    public String paymentConsul()
    {
        return "springcloud with consul: "+serverPort+"\t   "+ UUID.randomUUID().toString();
    }
}
```



###### 5.启动consul

cmd 中运行 `consul agent -dev `  ：**需要配置环境变量或者在该安装目录中cmd**

http://localhost:8500/ui/dc1/services 中可以查看到微服务情况