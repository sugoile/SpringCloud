#### Hystrix

###### 1.`Hystrix`的使用

  #####  1.1 服务降级

  + pom中导入依赖

    ```java
    <!--hystrix-->
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
    </dependency>
    ```

  + 创建yml文件，并注册服务进`eureka`中

    ```java
    server:
      port: 8001
    
    spring:
      application:
        name: cloud-providerHystrix-payment
    
    eureka:
      client:
        register-with-eureka: true
        fetch-registry: true
        service-url:
          #defaultZone: http://eureka7001.com:7001/eureka,http://eureka7002.com:7002/eureka
          defaultZone: http://eureka7001.com:7001/eureka
    ```

  + 主启动类

    ```java
    @SpringBootApplication
    @EnableEurekaClient
    @EnableCircuitBreaker
    public class PaymentHystrixMain8001
    {
        public static void main(String[] args) {
            SpringApplication.run(PaymentHystrixMain8001.class, args);
        }
    }
    ```

  + 在service层中分别创建两个方法作为演示的结果

    + ###### 正常访问

    + ###### 方法中超时/运行错误

    ```java
    @Service
    public class PaymentService {
        /**
         * 正常访问，肯定OK
         * @param id
         * @return
         */
        public String paymentInfo_OK(Integer id)
        {
            return "线程池:  "+Thread.currentThread().getName()+"  paymentInfo_OK,id:  "+id;
        }
    
    
        /**
         * 这里演示的是服务降级中返回一个兜底的fallback，超时的设置/运行错误 => fallback method
         * @param id
         * @return
         */
        @HystrixCommand(fallbackMethod = "paymentInfo_TimeOutHandler",commandProperties = {
                @HystrixProperty(name="execution.isolation.thread.timeoutInMilliseconds",value="3000")
        })
        public String paymentInfo_TimeOut(Integer id)
        {
            //int age = 10/0;
            try { TimeUnit.MILLISECONDS.sleep(4000); } catch (InterruptedException e) { e.printStackTrace(); }
            return "线程池:  "+Thread.currentThread().getName()+" id:  "+id;
        }
        public String paymentInfo_TimeOutHandler(Integer id)
        {
            return "线程池:  "+Thread.currentThread().getName()+"  8001系统繁忙或者运行报错，请稍后再试,id:  "+id;
        }
    }
    
    ```


  + controller层的建立

    ```java
    @RestController
    public class PaymentController {
    
        @Autowired
        private PaymentService paymentService;
    
        @Value("${server.port}")
        private String serverPort;
    
        @GetMapping("/payment/hystrix/ok/{id}")
        public String paymentInfo_OK(@PathVariable("id") Integer id)
        {
            String result = paymentService.paymentInfo_OK(id);
            return result;
        }
    
        @GetMapping("/payment/hystrix/timeout/{id}")
        public String paymentInfo_TimeOut(@PathVariable("id") Integer id)
        {
            String result = paymentService.paymentInfo_TimeOut(id);
            return result;
        }
    }
    ```

  #####  1.2其它调用微服务端`服务降级 + OpenFeign`

  + 微服务yml，启动类建立

    ```java
    <!--openfeign-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>
    <!--hystrix-->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
    </dependency>
    ```

    ```java
    server:
      port: 80
    
    eureka:
      client:
        register-with-eureka: true
        service-url:
          defaultZone: http://eureka7001.com:7001/eureka/
    
    #开启 feign 与 hystrix 交互
    feign:
      hystrix:
        enabled: true
    ```

+ service层建立（采用OpenFeign）

  ```java
  @Component
  @FeignClient(value = "CLOUD-PROVIDERHYSTRIX-PAYMENT")
  public interface PaymentHystrixService {
      @GetMapping("/payment/hystrix/ok/{id}")
      public String paymentInfo_OK(@PathVariable("id") Integer id);
  
      @GetMapping("/payment/hystrix/timeout/{id}")
      public String paymentInfo_TimeOut(@PathVariable("id") Integer id);
  }
  ```

