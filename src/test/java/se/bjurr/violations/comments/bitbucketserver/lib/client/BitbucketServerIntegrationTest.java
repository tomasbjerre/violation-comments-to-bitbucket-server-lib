package se.bjurr.violations.comments.bitbucketserver.lib.client;

import static org.assertj.core.api.Assertions.assertThat;
import static se.bjurr.violations.comments.bitbucketserver.lib.ViolationCommentsToBitbucketServerApi.violationCommentsToBitbucketServerApi;
import static se.bjurr.violations.lib.model.Violation.violationBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.bjurr.violations.comments.lib.ViolationsLogger;
import se.bjurr.violations.lib.model.SEVERITY;
import se.bjurr.violations.lib.model.Violation;
import se.bjurr.violations.lib.reports.Parser;

public class BitbucketServerIntegrationTest {
  private static final String CHANGED_FILE =
      "src/main/java/se/bjurr/violations/lib/example/MyClass.java";

  private final ViolationsLogger violationsLogger =
      new ViolationsLogger() {
        @Override
        public void log(final Level level, final String string) {
          Logger.getLogger(BitbucketServerIntegrationTest.class.getSimpleName()).info(string);
        }

        @Override
        public void log(final Level level, final String string, final Throwable t) {
          Logger.getLogger(BitbucketServerIntegrationTest.class.getSimpleName())
              .log(Level.SEVERE, string, t);
        }
      };

  private final String bitbucketServerBaseUrl = "http://localhost:7990";

  private final String bitbucketServerProject = "PROJ";

  private final String bitbucketServerRepo = "repo";

  private final Integer bitbucketServerPullRequestId = 1;

  private final String bitbucketServerUser = "admin";

  private final String bitbucketServerPassword = "admin";

  private final String bitbucketPersonalAccessToken = null;

  private final String proxyHostNameOrIp = null;

  private final Integer proxyHostPort = null;

  private final String proxyUser = null;

  private final String proxyPassword = null;

  private final BitbucketServerClient sut =
      new BitbucketServerClient(
          this.violationsLogger,
          this.bitbucketServerBaseUrl,
          this.bitbucketServerProject,
          this.bitbucketServerRepo,
          this.bitbucketServerPullRequestId,
          this.bitbucketServerUser,
          this.bitbucketServerPassword,
          this.bitbucketPersonalAccessToken,
          this.proxyHostNameOrIp,
          this.proxyHostPort,
          this.proxyUser,
          this.proxyPassword);

  // @Test
  public void withCreateCommentWithAllSingleFileComments() throws Exception {
    this.removeAllComments();

    final List<Violation> violations = new ArrayList<>();
    violations.add(
        violationBuilder() //
            .setFile(CHANGED_FILE) //
            .setMessage("no not ok!") //
            .setSeverity(SEVERITY.ERROR) //
            .setStartLine(12) //
            .setReporter("The tool") //
            .setParser(Parser.FINDBUGS) // CreateCommentWithAllSingleFileComments
            .build());

    violationCommentsToBitbucketServerApi()
        .withUsername(this.bitbucketServerUser)
        .withPassword(this.bitbucketServerPassword)
        .withBitbucketServerUrl(this.bitbucketServerBaseUrl) //
        .withPullRequestId(this.bitbucketServerPullRequestId) //
        .withProjectKey(this.bitbucketServerProject) //
        .withRepoSlug(this.bitbucketServerRepo) //
        .withViolations(violations) //
        .withCreateCommentWithAllSingleFileComments(true) //
        .withCreateSingleFileComments(false) //
        .withCreateSingleFileCommentsTasks(false) //
        .withCommentOnlyChangedContent(false) //
        .withShouldCommentOnlyChangedFiles(false) //
        .withCommentOnlyChangedContentContext(10) //
        .withShouldKeepOldComments(false) //
        .withMaxNumberOfViolations(9999) //
        .withViolationsLogger(this.violationsLogger) //
        .toPullRequest();

    assertThat(this.sut.pullRequestComments()).hasSize(1);
  }

