package se.bjurr.violations.comments.bitbucketserver.lib;

import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static se.bjurr.violations.comments.bitbucketserver.lib.ViolationCommentsToBitbucketServerApi.violationCommentsToBitbucketServerApi;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import se.bjurr.violations.comments.lib.model.ChangedFile;
import se.bjurr.violations.comments.lib.model.Comment;
import se.bjurr.violations.lib.ViolationsLogger;

/**
 * Integration tests that replay, via WireMock, request/response pairs for the Bitbucket Server REST
 * API. Unlike {@link
 * se.bjurr.violations.comments.bitbucketserver.lib.client.BitbucketServerClientTest}, which
 * intercepts calls below {@code BitbucketServerInvoker} (skipping the real HTTP/auth layer), these
 * tests drive {@link BitbucketServerCommentsProvider} through its public constructor over real
 * HTTP, so the invoker's URL/header/body construction is covered too.
 *
 * <p>The activity-feed fixture ({@code pull-request-comments-all-two.json}, at the classpath root)
 * is an unmodified response the repository already had, captured from a real local Bitbucket Server
 * instance. Nothing in this test run could reach the private Bitbucket Server instance this work
 * was requested against, and the "resolve a task" behavior added in PR #18
 * (https://github.com/tomasbjerre/violation-comments-to-bitbucket-server-lib/pull/18) is new, so
 * there was no existing real capture of a comment thread with a task attached. The fixtures under
 * {@code src/test/resources/bitbucket} that involve tasks were instead built field-for-field from
 * Atlassian's official Bitbucket Server REST API documentation
 * (https://docs.atlassian.com/bitbucket-server/rest/7.21.0/bitbucket-rest.html, resource {@code
 * /rest/api/1.0/tasks}), cross-checked against {@code BitbucketServerClientTest#testResolveTask()}
 * for the exact request body the client sends.
 */
class BitbucketServerCommentsProviderWireMockTest {

  private static final String PROJECT = "PROJ";
  private static final String REPO = "repo";
  private static final int PR_ID = 1;
  private static final String PR_PATH =
      "/rest/api/1.0/projects/" + PROJECT + "/repos/" + REPO + "/pull-requests/" + PR_ID;
  private static final String TASKS_PATH = "/rest/api/1.0/tasks";

  @RegisterExtension static WireMockExtension wireMock = WireMockExtension.newInstance().build();

  private ViolationsLogger violationsLogger;

  @BeforeEach
  void setUp() {
    this.wireMock.resetAll();
    this.violationsLogger =
        new ViolationsLogger() {
          @Override
          public void log(final Level level, final String string) {}

          @Override
          public void log(final Level level, final String string, final Throwable t) {}
        };
  }

  private ViolationCommentsToBitbucketServerApi newApi() {
    return violationCommentsToBitbucketServerApi()
        .withBitbucketServerUrl(this.wireMock.baseUrl())
        .withUsername("admin")
        .withPassword("admin")
        .withProjectKey(PROJECT)
        .withRepoSlug(REPO)
        .withPullRequestId(PR_ID)
        .withShouldCommentOnlyChangedFiles(false)
        .withCreateSingleFileComments(false)
        .withCreateCommentWithAllSingleFileComments(false);
  }

  private BitbucketServerCommentsProvider newProvider(
      final ViolationCommentsToBitbucketServerApi api) {
    return new BitbucketServerCommentsProvider(api, this.violationsLogger);
  }

