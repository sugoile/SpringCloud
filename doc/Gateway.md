#### GateWay

1. ###### 概述

   网关提供API全托管服务，丰富的API管理功能，辅助企业管理大规模的API，以降低管理成本和安全风险，包括协议适配、协议转发、安全策略、防刷、流量、监控日志等贡呢。一般来说网关对外暴露的URL或者接口信息，我们统称为路由信息。

   

2. ###### 创建

   + 建module模块/yml文件的配置/主启动类的配置

     ```java
     <!--gateway-->
     <dependency>
         <groupId>org.springframework.cloud</groupId>
         <artifactId>spring-cloud-starter-gateway</artifactId>
     </dependency>
     ```

     ```java
     @SpringBootApplication
     @EnableEurekaClient
     public class GateWayMain9527 {
         public static void main(String[] args) {
             SpringApplication.run(GateWayMain9527.class, args);
         }
     }
     ```

   + ###### Spring Cloud Gateway路由配置方式

     1. 基础URI一种路由配置方式

        + yml文件

        ```java
        server:
          port: 9527
        spring:
          application:
            name: cloud-gateway
          cloud:
            gateway:
              routes:
                - id: payment_get         #路由的ID，没有固定规则但要求唯一，建议配合服务名
                  uri: http://localhost:8001     #匹配后提供服务的路由地址
                  predicates:
                    - Path=/payment/get/**         # 断言，路径相匹配的进行路由
        
                - id: payment_serverport  #路由的ID，没有固定规则但要求唯一，建议配合服务名
                  uri: http://localhost:8001          #匹配后提供服务的路由地址
                  predicates:
                    - Path=/payment/serverport         # 断言，路径相匹配的进行路由
        
        eureka:
          instance:
            hostname: cloud-gateway-service
          #服务提供者provider注册进eureka服务列表内
          client:
            fetch-registry: true
            register-with-eureka: true
            service-url:
              defaultZone: http://localhost:7001/eureka
        ```

        + 启动类

          ```java
          @SpringBootApplication
          @EnableEurekaClient
          public class GateWayMain9527 {
              public static void main(String[] args) {
                  SpringApplication.run(GateWayMain9527.class, args);
              }
          }
          ```


        ###### 启动之后访问http://localhost:9527/payment/get/1 便可以访问到 http://localhost:8001/payment/get/1
    
     2. 和注册中心想结合的路由配置方式
    
        修改yml文件即可
    
        ###### uri的协议为lb，代表启用Gateway的负载均衡
    
        ```java
        server:
          port: 9527
        spring:
          application:
            name: cloud-gateway
          cloud:
            gateway:
        	  discovery:
                locator:
                  enabled: true #开启从注册中心动态创建路由的功能，利用微服务名进行路由
              routes:
                - id: payment_get         #路由的ID，没有固定规则但要求唯一，建议配合服务名
                  #uri: http://localhost:8001     #匹配后提供服务的路由地址
                  uri: lb://cloud-payment-service #匹配后提供服务的路由地址
                  predicates:
                    - Path=/payment/get/**         # 断言，路径相匹配的进行路由
        
                - id: payment_serverport  #路由的ID，没有固定规则但要求唯一，建议配合服务名
                  #uri: http://localhost:8001          #匹配后提供服务的路由地址
                  uri: lb://cloud-payment-service #匹配后提供服务的路由地址
                  predicates:
                    - Path=/payment/serverport         # 断言，路径相匹配的进行路由
        
        eureka:
          instance:
            hostname: cloud-gateway-service
          #服务提供者provider注册进eureka服务列表内
          client:
            fetch-registry: true
            register-with-eureka: true
            service-url:
              defaultZone: http://localhost:7001/eureka
        ```
    
     3. 基于代码的路由配置方式
    
        创建一个GateWayconfig类，使他链接到百度国内新闻，访问地址*http://localhost:9527/guonei*
    
        ```java
        @Configuration
        public class GateWayconfig {
            @Bean
            public RouteLocator customRouteLocator(RouteLocatorBuilder routeLocatorBuilder)
            {
                RouteLocatorBuilder.Builder routes = routeLocatorBuilder.routes();
        
                routes.route("route_to_baidu",
                        r -> r.path("/guonei")
                                .uri("http://news.baidu.com/guonei")).build();
        
                return routes.build();
            }
        }
        ```
    
        此时就会链接到百度国内新闻网。


​        

