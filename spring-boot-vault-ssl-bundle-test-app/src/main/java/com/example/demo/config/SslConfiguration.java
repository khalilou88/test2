package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * SSL Configuration for the test application.
 * Provides RestTemplate beans configured with SSL bundles.
 */
@Configuration
public class SslConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SslConfiguration.class);

    /**
     * RestTemplate configured with Vault SSL bundle for secure communications.
     */
    @Bean
    public RestTemplate secureRestTemplate(RestTemplateBuilder restTemplateBuilder, SslBundles sslBundles) {

        try {

            var restTemplate = restTemplateBuilder
                    .setSslBundle(sslBundles.getBundle("vault:secret/ssl-certs/server-a"))
                    .build();

            logger.info("Configured secure RestTemplate with Vault SSL bundle");

            return restTemplate;

        } catch (Exception e) {
            logger.error("Failed to configure secure RestTemplate, falling back to default", e);
            return restTemplateBuilder.build();
        }
    }
}
