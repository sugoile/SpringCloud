#### Sentinel

Sentinel作为Spring Cloud Alibaba的服务熔断、服务降级、服务限流的重要组件，相当于Spring Cloud Netflix 的 Hystrix，但是Sentinel有自己整合的web界面，且功能更加强大。

###### 1. 简介

随着微服务的流行，服务和服务之间的稳定性变得越来越重要。Sentinel 以流量为切入点，从流量控制、熔断降级、系统负载保护等多个维度保护服务的稳定性。

###### 2. 下载jar并运行

> https://github.com/alibaba/Sentinel/releases
>
> 运行 在安装目录下用 java -jar sentinel-dashboard-1.8.0.jar
>
> Sentinel默认监听8080端口

访问Sentinel客户端http://localhost:8080/

初始化账号和密码都是sentinel，就可以进入Sentinel dashboard界面

###### 3. 服务注册进Sentinel进行限流处理

**注：当服务写好客户端入驻之后并不会马上就入驻，而是在访问过一次该接口后才会入驻到Sentinel中**

###### 1. Pom、yml、启动类

```
<!--SpringCloud ailibaba nacos -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<!--SpringCloud ailibaba sentinel -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

```yaml
server:
  port: 8401

spring:
  application:
    name: cloudalibaba-sentinel-service
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848 #Nacos服务注册中心地址
    sentinel:
      transport:
        dashboard: localhost:8080 #配置Sentinel dashboard地址
        port: 8719                #在哪个端口开启监控
```

```java
@SpringBootApplication
@EnableDiscoveryClient
public class SentinelMain8401 {
    public static void main(String[] args) {
        SpringApplication.run(SentinelMain8401.class, args);
    }
}
```

###### 2. contoller测试类

```java
@RestController
@Slf4j
public class FlowLimitController {

    @GetMapping("/testA")
    public String testA()
    {
        return "test1";
    }

