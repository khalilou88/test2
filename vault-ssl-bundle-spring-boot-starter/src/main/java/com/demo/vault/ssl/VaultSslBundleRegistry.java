package com.demo.vault.ssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleRegistry;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing SSL bundles loaded from HashiCorp Vault.
 */
public class VaultSslBundleRegistry implements SslBundleRegistry {

    private static final Logger logger = LoggerFactory.getLogger(VaultSslBundleRegistry.class);

    private final VaultTemplate vaultTemplate;
    private final Map<String, SslBundle> bundles = new ConcurrentHashMap<>();

    public VaultSslBundleRegistry(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

//    @Override
    public SslBundle getBundle(String bundleName) {
        logger.debug("Requesting SSL bundle: {}", bundleName);

        // Check if bundle name starts with "vault:" protocol
        if (!bundleName.startsWith("vault:")) {
            throw new IllegalArgumentException("Bundle name must start with 'vault:' protocol");
        }

        return bundles.computeIfAbsent(bundleName, this::loadBundleFromVault);
    }

    private SslBundle loadBundleFromVault(String bundleName) {
        try {
            // Extract vault path from bundle name (remove "vault:" prefix)
            String vaultPath = bundleName.substring(6);
            logger.debug("Loading SSL bundle from Vault path: {}", vaultPath);

            VaultResponse response = vaultTemplate.read(vaultPath);
            if (response == null || response.getData() == null) {
                throw new RuntimeException("No SSL certificate data found at Vault path: " + vaultPath);
            }

            Map<String, Object> data = response.getData();
            String certificate = (String) data.get("certificate");
            String privateKey = (String) data.get("private_key");
            String caCertificate = (String) data.get("ca_certificate");

            if (certificate == null || privateKey == null) {
                throw new RuntimeException("Missing required certificate or private_key in Vault data");
            }

            return createSslBundle(certificate, privateKey, caCertificate);

        } catch (Exception e) {
            logger.error("Failed to load SSL bundle from Vault: {}", bundleName, e);
            throw new RuntimeException("Failed to load SSL bundle: " + bundleName, e);
        }
    }

    private SslBundle createSslBundle(String certificatePem, String privateKeyPem, String caCertificatePem) {
        try {
            // Parse certificate
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certificatePem.getBytes()));

            // Parse private key
            PrivateKey privKey = parsePrivateKey(privateKeyPem);

            // Parse CA certificate if present
            X509Certificate caCert = null;
            if (caCertificatePem != null && !caCertificatePem.trim().isEmpty()) {
                caCert = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(caCertificatePem.getBytes()));
            }

            // Create KeyStore for the key material
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);

            Certificate[] certChain = caCert != null ? new Certificate[]{cert, caCert} : new Certificate[]{cert};

            keyStore.setKeyEntry("vault-ssl", privKey, "changeit".toCharArray(), certChain);

            // Create trust store
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            trustStore.load(null, null);

            if (caCert != null) {
                trustStore.setCertificateEntry("vault-ca", caCert);
            }
            trustStore.setCertificateEntry("vault-cert", cert);

            return new VaultSslBundle(keyStore, trustStore, "changeit");

        } catch (Exception e) {
            throw new RuntimeException("Failed to create SSL bundle from Vault data", e);
        }
    }

    private PrivateKey parsePrivateKey(String privateKeyPem) {
        try {
            // Remove PEM headers and decode
            String keyData = privateKeyPem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replace("-----BEGIN RSA PRIVATE KEY-----", "").replace("-----END RSA PRIVATE KEY-----", "").replaceAll("\\s", "");

            byte[] keyBytes = java.util.Base64.getDecoder().decode(keyData);

            java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);

            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse private key", e);
        }
    }

    @Override
    public void registerBundle(String name, SslBundle bundle) {

    }

    @Override
    public void updateBundle(String name, SslBundle updatedBundle) throws NoSuchSslBundleException {

    }
}