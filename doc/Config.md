##### Config

> Spring Cloud Config项目是一个解决分布式系统的配置管理方案。它包含了Client和Server两个部分，server提供配置文件的存储、以接口的形式将配置文件的内容提供出去，client通过接口获取数据、并依据此数据初始化自己的应用。

>  配置文件是我们再熟悉不过的了，尤其是 Spring Boot 项目，除了引入相应的 maven 包之外，剩下的工作就是完善配置文件了，例如 mysql、redis 、security 相关的配置。除了项目运行的基础配置之外，还有一些配置是与我们业务有关系的，比如说七牛存储、短信相关、邮件相关，或者一些业务上的开关。
>
>  对于一些简单的项目来说，我们一般都是直接把相关配置放在单独的配置文件中，以 properties 或者 yml 的格式出现，更省事儿的方式是直接放到 application.properties 或 application.yml 中。但是这样的方式有个明显的问题，那就是，当修改了配置之后，必须重启服务，否则配置无法生效。
>
>  目前有一些用的比较多的开源的配置中心，比如携程的 Apollo、蚂蚁金服的 disconf 等，对比 Spring Cloud Config，这些配置中心功能更加强大。有兴趣的可以拿来试一试。

###### 1.实现简单的配置中心

+ 建立一个 server module，yml配置，pom依赖等

  ```java
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-config-server</artifactId>
  </dependency>
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
  </dependency>
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
  </dependency>
  ```

  ```java
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
  
  #服务注册到eureka地址
  eureka:
    client:
      service-url:
        defaultZone: http://localhost:7001/eureka
      register-with-eureka: true
      fetch-registry: true
  ```

  ```java
  @SpringBootApplication
  @EnableConfigServer
  @EnableEurekaClient
  public class ConfigServer3344 {
      public static void main(String[] args) {
          SpringApplication.run(ConfigServer3344.class, args);
      }
  }
  ```

+ 建立两个client module，yml配置，pom依赖等（**注意client端用的是bootstrap.yml**）

  ```java
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-config</artifactId>
  </dependency>
  <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
  </dependency>
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
  </dependency>
  ```

  ```java
  server:
    port: 3355
  
  spring:
    application:
      name: config-client
    cloud:
      #Config客户端配置
      config:
        label: main #分支名称
        name: config #配置文件名称
        profile: dev #读取后缀名称   
        #上述3个综合：master分支上config-dev.yml的配置文件被http://localhost:3344/main/config-dev.yml
        uri: http://localhost:3344 #配置中心地址
  
  #服务注册到eureka地址
  eureka:
    client:
      service-url:
        defaultZone: http://localhost:7001/eureka
      fetch-registry: true
      register-with-eureka: true
  ```

  client 配置信息：http://localhost:3355/configInfo

  ​							  http://localhost:3366/configInfo

  server 配置信息：http://localhost:3344/main/config-dev.yml

  Spring Cloud Config 有它的一套访问规则，我们通过这套规则在浏览器上直接访问就可以。

  ```yaml
  /{application}/{profile}[/{label}]
  /{application}-{profile}.yml
  /{label}/{application}-{profile}.yml
  /{application}-{profile}.properties
  /{label}/{application}-{profile}.properties
  ```

  {application} 就是应用名称，对应到配置文件上来，就是配置文件的名称部分，例如我上面创建的配置文件。

  {profile} 就是配置文件的版本，我们的项目有开发版本、测试环境版本、生产环境版本，对应到配置文件上来就是以 application-{profile}.yml 加以区分，例如application-dev.yml、application-sit.yml、application-prod.yml。

  {label} 表示 git 分支，默认是 master 分支，如果项目是以分支做区分也是可以的，那就可以通过不同的 label 来控制访问不同的配置文件了。

  上面的 5 条规则中，我们只看前三条，因为我这里的配置文件都是 yml 格式的。根据这三条规则，我们可以通过以下地址查看配置文件内容:

  http://localhost:3344/config-dev/main

  http://localhost:3344/config-prod.yml

  http://localhost:3344/main/config-dev.yml

  通过访问以上地址，如果可以正常返回数据，则说明配置中心服务端一切正常。

  ###### 但是以上的简单配置，当github上的yml更新配置时，将会导致server端访问http://localhost:3344/main/config-dev.yml将会直接更新，因为server端连接着github，数据信息由github直接获取。而client端访问http://localhost:3344/config-prod.yml将不会主动更新，配置文件还是在初版本，需要client微服务重启才会使配置文件更新，下面我们将会配置一个半自动更新的config信息，为什么是半自动呢？

+ 在client端controller层加入注释@RefreshScope

  ```java
  @RestController
  @RefreshScope
  public class ConfigClientController {
  
      @Value("${config.info}")
      private String configInfo;
  
      @Value("${server.port}")
      private String serverPort;
  
      @GetMapping("/configInfo")
      public String configInfo()
      {
          return "serverPort: "+serverPort+"\t\n\n configInfo: "+configInfo;
      }
  }
  ```

  在client的yml修改文件，暴露监控端点（*此时需要 maven 中有 spring-boot-starter-actuator 包*）

  ```java
  # 暴露监控端点
  management:
    endpoints:
      web:
        exposure:
          include: "*"
  ```

  在每次github上的yml更新后，需发送一个post请求，curl -X POST “http://localhost:3355/actuator/refresh”

  即要更新哪个接口就访问 curl -X POST “[http://localhost:{serverport} /actuator/refresh](http://localhost:{serverport} /actuator/refresh)”

  测试

  + 修改version版本为2（github上的config-dev.yml）

    ```java
    config:
      info: "master branch,SpringCloud-Config-Test/config-dev.application version=2"
    ```

  + 刷新server端（成功获取到）

    http://localhost:3344/config-dev.yml

    ```java
    config:
      info: master branch,SpringCloud-Config-Test/config-dev.application version=2
    ```

  + 刷新两个客户端（没有获取到）

    http://localhost:3355/configInfo

    ```java
    master branch,SpringCloud-Config-Test/config-dev.application version=1
    ```

    http://localhost:3366/configInfo

     ```java
    serverPort: 3366	configInfo: master branch,SpringCloud-Config-Test/config-dev.application version=1
     ```

  + 发送一个POST请求端口为3355（刷新两个接口，3355变化，3366无变化）

    > curl -X POST "http://localhost:3355/actuator/refresh"

    http://localhost:3355/configInfo

    ```java
    master branch,SpringCloud-Config-Test/config-dev.application version=2
    ```

    http://localhost:3366/configInfo

    ```java
    serverPort: 3366	configInfo: master branch,SpringCloud-Config-Test/config-dev.application version=1
    ```

    