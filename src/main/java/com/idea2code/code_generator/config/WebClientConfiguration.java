package com.idea2code.code_generator.config;

import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;

@Configuration
@RequiredArgsConstructor
public class WebClientConfiguration {
    private final CodeGenProperties codeGenProperties;

    @Bean
    public WebClient webClient() {
        int connectTimeout = codeGenProperties.getConnectTimeoutMs() == 0 ? 3000 : codeGenProperties.getConnectTimeoutMs();
        long readTimeout = codeGenProperties.getReadTimeoutMs() == 0 ? 5000 : codeGenProperties.getReadTimeoutMs();
        long writeTimeout = codeGenProperties.getWriteTimeoutMs() == 0 ? 5000 : codeGenProperties.getWriteTimeoutMs();
        LogLevel logLevel = LogLevel.INFO;
        ConnectionProvider connectionProvider = ConnectionProvider.builder("code-gen-webclient-conn-pool")
                .maxConnections(100)
                .maxIdleTime(Duration.ofMillis(6000))
                .maxLifeTime(Duration.ofMillis(7000))
                .evictInBackground(Duration.ofMillis(7000))
                .build();
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .secure(sslContextSpec -> {
                    try {
                        sslContextSpec.sslContext(
                                SslContextBuilder.forClient()
                                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                        .build());
                    } catch (SSLException e) {
                        throw new RuntimeException(e);
                    }
                }).
                doOnConnected(conn -> conn.addHandlerFirst(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS)
                ).addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS)))
                .option(CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .wiretap("reactor.netty.http.client.HttpClient", logLevel, AdvancedByteBufFormat.TEXTUAL)
                .followRedirect(true)
                .responseTimeout(Duration.ofMillis(readTimeout));
        ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        return WebClient.builder().clientConnector(connector).build();
    }
}
