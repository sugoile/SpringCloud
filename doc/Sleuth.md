#### Sleuth（服务链路追踪）

1. ###### 介绍

   Spring Cloud Sleuth 主要功能就是在分布式系统中提供追踪解决方案，并且兼容支持了 zipkin，你只需要在pom文件中引入相应的依赖即可。微服务架构上通过业务来划分服务的，通过REST调用，对外暴露的一个接口，可能需要很多个服务协同才能完成这个接口功能，如果链路上任何一个服务出现问题或者网络超时，都会形成导致接口调用失败。随着业务的不断扩张，服务之间互相调用会越来越复杂。

2. ###### 构建

   在spring Cloud为F版本的时候，已经不需要自己构建Zipkin Server了，只需要下载jar即可，下载地址：

   https://dl.bintray.com/openzipkin/maven/io/zipkin/java/zipkin-server/

   也可以在这里下载：

   链接: https://pan.baidu.com/s/1w614Z8gJXHtqLUB6dKWOpQ 密码: 26pf

   下载完成jar 包之后，需要运行jar，如下：

   > java -jar zipkin-server-2.10.1-exec.jar

   访问浏览器localhost:9494

3. ###### 引入

   ```java
   <!--包含了sleuth+zipkin-->
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-starter-zipkin</artifactId>
   </dependency>
   ```

   ```java
   spring:
     zipkin:
       base-url: http://localhost:9411
     sleuth:
       sampler:
       #采样率值介于 0 到 1 之间，1 则表示全部采集，一般是0.5足够
       probability: 1
   ```

   测试类

   ```java
   @GetMapping("/payment/zipkin")
   public String paymentZipkin() { return "zipkin-Sleuth";}
   ```

   如果设在客户端与提供端可以看到两个地址访问信息。

   