package com.demo.vault.ssl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ssl.*;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

/**
 * Registry for managing SSL bundles loaded from HashiCorp Vault.
 */
public class VaultSslBundleRegistry implements SslBundleRegistry, SslBundles {

    private static final Logger logger = LoggerFactory.getLogger(VaultSslBundleRegistry.class);

    private final VaultTemplate vaultTemplate;
    private final Map<String, SslBundle> bundles = new ConcurrentHashMap<>();

    public VaultSslBundleRegistry(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    @Override
    public void registerBundle(String name, SslBundle bundle) {
        logger.debug("Requesting SSL bundle: {}", name);

        // Check if bundle name starts with "vault:" protocol
        if (!name.startsWith("vault:")) {
            throw new IllegalArgumentException("Bundle name must start with 'vault:' protocol");
        }

        bundles.computeIfAbsent(name, this::loadBundleFromVault);
    }

    @Override
    public void updateBundle(String name, SslBundle updatedBundle) throws NoSuchSslBundleException {
        bundles.put(name, updatedBundle);
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

            // TODO check this field
            String keyPassword = (String) data.get("key_password");

            if (certificate == null || privateKey == null) {
                throw new RuntimeException("Missing required certificate or private_key in Vault data");
            }

            return new VaultSslBundle(certificate, privateKey, caCertificate, keyPassword);

        } catch (Exception e) {
            logger.error("Failed to load SSL bundle from Vault: {}", bundleName, e);
            throw new RuntimeException("Failed to load SSL bundle: " + bundleName, e);
        }
    }

    @Override
    public SslBundle getBundle(String name) throws NoSuchSslBundleException {
        return this.bundles.get(name);
    }

    @Override
    public void addBundleUpdateHandler(String name, Consumer<SslBundle> updateHandler)
            throws NoSuchSslBundleException {}
}
