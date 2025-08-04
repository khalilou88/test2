package com.demo.vault.ssl.autoconfigure;

import com.demo.vault.ssl.VaultSslBundleRegistry;
import com.demo.vault.ssl.VaultSslBundleRegistrar;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.cloud.vault.config.VaultOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for Vault SSL Bundle support.
 * Enables SSL bundles to be loaded from HashiCorp Vault.
 */
@AutoConfiguration
@ConditionalOnClass({VaultOperations.class, SslBundles.class})
@ConditionalOnProperty(prefix = "spring.cloud.vault", name = "enabled", matchIfMissing = true)
@Import(VaultSslBundleRegistrar.class)
public class VaultSslBundleAutoConfiguration {

    @Bean
    public VaultSslBundleRegistry vaultSslBundleRegistry(VaultOperations vaultOperations) {
        return new VaultSslBundleRegistry(vaultOperations);
    }
}