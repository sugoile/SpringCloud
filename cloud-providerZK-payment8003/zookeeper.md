#### Springboot整合Zookeeper

 1. 建立provide模块payment8003，添加pom依赖	

    zookeeper-discovery会自身携带zookeeper的包，但是可能会使与本机版本不符合的现象，故要先把带的版本排除掉

    > ```java
    > 
    > <!-- SpringBoot整合zookeeper客户端 -->
    > <!--该zookeeper-discovery会自身携带zookeeper的包，但是可能会使与本机版本不符合的现象，故要先把带的版本排除掉-->
    > <dependency>
    >     <groupId>org.springframework.cloud</groupId>
    >     <artifactId>spring-cloud-starter-zookeeper-discovery</artifactId>
    >     <!--先排除自带的zookeeper3.5.3-->
    >     <exclusions>
    >         <exclusion>
    >             <groupId>org.apache.zookeeper</groupId>
    >             <artifactId>zookeeper</artifactId>
    >         </exclusion>
    >     </exclusions>
    > </dependency>
    >     
    > <!--添加zookeeper3.4.12版本-->
    > <dependency>
    >     <groupId>org.apache.zookeeper</groupId>
    >     <artifactId>zookeeper</artifactId>
    >     <version>3.4.12</version>
    > </dependency>
    > ```

2. 修改application.yml

   > ```
   > server:
   >   port: 8003
   > spring:
   >   application:
   >     name: cloud-provider-payment
   >   cloud:
   >     zookeeper:
   >       connect-string: 127.0.0.1:2181
   > ```

3. 建立主运行类

   > ```
   > @SpringBootApplication
   > @EnableDiscoveryClient  //该注解用于向使用consul或者zookeeper作为注册中心时注册服务
   > public class PaymentMain8003 {
   >     public static void main(String[] args) {
   >         SpringApplication.run(PaymentMain8003.class, args);
   >     }
   > }
   > ```

4. 测试端口

   > ```
   > @RestController
   > @Slf4j
   > public class PaymentController
   > {
   >     @Value("${server.port}")
   >     private String serverPort;
   > 
   >     @RequestMapping(value = "/payment/zk")
   >     public String paymentzk()
   >     {
   >         return "springcloud with zookeeper: "+serverPort+"\t"+ UUID.randomUUID().toString();
   >     }
   > }
   > ```

5. **关于zookeeper在windows配置**

   [zookeeper_windows配置](https://www.cnblogs.com/mh-study/p/10368891.html)

6. zookeeper一些知识点

   + zookeeper是属于 CAP 中的 CP ，属于数据一致性的代表，一旦微服务关闭，zookeeper将会直接把该微服务除去来保证数据的一致性。

   - {CP：Zookeeper， AP： Eureka}