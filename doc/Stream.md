#### Stream(消息驱动)

1. ###### 为什么需要Spring Cloud Stream？

   > 在实际的企业开发中，消息中间件是至关重要的组件之一。消息中间件主要解决应用解耦，异步消
   > 息，流量削锋等问题，实现高性能，高可用，可伸缩和最终一致性架构。不同的中间件其实现方式，内
   > 部结构是不一样的。如常见的RabbitMQ和Kafka，由于这两个消息中间件的架构上的不同，像
   > RabbitMQ有exchange，Kafka有Topic，partitions分区，这些中间件的差异性导致我们实际项目开发
   > 给我们造成了一定的困扰，我们如果用了两个消息队列的其中一种，后面的业务需求，我想往另外一种
   > 消息队列进行迁移，这时候无疑就是一个灾难性的，一大堆东西都要重新推倒重新做，因为它跟我们的
   > 系统耦合了，这时候 Spring Cloud Stream 给我们提供了一种解耦合的方式。

2. ###### Spring Cloud Stream是什么？

   Spring Cloud Stream由一个中间件中立的核组成。应用通过Spring Cloud Stream插入的input(相当于
   消费者consumer，它是从队列中接收消息的)和output(相当于生产者producer，它是从队列中发送消
   息的。)通道与外界交流。通道通过指定中间件的Binder实现与外部代理连接。业务开发者不再关注具
   体消息中间件，只需关注Binder对应用程序提供的抽象概念来使用消息中间件实现业务即可。 

   ![](https://img2018.cnblogs.com/i-beta/1829785/202002/1829785-20200212182418673-1058929262.png)

3. ###### Binder

   Binder 绑定器是Spring Cloud Stream中一个非常重要的概念。在没有绑定器这个概念的情况下，我们
   的Spring Boot应用要直接与消息中间件进行信息交互的时候，由于各消息中间件构建的初衷不同，它
   们的实现细节上会有较大的差异性，这使得我们实现的消息交互逻辑就会非常笨重，因为对具体的中间
   件实现细节有太重的依赖，当中间件有较大的变动升级、或是更换中间件的时候，我们就需要付出非常
   大的代价来实施。
   通过定义绑定器作为中间层，实现了应用程序与消息中间件(Middleware)细节之间的隔离。通过向应用
   程序暴露统一的Channel通过，使得应用程序不需要再考虑各种不同的消息中间件的实现。当需要升级
   消息中间件，或者是更换其他消息中间件产品时，我们需要做的就是更换对应的Binder绑定器而不需要
   修改任何应用逻辑 。甚至可以任意的改变中间件的类型而不需要修改一行代码。

4. ###### 实现

   + 创建一个消息生产者模块，配置POM，YML文件等

     ```java
     <dependency>
         <groupId>org.springframework.cloud</groupId>
         <artifactId>spring-cloud-starter-stream-rabbit</artifactId>
     </dependency>
     ```

     ```java
     server:
       port: 8801
     
     spring:
       application:
         name: cloud-stream-provider
       cloud:
         stream:
           binders: # 在此处配置要绑定的rabbitmq的服务信息；
             defaultRabbit: # 表示定义的名称，用于于binding整合
               type: rabbit # 消息组件类型
               environment: # 设置rabbitmq的相关的环境配置
                 spring:
                   rabbitmq:
                     host: localhost
                     port: 5672
                     username: guest
                     password: guest
           bindings: # 服务的整合处理
             output: # 这个名字是一个通道的名称，在分析具体源代码的时候会进行说明
               destination: Stream_RabbitMQ # 表示要使用的Exchange名称定义
               content-type: application/json # 设置消息类型，本次为对象json，如果是文本则设置“text/plain”
               binder: defaultRabbit # 设置要绑定的消息服务的具体设置
     
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

   + service的建立与实现

     ```java
     @EnableBinding(Source.class) //定义消息的推送管道
     public class MessageProviderImpl implements IMessageProvider
     {
         @Resource
         private MessageChannel output; // 消息发送管道
     
         @Override
         public String send()
         {
             String serial = UUID.randomUUID().toString();
             output.send(MessageBuilder.withPayload(serial).build());
             System.out.println("serial: "+serial);
             return null;
         }
     }
     ```

   + controller测试

     ```java
     @RestController
     public class SendMessageController
     {
         @Resource
         private IMessageProvider messageProvider;
     
         @GetMapping(value = "/sendMessage")
         public String sendMessage()
         {
             return messageProvider.send();
         }
     
     }
     ```

     测试接口http://localhost:8801/sendMessage，会发现后台会打出

     ```
     serial: f1c10209-ef3a-4a5e-9f62-bd882bd3c99d
     ```

     并在RabbitMQ Exchange处有波动，证明生产者推送的信息成功

   + 消息消费者端引入

     + 引入两个消费端8802与8803，并建立对应的依赖，YML文件等

       ```
       <dependency>
           <groupId>org.springframework.cloud</groupId>
           <artifactId>spring-cloud-starter-stream-rabbit</artifactId>
       </dependency>
       ```

       与生产端不同的是，消费端绑定的是input，即 spring-cloud-stream-bindings-input:

       ```java
       server:
         port: 8802
       
       spring:
         application:
           name: cloud-stream-consumer
         cloud:
           stream:
             binders: # 在此处配置要绑定的rabbitmq的服务信息；
               defaultRabbit: # 表示定义的名称，用于于binding整合
                 type: rabbit # 消息组件类型
                 environment: # 设置rabbitmq的相关的环境配置
                   spring:
                     rabbitmq:
                       host: localhost
                       port: 5672
                       username: guest
                       password: guest
             bindings: # 服务的整合处理
               input: # 这个名字是一个通道的名称
                 destination: Stream_RabbitMQ # 表示要使用的Exchange名称定义
                 content-type: application/json # 设置消息类型，本次为对象json，如果是文本则设置“text/plain”
                 binder: defaultRabbit # 设置要绑定的消息服务的具体设置
       
       
       
       eureka:
         client: # 客户端进行Eureka注册的配置
           service-url:
             defaultZone: http://localhost:7001/eureka
         instance:
           lease-renewal-interval-in-seconds: 2 # 设置心跳的时间间隔（默认是30秒）
           lease-expiration-duration-in-seconds: 5 # 如果现在超过了5秒的间隔（默认是90秒）
           instance-id: receive-8802.com  # 在信息列表时显示主机名称
           prefer-ip-address: true     # 访问的路径变为IP地址
       ```

       ```java
       @SpringBootApplication
       public class StreamMQMain8802
       {
           public static void main(String[] args)
           {
               SpringApplication.run(StreamMQMain8802.class,args);
           }
       }
       ```

       **客户端可以不用注释`@EnableEurekaClient`便可以自动注入到Eureka中，这是因为配置文件中自动配置了确认注册到Eureka**

     + 建立controller测试

       ```java
       @Component
       @EnableBinding(Sink.class)
       public class ReceiveMessageListenerController
       {
           @Value("${server.port}")
           private String serverPort;
       
       
           @StreamListener(Sink.INPUT)
           public void input(Message<String> message)
           {
               System.out.println("消费者1号,----->接受到的消息: "+message.getPayload()+"\t  port: "+serverPort);
           }
       }
       ```

       访问http://localhost:8801/sendMessage发布消息，RabbitMQ中的Exchange将会波动，8801消息生产者会显示

       ```
       serial: 071e1d1d-6dbf-48d4-9d21-2d9125f58cba
       serial: a0aba53b-feec-4c51-bf72-c313ce82cd9d
       serial: ee7cba68-1cef-4876-8e84-996bf277f279
       serial: 613760f5-eaf6-4e4e-85dd-65b143d3e7cb
       serial: 43b641a6-aa68-4959-96f5-4a2dd77f3ee2
       ```

       8802/8803会消费该消息，显示：

       ```
       消费者1号,----->接受到的消息: 071e1d1d-6dbf-48d4-9d21-2d9125f58cba	  port: 8802
       消费者1号,----->接受到的消息: a0aba53b-feec-4c51-bf72-c313ce82cd9d	  port: 8802
       消费者1号,----->接受到的消息: ee7cba68-1cef-4876-8e84-996bf277f279	  port: 8802
       消费者1号,----->接受到的消息: 613760f5-eaf6-4e4e-85dd-65b143d3e7cb	  port: 8802
       消费者1号,----->接受到的消息: 43b641a6-aa68-4959-96f5-4a2dd77f3ee2	  port: 8802
       ```

       ```
       消费者2号,----->接受到的消息: 071e1d1d-6dbf-48d4-9d21-2d9125f58cba	  port: 8803
       消费者2号,----->接受到的消息: a0aba53b-feec-4c51-bf72-c313ce82cd9d	  port: 8803
       消费者2号,----->接受到的消息: ee7cba68-1cef-4876-8e84-996bf277f279	  port: 8803
       消费者2号,----->接受到的消息: 613760f5-eaf6-4e4e-85dd-65b143d3e7cb	  port: 8803
       消费者2号,----->接受到的消息: 43b641a6-aa68-4959-96f5-4a2dd77f3ee2	  port: 8803
       ```

       这时发现该消息会被两个端口重复消费，多数情况下，生产者发送消息给某个具体微服务时只希望被消费一次，为了解决这个问题，`SpringCloud Stream`中提供了消费组的概念，如果没有设置消费组就会出现上例情况，stream将会主动把它们归为不同的消费组，产生重复消费情况。

5. ###### 消费组

   ***注：Stream中处于同一个group中的多个消费者是竞争关系，就能够保证消息只会被其中一个应用消费一次，不同组是可以全面消费（重复消费）***

   针对以上的情况，我们将其分为相同的组来解决重复消费的情况

   在spring-cloud-stream-bindings-input加入group中即可，两个组的名字相同

   ```java
   spring:
     application:
       name: cloud-stream-consumer
     cloud:
       stream:
         binders: # 在此处配置要绑定的rabbitmq的服务信息；
           defaultRabbit: # 表示定义的名称，用于于binding整合
             type: rabbit # 消息组件类型
             environment: # 设置rabbitmq的相关的环境配置
               spring:
                 rabbitmq:
                   host: localhost
                   port: 5672
                   username: guest
                   password: guest
         bindings: # 服务的整合处理
           input: # 这个名字是一个通道的名称
             destination: Stream_RabbitMQ # 表示要使用的Exchange名称定义
             content-type: application/json # 设置消息类型，本次为对象json，如果是文本则设置“text/plain”
             binder: defaultRabbit # 设置要绑定的消息服务的具体设置
             group: groupA
   ```

   访问http://localhost:8801/sendMessage发布消息，观察8802与8803

   ```
   消费者1号,----->接受到的消息: d1425f47-0999-4332-bb1a-9462f4ba8462	  port: 8802
   消费者1号,----->接受到的消息: ea7383f9-b1b1-4298-9058-c6362f2e0ac5	  port: 8802
   消费者1号,----->接受到的消息: 2c11be12-e903-4e06-a461-83fdd066ad37	  port: 8802
   ```

   ```
   消费者2号,----->接受到的消息: d3448770-9a23-4dea-8ad8-0171631d0433	  port: 8803
   消费者2号,----->接受到的消息: df7b4bc1-580b-44cf-b863-af9f689d978d	  port: 8803
   ```

   此时两个消费者将会实现轮询分组，避免重复消费

   

   而分组的作用不止消除了重复消费，也可以使信息持久化。

   在上面的程序里面成功的实现了消息的发送以及接收，但是需要注意一个问题，所发送的消息在默认情况下它都属于一种临时消息，也就是说如果现在没有消费者进行消费处理，那么该消息是不会被保留的。

    如果要想实现持久化的消息处理，重点在于消息的消费端配置，同时也需要考虑到一个分组的情况（**有分组就表示该消息可以进行持久化**）。

   在 SpringCloud Stream 之中如果要设置持久化队列，则设置Group。此时关闭掉消费端的微服务之后该队列信息依然会被保留在 RabbitMQ 之中。而后在关闭消费端的情况下去运行消息生产者，发送完消息后再运行消息的消费端仍然可以接收到之前的消息。

   下面来测试一下8802有分组，而8803没有分组，两个在信息发布后启动，看消息消费情况：

   8802显示：

   ```
   消费者1号,----->接受到的消息: e5bca888-8e37-4410-8738-3f1f3464118e	  port: 8802
   消费者1号,----->接受到的消息: 4b4a4eb2-9b87-4eff-b471-9d10c6d17bdf	  port: 8802
   消费者1号,----->接受到的消息: 4440fd1f-f52c-4b73-b4b7-fde5ec691d61	  port: 8802
   消费者1号,----->接受到的消息: de7131d8-2009-4a7c-a5e9-574b52c355c2	  port: 8802
   消费者1号,----->接受到的消息: 87892322-0cb5-48dc-b066-0d65b41089e1	  port: 8802
   消费者1号,----->接受到的消息: 1ccaf62e-aaf3-40ef-84e0-6aa946de462b	  port: 8802
   消费者1号,----->接受到的消息: fe064872-bdf2-4445-bc53-36488451a1e9	  port: 8802
   消费者1号,----->接受到的消息: 8b9c6aad-3145-40cc-a49a-b581c3bef219	  port: 8802
   ```

   8803无任何消息消费

   证明消息在分组后可以进行持久化处理