package com.sunflower.model.monitor.anno;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 此异常不需要记录到标准的监控信息日志中， 被注解的异常信息将以WARN的级别记录到普通日志文件中， 用于后续需要的时候检查。
 * 
 * @author admin
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MonitorIgnoreExcept {

}
