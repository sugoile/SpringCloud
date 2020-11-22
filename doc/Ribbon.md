#### Ribbon

1. ###### `ribbon`主要作用

    `Spring Cloud Ribbon`是一个基于HTTP和TCP的客户端负载均衡工具，它基于Netflix Ribbon实现。通过Spring Cloud的封装，可以让我们轻松地将面向服务的REST模版请求自动转换成客户端负载均衡的服务调用。

    1. 目前主流的负载方案分为以下两种：

    - 集中式负载均衡，在消费者和服务提供方中间使用独立的代理方式进行负载，有硬件的（比如 F5），也有软件的（比如 Nginx）。
    - 客户端自己做负载均衡，根据自己的请求情况做负载，Ribbon 就属于客户端自己做负载。

    2. Ribbon负载均衡 VS Nginx负载均衡：

    - Nginx是服务器端负载均衡 ，客户端的请求都发给Nginx，Nginx实现分发，将请求发送到不同的服务器上。

    - Ribbon是客户端负载均衡 ，在调用微服务接口的时候，会在注册中心拿到注册信息服务列表缓存到本地JVM，从而在本地实现RPC远程服务调用技术。

      

2. ###### 调用

    通过Spring Cloud Ribbon的封装，我们在微服务架构中使用客户端负载均衡调用非常简单，只需要如下两步：

      *  ###### 服务提供者只需要启动多个服务实例并注册到一个注册中心或是多个相关联的服务注册中心。

      *  ###### 服务消费者直接通过调用被@LoadBalanced注解修饰过的RestTemplate来实现面向服务的接口调用。

    这样，我们就可以将服务提供者的高可用以及服务消费者的负载均衡调用一起实现了。

    

3. ###### 配置

    由于`eureka`自带了`ribbon`的包，所以引用了`ribbon`就不需要我们重新到`ribbon`包

    如若需要，导：

    ```java
    <dependency>
    	<groupId>org.springframework.cloud</groupId> 
    	<artifactId>spring-cloud-starter-netflix-ribbon</artifactId>
    </dependency>
    ```

    ribbon有官方自带以下的策略：

    | 策略名                    | 策略描述                                                     | 实现说明                                                     |
    | ------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
    | BestAvailableRule         | 选择一个最小的并发请求的server                               | 逐个考察Server，如果Server被tripped了，则忽略，在选择其中ActiveRequestsCount最小的server |
    | AvailabilityFilteringRule | 过滤掉那些因为一直连接失败的被标记为circuit tripped的后端server，并过滤掉那些高并发的的后端server（active connections 超过配置的阈值） | 使用一个AvailabilityPredicate来包含过滤server的逻辑，其实就就是检查status里记录的各个server的运行状态 |
    | WeightedResponseTimeRule  | 根据响应时间分配一个weight，响应时间越长，weight越小，被选中的可能性越低。 | 一个后台线程定期的从status里面读取评价响应时间，为每个server计算一个weight。Weight的计算也比较简单responsetime 减去每个server自己平均的responsetime是server的权重。当刚开始运行，没有形成status时，使用roubine策略选择server。 |
    | RetryRule                 | 对选定的负载均衡策略机上重试机制。                           | 在一个配置时间段内当选择server不成功，则一直尝试使用subRule的方式选择一个可用的server |
    | RoundRobinRule            | roundRobin方式轮询选择server                                 | 轮询index，选择index对应位置的server                         |
    | RandomRule                | 随机选择一个server                                           | 在index上随机，选择index对应位置的server                     |
    | ZoneAvoidanceRule         | 复合判断server所在区域的性能和server的可用性选择server       | 使用ZoneAvoidancePredicate和AvailabilityPredicate来判断是否选择某个server，前一个判断判定一个zone的运行性能是否可用，剔除不可用的zone（的所有server），AvailabilityPredicate用于过滤掉连接数过多的Server。 |

    

    以下实现了简单的负载均衡，使用的是官方自带的轮询策略：

    ```java
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    @Autowired
    private RestTemplate restTemplate;
    ```

    更换其他的负载均衡策略：

    官方文档说明：如果想要替换Ribbon的轮询算法，这个自定义配置类不能放在@ComponentScan所扫描的当前包下以及子包下，否则我们自定义的这个配置类就会被所有的Ribbon客户端所共享，达不到特殊化定制的目的了，也就是不能放在主启动类所在的包及子包下，因此新建一个包并定义一个配置类将轮询算法换为随机算法：

    ```java
    @Configuration
    public class RibbonRule {
        @Bean
        public IRule MyRule(){
            return new RandomRule();
        }
    }
    ```

    ```java
    @SpringBootApplication
    @EnableEurekaClient
    @RibbonClient(name = "CLOUD-PAYMENT-SERVICE", configuration= RibbonRule.class)
    //name是服务提供者名称（地址），表明负载均衡用在服务提供者上
    //configuration = RibbonRule.class是调用自定义的负载均衡配置类（使用自定义的随机算法）
    public class OrderMain80 {
        public static void main(String[] args) {
            SpringApplication.run(OrderMain80.class, args);
        }
    }
    ```

4. 轮询算法手写

    1. 测试接口

       ```java
       @GetMapping(value = "/payment/serverport")
       public String serverport(){
           return Server;
       }
       ```

    2. 注释restTemplate注释的@LoadBalanced

       ```java
       @Configuration
       public class ApplicationContextConfig {
       
           @Bean
           //@LoadBalanced
           public RestTemplate restTemplate() {
               return new RestTemplate();
           }
       }
       ```

    3. 自定义负载均衡算法和实现

       负载均衡算法：rest接口第几次请求数 % 服务器集群总数量 = 实际调用服务器位置下标  ，每次服务重启动后rest接口计数从1  开始。
    
       ```java
   public interface MyLoadBalanced {
           ServiceInstance instances(List<ServiceInstance> serviceInstances);
       }
       ```
    
       ```java
       @Component
       public class MyLoadBalancedImp implements MyLoadBalanced {
       
           private AtomicInteger atomicInteger = new AtomicInteger(0);
       
           public final int getAndIncrement()
           {
               int current;
               int next;
               do {
                   current = this.atomicInteger.get();
                   next = current >= 2147483647 ? 0 : current + 1;
               }while(!this.atomicInteger.compareAndSet(current,next));
               System.out.println("*****第几次访问，次数next: "+next);
               return next;
           }
       
           //负载均衡算法：rest接口第几次请求数 % 服务器集群总数量 = 实际调用服务器位置下标  ，每次服务重启动后rest接口计数从1开始。
           @Override
           public ServiceInstance instances(List<ServiceInstance> serviceInstances)
           {
               int index = getAndIncrement() % serviceInstances.size();
       
               return serviceInstances.get(index);
           }
       }
       ```
    
    4. 在controller层加入负载均衡算法
    
       ```java
       @Autowired
       private MyLoadBalanced myLoadBalanced;
       
       @Autowired
       private DiscoveryClient discoveryClient;
       
       @GetMapping(value = "/consumer/payment/serverport")
           public String getPaymentLB()
           {
               List<ServiceInstance> instances = discoveryClient.getInstances("CLOUD-PAYMENT-SERVICE");
       
               if(instances == null || instances.size() <= 0)
               {
                   return null;
               }
       
               ServiceInstance serviceInstance = myLoadBalanced.instances(instances);
               URI uri = serviceInstance.getUri();
       
               return restTemplate.getForObject(uri+"/payment/serverport",String.class);
       
           }
       ```