package com.sunflower.model.monitor.apm;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Scope;
import co.elastic.apm.api.Transaction;

/**
 * 切面类，apm监控xxl-job
 *
 * @author yanghaipeng
 * @date 2021/4/19 16:05
 */
@Aspect
@Component
public class XxlJobApmMonitor {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(XxlJobApmMonitor.class);

    /**
     * 将带有@xxljob注解的类作为切点
     *
     * @author wangjialong
     * @date 2021/4/21 14:54
     */
    @Pointcut("@annotation(com.xxl.job.core.handler.annotation.XxlJob)")
    private void xxlJobCut() {}

    @Around("xxlJobCut()")
    public Object apmMonitor(ProceedingJoinPoint joinPoint) throws Throwable {
        Object obj = null;
        String transactionName = null;
        Transaction transaction = ElasticApm.startTransaction();
        try (final Scope scope = transaction.activate()) {
            try {
                MethodSignature signature = (MethodSignature)joinPoint.getSignature();
                // 当前执行的方法名称
                String name = signature.getName();
                // 获取执行方法的类名
                String[] classNameStr = joinPoint.getTarget().getClass().getName().split("\\.");
                transactionName = classNameStr[classNameStr.length - 1] + "#" + name;
                // 设置transaction的名字与类型
                transaction.setName(transactionName);
                transaction.setType(APMConstant.TRANSACTION_TYPE_JOB);

            } catch (Exception e) {
                logger.error("set transaction error", e);
            }

            Object[] args = joinPoint.getArgs();
            // 进入我们的方法执行代码
            obj = joinPoint.proceed(args);

        } catch (Exception e) {
            // 发生异常时输出logger文件
            logger.error("proceed error, name : " + transactionName, e);

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

    @AfterThrowing(value = "xxlJobCut()", throwing = "ex")
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
