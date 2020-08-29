package se.bjurr.violations.comments.bitbucketserver.lib.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;

public class CertificateConfig {
  private final String keyStorePath;
  private final String keyStorePass;

  public CertificateConfig(String keyStorePath, String keyStorePass) {
    this.keyStorePath = keyStorePath;
    this.keyStorePass = keyStorePass;
  }

  public HttpClientBuilder addTo(HttpClientBuilder builder)
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
          UnrecoverableKeyException, KeyManagementException {
    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    keyStore.load(new FileInputStream(this.keyStorePath), null);

    SSLContext sslContext =
        SSLContexts.custom().loadKeyMaterial(keyStore, keyStorePass.toCharArray()).build();
    builder.setSSLContext(sslContext);
    return builder;
  }
}
