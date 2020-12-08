#### OpenFeign

###### 1.`OpenFeign`说明

OpenFeign可以使消费者将提供者提供的服务名伪装为接口进行消费，消费者只需使用“**Service接口 + 注解**”的方式即可直接调用Service接口方法，而无需再使用RestTemplate了。Ribbon是Netflix公司的一个开源的负载均衡项目，是一个客户端负载均衡器，运行在消费者端。OpenFeign也是运行在消费者端的，使用Ribbon进行负载均衡，所以OpenFeign直接内置了Ribbon。即在导入OpenFeign依赖后，无需再专门导入Ribbon依赖了。

###### 2.`OpenFeign`的使用

 2.1 建立`OpenFeign`模块，导入依赖，yml文件，启动类等

 ```
<!--openfeign-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
 ```

```java
@SpringBootApplication
@EnableFeignClients
public class OrderFeignMain80 {
    public static void main(String[] args) {
        SpringApplication.run(OrderFeignMain80.class, args);
    }
}
```

```java
server:
  port: 80

eureka:
  client:
    register-with-eureka: false
    fetch-registry: true
    service-url:
      defaultZone: http://eureka7001.com:7001/eureka/
```

 2.2 OpenFeign service接口

```java
@Component
@FeignClient(value = "CLOUD-PAYMENT-SERVICE")   //指向哪一个微服务地址
public interface PaymentService {
    @GetMapping(value = "/payment/get/{id}")
    CommonResult getPaymentById(@PathVariable("id") Long id);
}
```

 2.3 controller 测试类

```java
@RestController
public class OrderController {
    @Autowired
    private PaymentService paymentService;

    @GetMapping(value = "/consumer/payment/get/{id}")
    public CommonResult getPaymentById(@PathVariable("id") Long id)
    {
        return paymentService.getPaymentById(id);
    }
}
```

**OpenFeign会自带ribbon的负载均衡，所以刚开始测试会有轮询策略**

###### 3. `OpenFeign`的超时控制

`OpenFeign`的默认超时时间是 1s，当响应时间超过1s时服务将会报错，

```java
@GetMapping(value = "/payment/feign/timeout")
public String paymentFeignTimeout()
{
    // 业务逻辑处理正确，但是需要耗费3秒钟
    try { TimeUnit.SECONDS.sleep(3); } catch (InterruptedException e) { e.printStackTrace(); }
    return Server;
}
```

```java
@GetMapping(value = "/payment/feign/timeout")
String paymentFeignTimeout();
```

```java
@GetMapping(value = "/consumer/payment/feign/timeout")
public String paymentFeignTimeout(){
    return paymentService.paymentFeignTimeout();
}
```

做一个定时器3s测试，将会报

` Read timed out executing GET http://CLOUD-PAYMENT-SERVICE/payment/feign/timeout feign. `

可以改变超时的控制时间，使OpenFeign的不超时

在yml文件中加入

```java
#设置feign客户端超时时间(OpenFeign默认支持ribbon)
ribbon:
  #指的是建立连接所用的时间，适用于网络状况正常的情况下,两端连接所用的时间
  ReadTimeout: 5000
  #指的是建立连接后从服务器读取到可用资源所用的时间
  ConnectTimeout: 5000
```

###### 4.`OpenFeign`的日志

1. 在yml文件中加入

   ```java
   logging:
    level:
    # feign日志以什么级别监控哪个接口
    com.xsg.springcloud.service.PaymentService: debug
   ```

2. 配置日志类

   ```java
   @Configuration
   public class OpenFeignLog
   {
       @Bean
       Logger.Level feignLoggerLevel()
       {
           return Logger.Level.FULL;
       }
   }
   ```

3. 信息输出

   输出{"code":200,"message":"查询成功, Server: 8001","data":{"id":1,"serial":"aaaabbb"}}等具体的信息

4. 信息类型

   ```java
   NONE,		默认的，不显示任何日志
   BASIC,		仅记录请求方法、URL、响应状态码以及执行时间
   HEADERS,	除了BASIC中定义的信息以外，还有请求和响应的头信息
   FULL;		除了BASIC中定义的信息以外，还有请求和响应的头信息
   ```