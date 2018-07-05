package se.bjurr.violations.comments.bitbucketserver.lib.client;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Lists.newArrayList;
import static java.net.URLEncoder.encode;
import static se.bjurr.violations.lib.util.Utils.isNullOrEmpty;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.List;
import se.bjurr.violations.comments.bitbucketserver.lib.client.BitbucketServerInvoker.Method;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerComment;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerDiffResponse;

public class BitbucketServerClient {
  private static BitbucketServerInvoker bitbucketServerInvoker = new BitbucketServerInvoker();

  @VisibleForTesting
  public static void setBitbucketServerInvoker(
      final BitbucketServerInvoker bitbucketServerInvoker) {
    BitbucketServerClient.bitbucketServerInvoker = bitbucketServerInvoker;
  }

  private final String bitbucketServerBaseUrl;

  private final String bitbucketServerPassword;
  private final String bitbucketServerProject;
  private final Integer bitbucketServerPullRequestId;
  private final String bitbucketServerRepo;
  private final String bitbucketServerUser;
  private final String bitbucketPersonalAccessToken;
  private final ProxyConfig proxyInformation;

  public BitbucketServerClient(
      final String bitbucketServerBaseUrl,
      final String bitbucketServerProject,
      final String bitbucketServerRepo,
      final Integer bitbucketServerPullRequestId,
      final String bitbucketServerUser,
      final String bitbucketServerPassword,
      final String bitbucketPersonalAccessToken,
      final String proxyHostNameOrIp,
      final Integer proxyHostPort,
      final String proxyUser,
      final String proxyPassword) {
    if (bitbucketServerBaseUrl.endsWith("/")) {
      this.bitbucketServerBaseUrl =
          bitbucketServerBaseUrl.substring(0, bitbucketServerBaseUrl.length() - 1);
    } else {
      this.bitbucketServerBaseUrl = bitbucketServerBaseUrl;
    }
    this.bitbucketServerProject = bitbucketServerProject;
    this.bitbucketServerPullRequestId = bitbucketServerPullRequestId;
    this.bitbucketServerRepo = bitbucketServerRepo;
    this.bitbucketServerUser = bitbucketServerUser;
    this.bitbucketServerPassword = bitbucketServerPassword;
    this.bitbucketPersonalAccessToken = bitbucketPersonalAccessToken;
    this.proxyInformation =
        new ProxyConfig(proxyHostNameOrIp, proxyHostPort, proxyUser, proxyPassword);
  }

  private String getBitbucketServerPulLRequestBase() {
    return bitbucketServerBaseUrl
        + "/rest/api/1.0/projects/"
        + bitbucketServerProject
        + "/repos/"
        + bitbucketServerRepo
        + "/pull-requests/"
        + bitbucketServerPullRequestId;
  }

  private <T> T invokeAndParse(final String url, final String jsonPath) {
    final String json = doInvokeUrl(url, BitbucketServerInvoker.Method.GET, null);

    try {
      return JsonPath.read(json, jsonPath);
    } catch (final Exception e) {
      throw new RuntimeException(
          "Unable to parse diff response from " + url + " using " + jsonPath + "\n\n" + json, e);
    }
  }

  public List<String> pullRequestChanges() {
    return invokeAndParse(
        getBitbucketServerPulLRequestBase() + "/changes?limit=999999", "$..path.toString");
  }

  public void pullRequestComment(final String message) {
    final String postContent = "{ \"text\": \"" + safeJson(message) + "\"}";
    doInvokeUrl(
        getBitbucketServerPulLRequestBase() + "/comments",
        BitbucketServerInvoker.Method.POST,
        postContent);
  }

  private String doInvokeUrl(final String string, final Method method, final String postContent) {
    if (isNullOrEmpty(bitbucketServerUser) || isNullOrEmpty(bitbucketServerPassword)) {
      return bitbucketServerInvoker.invokeUrl(
          string, method, postContent, bitbucketPersonalAccessToken, proxyInformation);
    } else {
      return bitbucketServerInvoker.invokeUrl(
          string,
          method,
          postContent,
          bitbucketServerUser,
          bitbucketServerPassword,
          proxyInformation);
    }
  }

  public void pullRequestComment(final String changedFile, int line, final String message) {
    if (line == 0) {
      line = 1;
    }
    final String postContent =
        "{ \"text\": \""
            + safeJson(message)
            + "\", \"anchor\": { \"line\": "
            + line
            + ", \"lineType\": \"ADDED\", \"fileType\": \"TO\", \"path\": \""
            + changedFile
            + "\" }}";
    doInvokeUrl(
        getBitbucketServerPulLRequestBase() + "/comments",
        BitbucketServerInvoker.Method.POST,
        postContent);
  }

  public List<BitbucketServerComment> pullRequestComments(final String changedFile) {
    try {
      final String encodedChangedFile = encode(changedFile, UTF_8.name());
      final List<LinkedHashMap<?, ?>> parsed =
          invokeAndParse(
              getBitbucketServerPulLRequestBase()
                  + "/comments?path="
                  + encodedChangedFile
                  + "&limit=999999",
              "$.values[*]");
      return toBitbucketServerComments(parsed);
    } catch (final UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public BitbucketServerDiffResponse pullRequestDiff() {
    final String url = getBitbucketServerPulLRequestBase() + "/diff?limit=999999";
    final String json = doInvokeUrl(url, BitbucketServerInvoker.Method.GET, null);
    try {
      return new Gson().fromJson(json, BitbucketServerDiffResponse.class);
    } catch (final Exception e) {
      throw new RuntimeException("Unable to parse diff response from " + url + "\n\n" + json, e);
    }
  }

  public void pullRequestRemoveComment(final Integer commentId, final Integer commentVersion) {
    doInvokeUrl(
        getBitbucketServerPulLRequestBase()
            + "/comments/"
            + commentId
            + "?version="
            + commentVersion,
        BitbucketServerInvoker.Method.DELETE,
        null);
  }

  @VisibleForTesting
  String safeJson(final String message) {
    return message.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "").replaceAll("\n", "\\\\n");
  }

  private List<BitbucketServerComment> toBitbucketServerComments(
      final List<LinkedHashMap<?, ?>> parsed) {
    final List<BitbucketServerComment> transformed = newArrayList();
    for (final LinkedHashMap<?, ?> from : parsed) {
      final Integer version = (Integer) from.get("version");
      final String text = (String) from.get("text");
      final Integer id = (Integer) from.get("id");
      transformed.add(new BitbucketServerComment(version, text, id));
    }
    return transformed;
  }
}