    @GetMapping("/testB")
    public String testB()
    {
        return "test2";
    }
}
```

###### 3. 测试

开启SentinelMain8401，先看Sentinel dashboard是否入驻，刷新过后仍没有显示限流入驻

在访问过

```
http://localhost:8401/test1
http://localhost:8401/test2
```

/test1与/test2接口会入驻到Sentinel中

###### 4. 添加流控规则

> https://help.aliyun.com/document_detail/101077.html
>
> https://www.cnblogs.com/coder-zyc/p/12926644.html

+ 资源名：唯一名称，默认请求路径

- 针对来源：`Sentinel`可以针对调用者进行限流，填写微服务名，指定对哪个微服务进行限流 ，默认`default`(不区分来源，全部限制)
- 阈值类型/单机阈值：
  - QPS(每秒钟的请求数量)：当调用该接口的QPS达到了阈值的时候，进行限流；
  - 线程数：当调用该接口的线程数达到阈值时，进行限流
- 是否集群：不需要集群
- 流控模式：
  - 直接：接口达到限流条件时，直接限流
  - 关联：当关联的资源达到阈值时，就限流自己
  - 链路：只记录指定链路上的流量（指定资源从入口资源进来的流量，如果达到阈值，就可以限流）[api级别的针对来源]
- 流控效果
  - 快速失败：直接失败，就异常
  - Warm Up：根据**`codeFactor`**（冷加载因子，默认为3）的值，即请求 QPS 从 **`threshold / 3`** 开始，经预热时长逐渐升至设定的 QPS 阈值，详见[原文地址]( [[https://github.com/alibaba/Sentinel/wiki/%E9%99%90%E6%B5%81---%E5%86%B7%E5%90%AF%E5%8A%A8#%E6%A6%82%E8%BF%B0](https://github.com/alibaba/Sentinel/wiki/限流---冷启动#概述)])

- 直接快速失败

　　QPS(每秒钟请求的数量)：当调用该接口的QPS达到阈值的时候，进行限流

　　<img src="https://img2020.cnblogs.com/blog/1334716/202005/1334716-20200520220529716-1043239713.png" alt="img" style="zoom: 80%;" />

 

 　直接快速失败的效果:

　　<img src="https://img2020.cnblogs.com/blog/1334716/202005/1334716-20200520220614828-1496971974.png" alt="img" style="zoom:80%;" />



- 线程数

　　<img src="https://img2020.cnblogs.com/blog/1334716/202005/1334716-20200520220649150-453316449.png" alt="img" style="zoom:80%;" />

 

　　当请求A过来访问该接口，该请求处理的很慢，还没有返回数据；此时请求B也过来访问该接口，这个时候处理请求B需要额外开启一个线程，请求B则会报错；

 

　　效果如下：

 　<img src="https://img2020.cnblogs.com/blog/1334716/202005/1334716-20200520220724518-17073676.png" alt="img" style="zoom:80%;" />

+ 流控模式

  - 直接模式

    Sentinel的流控模式代表的流控的方式，默认【直接】；
    上面的/testA接口的流控，QPS单机阀值为1，代表每秒请求不能超出1，要不然就做流控处理，处理方式直接调用失败；
    调用/testA，慢一点请求，正常返回；快速请求几次，超过阀值；**接口返回了Blocked by Sentinel (flow limiting)，代表被限流了**；

  - 关联模式

  　<img src="https://img2020.cnblogs.com/blog/1334716/202006/1334716-20200601220614193-2018352696.png" alt="img" style="zoom:80%;" />

   设置效果：当关联资源/testB的QPS阈值超过1时，就限流/testA的Rest的访问地址，**当关联资源到资源阈值后限制配置好的资源名**；

  　关联通俗点说就是，当关联的资源达到阀值，就限流自己；

  　**应用场景: 比如支付接口达到阈值,就要限流下订单的接口,防止一直有订单**

  

+ 链路模式

  　　**链路流控模式指的是，当从某个接口过来的资源达到限流条件时，开启限流；它的功能有点类似于针对 来源配置项，区别在于：针对来源是针对上级微服务，而链路流控是针对上级接口，也就是说它的粒度 更细；**

  如下：

  1.编写一个service

  ```java
  @Service
  public class OrderServiceImpl implements OrderService {
   
      @Override
      @SentinelResource(value = "getOrder", blockHandler = "handleException")
      public CommonResult getOrder() {
          return new CommonResult(0, String.valueOf(new Random().nextInt()));
      }
   
      public CommonResult handleException(BlockException ex) {
          return new CommonResult(-1,
                  ex.getClass().getCanonicalName() + "\t服务不可用");
      }
  }
  ```

  　　

  2.在Controller声明两个方法

  ```java
  @Autowired
  private OrderService orderService;
   
  @GetMapping("/test1")
  public CommonResult test1() {
      return orderService.getOrder();
  }
   
  @GetMapping("/test2")
  public CommonResult test2() {
      return orderService.getOrder();
  }
  ```

  　　当流控规则配置了流控模式为链路时，发现当访问`/test1`和`/test2`接口都不能进行限流；

  　　<img src="https://img2020.cnblogs.com/blog/1334716/202006/1334716-20200601221922670-249981919.png" alt="img" style="zoom:80%;" />

   

   注意：

  　　从1.6.3 版本开始， Sentinel Web filter默认收敛所有URL的入口context，因此链路限流不生效； 

  　　1.7.0 版本开始（对应Spring Cloud Alibaba的2.1.1.RELEASE)，官方在**`CommonFilter`** 引入了**`WEB_CONTEXT_UNIFY`** 参数，用于控制是否收敛context；将其配置为 false 即可根据不同的URL 进行链路限流；[原文](https://github.com/alibaba/Sentinel/issues/1213)

  ```java
  @Configuration
  public class FilterContextConfig {
      @Bean
      public FilterRegistrationBean sentinelFilterRegistration() {
          FilterRegistrationBean registration = new FilterRegistrationBean();
          registration.setFilter(new CommonFilter());
          registration.addUrlPatterns("/*");
          // 入口资源关闭聚合
          registration.addInitParameter(CommonFilter.WEB_CONTEXT_UNIFY, "false");
          registration.setName("sentinelFilter");
          registration.setOrder(1);
          return registration;
      }
  }
  ```

  　　关于新版本`web-context-unify`不起作用，参考[原文](https://github.com/alibaba/Sentinel/issues/1313)

+ 流量效果

  + 快速失败

    直接拒绝（`RuleConstant.CONTROL_BEHAVIOR_DEFAULT`）方式是默认的流量控制方式，当QPS超过任意规则的阈值后，新的请求就会被立即拒绝，拒绝方式为抛出**`FlowException`**。这种方式适用于对系统处理能力确切已知的情况下，比如通过压测确定了系统的准确水位时。

  + 预热 Warm Up

    Warm Up（`RuleConstant.CONTROL_BEHAVIOR_WARM_UP`）方式，即预热/冷启动方式。当系统长期处于低水位的情况下，当流量突然增加时，直接把系统拉升到高水位可能瞬间把系统压垮。通过"冷启动"，让通过的流量缓慢增加，在一定时间内逐渐增加到阈值上限，给冷系统一个预热的时间，避免冷系统被压垮。
    
    根据`codeFactor`（冷加载因子，默认为3）的值，即请求 QPS 从 `threshold / 3` 开始，经预热时长逐渐升至设定的 QPS 阈值;
	<img src="https://img2020.cnblogs.com/blog/1334716/202006/1334716-20200601222817077-217755984.png" alt="img" style="zoom:80%;" />　　

    系统初始化的默认阈值为10 / 3，即为3，也就是刚开始的时候阈值只有3，当经过5s后，阈值才慢慢提高到10；

    应用场景：秒杀系统的开启瞬间，会有很多流量上来，很可能会把系统打挂，预热方式就是为了保护系统，可以慢慢的把流量放进来，慢慢的把阈值增长到设定值；

  + 排队等待

  　　匀速排队，让请求以均匀的速度通过，阈值类型必须设置成`QPS`，否则无效；

  　　<img src="https://img2020.cnblogs.com/blog/1334716/202006/1334716-20200601223841574-1950177714.png" alt="img" style="zoom:80%;" />

  　　设置的含义：/testA每秒1次请求，QPS大于1后，再有请求就排队，等待超时时间为20000毫秒；

  　　这种方式主要用于处理间隔性突发的流量，例如消息队列。想象一下这样的场景，在某一秒有大量的请求到来，而接下来的几秒则处于空闲状态，我们希望系统能够在接下来的空闲期间逐渐处理这些请求，而不是在第一秒直接拒绝多余的请求。