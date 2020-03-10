package se.bjurr.violations.comments.bitbucketserver.lib.client;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Lists.newArrayList;
import static java.net.URLEncoder.encode;
import static java.util.logging.Level.INFO;
import static se.bjurr.violations.lib.util.Utils.isNullOrEmpty;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import net.minidev.json.JSONArray;
import se.bjurr.violations.comments.bitbucketserver.lib.client.BitbucketServerInvoker.Method;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerComment;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerDiffResponse;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerTask;
import se.bjurr.violations.comments.lib.ViolationsLogger;

public class BitbucketServerClient {
  private static BitbucketServerInvoker bitbucketServerInvoker = new BitbucketServerInvoker();
  private final ViolationsLogger violationsLogger;

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
      final ViolationsLogger violationsLogger,
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
    this.violationsLogger = violationsLogger;
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

  private String getBitbucketServerApiBase() {
    return bitbucketServerBaseUrl + "/rest/api/1.0";
  }

  private String getBitbucketServerPullRequestBase() {
    return getBitbucketServerApiBase()
        + "/projects/"
        + bitbucketServerProject
        + "/repos/"
        + bitbucketServerRepo
        + "/pull-requests/"
        + bitbucketServerPullRequestId;
  }

  private <T> T invokeAndParse(
      final String url, final Method method, final String postContent, final String jsonPath) {
    final String json = doInvokeUrl(url, method, postContent);

    try {
      return JsonPath.read(json, jsonPath);
    } catch (final Exception e) {
      throw new RuntimeException(
          "Unable to parse diff response from " + url + " using " + jsonPath + "\n\n" + json, e);
    }
  }

  public List<String> pullRequestChanges() {
    final String url = getBitbucketServerPullRequestBase() + "/changes?limit=999999";
    final String json = doInvokeUrl(url, Method.GET, null);

    final String jsonPath = "$..path.toString";
    try {
      final List<String> response = JsonPath.read(json, jsonPath);
      if (response.isEmpty()) {
        violationsLogger.log(
            INFO,
            "Found no changed files from "
                + url
                + " with JSONPath "
                + jsonPath
                + " in JSON:\n"
                + json);
      }
      return response;
    } catch (final Exception e) {
      throw new RuntimeException(
          "Unable to parse diff response from " + url + " using " + jsonPath + "\n\n" + json, e);
    }
  }

  public void pullRequestComment(final String message) {
    final String postContent = "{ \"text\": \"" + safeJson(message) + "\"}";
    doInvokeUrl(
        getBitbucketServerPullRequestBase() + "/comments",
        BitbucketServerInvoker.Method.POST,
        postContent);
  }

  private String doInvokeUrl(final String url, final Method method, final String postContent) {
    if (isNullOrEmpty(bitbucketServerUser) || isNullOrEmpty(bitbucketServerPassword)) {
      return bitbucketServerInvoker.invokeUrl(
          violationsLogger,
          url,
          method,
          postContent,
          bitbucketPersonalAccessToken,
          proxyInformation);
    } else {
      return bitbucketServerInvoker.invokeUrl(
          violationsLogger,
          url,
          method,
          postContent,
          bitbucketServerUser,
          bitbucketServerPassword,
          proxyInformation);
    }
  }

  public BitbucketServerComment pullRequestComment(
      final String changedFile, int line, final String message) {
    if (line == 0) {
      line = 1;
    }
    final String commentPostContent =
        "{ \"text\": \""
            + safeJson(message)
            + "\", \"anchor\": { \"line\": "
            + line
            + ", \"lineType\": \"ADDED\", \"fileType\": \"TO\", \"path\": \""
            + changedFile
            + "\" }}";

    final LinkedHashMap<?, ?> parsed =
        invokeAndParse(
            getBitbucketServerPullRequestBase() + "/comments",
            BitbucketServerInvoker.Method.POST,
            commentPostContent,
            "$");

    return toBitbucketServerComment(parsed);
  }

  public BitbucketServerComment pullRequestComment(final Long commentId) {
    final String url = getBitbucketServerPullRequestBase() + "/comments/" + commentId;

    final LinkedHashMap<?, ?> parsed =
        invokeAndParse(url, BitbucketServerInvoker.Method.GET, null, "$");

    return toBitbucketServerComment(parsed);
  }

  public List<BitbucketServerComment> pullRequestComments() {
    final String url = getBitbucketServerPullRequestBase() + "/activities?activities?limit=9999";
    return getComments(url, "$.values.[*].comment");
  }

