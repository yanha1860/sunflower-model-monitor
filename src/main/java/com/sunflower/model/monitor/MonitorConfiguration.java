package com.sunflower.model.monitor;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * 监控配置类
 * 
 * 
 * @author szekinwin
 *
 */
@Slf4j
@Configuration
@ComponentScan("com.sunflower.model.monitor.apm")
public class MonitorConfiguration {

    public MonitorConfiguration() {
        log.info("MonitorConfiguration:init..............");
    }

}
