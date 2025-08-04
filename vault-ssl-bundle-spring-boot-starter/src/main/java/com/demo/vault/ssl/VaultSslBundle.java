package com.demo.vault.ssl;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.boot.ssl.SslStoreBundle;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;

/**
 * SSL Bundle implementation that wraps certificates loaded from Vault.
 */
public class VaultSslBundle implements SslBundle {

    private final KeyStore keyStore;
    private final KeyStore trustStore;
    private final String password;
    private final SslStoreBundle storeBundle;
    private final SslManagerBundle managerBundle;

    public VaultSslBundle(KeyStore keyStore, KeyStore trustStore, String password) {
        this.keyStore = keyStore;
        this.trustStore = trustStore;
        this.password = password;
        this.storeBundle = new VaultSslStoreBundle(keyStore, trustStore, password);
        this.managerBundle = new VaultSslManagerBundle(keyStore, trustStore, password);
    }

    @Override
    public SslStoreBundle getStores() {
        return storeBundle;
    }

    @Override
    public SslManagerBundle getManagers() {
        return managerBundle;
    }

    private static class VaultSslStoreBundle implements SslStoreBundle {
        private final KeyStore keyStore;
        private final KeyStore trustStore;
        private final String password;

        public VaultSslStoreBundle(KeyStore keyStore, KeyStore trustStore, String password) {
            this.keyStore = keyStore;
            this.trustStore = trustStore;
            this.password = password;
        }

        @Override
        public KeyStore getKeyStore() {
            return keyStore;
        }

        @Override
        public String getKeyStorePassword() {
            return password;
        }

        @Override
        public KeyStore getTrustStore() {
            return trustStore;
        }
    }

    private static class VaultSslManagerBundle implements SslManagerBundle {
        private final KeyStore keyStore;
        private final KeyStore trustStore;
        private final String password;

        public VaultSslManagerBundle(KeyStore keyStore, KeyStore trustStore, String password) {
            this.keyStore = keyStore;
            this.trustStore = trustStore;
            this.password = password;
        }

        @Override
        public KeyManager[] getKeyManagers() {
            try {
                KeyManagerFactory factory = KeyManagerFactory.getInstance(
                        KeyManagerFactory.getDefaultAlgorithm());
                factory.init(keyStore, password.toCharArray());
                return factory.getKeyManagers();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create KeyManagers", e);
            }
        }

        @Override
        public TrustManager[] getTrustManagers() {
            try {
                TrustManagerFactory factory = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                factory.init(trustStore);
                return factory.getTrustManagers();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create TrustManagers", e);
            }
        }
    }
}