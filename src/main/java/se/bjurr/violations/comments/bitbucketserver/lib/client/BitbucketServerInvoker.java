package se.bjurr.violations.comments.bitbucketserver.lib.client;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.logging.Level.INFO;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import se.bjurr.violations.lib.ViolationsLogger;

public class BitbucketServerInvoker {

  private static final Duration TIMEOUT = Duration.ofMillis(30_000);

  public enum Method {
    DELETE,
    GET,
    POST,
    PUT
  }

  private CertificateConfig certificateConfig = null;

  public String invokeUrl(
      final ViolationsLogger violationsLogger,
      final String url,
      final Method method,
      final String postContent,
      final String bearer,
      final ProxyConfig proxyConfig) {

    final String authorizationValue = "Bearer " + bearer;

    return this.doInvokeUrl(
        violationsLogger, url, method, postContent, authorizationValue, proxyConfig);
  }

  public String invokeUrl(
      final ViolationsLogger violationsLogger,
      final String url,
      final Method method,
      final String postContent,
      final String bitbucketServerUser,
      final String bitbucketServerPassword,
      final ProxyConfig proxyConfig) {

    final String userAndPass = bitbucketServerUser + ":" + bitbucketServerPassword;
    final String authString =
        Base64.getEncoder().encodeToString(userAndPass.getBytes(StandardCharsets.UTF_8));
    final String authorizationValue = "Basic " + authString;

    return this.doInvokeUrl(
        violationsLogger, url, method, postContent, authorizationValue, proxyConfig);
  }

  public String invokeUrl(
      final ViolationsLogger violationsLogger,
      final String url,
      final Method method,
      final String postContent,
      final CertificateConfig certificateConfig,
      final ProxyConfig proxyConfig) {

    this.certificateConfig = certificateConfig;
    return this.doInvokeUrl(violationsLogger, url, method, postContent, "", proxyConfig);
  }

  private String doInvokeUrl(
      final ViolationsLogger violationsLogger,
      final String url,
      final Method method,
      final String postContent,
      final String authorizationValue,
      final ProxyConfig proxyConfig) {
    try {
      final HttpClient.Builder httpClientBuilder =
          HttpClient.newBuilder()
              .connectTimeout(TIMEOUT)
              .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
      proxyConfig.addTo(httpClientBuilder);
      if (this.certificateConfig != null) {
        this.certificateConfig.addTo(httpClientBuilder);
      }

      final HttpRequest.Builder requestBuilder =
          HttpRequest.newBuilder()
              .uri(this.convertToURIEscapingIllegalCharacters(url))
              .timeout(TIMEOUT)
              .header("Authorization", authorizationValue)
              .header("X-Atlassian-Token", "no-check")
              .header("Content-Type", "application/json")
              .header("Accept", "application/json");

      final boolean hasBody = postContent != null && !postContent.isEmpty();
      switch (method) {
        case DELETE:
          requestBuilder.DELETE();
          break;
        case GET:
          requestBuilder.GET();
          break;
        case POST:
          requestBuilder.POST(
              hasBody ? BodyPublishers.ofString(postContent, UTF_8) : BodyPublishers.noBody());
          break;
        case PUT:
          requestBuilder.PUT(
              hasBody ? BodyPublishers.ofString(postContent, UTF_8) : BodyPublishers.noBody());
          break;
        default:
          throw new IllegalArgumentException(
              "Unsupported http method:\n" + url + "\n" + method + "\n" + postContent);
      }

      final HttpClient httpClient = httpClientBuilder.build();
      final HttpResponse<String> response =
          httpClient.send(requestBuilder.build(), BodyHandlers.ofString(UTF_8));

      final String statusCode = "" + response.statusCode();
      final boolean wasNotOk = !statusCode.startsWith("2");
      if (wasNotOk) {
        violationsLogger.log(
            INFO, method + " " + url + " " + statusCode + "\nSent:\n" + postContent);
      } else {
        violationsLogger.log(INFO, method + " " + url + " " + statusCode);
      }

      final String json = response.body();
      if (json == null || json.isEmpty()) {
        return null;
      }
      if (wasNotOk) {
        violationsLogger.log(INFO, "Response:\n" + json);
      }
      return json;
    } catch (final Throwable e) {
      throw new RuntimeException("Error calling:\n" + url + "\n" + method + "\n" + postContent, e);
    }
  }

  URI convertToURIEscapingIllegalCharacters(final String string) throws Exception {
    final URL url = new URL(string);
    final URI uri =
        new URI(
            url.getProtocol(),
            url.getUserInfo(),
            url.getHost(),
            url.getPort(),
            url.getPath(),
            url.getQuery(),
            url.getRef());
    return uri;
  }
}
