package se.bjurr.violations.comments.bitbucketserver.lib.client;

import static se.bjurr.violations.lib.util.Utils.isNullOrEmpty;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.http.HttpClient;

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

  public HttpClient.Builder addTo(HttpClient.Builder builder) {
    if (!isNullOrEmpty(proxyHostNameOrIp)) {
      builder =
          builder.proxy(ProxySelector.of(new InetSocketAddress(proxyHostNameOrIp, proxyHostPort)));

      if (!isNullOrEmpty(proxyUser)) {
        builder =
            builder.authenticator(
                new Authenticator() {
                  @Override
                  protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                        proxyUser,
                        proxyPassword == null ? new char[0] : proxyPassword.toCharArray());
                  }
                });
      }
    }
    return builder;
  }
}
