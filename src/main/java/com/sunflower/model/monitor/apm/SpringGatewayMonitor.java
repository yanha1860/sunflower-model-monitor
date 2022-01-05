package com.sunflower.model.monitor.apm;

import java.net.InetSocketAddress;
import java.util.Locale;

import javax.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.alibaba.fastjson.JSON;
import com.yesoulchina.service.common.BizException;
import com.yesoulchina.service.common.ResultMsg;

import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Scope;
import co.elastic.apm.api.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;

/**
 * 对webflux的apm监控处理
 * 
 * @author yangahaipeng
 *
 */
@Component
public class SpringGatewayMonitor implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(MonitorFilter.class);

    @Resource
    private MessageSource messageSource;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Transaction transaction = ElasticApm.startTransaction();

        try (final Scope scope = transaction.activate()) {
            String name = exchange.getRequest().getPath().toString();
            transaction.setName(name);

            try {
                // 记录请求header信息
                HttpHeaders httpHeaders = exchange.getRequest().getHeaders();
                for (String key : httpHeaders.keySet()) {
                    // 只获取第一个header信息
                    transaction.addLabel(key, httpHeaders.getFirst(key));
                }
            } catch (Exception e) {
            }

            try {
                // 记录client ip
                InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
                String clientIp = remoteAddress.getAddress().getHostAddress();
                transaction.addLabel("client.ip", clientIp);
            } catch (Exception e) {
            }

            // 放到头信息，跨请求线程的处理
            ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();
            transaction.injectTraceHeaders((key, value) -> requestBuilder.header(key, value));
            ServerWebExchange mutatedExchange = exchange.mutate().request(requestBuilder.build()).build();

            // do your thing...
            ServerHttpResponse response = exchange.getResponse();
            ServerHttpRequest request = exchange.getRequest();
            if (!ObjectUtil.isEmpty(request.getPath())) {
                transaction.addLabel("labels.url.path",  request.getPath().toString());
            }
            if (!ObjectUtil.isEmpty(request.getBody())) {
                transaction.addLabel("labels.body.original",  request.getBody().toString());

            }
            final DataBuffer bodyDataBuffer = response.bufferFactory().allocateBuffer();
            return chain.filter(mutatedExchange).onErrorResume(Exception.class, (e -> {
                log.error("system error", (Exception)e);
                transaction.captureException(e);

                response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

                String errorCode = "system_error";
                if (e instanceof BizException) {
                    errorCode = ((BizException)e).getErrorCode();
                }
                String resultStr = JSON.toJSONString(ResultMsg.fail(errorCode, getMessage(errorCode)));
                bodyDataBuffer.write(resultStr.getBytes());

                return response.writeAndFlushWith(Flux.just(ByteBufFlux.just(bodyDataBuffer)));

            })).doFinally(signal -> {
                DataBufferUtils.release(bodyDataBuffer);

                transaction.end();
            });
        }
    }

    @Override
    public int getOrder() {
        return -2;
    }

    /**
     * 根据异常code获取国际化信息
     * 
     * @param key
     * @param args
     * @return
     */
    private String getMessage(String key, String... args) {
        return messageSource.getMessage(String.valueOf(key), args, Locale.getDefault());
    }

}
