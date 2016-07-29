package se.bjurr.violations.comments.bitbucketserver.lib.client;

import static com.google.common.base.Strings.isNullOrEmpty;

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

import com.google.common.base.Throwables;

public class BitbucketServerInvoker {

 public enum Method {
  DELETE, GET, POST
 }

 public String invokeUrl(String url, Method method, String postContent, String bitbucketServerUser,
   String bitbucketServerPassword) {
  HttpURLConnection conn = null;
  OutputStream output = null;
  BufferedReader reader = null;
  try {
   CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
   conn = (HttpURLConnection) new URL(url).openConnection();
   String userAndPass = bitbucketServerUser + ":" + bitbucketServerPassword;
   String authString = new String(DatatypeConverter.printBase64Binary(userAndPass.getBytes()));
   conn.setRequestProperty("Authorization", "Basic " + authString);
   conn.setRequestMethod(method.name());
   String charset = "UTF-8";
   conn.setDoOutput(true);
   conn.setRequestProperty("X-Atlassian-Token", "no-check");
   conn.setRequestProperty("Content-Type", "application/json");
   conn.setRequestProperty("Accept", "application/json");
   conn.connect();
   if (!isNullOrEmpty(postContent)) {
    output = conn.getOutputStream();
    output.write(postContent.getBytes(charset));
   }
   reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
   StringBuilder stringBuilder = new StringBuilder();
   String line = null;
   while ((line = reader.readLine()) != null) {
    stringBuilder.append(line + "\n");
   }
   String json = stringBuilder.toString();
   return json;
  } catch (Exception e) {
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
   } catch (IOException e) {
    throw Throwables.propagate(e);
   }
  }
 }
}
