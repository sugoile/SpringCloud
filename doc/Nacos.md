#### Nacos

> Nacos官方文档：https://nacos.io/zh-cn/docs/what-is-nacos.html

###### 1. 简介

Nacos 致力于帮助您发现、配置和管理微服务。Nacos 提供了一组简单易用的特性集，帮助您快速实现动态服务发现、服务配置、服务元数据及流量管理。

Nacos 帮助您更敏捷和容易地构建、交付和管理微服务平台。 Nacos 是构建以“服务”为中心的现代应用架构 (例如微服务范式、云原生范式) 的服务基础设施。

简单来说：Nacos取代了Eureka，Config+Bus的职责，自己建立了一套主打服务发现和服务配置的微服务。



###### 2. Nacos服务注册（提供者）

`Nacos`自带`server`端，不需要像`Eureka`那样自己书写`server`端，我们只需要把`client`注册进`Nacos`即可

+ pom依赖，yml文件，启动类

  ```
  <dependency>
      <groupId>com.alibaba.cloud</groupId>
      <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
  </dependency>
  ```

  ```yaml
  server:
    port: 9001
  
  spring:
    application:
      name: nacos-payment-provider
    cloud:
      nacos:
        discovery:
          server-addr: localhost:8848 #配置Nacos地址
  ```

  ```java
  @EnableDiscoveryClient
  @SpringBootApplication
  public class PaymentMain9001
  {
      public static void main(String[] args) {
              SpringApplication.run(PaymentMain9001.class, args);
      }
  }
  ```

+ 创建一个简单的Controller

  ```java
  @RestController
  public class PaymentController
  {
      @Value("${server.port}")
      private String serverPort;
  
      @GetMapping(value = "/payment/nacos")
      public String getPayment()
      {
          return "nacos start,serverPort: "+ serverPort;
      }
  }
  ```

+ 测试

  开启Nacos服务，访问http://localhost:8848/nacos，**注：单机服务启动为`startup.cmd -m standalone`**

  启动9001端口，可以看到服务注册进入Nacos

  访问http://localhost:9001/payment/nacos，显示：

  ```
  nacos start,serverPort: 9001
  ```

+ 下面按照同样的形式创建同一个9002微服务供给消费端验证负载均衡和服务调用



###### 3. Nacos服务注册（消费者）

由于Nacos集成了Ribbon，所以它可以与RestTemplate搭建负载均衡

+ pom依赖，yml文件，启动类

  ```
  <!--SpringCloud ailibaba nacos -->
  <dependency>
      <groupId>com.alibaba.cloud</groupId>
      <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
  </dependency>
  ```

  ```yml
  server:
    port: 83
  
  
  spring:
    application:
      name: nacos-order-consumer
    cloud:
      nacos:
        discovery:
          server-addr: localhost:8848
  
  #消费者将要去访问的微服务名称(注册成功进nacos的微服务提供者)
  service-url:
    nacos-user-service: http://nacos-payment-provider
  ```

  ```java
  @EnableDiscoveryClient
  @SpringBootApplication
  public class OrderNacosMain83 {
      public static void main(String[] args) {
          SpringApplication.run(OrderNacosMain83.class, args);
      }
  }
  ```

+ config配置RestTemplate负载均衡和注入成为Bean

  ```java
  @Configuration
  public class ApplicationConfig {
      @Bean
      @LoadBalanced
      public RestTemplate restTemplate(){
          return new RestTemplate();
      }
  }
  ```

+ 创建一个简单的Controller消费提供者端

  ```java
  @RestController
  public class OrderNacosController {
      @Autowired
      private RestTemplate restTemplate;
  
      @Value("${service-url.nacos-user-service}")
      private String serverURL;
  
      @GetMapping(value = "/consumer/payment/nacos")
      public String paymentInfo()
      {
          return restTemplate.getForObject(serverURL+"/payment/nacos",String.class);
      }
  }
  ```

+ 测试

  开启两个提供者，一个消费者，访问端口

  http://localhost:83/consumer/payment/nacos

  显示：

  ```
  nacos start,serverPort: 9001
  ```

  在访问一次，显示：

  ```
  nacos start,serverPort: 9002
  ```

  不管访问多少次，Nacos表示的负载均衡为轮询策略。