package com.yowyob.easyrental.kernel.config;

import com.yowyob.easyrental.config.EasyRentalProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient beans for kernel-core integration.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({KernelClientProperties.class, EasyRentalProperties.class})
public class KernelWebClientConfig {

    @Bean
    public WebClient kernelWebClient(KernelClientProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeoutMs());

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(config -> config.defaultCodecs().maxInMemorySize(32 * 1024 * 1024))
                .filter(logRequestHeaders())
                .baseUrl(properties.getBaseUrl().endsWith("/")
                        ? properties.getBaseUrl().substring(0, properties.getBaseUrl().length() - 1)
                        : properties.getBaseUrl())
                .build();
    }

    private ExchangeFilterFunction logRequestHeaders() {
        return (request, next) -> {
            boolean traced = request.url().getPath().contains("/approve");
            if (traced) {
                log.info("[kernel-req] {} {} — headers: {}", request.method(), request.url(),
                        request.headers().entrySet().stream()
                                .map(e -> e.getKey() + "=[" + String.join(",", e.getValue().stream()
                                        .map(v -> v.length() > 30 ? v.substring(0, 30) + "…" : v).toList()) + "]")
                                .toList());
            }
            return next.exchange(request).doOnNext(response -> {
                if (traced) {
                    log.info("[kernel-resp] status={} — headers: {}",
                            response.statusCode(), response.headers().asHttpHeaders());
                }
            });
        };
    }

    /**
     * WebClient dédié aux uploads de fichiers vers kernel-core.
     * Timeouts très larges pour supporter les gros fichiers (jusqu'à ~16 Mo)
     * sur des connexions lentes sans déclencher le "Last write attempt timed out" de Netty.
     */
    @Bean
    public WebClient kernelFileWebClient(KernelClientProperties properties) {
        int timeoutMs = properties.getFileUploadTimeoutMs();
        int timeoutSec = Math.max(1, timeoutMs / 1000);

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(timeoutMs))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeoutMs())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(timeoutSec, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeoutSec, TimeUnit.SECONDS)));

        int maxBytes = Math.max(properties.getFileUploadMaxBytes(), 32 * 1024 * 1024);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(config -> config.defaultCodecs().maxInMemorySize(maxBytes))
                .baseUrl(properties.getBaseUrl().endsWith("/")
                        ? properties.getBaseUrl().substring(0, properties.getBaseUrl().length() - 1)
                        : properties.getBaseUrl())
                .build();
    }

    @Bean
    public WebClient kernelJwksWebClient(KernelClientProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeoutMs());

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
