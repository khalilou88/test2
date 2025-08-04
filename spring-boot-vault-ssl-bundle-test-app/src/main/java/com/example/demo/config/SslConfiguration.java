package com.example.demo.config;


import io.netty.handler.ssl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;

/**
 * SSL Configuration for the test application.
 * Provides WebClient beans configured with SSL bundles.
 */
@Configuration
public class SslConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SslConfiguration.class);

    /**
     * WebClient configured with Vault SSL bundle for secure communications.
     */
    @Bean
    public WebClient secureWebClient(SslBundles sslBundles) {
        try {
            var bundle = sslBundles.getBundle("vault:secret/ssl-certs/server-a");
//            var sslContext = bundle.createSslContext();

//            HttpClient httpClient = HttpClient.create()
//                    .secure(spec -> spec.sslContext(sslContext));


            javax.net.ssl.SSLContext jdkSslContext = bundle.createSslContext();

            SslContext nettySslContext = new JdkSslContext(
                    jdkSslContext,
                    /* isClient */ true,
                    null,   // cipher suites (null = default)
                    IdentityCipherSuiteFilter.INSTANCE,
                    ApplicationProtocolConfig.DISABLED,
                    ClientAuth.NONE,
                    null,   // protocols
                    false   // startTls
            );

            HttpClient httpClient = HttpClient.create().secure(spec -> spec.sslContext(nettySslContext));

            ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

            logger.info("Configured secure WebClient with Vault SSL bundle");

            return WebClient.builder()
                    .clientConnector(connector)
                    .build();

        } catch (Exception e) {
            logger.error("Failed to configure secure WebClient, falling back to default", e);
            return WebClient.builder().build();
        }
    }

    /**
     * Insecure WebClient for testing purposes (bypasses SSL verification).
     */
    @Bean("insecureWebClient")
    public WebClient insecureWebClient() {
        try {
            var sslContext = io.netty.handler.ssl.SslContextBuilder
                    .forClient()
                    .trustManager(io.netty.handler.ssl.util.InsecureTrustManagerFactory.INSTANCE)
                    .build();

            HttpClient httpClient = HttpClient.create()
                    .secure(spec -> spec.sslContext(sslContext));

            ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

            logger.warn("Configured INSECURE WebClient - DO NOT USE IN PRODUCTION");

            return WebClient.builder().clientConnector(connector).build();

        } catch (Exception e) {
            logger.error("Failed to configure insecure WebClient", e);
            return WebClient.builder().build();
        }
    }
}