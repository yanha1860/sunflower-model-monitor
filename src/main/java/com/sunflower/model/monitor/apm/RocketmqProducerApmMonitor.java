package com.sunflower.model.monitor.apm;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Scope;
import co.elastic.apm.api.Span;

/**
 * 切面类，apm监控rocketmq
 *
 * @author yanghaipeng
 * @date 2021/12/29
 */
@Aspect
@Component
public class RocketmqProducerApmMonitor {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RocketmqProducerApmMonitor.class);

    // 这里实现的及其不优雅，甚至会照成组件相互依赖的风险（监控模块）
    // 为什么不aop更底层的接口（rocketmq接口）？
    // 参照：《rocketMqTemplataop失效分析》，https://github.com/tanghuibo/rocket-mq-template-aop-invalid-analysis
    @Pointcut("execution(* com.sunflower.model.mq.rocketmq.service.MqService.send(String,com.alibaba.fastjson.JSONObject,com.alibaba.fastjson.JSONObject))")
    private void send() {}

    @Around("send()")
    public Object apmMonitor(ProceedingJoinPoint joinPoint) throws Throwable {
        Object obj = null;

        Object[] args = joinPoint.getArgs();
        Span transaction = ElasticApm.currentTransaction().startSpan(APMConstant.TRANSACTION_TYPE_MQ,
            APMConstant.TRANSACTION_TYPE_MQ, APMConstant.TRANSACTION_ACTION_PRODUCE);
        try (final Scope scope = transaction.activate()) {
            try {
                MethodSignature signature = (MethodSignature)joinPoint.getSignature();
                // 当前执行的方法名称
                String name = signature.getName();
                // 获取执行方法的类名
                String[] classNameStr = joinPoint.getTarget().getClass().getName().split("\\.");
                String transactionName = classNameStr[classNameStr.length - 1] + "#" + name;
                // 设置transaction的名字与类型
                transaction.setName(transactionName);
                // 注入trace
                JSONObject traceHeaders = new JSONObject();
                transaction.injectTraceHeaders((key, value) -> traceHeaders.put(key, value));
                JSONObject headers = (JSONObject)args[2];
                headers.put("apm", traceHeaders.toJSONString());
                String queue = (String)args[0];
                JSONObject data = (JSONObject)args[1];
                transaction.addLabel("queue", queue);
                transaction.addLabel("data", data.toJSONString());

            } catch (Exception e) {
                logger.error("set transaction error", e);
            }

            // 进入我们的方法执行代码
            obj = joinPoint.proceed(args);

        } finally {
            // 结束transaction
            if (transaction != null) {
                try {
                    transaction.end();
                } catch (Exception e) {
                    logger.error("end transaction error", e);
                }
            }
        }

        return obj;
    }

    @AfterThrowing(value = "send()", throwing = "ex")
    public void exceptionReport(Throwable ex) {
        try {
            // 建立span，切点发生异常时，上报apm
            ElasticApm.currentTransaction().captureException(ex);
        } catch (Exception e) {
            // 发生异常时输出logger文件
            logger.error("captureException error", e);
        }
    }

}
