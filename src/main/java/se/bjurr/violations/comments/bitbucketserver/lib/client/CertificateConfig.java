package se.bjurr.violations.comments.bitbucketserver.lib.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;

public class CertificateConfig {
  private final String keyStorePath;
  private final String keyStorePass;

  public CertificateConfig(final String keyStorePath, final String keyStorePass) {
    this.keyStorePath = keyStorePath;
    this.keyStorePass = keyStorePass;
  }

  public HttpClientBuilder addTo(final HttpClientBuilder builder)
      throws KeyStoreException,
          NoSuchAlgorithmException,
          CertificateException,
          IOException,
          UnrecoverableKeyException,
          KeyManagementException {
    final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    try (final InputStream is = Files.newInputStream(Path.of(this.keyStorePath))) {
      keyStore.load(is, null);
    }
    final SSLContext sslContext =
        SSLContexts.custom().loadKeyMaterial(keyStore, this.keyStorePass.toCharArray()).build();
    builder.setSSLContext(sslContext);
    return builder;
  }
}
