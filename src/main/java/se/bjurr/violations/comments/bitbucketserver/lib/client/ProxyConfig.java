package se.bjurr.violations.comments.bitbucketserver.lib.client;

import static com.google.common.base.Strings.isNullOrEmpty;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;

public class ProxyConfig {

  private final String proxyHostNameOrIp;
  private final Integer proxyHostPort;
  private final String proxyUser;
  private final String proxyPassword;

  public ProxyConfig(
      String proxyHostNameOrIp, Integer proxyHostPort, String proxyUser, String proxyPassword) {
    this.proxyHostNameOrIp = proxyHostNameOrIp;
    this.proxyHostPort = proxyHostPort;
    this.proxyUser = proxyUser;
    this.proxyPassword = proxyPassword;
  }

  public HttpClientBuilder addTo(HttpClientBuilder builder) {
    if (!isNullOrEmpty(proxyHostNameOrIp)) {
      HttpHost proxyHost = new HttpHost(proxyHostNameOrIp, proxyHostPort);
      builder = builder.setProxy(proxyHost);

      if (!isNullOrEmpty(proxyUser)) {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
            new AuthScope(proxyHostNameOrIp, proxyHostPort),
            new UsernamePasswordCredentials(proxyUser, proxyPassword));

        builder =
            builder
                .setDefaultCredentialsProvider(credsProvider)
                .setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
      }
    }
    return builder;
  }
}