+ controller层建立

  1.局部Fallback方法

  ```java
  @RestController
  public class OrderHystirxController {
  
      @Autowired
      private PaymentHystrixService paymentHystrixService;
  
  
      @GetMapping("/consumer/payment/hystrix/ok/{id}")
      public String paymentInfo_OK(@PathVariable("id") Integer id)
      {
          String result = paymentHystrixService.paymentInfo_OK(id);
          return result;
      }
  
      @GetMapping("/consumer/payment/hystrix/timeout/{id}")
      @HystrixCommand(fallbackMethod = "paymentTimeOutFallbackMethod",commandProperties = {
              @HystrixProperty(name="execution.isolation.thread.timeoutInMilliseconds",value="1500")
      })
      //@HystrixCommand
      public String paymentInfo_TimeOut(@PathVariable("id") Integer id)
      {
          //int age = 10/0;
          try { TimeUnit.MILLISECONDS.sleep(4000); } catch (InterruptedException e) { e.printStackTrace(); }
          String result = paymentHystrixService.paymentInfo_TimeOut(id);
          return result;
      }
      public String paymentTimeOutFallbackMethod(@PathVariable("id") Integer id)
      {
          return "消费者80--对方支付系统繁忙请10秒钟后再试或者自己运行出错请检查自己";
      }
  }
  ```

  2.默认调回Fallback

  只需要在controller加一个`@DefaultProperties`，并添加 `defaultFallback` 方法

  ```java
  @RestController
  @DefaultProperties(defaultFallback = "payment_Global_FallbackMethod")
  public class OrderHystirxController {
  
      @Autowired
      private PaymentHystrixService paymentHystrixService;
  
  
      @GetMapping("/consumer/payment/hystrix/ok/{id}")
      public String paymentInfo_OK(@PathVariable("id") Integer id)
      {
          String result = paymentHystrixService.paymentInfo_OK(id);
          return result;
      }
  
      @GetMapping("/consumer/payment/hystrix/timeout/{id}")
      //    @HystrixCommand(fallbackMethod = "paymentTimeOutFallbackMethod",commandProperties = {
  //            @HystrixProperty(name="execution.isolation.thread.timeoutInMilliseconds",value="1500")
  //    })
      @HystrixCommand
      public String paymentInfo_TimeOut(@PathVariable("id") Integer id)
      {
          //int age = 10/0;
          try { TimeUnit.MILLISECONDS.sleep(4000); } catch (InterruptedException e) { e.printStackTrace(); }
          String result = paymentHystrixService.paymentInfo_TimeOut(id);
          return result;
      }
      public String paymentTimeOutFallbackMethod(@PathVariable("id") Integer id)
      {
          return "消费者80--对方支付系统繁忙请10秒钟后再试或者自己运行出错请检查自己";
      }
  
      // 下面是全局fallback方法
      public String payment_Global_FallbackMethod()
      {
          return "Global异常处理信息，请稍后再试";
      }
  
  }
  ```

  3.可以结合OpenFeign + Hystrix方法来实现

  yml中需开启

  ```java
  #开启 feign 与 hystrix 交互
  feign:
    hystrix:
      enabled: true
  ```

  写一个FallBack类来实现PaymentHystrixService的OpenFeign接口

  ```java
  @Service
  public class PaymentFallbackService implements PaymentHystrixService {
      @Override
      public String paymentInfo_OK(Integer id) {
          return "PaymentFallbackService fall back-paymentInfo_OK";
      }
  
      @Override
      public String paymentInfo_TimeOut(Integer id) {
          return "PaymentFallbackService fall back-paymentInfo_TimeOut";
      }
  }
  ```

  并在PaymentHystrixService类中注明fallback的类

  ```java
  @Component
  @FeignClient(value = "CLOUD-PROVIDERHYSTRIX-PAYMENT", fallback = PaymentFallbackService.class)
  public interface PaymentHystrixService {
      @GetMapping("/payment/hystrix/ok/{id}")
      public String paymentInfo_OK(@PathVariable("id") Integer id);
  
      @GetMapping("/payment/hystrix/timeout/{id}")
      public String paymentInfo_TimeOut(@PathVariable("id") Integer id);
  }
  ```

  此时如果服务提供者端口发生错误将会调回PaymentFallbackService的具体实现方法不会造成拥堵。

  ##### 服务降级总结

+ 宕机
  + 服务器宕机(OpenFeign接口模式实现具体返回实现类) 
+ 运行出错
  + 服务端运行出错(在服务端使用@HystrixCommand来是实现Fallback) 
  + 消费端运行出错(在消费端使用@HystrixCommand来是实现Fallback) 
+ 超时
  + 服务端运行出错(在服务端使用@HystrixCommand来是实现Fallback) 
  + 消费端运行出错(在消费端使用@HystrixCommand来是实现Fallback) 

  

  ##### 服务熔断

`Hystrix`中的熔断器(Circuit Breaker)，`Hystrix`在运行过程中会向每个commandKey对应的熔断器报告成功、失败、超时和拒绝的状态，熔断器维护并统计这些数据，并根据这些统计信息来决策熔断开关是否打开。如果打开，熔断后续请求，快速返回。隔一段时间（默认是5s）之后熔断器尝试半开，放入一部分流量请求进来，相当于对依赖服务进行一次健康检查，如果请求成功，熔断器关闭。

+ 在controller层建立熔断接口

  ```java
  @GetMapping("/payment/circuit/{id}")
  public String paymentCircuitBreaker(@PathVariable("id") Integer id)
  {
      String result = paymentService.paymentCircuitBreaker(id);
      return result;
  }
  ```

