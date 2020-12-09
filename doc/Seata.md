#### Seata

> http://seata.io/zh-cn/docs/overview/what-is-seata.html

Seata 是一款开源的分布式事务解决方案，致力于提供高性能和简单易用的分布式事务服务。Seata 将为用户提供了 AT、TCC、SAGA 和 XA 事务模式，为用户打造一站式的分布式解决方案。（官方文档注释很清楚）

###### 1.从一个微服务实例开始

由3个模块组成

- 仓储服务：对给定的商品扣除仓储数量。
- 订单服务：根据采购需求创建订单。
- 帐户服务：从用户帐户中扣除余额。
<img src="http://seata.io/img/architecture.png" alt="Architecture" style="zoom: 33%;" />

###### 2.下载安装Seata并配置具体的信息

> https://github.com/seata/seata/releases

打开Seata安装目录下的seata\conf\file.conf，修改前

```
service {
  #transaction service group mapping
  vgroup_mapping.my_test_tx_group = "default"
  #only support when registry.type=file, please don't set multiple addresses
  default.grouplist = "127.0.0.1:8091"
  #disable seata
  disableGlobalTransaction = false
}

## transaction log store, only used in seata-server
store {
  ## store mode: file、db
  mode = "file"

  ## file store property
  file {
    ## store location dir
    dir = "sessionStore"
  }

  ## database store property
  db {
    ## the implement of javax.sql.DataSource, such as DruidDataSource(druid)/BasicDataSource(dbcp) etc.
    datasource = "dbcp"
    ## mysql/oracle/h2/oceanbase etc.
    db-type = "mysql"
    driver-class-name = "com.mysql.jdbc.Driver"
    url = "jdbc:mysql://127.0.0.1:3306/seata"
    user = "mysql"
    password = "mysql"
  }
}
```

修改后：

```
service {
  #transaction service group mapping
  vgroup_mapping.my_test_tx_group = "seata_tx_group"   #修改分组名字
  #only support when registry.type=file, please don't set multiple addresses
  default.grouplist = "127.0.0.1:8091"
  #disable seata
  disableGlobalTransaction = false
}

## transaction log store, only used in seata-server
store {
  ## store mode: file、db
  mode = "db"

  ## file store property
  file {
    ## store location dir
    dir = "sessionStore"
  }

  ##修改数据库的信息
  ## database store property
  db {
    ## the implement of javax.sql.DataSource, such as DruidDataSource(druid)/BasicDataSource(dbcp) etc.
    datasource = "dbcp"
    ## mysql/oracle/h2/oceanbase etc.
    db-type = "mysql"
    driver-class-name = "com.mysql.jdbc.Driver"
    url = "jdbc:mysql://127.0.0.1:3306/seata"
    user = "root"
    password = "root"
  }
}
```

修改Seata安装目录下的seata\conf\registry.conf，注册进nacos中

```
registry {
  # file 、nacos 、eureka、redis、zk、consul、etcd3、sofa
  type = "nacos"   ##注册进nacos

  nacos {
    serverAddr = "localhost"
    namespace = ""
    cluster = "default"
  }
```

创建undo_log表

SEATA AT 模式需要 `UNDO_LOG` 表

```sql
-- 注意此处0.3.0+ 增加唯一索引 ux_undo_log
CREATE TABLE `undo_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `branch_id` bigint(20) NOT NULL,
  `xid` varchar(100) NOT NULL,
  `context` varchar(128) NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int(11) NOT NULL,
  `log_created` datetime NOT NULL,
  `log_modified` datetime NOT NULL,
  `ext` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
