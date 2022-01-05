* ElisticAPM,Rocketmq support apm,OpenTracing,OpenTelemetry
* 支持对Rocketmq,SpringGateway,XxlJob组件的ElasticAPM监控

# 背景

## 什么是ElasticAPM?

Elastic APM方案是世界上第一个开源的APM 解决方案：

* 记录数据库查询，外部HTTP请求以及对应用程序的请求期间发生的其他缓慢操作的跟踪
* 很容易让程序员看到应用在运行时各个部分所花的时间
* 它收集未处理的错误和异常
* 很容让程序员调试错误
* 在客户面对性能瓶颈和错误之前先定位到问题所在
* 提高开发团队的生产力

## 工程介绍

Elastic APM默认是不支持对一些中间件进行链路追踪的，这会导致分布式系统监控出现监控断层，影响全链路监控的完整性。

* 支持中间件
  * RocketMQ
    * [Consumer](https://github.com/yanha1860/sunflower-model-monitor/blob/main/src/main/java/com/sunflower/model/monitor/apm/RocketmqConsumerApmMonitor.java)
    * [Producer](https://github.com/yanha1860/sunflower-model-monitor/blob/main/src/main/java/com/sunflower/model/monitor/apm/RocketmqProducerApmMonitor.java)
    * <img src="https://github.com/yanha1860/sunflower-model-monitor/blob/main/src/main/resources/img/RocketMQOfAPM.png">
  * [XxlJob](https://github.com/yanha1860/sunflower-model-monitor/blob/main/src/main/java/com/sunflower/model/monitor/apm/XxlJobApmMonitor.java)
    * <img src="https://github.com/yanha1860/sunflower-model-monitor/blob/main/src/main/resources/img/XxlJob.png"> 
  * [SpringGateway](https://github.com/yanha1860/sunflower-model-monitor/blob/main/src/main/java/com/sunflower/model/monitor/apm/SpringGatewayMonitor.java)
    * <img src="https://github.com/yanha1860/sunflower-model-monitor/blob/main/src/main/resources/img/SpringGatewayOfAPM.jpg"> 