+ service层实现具体的熔断实现机制

  ```java
  @HystrixCommand(fallbackMethod = "paymentCircuitBreaker_fallback",commandProperties = {
          @HystrixProperty(name = "circuitBreaker.enabled",value = "true"),// 是否开启断路器
          @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold",value = "10"),// 请求次数
          @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds",value = "10000"), // 时间窗口期
          @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage",value = "60"),// 失败率达到多少后跳闸
  })
  public String paymentCircuitBreaker(@PathVariable("id") Integer id)
  {
      if(id < 0)
      {
          throw new RuntimeException("******id 不能负数");
      }
      String serialNumber = IdUtil.simpleUUID();
  
      return Thread.currentThread().getName()+"\t"+"调用成功，流水号: " + serialNumber;
  }
  public String paymentCircuitBreaker_fallback(@PathVariable("id") Integer id)
  {
      return "id 不能负数，请稍后再试，/(ㄒoㄒ)/~~   id: " +id;
  }
  ```

+ `Hystrix`熔断器`Circuit Breaker`中的参数信息

  由于Hystrix是一个容错框架，因此我们在使用的时候，要达到熔断的目的只需配置一些参数就可以了。但我们要达到真正的效果，就必须要了解这些参数。Circuit Breaker一共包括如下6个参数。
  **1、circuitBreaker.enabled**
  是否启用熔断器，默认是TURE。
  **2、circuitBreaker.forceOpen**
  熔断器强制打开，始终保持打开状态。默认值FLASE。
  **3、circuitBreaker.forceClosed**
  熔断器强制关闭，始终保持关闭状态。默认值FLASE。
  **4、circuitBreaker.errorThresholdPercentage**
  设定错误百分比，默认值50%，例如一段时间（10s）内有100个请求，其中有55个超时或者异常返回了，那么这段时间内的错误百分比是55%，大于了默认值50%，这种情况下触发熔断器-打开。
  **5、circuitBreaker.requestVolumeThreshold**
  默认值20.意思是至少有20个请求才进行errorThresholdPercentage错误百分比计算。比如一段时间（10s）内有19个请求全部失败了。错误百分比是100%，但熔断器不会打开，因为requestVolumeThreshold的值是20.

  **6、circuitBreaker.sleepWindowInMilliseconds**
  半开试探休眠时间，默认值5000ms。当熔断器开启一段时间之后比如5000ms，会尝试放过去一部分流量进行试探，确定依赖服务是否恢复。

+ 查看熔断器的信息（Dashboard）

  需要查看到`Hystrix`的熔断信息等，可以使用`Dashboard`可视化工具来查看

  1.依赖

  **主要依赖不需要spring-boot-starter-web这个依赖，不然会报错**

  ```java
  <dependencies>
      <dependency>
          <groupId>org.springframework.cloud</groupId>
          <artifactId>spring-cloud-starter-netflix-hystrix-dashboard</artifactId>
      </dependency>
      <dependency>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-actuator</artifactId>
      </dependency>
  
      <dependency>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-devtools</artifactId>
          <scope>runtime</scope>
          <optional>true</optional>
      </dependency>
      <dependency>
          <groupId>org.projectlombok</groupId>
          <artifactId>lombok</artifactId>
          <optional>true</optional>
      </dependency>
      <dependency>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-test</artifactId>
          <scope>test</scope>
      </dependency>
  </dependencies>
  ```

  2.yml配置文件

  ```java
  server:
    port: 9001
  hystrix:
    dashboard:
      proxy-stream-allow-list: localhost
  ```

  3.主启动类

  ```java
  @SpringBootApplication
  @EnableHystrixDashboard
  public class HystrixDashboardMain9001
  {
      public static void main(String[] args) {
          SpringApplication.run(HystrixDashboardMain9001.class, args);
      }
  }
  ```

  4.需要在具体提供的Hystrix端添加ServletRegistrationBean

  ```java
  /**
   *此配置是为了服务监控而配置，与服务容错本身无关，springcloud升级后的坑
   *ServletRegistrationBean因为springboot的默认路径不是"/hystrix.stream"，
   *只要在自己的项目里配置上下面的servlet就可以了
   */
  @Bean
  public ServletRegistrationBean getServlet() {
      HystrixMetricsStreamServlet streamServlet = new HystrixMetricsStreamServlet();
      ServletRegistrationBean registrationBean = new ServletRegistrationBean(streamServlet);
      registrationBean.setLoadOnStartup(1);
      registrationBean.addUrlMappings("/hystrix.stream");
      registrationBean.setName("HystrixMetricsStreamServlet");
      return registrationBean;
  }
  ```

  