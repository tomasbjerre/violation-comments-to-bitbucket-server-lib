package se.bjurr.violations.comments.bitbucketserver.lib.client;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.base.Throwables;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import javax.xml.bind.DatatypeConverter;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

public class BitbucketServerInvoker {

  public enum Method {
    DELETE,
    GET,
    POST
  }

  public String invokeUrl(
      final String url,
      final Method method,
      final String postContent,
      final String bearer,
      final ProxyConfig proxyConfig) {

    final String authorizationValue = "Bearer " + bearer;

    return doInvokeUrl(url, method, postContent, authorizationValue, proxyConfig);
  }

  public String invokeUrl(
      final String url,
      final Method method,
      final String postContent,
      final String bitbucketServerUser,
      final String bitbucketServerPassword,
      final ProxyConfig proxyConfig) {

    final String userAndPass = bitbucketServerUser + ":" + bitbucketServerPassword;
    String authString;
    try {
      authString = DatatypeConverter.printBase64Binary(userAndPass.getBytes("UTF-8"));
    } catch (final UnsupportedEncodingException e1) {
      throw new RuntimeException(e1);
    }
    final String authorizationValue = "Basic " + authString;

    return doInvokeUrl(url, method, postContent, authorizationValue, proxyConfig);
  }

  private String doInvokeUrl(
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
      request.setURI(new URI(url));
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
      final HttpClient httpClient = httpClientBuilder.build();

      // Execute the request and get the response
      final HttpResponse response = httpClient.execute(request);
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
      return json;
    } catch (final Throwable e) {
      throw new RuntimeException("Error calling:\n" + url + "\n" + method + "\n" + postContent, e);
    } finally {
      try {
        if (bufferedReader != null) {
          bufferedReader.close();
        }

      } catch (final IOException e) {
        throw Throwables.propagate(e);
      }
    }
  }
}