3. ######  Predicate 断言

   + ###### 通过请求参数匹配
   
     ```java
     server:
       port: 9527
     spring:
       application:
         name: cloud-gateway
       cloud:
         gateway:
           discovery:
             locator:
               enabled: true #开启从注册中心动态创建路由的功能，利用微服务名进行路由
           routes:
             - id: payment_get         #路由的ID，没有固定规则但要求唯一，建议配合服务名
               #uri: http://localhost:8001     #匹配后提供服务的路由地址
               uri: lb://cloud-payment-service #匹配后提供服务的路由地址
               predicates:
                 - Path=/payment/get/**         # 断言，路径相匹配的进行路由
     
             - id: payment_serverport  #路由的ID，没有固定规则但要求唯一，建议配合服务名
               #uri: http://localhost:8001          #匹配后提供服务的路由地址
               uri: lb://cloud-payment-service #匹配后提供服务的路由地址
               predicates:
                 - Path=/payment/serverport         # 断言，路径相匹配的进行路由
                 #- Query=smile		#通过请求参数来实现断言
                 #- Query=keep, pu.
                 #- Header=X-Request-Id, \d+ 	#通过 Header 属性匹配,一个 header 中属性名称和一个正则表达式，这个属性值和正则表达式匹配则执行
                 #- Cookie=sessionId, test		#通过 Cookie 匹配，一个是 Cookie name ,一个是正则表达式
                 #- Host=**.baidu.com				#通过 Host 匹配
                 #- Method=GET						#通过请求方式匹配
                 #- RemoteAddr=192.168.1.1/24		#通过请求 ip 地址进行匹配
                 
     
     eureka:
       instance:
         hostname: cloud-gateway-service
       #服务提供者provider注册进eureka服务列表内
       client:
         fetch-registry: true
         register-with-eureka: true
         service-url:
           defaultZone: http://localhost:7001/eureka
     ```
   
     使用 curl 测试，命令行输入:
   
     curl [localhost:9527/payment/serverport?smile=x]()
   
     经过测试发现只要请求汇总带有 smile 参数即会匹配路由，不带 smile 参数则不会匹配。
   
     curl [localhost:9527/payment/serverport?keep=pub]()
   
     测试可以返回页面代码，将 keep 的属性值改为 pubx 再次访问就会报 404,证明路由需要匹配正则表达式才会进行路由。
   
   + ######  通过 Header 属性匹配
   
     curl [localhost:9527/payment/serverport -H "X-Request-Id:88"]()
   
     则返回页面代码证明匹配成功。将参数-H "X-Request-Id:88"改为-H "X-Request-Id:spring"再次执行时返回404证明没有匹配。
   
   + ######  通过 Cookie 匹配
   
     curl  [localhost:9527/payment/serverport --cookie "sessionId=test"]()
   
     则会返回页面代码，如果去掉--cookie "sessionId=test"，后台汇报 404 错误。
   
   + ######  通过 Host 匹配
   
     curl  [localhost:9527/payment/serverport -H "Host: www.baidu.com"]()
   
     curl  [localhost:9527/payment/serverport -H "Host: md.baidu.com"]()
   
     经测试以上两种 host 均可匹配到 host_route 路由，去掉 host 参数则会报 404 错误。
   
   + ######  通过请求方式匹配
   
      curl 默认是以 GET 的方式去请求
   
     curl [localhost:9527/payment/serverport]()
   
     测试返回页面代码，证明匹配到路由，我们再以 POST 的方式请求测试。
   
     curl 默认是以 GET 的方式去请求
   
     curl -X POST  [localhost:9527/payment/serverport]()
   
     返回 404 没有找到，证明没有匹配上路由
   
   + ######   通过请求路径匹配
   
     curl  [localhost:9527/payment/serverport]()
   
     curl  [localhost:9527]()
   
     经测试以上带匹配到 /payment/serverport  访问到路由，去掉参数则会报 404 错误。
   
   + ######  通过请求 ip 地址进行匹配
   
     可以将此地址设置为本机的 ip 地址进行测试。
   
     curl  [localhost:9527/payment/serverport]()
   
     如果请求的远程地址是 192.168.1.10，则此路由将匹配。



4. ###### 设置过滤器

   由于官方自带过滤器虽多，但是还是没自身设置的好啊

   ```java
   @Component
   @Slf4j
   public class GateWayFilter implements GlobalFilter, Ordered
   {
   
       @Override
       public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
       {
           log.info("***********come in MyLogGateWayFilter:  "+new Date());
   
           String uname = exchange.getRequest().getQueryParams().getFirst("uname");
   
           if(uname == null)
           {
               log.info("*******用户名为null，非法用户，o(╥﹏╥)o");
               exchange.getResponse().setStatusCode(HttpStatus.NOT_ACCEPTABLE);
               return exchange.getResponse().setComplete();
           }
   
           return chain.filter(exchange);
       }
   
       @Override
       public int getOrder()
       {
           return 0;
       }
   }
   ```

   设置一个简单的过滤器，如果前端传来的不带uname参数，则把它拦截下来，并返回错误码，`getOrder()`是`Ordered`接口中的一个方法，主要是来定义过滤器的等级，相当于前端的`z-index`，`GlobalFilter`是一个过滤器接口。也可以结合`Hystrix`做一个熔断降级的过滤器。

   ###### 过滤器与断言不同的是，断言只是单纯的限制一些前端的参数等，过滤器可以做到过滤限制，信息收集等，而GateWay是由大量路由Router包含了大量过滤器与大量断言组成。