  public List<BitbucketServerComment> pullRequestComments(final String changedFile) {
    try {
      final String encodedChangedFile = encode(changedFile, UTF_8.name());
      final String url =
          getBitbucketServerPullRequestBase()
              + "/comments?path="
              + encodedChangedFile
              + "&limit=999999&anchorState=ALL";
      return getComments(url, "$.values[*]");
    } catch (final UnsupportedEncodingException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private List<BitbucketServerComment> getComments(final String url, final String jsonPath) {

    final String json = doInvokeUrl(url, Method.GET, null);
    List<LinkedHashMap<?, ?>> parsed = null;
    try {
      parsed = JsonPath.read(json, jsonPath);
    } catch (final Exception e) {
      throw new RuntimeException(
          "Unable to parse diff response from " + url + " using " + jsonPath + "\n\n" + json, e);
    }

    if (parsed.isEmpty()) {
      violationsLogger.log(
          INFO,
          "Found no comments from " + url + " with JSONPath " + jsonPath + " in JSON:\n" + json);
    }

    return toBitbucketServerComments(parsed);
  }

  public BitbucketServerDiffResponse pullRequestDiff(final String path) {
    final String url = getBitbucketServerPullRequestBase() + "/diff/" + path;
    final String json = doInvokeUrl(url, BitbucketServerInvoker.Method.GET, null);
    try {
      final BitbucketServerDiffResponse diff =
          new Gson().fromJson(json, BitbucketServerDiffResponse.class);
      if (diff.getDiffs().isEmpty()) {
        violationsLogger.log(INFO, "Found no diffs from " + url + " in JSON:\n" + json);
      }
      return diff;
    } catch (final Exception e) {
      throw new RuntimeException("Unable to parse diff response from " + url + "\n\n" + json, e);
    }
  }

  public void pullRequestRemoveComment(final Integer commentId, final Integer commentVersion) {
    doInvokeUrl(
        getBitbucketServerPullRequestBase()
            + "/comments/"
            + commentId
            + "?version="
            + commentVersion,
        BitbucketServerInvoker.Method.DELETE,
        null);
  }

  public void removeTask(final BitbucketServerTask task) {
    doInvokeUrl(
        getBitbucketServerApiBase() + "/tasks/" + task.getId(),
        BitbucketServerInvoker.Method.DELETE,
        null);
  }

  public void commentCreateTask(
      final BitbucketServerComment comment, final String changedFile, final int line) {
    final String changedFileName = new File(changedFile).getName();

    final String taskPostContent =
        "{ \"anchor\": { \"id\": "
            + comment.getId()
            + ", \"type\": \"COMMENT\" }, \"text\": \"[Violation] "
            + changedFileName
            + " L"
            + line
            + "\" }}";

    doInvokeUrl(getBitbucketServerApiBase() + "/tasks", Method.POST, taskPostContent);
  }

  @VisibleForTesting
  String safeJson(final String message) {
    return message
        .replaceAll("\\\\", "\\\\\\\\")
        .replaceAll("\"", "")
        .replaceAll("\n", "\\\\n")
        .replaceAll("\t", "    ");
  }

  private List<BitbucketServerComment> toBitbucketServerComments(
      final List<LinkedHashMap<?, ?>> parsed) {
    final List<BitbucketServerComment> transformed = newArrayList();
    for (final LinkedHashMap<?, ?> from : parsed) {
      transformed.add(toBitbucketServerComment(from));
    }
    return transformed;
  }

  private BitbucketServerComment toBitbucketServerComment(final LinkedHashMap<?, ?> parsed) {
    final Integer version = (Integer) parsed.get("version");
    final String text = (String) parsed.get("text");
    final Integer id = (Integer) parsed.get("id");

    List<LinkedHashMap<?, ?>> tasks = new ArrayList<LinkedHashMap<?, ?>>();
    final JSONArray jsonArrayTasks = (JSONArray) parsed.get("tasks");
    if (jsonArrayTasks != null) {
      tasks = Arrays.asList(jsonArrayTasks.toArray(new LinkedHashMap<?, ?>[0]));
    }

    List<LinkedHashMap<?, ?>> subComments = new ArrayList<>();
    final JSONArray jsonArraySubComments = (JSONArray) parsed.get("comments");
    if (jsonArraySubComments != null) {
      subComments = Arrays.asList(jsonArraySubComments.toArray(new LinkedHashMap<?, ?>[0]));
    }

    final List<BitbucketServerTask> bitbucketServerTasks = toBitbucketServerTasks(tasks);
    final List<BitbucketServerComment> bitbucketServerSubComments =
        toBitbucketServerComments(subComments);

    final BitbucketServerComment comment = new BitbucketServerComment(version, text, id);

    comment.setTasks(bitbucketServerTasks);
    comment.setComments(bitbucketServerSubComments);

    return comment;
  }

  private List<BitbucketServerTask> toBitbucketServerTasks(final List<LinkedHashMap<?, ?>> parsed) {
    final List<BitbucketServerTask> bitbucketServerTasks = new ArrayList<>();

    for (final LinkedHashMap<?, ?> parsedTask : parsed) {
      bitbucketServerTasks.add(toBitbucketServerTask(parsedTask));
    }

    return bitbucketServerTasks;
  }

  private BitbucketServerTask toBitbucketServerTask(final LinkedHashMap<?, ?> parsed) {
    final Integer id = (Integer) parsed.get("id");
    final String text = (String) parsed.get("text");
    return new BitbucketServerTask(id, text);
  }
}
