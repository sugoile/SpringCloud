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

###### 4. 服务流控规则

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



###### 5. 服务降级规则

> https://blog.csdn.net/xiongxianze/article/details/87572916
>
> https://blog.csdn.net/love1793912554/article/details/106496273

Sentinel除了流量控制以外，对调用链路中不稳定的资源进行熔断降级也是保障高可用的重要措施之一。由于调用关系的复杂性，如果调用链路中的某个资源不稳定，最终会导致请求发生堆积。Sentinel **熔断降级**会在调用链路中某个资源出现不稳定状态时（例如调用超时或异常比例升高），对这个资源的调用进行限制，让请求快速失败，避免影响到其它的资源而导致级联错误。当资源被降级后，在接下来的降级时间窗口之内，对该资源的调用都自动熔断（默认行为是抛出 `DegradeException`）。

**Sentinel以三种方式衡量被访问的资源是否处理稳定的状态**

1、 平均响应时间 (`DEGRADE_GRADE_RT`)：当资源的平均响应时间超过阈值（`DegradeRule` 中的 `count`，以 ms 为单位）之后，资源进入准降级状态。接下来如果**持续**进入 5 个请求，它们的 RT 都持续超过这个阈值，那么在接下的时间窗口（`DegradeRule` 中的 `timeWindow`，以 s 为单位）之内，对这个方法的调用都会自动地返回（抛出 `DegradeException`）。在下一个时间窗口到来时, 会接着再放入5个请求, 再重复上面的判断.

2、 异常比例 (`DEGRADE_GRADE_EXCEPTION_RATIO`)：当资源的每秒异常总数占通过量的比值超过阈值（`DegradeRule` 中的 `count`）之后，资源进入降级状态，即在接下的时间窗口（`DegradeRule`中的 `timeWindow`，以 s 为单位）之内，对这个方法的调用都会自动地返回。异常比率的阈值范围是 `[0.0, 1.0]`，代表 0% - 100%。

3、 异常数 (`DEGRADE_GRADE_EXCEPTION_COUNT`)：当资源近 1 分钟的异常数目超过阈值之后会进行熔断。注意由于统计时间窗口是分钟级别的，若 timeWindow 小于 60s，则结束熔断状态后仍可能再进入熔断状态。

+ 平均响应时间
  <img src="https://img-blog.csdnimg.cn/20200312103659229.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5OTQwMjA1,size_16,color_FFFFFF,t_70" alt="img" style="zoom:80%;" />

  
  
+ 异常比例
  <img src="https://img-blog.csdnimg.cn/20200312105409472.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5OTQwMjA1,size_16,color_FFFFFF,t_70" alt="img" style="zoom:80%;" />

  
  
+ 异常数
  <img src="https://img-blog.csdnimg.cn/2020031210582188.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzM5OTQwMjA1,size_16,color_FFFFFF,t_70" alt="img" style="zoom:80%;" />



+ 写一个异常接口来测试服务降级规则

  ```java
  @GetMapping("/test3")
  public String test3()
  {
      int age = 10 / 0;
      return "test3";
  }
  
  @GetMapping("/test4")
      public String test4()
      {
          try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException e) { e.printStackTrace(); }
          return "test4";
      }
  ```

  此时访问必会报error界面，而服务断路器报的不是error界面，而是

  ```
  Blocked by Sentinel（flow limiting）
  ```



