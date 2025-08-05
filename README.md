# Spring Boot Vault SSL Bundle

This Spring Boot starter enables loading SSL certificates from HashiCorp Vault using the `vault:` protocol in your SSL bundle configuration.

## Features

- Seamless integration with Spring Boot's SSL bundle system
- Automatic loading of certificates from Vault at startup
- Support for certificate, private key, and CA certificate
- Caching of loaded certificates for performance
- Compatible with Spring Cloud Vault configuration

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.demo</groupId>
    <artifactId>spring-boot-vault-ssl-bundle</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Vault Certificate Format

Store your SSL certificates in Vault with the following structure:

```json
{
  "certificate": "-----BEGIN CERTIFICATE-----\nMIIE...\n-----END CERTIFICATE-----",
  "private_key": "-----BEGIN PRIVATE KEY-----\nMIIE...\n-----END PRIVATE KEY-----",
  "ca_certificate": "-----BEGIN CERTIFICATE-----\nMIIE...\n-----END CERTIFICATE-----"
}
```

### Required Fields:
- `certificate`: The server certificate in PEM format
- `private_key`: The private key in PEM format (PKCS#8 or RSA format)

### Optional Fields:
- `ca_certificate`: The CA certificate in PEM format

## Configuration

### Application Properties

Your `application.yml` can reference Vault-stored certificates using the `vault:` protocol:

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    bundle: "vault:secret/ssl-certs/server-a"

spring:
  application:
    name: server-a
  cloud:
    vault:
      host: localhost
      port: 8200
      scheme: http
      authentication: TOKEN
      token: demo-root-token
      kv:
        enabled: true
        backend: secret
        default-context: ssl-certs
```

### Bundle Name Format

The bundle name must follow this pattern:
```
vault:<vault-path>
```

Examples:
- `vault:secret/ssl-certs/server-a`
- `vault:kv-v2/certificates/web-server`
- `vault:pki/cert/example.com`

## Vault Setup Example

Store certificates in Vault using the CLI:

```bash
# Enable KV secrets engine
vault secrets enable -path=secret kv-v2

# Store certificate data
vault kv put secret/ssl-certs/server-a \
  certificate=@server.crt \
  private_key=@server.key \
  ca_certificate=@ca.crt
```

Or using the Vault API:

```bash
curl -X POST \
  -H "X-Vault-Token: $VAULT_TOKEN" \
  -d '{
    "data": {
      "certificate": "-----BEGIN CERTIFICATE-----\n...",
      "private_key": "-----BEGIN PRIVATE KEY-----\n...",
      "ca_certificate": "-----BEGIN CERTIFICATE-----\n..."
    }
  }' \
  http://localhost:8200/v1/secret/data/ssl-certs/server-a
```

## How It Works

1. **Bundle Resolution**: When Spring Boot encounters a bundle name starting with `vault:`, it delegates to the `VaultSslBundleRegistry`

2. **Vault Query**: The registry strips the `vault:` prefix and queries Vault at the specified path

3. **Certificate Parsing**: The retrieved certificate data is parsed and converted into Java KeyStore format

4. **SSL Context**: The certificates are made available to Spring Boot's SSL context for HTTPS configuration

## Security Considerations

- **Vault Authentication**: Ensure your application has appropriate Vault authentication configured
- **Network Security**: Use HTTPS/TLS for Vault communication in production
- **Token Management**: Rotate Vault tokens regularly and use appropriate authentication methods
- **Certificate Rotation**: Consider implementing automatic certificate renewal workflows

## Troubleshooting

### Enable Debug Logging

Add to your `application.yml`:

```yaml
logging:
  level:
    com.demo.vault.ssl: DEBUG
    org.springframework.vault: DEBUG
    org.springframework.cloud.vault: DEBUG
```

### Common Issues

1. **Bundle Not Found**: Verify the Vault path and ensure the certificate data exists
2. **Authentication Failed**: Check Vault token and authentication configuration
3. **Certificate Format**: Ensure certificates are in proper PEM format
4. **Private Key Format**: The library supports PKCS#8 and RSA private key formats

### Error Messages

- `"Bundle name must start with 'vault:' protocol"`: Use correct bundle name format
- `"No SSL certificate data found at Vault path"`: Check if the path exists in Vault
- `"Missing required certificate or private_key"`: Ensure both fields are present in Vault data

## Advanced Configuration

### Multiple Bundles

You can configure multiple SSL bundles from different Vault paths:

```yaml
spring:
  ssl:
    bundle:
      pem:
        web-server:
          bundle: "vault:secret/ssl-certs/web-server"
        api-server:
          bundle: "vault:secret/ssl-certs/api-server"
```

### Custom Vault Configuration

The library uses your existing Spring Cloud Vault configuration, so all standard Vault settings apply.