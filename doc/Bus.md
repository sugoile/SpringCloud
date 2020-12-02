#### Bus

> 引文地址：https://www.cnblogs.com/babycomeon/p/11141160.html

1. ##### 简介

   Spring cloud bus通过轻量消息代理连接各个分布的节点。这会用在广播状态的变化（例如配置变化）或者其他的消息指令。Spring bus的一个核心思想是通过分布式的启动器对spring boot应用进行扩展，也可以用来建立一个多个应用之间的通信频道。目前唯一实现的方式是用AMQP消息代理作为通道，同样特性的设置（有些取决于通道的设置）在更多通道的文档中。

   大家可以将它理解为管理和传播所有分布式项目中的消息既可，其实本质是利用了MQ的广播机制在分布式的系统中传播消息，目前常用的有Kafka和RabbitMQ。利用bus的机制可以做很多的事情，其中配置中心客户端刷新就是典型的应用场景之一，我们用一张图来描述bus在配置中心使用的机制。

   ![](https://springcloud-oss.oss-cn-shanghai.aliyuncs.com/chapter8/configbus1.jpg)

   根据此图我们可以看出利用Spring Cloud Bus做配置更新的步骤:

   1. 提交代码触发post给客户端A发送bus/refresh

   2. 客户端A接收到请求从Server端更新配置并且发送给Spring Cloud Bus

   3. Spring Cloud bus接到消息并通知给其它客户端

   4. 其它客户端接收到通知，请求Server端获取最新配置

   5. 全部客户端均获取到最新的配置

2. ##### 示例

   我们使用上一篇文章中的config-server和config-client来进行改造，`mq`使用`rabbitmq`来做示例。

   + 添加依赖

     ```
     <!--添加消息总线RabbitMQ支持-->
     <dependency>
         <groupId>org.springframework.cloud</groupId>
         <artifactId>spring-cloud-starter-bus-amqp</artifactId>
     </dependency>
     ```

   + `yml`文件

     ```
     #rabbitmq相关配置
     spring:
      rabbitmq:
       host: 127.0.0.1
       port: 5672
       username: guest
       password: guest
     
     ## 开启消息跟踪
     spring.cloud.bus.trace.enabled=true
     ```

     

     配置文件需要增加`RabbitMq`的相关配置，这样客户端代码就改造完成了。

   + 测试

     server端：http://localhost:3344/config-dev.yml

     client端：http://localhost:3355/configInfo，http://localhost:3366/configInfo

     显示内容都为：

     ```
     config:
       	info: master branch,SpringCloud-Config-Test/config-dev.application version=2
     ```

     现在我们更新github上的配置文件，将配置内容改为 

     ```
     master branch,SpringCloud-Config-Test/config-dev.application version=3
     ```

     先访问http://localhost:3344/config-dev.yml，可以看到页面显示为:

     ```
     master branch,SpringCloud-Config-Test/config-dev.application version=3
     ```

      再访问http://localhost:3355/configInfo，http://localhost:3366/configInfo，可以看到页面依然显示为：

     ```
     config:
          info: master branch,SpringCloud-Config-Test/config-dev.application version=2
     ```

     我们对端口为8081的服务发送一个/actuator/bus-refresh的POST请求，在win10下使用下面命令来模拟webhook。

     ```
     curl -X POST http://localhost:3355/actuator/bus-refresh		
     ```

     **注意：** 在springboot2.x的版本中刷新路径为：`/actuator/bus-refresh`，在springboot1.5.x的版本中刷新为：`/bus/refresh`。

     执行完成后，我们访问http://localhost:3355/configInfo , http://localhost:3366/configInfo
     可以看到页面打印内容已经变为：
     ```
     master branch,SpringCloud-Config-Test/config-dev.application version=3
     ```

     这样说明，我们3344端口的服务已经把更新后的信息通过`rabbitmq`推送给了3355与3366端口的服务，这样我们就实现了图一中的示例。

 	

3. ##### 改进

   上面的流程中，虽然我们做到了利用一个消息总线触发刷新，而刷新所有客户端配置的目的，但是这种方式并不合适，如下：

   - 打破了微服务的职责单一性。微服务本身是业务模块，它本不应该承担配置刷新的职责。
   - 破坏了微服务各节点的对等性。
   - 如果客户端ip有变化，这时我们就需要修改WebHook的配置。

   我们可以将上面的流程改进一下：

   ![](https://springcloud-oss.oss-cn-shanghai.aliyuncs.com/chapter8/configbus2.jpg)

   这时Spring Cloud Bus做配置更新步骤如下:

   1. 提交代码触发post给Server端发送bus/refresh
   2. Server端接收到请求并发送给Spring Cloud Bus
   3. Spring Cloud bus接到消息并通知给其它客户端
   4. 其它客户端接收到通知，请求Server端获取最新配置
   5. 全部客户端均获取到最新的配置

   这样的话我们在server端的代码做一些改动，来支持/actuator/bus-refresh

   + 添加依赖

     ```
     <dependency>
              <groupId>org.springframework.cloud</groupId>
              <artifactId>spring-cloud-starter-bus-amqp</artifactId>
          </dependency>
     ```

   + 配置application.yml

     ```
     server:
       port: 3344
     
     spring:
       cloud:
         config:
           server:
             git:
               uri: https://github.com/sugoile/SpringCloud-Config-Test.git   #GitHub上面的git仓库名字
               search-paths: SpringCloud-Config-Test   #搜索目录
               default-label: main                     #默认读取分支
               username: github账号
               password: github密码
         # 开启消息跟踪
         bus:
           trace:
             enabled: true
     
       #rabbitmq相关配置
       rabbitmq:
         host: 127.0.0.1
         port: 5672
         username: guest
         password: guest
     
     #服务注册到eureka地址
     eureka:
       client:
         service-url:
           defaultZone: http://localhost:7001/eureka
         register-with-eureka: true
         fetch-registry: true
     
     #rabbitmq相关配置,暴露bus刷新配置的端点
     management:
       endpoints: #暴露bus刷新配置的端点
         web:
           exposure:
             include: 'bus-refresh'
     
     ```

   + 测试

     按照上面的测试方式，访问两个客户端测试均可以正确返回信息。同样修改配置文件，将值改为：

     ```
       master branch,SpringCloud-Config-Test/config-dev.application version=4
     ```

     

     并提交到仓库中。在win10下使用下面命令来模拟webhook。

     ```
     curl -X POST http://localhost:3344/actuator/bus-refresh
     ```

     执行完成后，依次访问两个客户端，返回：

     ```
      master branch,SpringCloud-Config-Test/config-dev.application version=4
     ```

     说明三个客户端均已经拿到了最新配置文件的信息，这样我们就实现了上图中的示例。