```

为实际业务创造表

```sql
DROP TABLE IF EXISTS `storage_tbl`;
CREATE TABLE `storage_tbl` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `commodity_code` varchar(255) DEFAULT NULL,
  `count` int(11) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY (`commodity_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `order_tbl`;
CREATE TABLE `order_tbl` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` varchar(255) DEFAULT NULL,
  `commodity_code` varchar(255) DEFAULT NULL,
  `count` int(11) DEFAULT 0,
  `money` int(11) DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP TABLE IF EXISTS `account_tbl`;
CREATE TABLE `account_tbl` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` varchar(255) DEFAULT NULL,
  `money` int(11) DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

启动nacos服务，在启动Seata

```
2020-12-09 11:34:18.670 INFO [main]io.seata.core.rpc.netty.AbstractRpcRemotingServer.start:155 -Server started ...
2020-12-09 11:34:18.685 INFO [main]io.seata.common.loader.EnhancedServiceLoader.loadFile:236 -load RegistryProvider[Nacos] extension by class[io.seata.discovery.registry.nacos.NacosRegistryProvider]
```



###### 3. 建立仓储服务模块、订单服务模块、帐户服务模块

+ 订单模块

  1.依赖（seata版本是1.0.0，配置seata-all与版本相同较合适）

  ```
   <groupId>io.seata</groupId>
   <artifactId>seata-all</artifactId>
   <version>1.0.0</version>
  ```

  ```
  <dependencies>
      <!--nacos-->
      <dependency>
          <groupId>com.alibaba.cloud</groupId>
          <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
      </dependency>
      <!--seata-->
      <dependency>
          <groupId>com.alibaba.cloud</groupId>
          <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
          <exclusions>
              <exclusion>
                  <artifactId>seata-all</artifactId>
                  <groupId>io.seata</groupId>
              </exclusion>
          </exclusions>
      </dependency>
      <dependency>
          <groupId>io.seata</groupId>
          <artifactId>seata-all</artifactId>
          <version>1.0.0</version>
      </dependency>
      <!--feign-->
      <dependency>
          <groupId>org.springframework.cloud</groupId>
          <artifactId>spring-cloud-starter-openfeign</artifactId>
      </dependency>
      <!--web-actuator-->
      <dependency>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-web</artifactId>
      </dependency>
      <dependency>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-actuator</artifactId>
      </dependency>
      <!--mysql-druid-->
      <dependency>
          <groupId>mysql</groupId>
          <artifactId>mysql-connector-java</artifactId>
          <version>5.1.37</version>
      </dependency>
      <dependency>
          <groupId>com.alibaba</groupId>
          <artifactId>druid-spring-boot-starter</artifactId>
          <version>1.1.10</version>
      </dependency>
      <dependency>
          <groupId>org.mybatis.spring.boot</groupId>
          <artifactId>mybatis-spring-boot-starter</artifactId>
          <version>2.0.0</version>
      </dependency>
      <dependency>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-test</artifactId>
          <scope>test</scope>
      </dependency>
      <dependency>
          <groupId>org.projectlombok</groupId>
          <artifactId>lombok</artifactId>
          <optional>true</optional>
      </dependency>
  </dependencies>
  ```

  2.yml（1.0.0版本）

  ```yaml
  server:
    port: 2001
  
  spring:
    application:
      name: seata-order-service
    cloud:
      nacos:
        discovery:
          server-addr: localhost:8848
    datasource:
      driver-class-name: com.mysql.jdbc.Driver
      url: jdbc:mysql://localhost:3306/seata_order
      username: root
      password: root
  
  feign:
    hystrix:
      enabled: false
  
  logging:
    level:
      io:
        seata: info
  
  mybatis:
    mapperLocations: classpath:mapper/*.xml
  
  
  # 1.0新添加的enabled激活自动配置，使得我们可以在yaml/properties文件中配置，
  # 避免了以前需要客户端引入2个文件：
  # file.conf 和 registry.conf
  seata:
    enabled: true # 1.0新特性，需要依赖seata-spring-boot-starter,默认为true
    tx-service-group: seata_tx_group
    #  transport:
    #    type: TCP #default value is TCP
    #    server: NIO #default value is NIO
    #    heartbeat: true #enable heartbeat
    #    enable-client-batch-send-request: true
    #    serialization: seata
    #    compressor: none
    #    shutdown:
    #      wait: 3 #when destroy server, wait seconds
    registry:
      type: nacos
    #      application: default
    #      weight: 1
      service:
    #    vgroup-mapping: geekplus_tx_group
    #    disable-global-transaction: false
        disableGlobalTransaction: true
    client:
      support:
        spring:
          datasource-autoproxy: false
  #因为1.0.0反应极慢会超时，所以配置超时时间长一些     
  #hystrix的超时时间
  hystrix:
    command:
      default:
        execution:
          timeout:
            enabled: true
          isolation:
            thread:
              timeoutInMilliseconds: 30000
  #ribbon的超时时间
  ribbon:
    ReadTimeout: 30000
    ConnectTimeout: 30000
  ```

  如果一直报Could not found property service.disableGlobalTransaction, try to use default value instead.

  请在resources写一个file.conf，内容为：

  ```
  service {
    disableGlobalTransaction = false
  }
  ```

  在yml格式的配置中件中配置service.disableGlobalTransaction属性会无效，需创建file.conf文件在该文件中指定service.disableGlobalTransaction属性的值；或者使用properties格式的配置文件代替yml格式的配置文件（但我没有对这种说法进行验证）。

  

  3.config配置自己的数据源作为代理

  ```java
  @Configuration
  public class DataSourceProxyConfig {
  
      @Value("${mybatis.mapperLocations}")
      private String mapperLocations;
  
      @Bean
      @ConfigurationProperties(prefix = "spring.datasource")
      public DataSource druidDataSource(){
          return new DruidDataSource();
      }
  
      @Bean
      public DataSourceProxy dataSourceProxy(DataSource dataSource) {
          return new DataSourceProxy(dataSource);
      }
  
      @Bean
      public SqlSessionFactory sqlSessionFactoryBean(DataSourceProxy dataSourceProxy) throws Exception {
          SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
          sqlSessionFactoryBean.setDataSource(dataSourceProxy);
          sqlSessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(mapperLocations));
          sqlSessionFactoryBean.setTransactionFactory(new SpringManagedTransactionFactory());
          return sqlSessionFactoryBean.getObject();
      }
  
  }
  ```

  4.订单service实现模块

  ```java
  @Service
  @Slf4j
  public class OrderServiceImpl implements OrderService
  {
      @Resource
      private OrderDao orderDao;
      @Resource
      private StorageService storageService;
      @Resource
      private AccountService accountService;
  
      /**
       * 创建订单->调用库存服务扣减库存->调用账户服务扣减账户余额->修改订单状态
       * 简单说：下订单->扣库存->减余额->改状态
       *@GlobalTransactional 开启事务
       */
      @Override
      @GlobalTransactional(name = "seata-create-order",rollbackFor = Exception.class)
      public void create(Order order)
      {
          log.info("----->开始新建订单");
          //1 新建订单
          orderDao.create(order);
  
          //2 扣减库存
          log.info("----->订单微服务开始调用库存，做扣减Count");
          storageService.decrease(order.getProductId(),order.getCount());
          log.info("----->订单微服务开始调用库存，做扣减end");
  
          //3 扣减账户
          log.info("----->订单微服务开始调用账户，做扣减Money");
          accountService.decrease(order.getUserId(),order.getMoney());
          log.info("----->订单微服务开始调用账户，做扣减end");
  
          //4 修改订单状态，从零到1,1代表已经完成
          log.info("----->修改订单状态开始");
          orderDao.update(order.getUserId(),0);
          log.info("----->修改订单状态结束");
  
          log.info("----->下订单结束了");
  
      }
  }
  ```

  5.仓储模块接口，帐户模块接口

  ```java
  @FeignClient(value = "seata-account-service")
  public interface AccountService
  {
      @PostMapping(value = "/account/decrease")
      CommonResult decrease(@RequestParam("userId") Long userId, @RequestParam("money") BigDecimal money);
  }
  ```

  ```java
  @FeignClient(value = "seata-storage-service")
  public interface StorageService
  {
      @PostMapping(value = "/storage/decrease")
      CommonResult decrease(@RequestParam("productId") Long productId, @RequestParam("count") Integer count);
  }
  ```

+ 另外两个模块建立也是差不多类型的建立模块

+ 测试

  http://localhost:2001/order/create?userId=1&productId=1&count=1&money=123

  成功