  private static String classpathFixture(final String name) {
    try {
      return Files.readString(Path.of("src/test/resources", name));
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String fixture(final String name) {
    return classpathFixture("bitbucket/" + name);
  }

  private String lastRequestBodyDecoded(final RequestPatternBuilder pattern) {
    final List<LoggedRequest> requests = this.wireMock.findAll(pattern);
    assertThat(requests).isNotEmpty();
    final LoggedRequest last = requests.get(requests.size() - 1);
    return new String(last.getBody(), StandardCharsets.UTF_8);
  }

  @Test
  void getCommentsReturnsBothCommentsFromTheRealActivitiesFeed() {
    this.wireMock.stubFor(
        get(urlPathEqualTo(PR_PATH + "/activities"))
            .willReturn(okJson(classpathFixture("pull-request-comments-all-two.json"))));

    final BitbucketServerCommentsProvider provider = this.newProvider(this.newApi());

    final List<Comment> comments = provider.getComments();

    assertThat(comments).hasSize(2);
    assertThat(comments.stream().map(Comment::getIdentifier)).containsExactlyInAnyOrder("22", "23");
  }

  @Test
  void removeCommentsDeletesTheCommentWhenItsThreadHasNoTask() {
    this.wireMock.stubFor(
        get(urlPathEqualTo(PR_PATH + "/comments/22"))
            .willReturn(okJson(fixture("comment-22-no-task.json"))));
    this.wireMock.stubFor(
        delete(urlPathEqualTo(PR_PATH + "/comments/22")) //
            .willReturn(noContent()));

    final BitbucketServerCommentsProvider provider = this.newProvider(this.newApi());
    final Comment comment = new Comment("22", "this is a comment", null, List.of("0", ""));

    provider.removeComments(List.of(comment));

    this.wireMock.verify(
        deleteRequestedFor(urlPathEqualTo(PR_PATH + "/comments/22"))
            .withQueryParam(
                "version", com.github.tomakehurst.wiremock.client.WireMock.equalTo("0")));
    this.wireMock.verify(0, putRequestedFor(urlPathEqualTo(TASKS_PATH + "/5")));
  }

  @Test
  void removeCommentsResolvesTheTaskOnAReplyInsteadOfDeletingTheComment() {
    this.wireMock.stubFor(
        get(urlPathEqualTo(PR_PATH + "/comments/23"))
            .willReturn(okJson(fixture("comment-23-with-task-on-reply.json"))));
    this.wireMock.stubFor(
        put(urlPathEqualTo(TASKS_PATH + "/5")) //
            .willReturn(okJson(fixture("resolve-task-response.json"))));

    final BitbucketServerCommentsProvider provider = this.newProvider(this.newApi());
    final Comment comment = new Comment("23", "this is another comment\n", null, List.of("0", ""));

    provider.removeComments(List.of(comment));

    final String resolveBody =
        this.lastRequestBodyDecoded(putRequestedFor(urlPathEqualTo(TASKS_PATH + "/5")));
    assertThat(resolveBody).isEqualTo("{ \"state\": \"RESOLVED\" }");
    this.wireMock.verify(0, deleteRequestedFor(urlPathEqualTo(PR_PATH + "/comments/23")));
    this.wireMock.verify(0, deleteRequestedFor(urlPathEqualTo(PR_PATH + "/comments/41")));
  }

  @Test
  void createCommentPostsATopLevelComment() {
    this.wireMock.stubFor(
        post(urlPathEqualTo(PR_PATH + "/comments")) //
            .willReturn(okJson(fixture("comment-22-no-task.json"))));

    final BitbucketServerCommentsProvider provider = this.newProvider(this.newApi());

    provider.createComment("Integration test top-level comment");

    final String body =
        this.lastRequestBodyDecoded(postRequestedFor(urlPathEqualTo(PR_PATH + "/comments")));
    assertThat(body).isEqualTo("{ \"text\": \"Integration test top-level comment\"}");
  }

  @Test
  void createSingleFileCommentPostsAnInlineCommentAndCreatesATaskOnIt() {
    this.wireMock.stubFor(
        post(urlPathEqualTo(PR_PATH + "/comments")) //
            .willReturn(okJson(fixture("create-comment-response.json"))));
    this.wireMock.stubFor(
        post(urlPathEqualTo(TASKS_PATH)) //
            .willReturn(okJson(fixture("create-task-response.json"))));

    final ViolationCommentsToBitbucketServerApi api =
        this.newApi().withCreateSingleFileCommentsTasks(true);
    final BitbucketServerCommentsProvider provider = this.newProvider(api);

    final ChangedFile file =
        new ChangedFile("src/main/java/com/test/SomeClass.java", new ArrayList<>());

    provider.createSingleFileComment(file, 5, "no not ok!");

    final String commentBody =
        this.lastRequestBodyDecoded(postRequestedFor(urlPathEqualTo(PR_PATH + "/comments")));
    assertThat(commentBody)
        .isEqualTo(
            "{ \"text\": \"no not ok!\", \"anchor\": { \"line\": 5, \"lineType\": \"ADDED\","
                + " \"fileType\": \"TO\", \"path\": \"src/main/java/com/test/SomeClass.java\" }}");

    final String taskBody =
        this.lastRequestBodyDecoded(postRequestedFor(urlPathEqualTo(TASKS_PATH)));
    assertThat(taskBody)
        .isEqualTo(
            "{ \"anchor\": { \"id\": 50, \"type\": \"COMMENT\" }, \"text\": \"[Violation] SomeClass.java L5\" }");
  }
}
