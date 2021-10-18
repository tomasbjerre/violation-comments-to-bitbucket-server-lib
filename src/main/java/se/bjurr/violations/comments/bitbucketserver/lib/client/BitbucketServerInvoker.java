package se.bjurr.violations.comments.bitbucketserver.lib.client;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.logging.Level.INFO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import se.bjurr.violations.lib.ViolationsLogger;

public class BitbucketServerInvoker {

  public enum Method {
    DELETE,
    GET,
    POST
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
    BufferedReader bufferedReader = null;
    try {
      CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
      // Preparation of the request
      HttpRequestBase request;
      switch (method) {
        case DELETE:
          request = new HttpDelete();
          break;
        case GET:
          request = new HttpGet();
          break;
        case POST:
          request = new HttpPost();
          break;
        default:
          throw new IllegalArgumentException(
              "Unsupported http method:\n" + url + "\n" + method + "\n" + postContent);
      }
      request.setURI(this.convertToURIEscapingIllegalCharacters(url));
      final RequestConfig.Builder requestBuilder =
          RequestConfig.custom().setConnectionRequestTimeout(30000).setConnectTimeout(30000);
      request.setConfig(requestBuilder.build());
      request.addHeader("Authorization", authorizationValue);
      request.addHeader("X-Atlassian-Token", "no-check");
      request.addHeader("Content-Type", "application/json");
      request.addHeader("Accept", "application/json");

      if (request instanceof HttpPost && !isNullOrEmpty(postContent)) {
        final StringEntity entity = new StringEntity(postContent, UTF_8);
        ((HttpPost) request).setEntity(entity);
      }

      final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
      proxyConfig.addTo(httpClientBuilder);
      if (this.certificateConfig != null) {
        this.certificateConfig.addTo(httpClientBuilder);
      }

      final HttpClient httpClient = httpClientBuilder.build();

      // Execute the request and get the response
      final HttpResponse response = httpClient.execute(request);
      String statusCode = "";
      String reasonPhrase = "";
      if (response.getStatusLine() != null) {
        statusCode = "" + response.getStatusLine().getStatusCode();
        reasonPhrase = response.getStatusLine().getReasonPhrase();
      }
      final boolean wasNotOk = !statusCode.startsWith("2");
      if (wasNotOk) {
        violationsLogger.log(
            INFO,
            method + " " + url + " " + statusCode + " " + reasonPhrase + "\nSent:\n" + postContent);
      } else {
        violationsLogger.log(INFO, method + " " + url + " " + statusCode + " " + reasonPhrase);
      }
      if (response.getEntity() == null) {
        return null;
      }
      bufferedReader =
          new BufferedReader(new InputStreamReader(response.getEntity().getContent(), UTF_8));
      final StringBuilder stringBuilder = new StringBuilder();
      String line = null;
      while ((line = bufferedReader.readLine()) != null) {
        stringBuilder.append(line + "\n");
      }
      final String json = stringBuilder.toString();
      if (wasNotOk) {
        violationsLogger.log(INFO, "Response:\n" + json);
      }
      return json;
    } catch (final Throwable e) {
      throw new RuntimeException("Error calling:\n" + url + "\n" + method + "\n" + postContent, e);
    } finally {
      try {
        if (bufferedReader != null) {
          bufferedReader.close();
        }

      } catch (final IOException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
  }

   URI convertToURIEscapingIllegalCharacters(final String string) throws Exception {
      final URL url = new URL(string);
      final URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
      return uri;
  }
}
