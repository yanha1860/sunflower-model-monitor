package com.sunflower.model.monitor.apm;

import java.util.Map;

import org.apache.rocketmq.common.message.Message;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.HeaderExtractor;
import co.elastic.apm.api.Scope;
import co.elastic.apm.api.Transaction;

/**
 * 切面类，apm监控rocketmq
 *
 * @author yanghaipeng
 * @date 2021/12/29
 */
@Aspect
@Component
public class RocketmqConsumerApmMonitor {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RocketmqConsumerApmMonitor.class);

    @Pointcut("execution(* org.apache.rocketmq.spring.core.RocketMQListener.onMessage(..))")
    private void onMessage() {}

    @Around("onMessage()")
    public Object apmMonitor(ProceedingJoinPoint joinPoint) throws Throwable {
        Object obj = null;

        Object[] args = joinPoint.getArgs();
        Transaction transaction = null;
        try {
            Message message = (Message)args[0];
            Map<String, String> properties = message.getProperties();
            String traceHeadersStr = properties.get("apm");
            if (StringUtils.isEmpty(traceHeadersStr)) {
                transaction = ElasticApm.startTransaction();

            } else {
                JSONObject traceHeaders = JSON.parseObject(traceHeadersStr);
                transaction = ElasticApm.startTransactionWithRemoteParent(new HeaderExtractor() {
                    @Override
                    public String getFirstHeader(String headerName) {
                        return traceHeaders.getString(headerName);
                    }
                });
            }
            MethodSignature signature = (MethodSignature)joinPoint.getSignature();
            // 当前执行的方法名称
            String name = signature.getName();
            // 获取执行方法的类名
            String[] classNameStr = joinPoint.getTarget().getClass().getName().split("\\.");
            String transactionName = classNameStr[classNameStr.length - 1] + "#" + name;
            // 设置transaction的名字与类型
            transaction.setName(transactionName);
            transaction.setType(APMConstant.TRANSACTION_TYPE_MQ);
            transaction.addLabel("message", JSON.toJSONString(message));

        } catch (Exception e) {
            logger.error("create transaction error, args : " + JSON.toJSONString(args), e);
        }

        try (final Scope scope = transaction.activate()) {
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

    @AfterThrowing(value = "onMessage()", throwing = "ex")
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