1.8.0版本[原文](https://github.com/alibaba/Sentinel/wiki/熔断降级)

Sentinel 提供以下几种熔断策略：	

- 慢调用比例 (`SLOW_REQUEST_RATIO`)：选择以慢调用比例作为阈值，需要设置允许的慢调用 RT（即最大的响应时间），请求的响应时间大于该值则统计为慢调用。当单位统计时长（`statIntervalMs`）内请求数目大于设置的最小请求数目，并且慢调用的比例大于阈值，则接下来的熔断时长内请求会自动被熔断。经过熔断时长后熔断器会进入探测恢复状态（HALF-OPEN 状态），若接下来的一个请求响应时间小于设置的慢调用 RT 则结束熔断，若大于设置的慢调用 RT 则会再次被熔断。
- 异常比例 (`ERROR_RATIO`)：当单位统计时长（`statIntervalMs`）内请求数目大于设置的最小请求数目，并且异常的比例大于阈值，则接下来的熔断时长内请求会自动被熔断。经过熔断时长后熔断器会进入探测恢复状态（HALF-OPEN 状态），若接下来的一个请求成功完成（没有错误）则结束熔断，否则会再次被熔断。异常比率的阈值范围是 `[0.0, 1.0]`，代表 0% - 100%。
- 异常数 (`ERROR_COUNT`)：当单位统计时长内的异常数目超过阈值之后会自动进行熔断。经过熔断时长后熔断器会进入探测恢复状态（HALF-OPEN 状态），若接下来的一个请求成功完成（没有错误）则结束熔断，否则会再次被熔断。

  

###### 6. 热点规则

何为热点？热点即经常访问的数据。很多时候我们希望统计某个热点数据中访问频次最高的 Top K 数据，并对其访问进行限制。比如：

- 商品 ID 为参数，统计一段时间内最常购买的商品 ID 并进行限制
- 用户 ID 为参数，针对一段时间内频繁访问的用户 ID 进行限制

热点参数限流会统计传入参数中的热点参数，并根据配置的限流阈值与模式，对包含热点参数的资源调用进行限流。热点参数限流可以看做是一种特殊的流量控制，仅对包含热点参数的资源调用生效。

<img src="https://img-blog.csdnimg.cn/20191025165126419.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L29veWhhbw==,size_16,color_FFFFFF,t_70" alt="在这里插入图片描述" style="zoom:80%;" />

Sentinel 利用 LRU 策略统计最近最常访问的热点参数，结合令牌桶算法来进行参数级别的流控

<img src="https://img-blog.csdnimg.cn/20191025165238668.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L29veWhhbw==,size_16,color_FFFFFF,t_70" alt="å¨è¿éæå¥å¾çæè¿°" style="zoom:80%;" />

其中参数索引为一个url带参数的第几个参数，

+ 测试

  ```java
  @GetMapping("/test5")
      @SentinelResource(value = "test", blockHandler = "deal_testHotKey")
      public String test5(@RequestParam(value = "p1",required = false) String p1,
                          @RequestParam(value = "p2",required = false) String p2){
         return "/test5";
      }
      public String deal_testHotKey (String p1, String p2, BlockException exception)
      {
          return "------deal_testHotKey";  //sentinel系统默认的提示：Blocked by Sentinel (flow limiting)
      }
  ##需要写一个服务降级的方案，不然会直接报错给前端
  ```

  这里为我们传不传参数都可以，即可以是

  ```
  http://localhost:8401/test5			结果是没有走降级
  http://localhost:8401/test5?p1=5  #参数索引0代表的是p1，结果是走降级
  http://localhost:8401/test5?p2=5  #参数索引0代表的是p2，结果是走降级
  http://localhost:8401/test5?p1=5&&p2=5	#参数索引0代表的是p1，结果是走降级
  ```

  当我们配置参数索引为0，单机阈值为1时，这时当带有第一个参数访问时就会造成限流，不管是p1还是p2

+ 参数例外项

  这里指代的是参数索引的扩展，当参数索引的值为某个值就可以设置什么样的结果

  | 参数值 | 参数类型         | 限流阈值 |
  | :----- | :--------------- | :------- |
  | aaa    | java.lang.String | 500      |
  | bbb    | java.lang.String | 1        |

  这里参数值为aaa时访问阈值为500，bbb为1，代表不同的参数值会造成不同的后果。

  访问http://localhost:8401/test5?p1=aaa与http://localhost:8401/test5?p1=bbb

  第一项不会降级处理，第二项会降级处理

###### 7. @SentinelResource注解及自定义限流的处理逻辑

上面例子我们处理运用`@SentinelResource`来处理了热点规则，`@SentinelResource`的blockHandler可以用于当不满足规则作为降级的条件，而fallback可以作为程序错误时返回给客户端的降级条件。可以针对不同的场景设置两个不同的降级规则返回不同的处理结果，如果当设置两个降级时，此时发生规则的错误，则返回blockHandler的处理方法。

下面主要介绍的是@SentinelResource里的blockHandler关于规则的降级：

+ 基于资源名称的限流

  是通过配置@SentinelResource里的value属性值作为限流资源

  ```java
  @GetMapping("/byResource")
  @SentinelResource(value = "byResource",blockHandler = "handleException")
  public String byResource()
  {
      return "按资源名称限流测试";
  }
  ```

+ 基于url地址的限流

  是通过@requestmapping里的value属性值作为限流资源

  ```java
  @GetMapping("/byUrl")
  //@SentinelResource(value = "byUrl",blockHandler = "handleException")
  public String byUrl()
  {
      return "按url限流测试";
  }
  
  //此时不管加不加@SentinelResource(value = "byUrl",blockHandler = "handleException")都不会走自己降级方案，浏览器都会返回自身携带的兜底方案Blocked by Sentinel (flow limiting)
  ```

+ 以上两张兜底的方案都可以设置成如下，但基于url地址的限流并不会走下面的限流解决方案

  ```java
  public String handleException(BlockException exception)
  {
      return exception.getClass().getCanonicalName()+"\t 服务不可用";
  }
  ```

+ 自定义全局自己的限流解决方案

  在限流处理类中定义两个方法

  ```java
  public class CustomerBlockHandler
  {
      public static String handlerException(BlockException exception)
      {
          return "按客戶自定义,global handlerException----1";
      }
      public static String handlerException2(BlockException exception)
      {
          return "按客戶自定义,global handlerException----2";
      }
  }
  ```

  ```java
  @GetMapping("/rateLimit/customerBlockHandler")
  @SentinelResource(value = "customerBlockHandler",
          blockHandlerClass = CustomerBlockHandler.class,
          blockHandler = "handlerException2")
  public String customerBlockHandler()
  {
      return "按客戶自定义测试";
  }
  ```

  blockHandlerClass = CustomerBlockHandler.class属性中找的是CustomerBlockHandler类，blockHandler = "handlerException2"找的是CustomerBlockHandler里的handlerException2方法。

  都可以实现@SentinelResource里的降级，相当于Hystix里的@HystrixCommand注解

  **注：@SentinelResource里还有一个属性值exceptionsToIgnore = {IllegalArgumentException.class}，这个属性者代表的是服务降级时忽略该异常，如果出现该异常则会直接报error错误，不会走降级路线。**

###### 8. 服务熔断

熔断主要分两方面处理：1.服务器自身运算错误；2.服务器没有遵循规则的错误。

处理方案：1.fallback属性；2.blockHandler属性

+ 基于Ribbon

  1.yml文件

  ```yaml
  server:
    port: 84
  
  spring:
    application:
      name: nacos-order-consumer
    cloud:
      nacos:
        discovery:
          server-addr: localhost:8848
      sentinel:
        transport:
          #配置Sentinel dashboard地址
          dashboard: localhost:8080
          #默认8719端口，假如被占用会自动从8719开始依次+1扫描,直至找到未被占用的端口
          port: 8719
  
  ```

  

  2.controller类

  ```java
  @RestController
  public class CircleBreakerController {
      public static final String SERVICE_URL = "http://nacos-payment-provider";
  
      @Autowired
      private RestTemplate restTemplate;
  
  
      @RequestMapping("/consumer/fallback/{id}")
      //@SentinelResource(value = "fallback") //没有配置
      //@SentinelResource(value = "fallback",fallback = "handlerFallback") //fallback只负责业务异常
      //@SentinelResource(value = "fallback",blockHandler = "blockHandler") //blockHandler只负责sentinel控制台配置违规
      @SentinelResource(value = "fallback",fallback = "handlerFallback",blockHandler = "blockHandler")
      public String fallback(@PathVariable Long id)
      {
          String result = restTemplate.getForObject(SERVICE_URL + "/paymentSQL/"+id,String.class,id);
  
          if (id == 2) {
              throw new IllegalArgumentException ("IllegalArgumentException,非法参数异常....");
          }else if (result == null) {
              throw new NullPointerException ("NullPointerException,该ID没有对应记录,空指针异常");
          }
  
          return result;
      }
      //本例是fallback
      public String handlerFallback(@PathVariable  Long id,Throwable e) {
          return "服务降级fallback,exception内容  "+e.getMessage();
      }
      //本例是blockHandler
      public String blockHandler(@PathVariable  Long id, BlockException blockException) {
          return "服务降级blockHandler,exception内容  "+blockException.getMessage();
      }
  ```

  3.测试

  开启nacos-payment-provider两个微服务9001、9002，开启84消费端口

  访问

  ```
  http://localhost:84/consumer/fallback/1
  #实现了负载均衡的轮询策略
  http://localhost:84/consumer/fallback/2
  #调用了fallback里面的handlerFallback
  快速访问http://localhost:84/consumer/fallback/1
  #调用了blockHandler里面的blockHandler
  ```

  

+ 基于OpenFeign（当服务提供者宕机时的降级方案）

  ```
  <!--SpringCloud openfeign -->
          <dependency>
              <groupId>org.springframework.cloud</groupId>
              <artifactId>spring-cloud-starter-openfeign</artifactId>
              <version>2.2.2.RELEASE</version>
          </dependency>
          <!--SpringCloud ailibaba nacos -->
          <dependency>
              <groupId>com.alibaba.cloud</groupId>
              <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
          </dependency>
          <!--SpringCloud ailibaba sentinel -->
          <dependency>
              <groupId>com.alibaba.cloud</groupId>
              <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
              <version>2.2.0.RELEASE</version>
          </dependency>
          
          #这里整合需要spring cloud 是 Hoxton.SR1  版本，spring-cloud-starter-alibaba-sentinel 是 2.2.0.RELEASE 版本。不然会报错
  ```

  ```yaml
  # 激活Sentinel对Feign的支持
  feign:
    sentinel:
      enabled: true
  ```

  ```java
  @SpringBootApplication
  @EnableDiscoveryClient
  @EnableFeignClients
  public class OrderMain84 {
      public static void main(String[] args) {
          SpringApplication.run(OrderMain84.class, args);
      }
  }
  ```

  ```java
  @FeignClient(value = "nacos-payment-provider",fallback = PaymentFallbackService.class)
  public interface PaymentService
  {
      @GetMapping(value = "/paymentSQL/{id}")
      public String paymentSQL(@PathVariable("id") Long id);
  }
  ```

  ```java
  @Component
  public class PaymentFallbackService implements PaymentService
  {
      @Override
      public String paymentSQL(Long id)
      {
          return "服务降级返回";
      }
  }
  ```

  ```java
  @Autowired
  private PaymentService paymentService;
  
  @GetMapping(value = "/consumer/paymentSQL/{id}")
  public String paymentSQL(@PathVariable("id") Long id)
  {
      return paymentService.paymentSQL(id);
  }
  ```

  测试

  把服务提供者9001、9002都关掉，则消费者会走PaymentFallbackService里的paymentSQL方法

  

###### 9.持久化

sentinel中的规则持久化可以整合nacos，即将规则写进nacos的配置中，nacos再将配置写进数据库中达到持久化的效果。

> https://www.cnblogs.com/gyli20170901/p/11279576.html

1.依赖

```
<!--SpringCloud ailibaba sentinel-datasource-nacos 后续做持久化用到-->
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-datasource-nacos</artifactId>
</dependency>
```

2.在application.properties中配置sentinel-nacos信息

```yaml
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
        port: 8719
       #持久化管理
      datasource:
        ds1:
          nacos:
            server-addr: localhost:8848
            dataId: cloudalibaba-sentinel-service
            groupId: DEFAULT_GROUP
            data-type: json
            rule-type: flow
```

3.在nacos中配置

[<img src="https://img2018.cnblogs.com/blog/1230278/201907/1230278-20190731222443568-75493756.png" alt="img" style="zoom:80%;" />](https://img2018.cnblogs.com/blog/1230278/201907/1230278-20190731222443568-75493756.png)

```
[
    {
        "resource": "/hello",
        "limitApp": "default",
        "grade": 1,
        "count": 5,
        "strategy": 0,
        "controlBehavior": 0,
        "clusterMode": false
    }
]
```

| Field           | 说明                                                         |
| --------------- | ------------------------------------------------------------ |
| resource        | 资源名，资源名是限流规则的作用对象                           |
| count           | 限流阈值                                                     |
| grade           | 限流阈值类型，QPS 模式（1）或并发线程数模式（0）             |
| limitApp        | 流控针对的调用来源                                           |
| strategy        | 调用关系限流策略：直接、链路、关联                           |
| controlBehavior | 流控效果（直接拒绝/WarmUp/匀速+排队等待），不支持按调用关系限流 |
| clusterMode     | 是否集群限流                                                 |

4.测试类

```java
@GetMapping("/hello")
public String hello() {
    return "hello sentinel";
}
```

可以看到我们访问到该接口都是有规则限流，达到持久化的效果