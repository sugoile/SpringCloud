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



###### 4.Nacos配置中心

`Nacos`自带配置`server`端，不需要像`Config`那样自己书写`server`端，我们只需要把`client`注册进`Nacos`即可，也不需要连接github作为配置中心存储，`Nacos`自带了一个小型数据库`derby`，虽然能够作为数据库存储，但是在集群时，每一个`Nacos`都带有自己的数据库，会造成数据的存储混乱，且数据库不具有持久性，关闭了`Nacos`服务后就会失效，Nacos作为处理，加入了数据库作为持久性和集群的配置。

+ 配置client端，pom，yml，启动类

  ```
  <!--nacos-config-->
  <dependency>
      <groupId>com.alibaba.cloud</groupId>
      <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
  </dependency>
  <!--nacos-discovery-->
  <dependency>
      <groupId>com.alibaba.cloud</groupId>
      <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
  </dependency>
  ```

  yml文件我们建立一个`boostrap.yml`和一个`application.yml`，这是因为`boostrap.yml`优于`application.yml`的加载，在配置中我们先放入`boostrap.yml`，而具体是哪一个开发环境在`application.yml`配置自由切换

  `bootstrap.yml`:

  ```yaml
  # nacos配置
  server:
    port: 3377
  
  spring:
    application:
      name: nacos-config-client
    cloud:
      nacos:
        discovery:
          server-addr: localhost:8848 #Nacos服务注册中心地址
        config:
          server-addr: localhost:8848 #Nacos作为配置中心地址
          file-extension: yaml #指定yaml格式的配置
  
  
  # ${spring.application.name}-${spring.profile.active}.${spring.cloud.nacos.config.file-extension}
  ```

  application.yml :

  ```yaml
  #环境为dev.yml
  spring:
    profiles:
      active: dev
  ```

  ```java
  @EnableDiscoveryClient
  @SpringBootApplication
  public class NacosConfigClientMain3377
  {
      public static void main(String[] args) {
          SpringApplication.run(NacosConfigClientMain3377.class, args);
      }
  }
  ```

+ 配置controller端

  ```java
  @RestController
  @RefreshScope       //支持Nacos的动态刷新功能。
  public class ConfigClientController {
      @Value("${config.info}")
      private String configInfo;
  
      @GetMapping("/config/info")
      public String getConfigInfo() {
          return configInfo;
      }
  }
  ```

+ 注意点

  在 Nacos Spring Cloud 中，`dataId` 的完整格式如下：

  ```plain
  ${prefix}-${spring.profiles.active}.${file-extension}
  ```

  + `prefix` 默认为 `spring.application.name` 的值，也可以通过配置项 `spring.cloud.nacos.config.prefix`来配置。

  - `spring.profiles.active` 即为当前环境对应的 profile，详情可以参考 [Spring Boot文档](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-profiles.html#boot-features-profiles)。 **注意：当 `spring.profiles.active` 为空时，对应的连接符 `-` 也将不存在，dataId 的拼接格式变成 `${prefix}.${file-extension}`**
  - `file-exetension` 为配置内容的数据格式，可以通过配置项 `spring.cloud.nacos.config.file-extension` 来配置。目前只支持 `properties` 和 `yaml` 类型。

  `Nacos `的 `@RefreshScope` 不像`Config`一样需要发送一个推送请求post给sever端才能使client端的需要的`bootstrap.yml`更新，`Nacos`结合`Config`与`Bus`实现了不需要推送直接更新。

+ 测试

  在Nacos中自己创建一个配置信息供测试，也可以远程自己配置信息

  首先通过调用 [Nacos Open API](https://nacos.io/zh-cn/docs/open-api.html) 向 Nacos Server 发布配置：dataId 为`dev.yaml`，内容为`useLocalCache=true`

  ```
  curl -X POST "http://127.0.0.1:8848/nacos/v1/cs/configs?dataId=nacos-config-client-dev.yaml&group=DEFAULT_GROUP&content=useLocalCache=true"
  ```

  Data ID: nacos-config-client-dev.yaml

  Group: DEFAULT_GROUP

  访问http://localhost:3377/config/info显示：

  ```
  nacos-config-client-dev.yaml
  ```

  当我们修改/添加server端的YAML文档时，

  再次刷新http://localhost:3377/config/info即可，显示：

  ```
  nacos-config-client-dev.yaml update
  ```

+ Nacos配置实时刷新原理

  > https://blog.csdn.net/c18298182575/article/details/102834106
  >
  > https://www.jianshu.com/p/acb9b1093a54



###### 5. Namespace，Group

Namespace指命名空间，不同的Namespace是互相隔离的，如：开发，测试，生产环境

Group指不同微服务可以放入同一个分组里面，默认分组是DEFAULT_GROUP

实现不同Namespace，不同Group之间的配置查看

 每建一个Namespace就会有UUID输出

group可以自己命名

```yaml
spring:
  application:
    name: nacos-config-client
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848 #Nacos服务注册中心地址
      config:
        server-addr: localhost:8848 #Nacos作为配置中心地址
        file-extension: yaml #指定yaml格式的配置
        group: DEV_GROUP
        namespace: 7d8f0f5a-6a53-4785-9686-dd460158e5d4
```



###### 6. 持久化到Mysql

windows下数据信息持久化到mysql数据库，只需要在Nacos安装路径下的config文件夹中

```
#*************** Config Module Related Configurations ***************#
### If use MySQL as datasource:
# spring.datasource.platform=mysql

### Count of DB:
# db.num=1

### Connect URL of DB:
# db.url.0=jdbc:mysql://127.0.0.1:3306/nacos?characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useUnicode=true&useSSL=false&serverTimezone=UTC
# db.user=nacos
# db.password=nacos
```

将注解开启即可，数据库需要自己配置

里面有nacos-mysql.sql导入新建的数据库即可实现持久化，每次一新建就会进入数据库中，保持数据的持久性。



