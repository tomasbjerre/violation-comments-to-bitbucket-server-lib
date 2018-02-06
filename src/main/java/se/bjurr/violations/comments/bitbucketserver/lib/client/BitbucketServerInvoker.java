package se.bjurr.violations.comments.bitbucketserver.lib.client;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.base.Throwables;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.xml.bind.DatatypeConverter;

public class BitbucketServerInvoker {

  public enum Method {
    DELETE,
    GET,
    POST
  }

  public String invokeUrl(
      String url,
      Method method,
      String postContent,
      String bitbucketServerUser,
      String bitbucketServerPassword) {
    HttpURLConnection conn = null;
    OutputStream output = null;
    BufferedReader reader = null;
    try {
      CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
      conn = (HttpURLConnection) new URL(url).openConnection();
      conn.setReadTimeout(30000);
      conn.setConnectTimeout(30000);
      final String userAndPass = bitbucketServerUser + ":" + bitbucketServerPassword;
      final String authString = DatatypeConverter.printBase64Binary(userAndPass.getBytes("UTF-8"));
      conn.setRequestProperty("Authorization", "Basic " + authString);
      conn.setRequestMethod(method.name());
      final String charset = "UTF-8";
      conn.setDoOutput(true);
      conn.setRequestProperty("X-Atlassian-Token", "no-check");
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setRequestProperty("Accept", "application/json");
      conn.connect();
      if (!isNullOrEmpty(postContent)) {
        output = conn.getOutputStream();
        output.write(postContent.getBytes(charset));
      }
      reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), UTF_8));
      final StringBuilder stringBuilder = new StringBuilder();
      String line = null;
      while ((line = reader.readLine()) != null) {
        stringBuilder.append(line + "\n");
      }
      final String json = stringBuilder.toString();
      return json;
    } catch (final Throwable e) {
      throw new RuntimeException("Error calling:\n" + url + "\n" + method + "\n" + postContent, e);
    } finally {
      try {
        if (conn != null) {
          conn.disconnect();
        }
        if (reader != null) {
          reader.close();
        }
        if (output != null) {
          output.close();
        }
      } catch (final IOException e) {
        throw Throwables.propagate(e);
      }
    }
  }
}