  // @Test
  public void withCreateSingleFileComments() throws Exception {
    this.removeAllComments();

    final List<Violation> violations = new ArrayList<>();
    violations.add(
        violationBuilder() //
            .setFile(CHANGED_FILE) //
            .setMessage("no not ok!") //
            .setSeverity(SEVERITY.ERROR) //
            .setStartLine(12) //
            .setReporter("The tool") //
            .setParser(Parser.FINDBUGS) // CreateCommentWithAllSingleFileComments
            .build());

    violationCommentsToBitbucketServerApi()
        .withUsername(this.bitbucketServerUser)
        .withPassword(this.bitbucketServerPassword)
        .withBitbucketServerUrl(this.bitbucketServerBaseUrl) //
        .withPullRequestId(this.bitbucketServerPullRequestId) //
        .withProjectKey(this.bitbucketServerProject) //
        .withRepoSlug(this.bitbucketServerRepo) //
        .withViolations(violations) //
        .withCreateCommentWithAllSingleFileComments(false) //
        .withCreateSingleFileComments(true) //
        .withCreateSingleFileCommentsTasks(false) //
        .withCommentOnlyChangedContent(false) //
        .withShouldCommentOnlyChangedFiles(true) //
        .withCommentOnlyChangedContentContext(10) //
        .withShouldKeepOldComments(false) //
        .withMaxNumberOfViolations(9999) //
        .withViolationsLogger(this.violationsLogger) //
        .toPullRequest();

    assertThat(this.sut.pullRequestComments()).hasSize(1);
  }

  // @Test
  public void withCreateSingleFileCommentsTasks() throws Exception {
    this.removeAllComments();

    final List<Violation> violations = new ArrayList<>();
    violations.add(
        violationBuilder() //
            .setFile(CHANGED_FILE) //
            .setMessage("no not ok!") //
            .setSeverity(SEVERITY.ERROR) //
            .setStartLine(12) //
            .setReporter("The tool") //
            .setParser(Parser.FINDBUGS) //
            .build());

    violationCommentsToBitbucketServerApi()
        .withUsername(this.bitbucketServerUser)
        .withPassword(this.bitbucketServerPassword)
        .withBitbucketServerUrl(this.bitbucketServerBaseUrl) //
        .withPullRequestId(this.bitbucketServerPullRequestId) //
        .withProjectKey(this.bitbucketServerProject) //
        .withRepoSlug(this.bitbucketServerRepo) //
        .withViolations(violations) //
        .withCreateCommentWithAllSingleFileComments(false) //
        .withCreateSingleFileComments(true) //
        .withCreateSingleFileCommentsTasks(true) //
        .withCommentOnlyChangedContent(false) //
        .withShouldCommentOnlyChangedFiles(true) //
        .withCommentOnlyChangedContentContext(10) //
        .withShouldKeepOldComments(false) //
        .withMaxNumberOfViolations(9999) //
        .withViolationsLogger(this.violationsLogger) //
        .toPullRequest();

    assertThat(this.sut.pullRequestComments()).hasSize(1);
  }

  private void removeAllComments() throws Exception {
    this.removeSingleFileComments();
    this.removeAllSingleFileComments();

    assertThat(this.sut.pullRequestComments()).isEmpty();
  }

  private void removeAllSingleFileComments() throws Exception {
    violationCommentsToBitbucketServerApi()
        .withUsername(this.bitbucketServerUser)
        .withPassword(this.bitbucketServerPassword)
        .withBitbucketServerUrl(this.bitbucketServerBaseUrl) //
        .withPullRequestId(this.bitbucketServerPullRequestId) //
        .withProjectKey(this.bitbucketServerProject) //
        .withRepoSlug(this.bitbucketServerRepo) //
        .withViolations(new ArrayList<>()) //
        .withCreateCommentWithAllSingleFileComments(true) //
        .withCreateSingleFileComments(false) //
        .withCreateSingleFileCommentsTasks(false) //
        .withCommentOnlyChangedContent(false) //
        .withShouldCommentOnlyChangedFiles(true) //
        .withCommentOnlyChangedContentContext(10) //
        .withShouldKeepOldComments(false) //
        .withMaxNumberOfViolations(9999) //
        .withViolationsLogger(this.violationsLogger) //
        .toPullRequest();
  }

  private void removeSingleFileComments() throws Exception {
    violationCommentsToBitbucketServerApi()
        .withUsername(this.bitbucketServerUser)
        .withPassword(this.bitbucketServerPassword)
        .withBitbucketServerUrl(this.bitbucketServerBaseUrl) //
        .withPullRequestId(this.bitbucketServerPullRequestId) //
        .withProjectKey(this.bitbucketServerProject) //
        .withRepoSlug(this.bitbucketServerRepo) //
        .withViolations(new ArrayList<>()) //
        .withCreateCommentWithAllSingleFileComments(false) //
        .withCreateSingleFileComments(true) //
        .withCreateSingleFileCommentsTasks(false) //
        .withCommentOnlyChangedContent(false) //
        .withShouldCommentOnlyChangedFiles(true) //
        .withCommentOnlyChangedContentContext(10) //
        .withShouldKeepOldComments(false) //
        .withMaxNumberOfViolations(9999) //
        .withViolationsLogger(this.violationsLogger) //
        .toPullRequest();
  }
}
