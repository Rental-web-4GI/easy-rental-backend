package com.yowyob.easyrental.kernel.config;

import com.yowyob.easyrental.config.EasyRentalProperties;
import io.netty.channel.ChannelOption;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * WebClient beans for kernel-core integration.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
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
