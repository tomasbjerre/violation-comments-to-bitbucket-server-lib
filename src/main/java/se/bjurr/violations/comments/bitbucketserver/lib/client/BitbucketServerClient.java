package se.bjurr.violations.comments.bitbucketserver.lib.client;

import static com.google.common.collect.Lists.newArrayList;

import java.util.LinkedHashMap;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.jayway.jsonpath.JsonPath;

public class BitbucketServerClient {
 private static BitbucketServerInvoker bitbucketServerInvoker = new BitbucketServerInvoker();

 @VisibleForTesting
 public static void setBitbucketServerInvoker(BitbucketServerInvoker bitbucketServerInvoker) {
  BitbucketServerClient.bitbucketServerInvoker = bitbucketServerInvoker;
 }

 private final String bitbucketServerBaseUrl;
 private final String bitbucketServerPassword;
 private final String bitbucketServerProject;
 private final Integer bitbucketServerPullRequestId;
 private final String bitbucketServerRepo;
 private final String bitbucketServerUser;

 public BitbucketServerClient(String bitbucketServerBaseUrl, String bitbucketServerProject, String bitbucketServerRepo,
   Integer bitbucketServerPullRequestId, String bitbucketServerUser, String bitbucketServerPassword) {
  if (bitbucketServerBaseUrl.endsWith("/")) {
   this.bitbucketServerBaseUrl = bitbucketServerBaseUrl.substring(0, bitbucketServerBaseUrl.length() - 1);
  } else {
   this.bitbucketServerBaseUrl = bitbucketServerBaseUrl;
  }
  this.bitbucketServerPassword = bitbucketServerPassword;
  this.bitbucketServerProject = bitbucketServerProject;
  this.bitbucketServerPullRequestId = bitbucketServerPullRequestId;
  this.bitbucketServerRepo = bitbucketServerRepo;
  this.bitbucketServerUser = bitbucketServerUser;
 }

 public List<String> pullRequestChanges() {
  return invokeAndParse(getBitbucketServerPulLRequestBase() + "/changes?limit=999999", "$..path.toString");
 }

 public void pullRequestComment(String message) {
  String postContent = "{ \"text\": \"" + safeJson(message) + "\"}";
  bitbucketServerInvoker.invokeUrl(getBitbucketServerPulLRequestBase() + "/comments",
    BitbucketServerInvoker.Method.POST, postContent, this.bitbucketServerUser, this.bitbucketServerPassword);
 }

 public void pullRequestComment(String changedFile, int line, String message) {
  String postContent = "{ \"text\": \"" + safeJson(message) + "\", \"anchor\": { \"line\": " + line
    + ", \"lineType\": \"ADDED\", \"fileType\": \"TO\", \"path\": \"" + changedFile + "\" }}";
  bitbucketServerInvoker.invokeUrl(getBitbucketServerPulLRequestBase() + "/comments",
    BitbucketServerInvoker.Method.POST, postContent, this.bitbucketServerUser, this.bitbucketServerPassword);
 }

 public List<BitbucketServerComment> pullRequestComments(String changedFile) {
  List<LinkedHashMap<?, ?>> parsed = invokeAndParse(getBitbucketServerPulLRequestBase() + "/comments?path="
    + changedFile + "&limit=999999", "$.values[*]");
  return toBitbucketServerComments(parsed);
 }

 public void pullRequestRemoveComment(Integer commentId, Integer commentVersion) {
  bitbucketServerInvoker.invokeUrl(getBitbucketServerPulLRequestBase() + "/comments/" + commentId + "?version="
    + commentVersion, BitbucketServerInvoker.Method.DELETE, null, this.bitbucketServerUser,
    this.bitbucketServerPassword);
 }

 private String getBitbucketServerPulLRequestBase() {
  return this.bitbucketServerBaseUrl + "/rest/api/1.0/projects/" + this.bitbucketServerProject + "/repos/"
    + this.bitbucketServerRepo + "/pull-requests/" + this.bitbucketServerPullRequestId;
 }

 private <T> T invokeAndParse(String url, String jsonPath) {
  String json = bitbucketServerInvoker.invokeUrl(url, BitbucketServerInvoker.Method.GET, null,
    this.bitbucketServerUser, this.bitbucketServerPassword);
  try {
   return JsonPath.read(json, jsonPath);
  } catch (Exception e) {
   throw e;
  }
 }

 private String safeJson(String message) {
  return message.replaceAll("\"", "").replaceAll("\n", "\\\\n");
 }

 private List<BitbucketServerComment> toBitbucketServerComments(List<LinkedHashMap<?, ?>> parsed) {
  List<BitbucketServerComment> transformed = newArrayList();
  for (LinkedHashMap<?, ?> from : parsed) {
   Integer version = (Integer) from.get("version");
   String text = (String) from.get("text");
   Integer id = (Integer) from.get("id");
   transformed.add(new BitbucketServerComment(version, text, id));
  }
  return transformed;
 }
}
