package com.example.demo.controller;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * Test controller to verify SSL bundle functionality.
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    private final SslBundles sslBundles;
    private final RestTemplate restTemplate;
    private final String serverBUrl;

    public TestController(
            SslBundles sslBundles,
            RestTemplate restTemplate,
            @Value("${app.server-b.url:https://localhost:8444}") String serverBUrl) {
        this.sslBundles = sslBundles;
        this.restTemplate = restTemplate;
        this.serverBUrl = serverBUrl;
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "Vault SSL Test Application is running");

        logger.info("Health check requested");
        return ResponseEntity.ok(response);
    }

    /**
     * SSL bundle information endpoint
     */
    @GetMapping("/ssl-info")
    public ResponseEntity<Map<String, Object>> getSslInfo() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Get SSL bundle information
            var bundle = sslBundles.getBundle("vault:secret/ssl-certs/server-a");
            var keyStore = bundle.getStores().getKeyStore();
            var trustStore = bundle.getStores().getTrustStore();

            response.put("status", "SUCCESS");
            response.put("bundleLoaded", true);
            response.put("keyStoreType", keyStore.getType());
            response.put("keyStoreSize", keyStore.size());
            response.put("trustStoreType", trustStore.getType());
            response.put("trustStoreSize", trustStore.size());

            // Get certificate information
            if (keyStore.containsAlias("vault-ssl")) {
                Certificate cert = keyStore.getCertificate("vault-ssl");
                if (cert instanceof X509Certificate x509) {
                    response.put(
                            "certificateSubject", x509.getSubjectX500Principal().getName());
                    response.put(
                            "certificateIssuer", x509.getIssuerX500Principal().getName());
                    response.put("certificateNotBefore", x509.getNotBefore());
                    response.put("certificateNotAfter", x509.getNotAfter());
                    response.put(
                            "certificateSerialNumber", x509.getSerialNumber().toString());
                }
            }

            logger.info("SSL bundle information retrieved successfully");

        } catch (Exception e) {
            logger.error("Failed to retrieve SSL bundle information", e);
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            response.put("bundleLoaded", false);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Test SSL connection to another service
     */
    @GetMapping("/test-ssl-connection")
    public ResponseEntity<Map<String, Object>> testSslConnection() {
        Map<String, Object> response = new HashMap<>();

        try {

            // Make a test request
            String result = restTemplate.getForObject(serverBUrl + "/api/test/health", String.class);

            response.put("status", "SUCCESS");
            response.put("targetUrl", serverBUrl);
            response.put("connectionResult", "Connected successfully");
            response.put("responseData", result);

            logger.info("SSL connection test successful to: {}", serverBUrl);

        } catch (Exception e) {
            logger.error("SSL connection test failed", e);
            response.put("status", "ERROR");
            response.put("targetUrl", serverBUrl);
            response.put("error", e.getMessage());
            response.put("connectionResult", "Connection failed");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get current SSL session information
     */
    @GetMapping("/ssl-session")
    public ResponseEntity<Map<String, Object>> getSslSession(
            @RequestHeader(value = "X-SSL-CERT", required = false) String clientCert) {

        Map<String, Object> response = new HashMap<>();

        try {
            response.put("status", "SUCCESS");
            response.put("httpsEnabled", true);
            response.put("clientCertificatePresent", clientCert != null);

            if (clientCert != null) {
                response.put("clientCertificateHeader", clientCert);
            }

            logger.info("SSL session information retrieved");

        } catch (Exception e) {
            logger.error("Failed to retrieve SSL session information", e);
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Reload SSL bundle (for testing certificate rotation)
     */
    @PostMapping("/reload-ssl")
    public ResponseEntity<Map<String, Object>> reloadSsl() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Force reload by requesting the bundle again
            var bundle = sslBundles.getBundle("vault:secret/ssl-certs/server-a");

            response.put("status", "SUCCESS");
            response.put("message", "SSL bundle reloaded successfully");
            response.put("timestamp", LocalDateTime.now());

            logger.info("SSL bundle reloaded");

        } catch (Exception e) {
            logger.error("Failed to reload SSL bundle", e);
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}
