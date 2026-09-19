package se.bjurr.violations.comments.bitbucketserver.lib.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

public class CertificateConfig {
  private final String keyStorePath;
  private final String keyStorePass;

  public CertificateConfig(final String keyStorePath, final String keyStorePass) {
    this.keyStorePath = keyStorePath;
    this.keyStorePass = keyStorePass;
  }

  public HttpClient.Builder addTo(final HttpClient.Builder builder)
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
    final KeyManagerFactory keyManagerFactory =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keyStore, this.keyStorePass.toCharArray());
    final SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
    builder.sslContext(sslContext);
    return builder;
  }
}
