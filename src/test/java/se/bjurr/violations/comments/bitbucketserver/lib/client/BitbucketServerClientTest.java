package se.bjurr.violations.comments.bitbucketserver.lib.client;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static se.bjurr.violations.comments.bitbucketserver.lib.client.model.DIFFTYPE.ADDED;
import static se.bjurr.violations.comments.bitbucketserver.lib.client.model.DIFFTYPE.CONTEXT;
import static se.bjurr.violations.comments.bitbucketserver.lib.client.model.DIFFTYPE.REMOVED;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerComment;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerDiff;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerDiffResponse;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.DIFFTYPE;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.DiffHunk;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.Segment;
import se.bjurr.violations.lib.ViolationsLogger;

public class BitbucketServerClientTest {
  private String mockedJson = null;
  private final BitbucketServerClient sut =
      new BitbucketServerClient(
          new ViolationsLogger() {
            @Override
            public void log(final Level level, final String string) {
              Logger.getLogger(BitbucketServerClientTest.class.getSimpleName()).info(string);
            }

            @Override
            public void log(final Level level, final String string, final Throwable t) {
              Logger.getLogger(BitbucketServerClientTest.class.getSimpleName())
                  .log(Level.SEVERE, string, t);
            }
          },
          "bitbucketServerBaseUrl",
          "bitbucketServerProject",
          "bitbucketServerRepo",
          1,
          "bitbucketServerUser",
          "bitbucketServerPassword",
          null,
          null,
          null,
          null,
          0,
          null,
          null);
  private String invoked;
  private final String path = "anypath";

  @Before
  public void before() {
    BitbucketServerClient.setBitbucketServerInvoker(
        new BitbucketServerInvoker() {
          @Override
          public String invokeUrl(
              final ViolationsLogger violationsLogger,
              final String url,
              final Method method,
              final String postContent,
              final String bitbucketServerUser,
              final String bitbucketServerPassword,
              final ProxyConfig proxyConfig) {
            BitbucketServerClientTest.this.invoked = url;
            return BitbucketServerClientTest.this.mockedJson;
          }
        });
  }

  public List<BitbucketServerDiff> filterByFile(
      final BitbucketServerDiffResponse response, final String filename) {
    final List<BitbucketServerDiff> filtered = newArrayList();
    final List<BitbucketServerDiff> mixed = response.getDiffs();
    for (final BitbucketServerDiff d : mixed) {
      if (d.getDestination().getToString().equals(filename)) {
        filtered.add(d);
      }
    }
    return filtered;
  }

  public List<Segment> filterSegments(
      final BitbucketServerDiff bitbucketServerDiff, final DIFFTYPE diffType) {
    final List<Segment> filtered = newArrayList();
    for (final DiffHunk hunk : bitbucketServerDiff.getHunks()) {
      final List<Segment> mixed = hunk.getSegments();
      for (final Segment s : mixed) {
        if (s.getType() == diffType) {
          filtered.add(s);
        }
      }
    }

    return filtered;
  }

  private void mockJson(final String resourceName) {
    try {
      this.mockedJson = Resources.toString(Resources.getResource(resourceName), Charsets.UTF_8);
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testPullRequestChanges() {
    this.mockJson("pull-request-changes.json");
    final List<String> actual = this.sut.pullRequestChanges();
    assertThat(actual).isNotEmpty();
    assertThat(actual.get(0)).isEqualTo("basic_branching/file.txt");
  }

  @Test
  public void testPullRequestCommentsOnFile() {
    this.mockJson("pull-request-comments.json");
    final List<BitbucketServerComment> actual = this.sut.pullRequestComments("any/file.txt");
    assertThat(actual).isNotEmpty();
    assertThat(actual.get(0).getId()).isEqualTo(2);
    assertThat(actual.get(0).getText()).isEqualTo("in diff comment");
    assertThat(actual.get(0).getVersion()).isEqualTo(0);
    assertThat(this.invoked)
        .isEqualTo(
            "bitbucketServerBaseUrl/rest/api/1.0/projects/bitbucketServerProject/repos/bitbucketServerRepo/pull-requests/1/comments?path=any%2Ffile.txt&limit=999999&anchorState=ALL");
  }

  @Test
  public void testPullRequestCommentsEmpty() {
    this.mockJson("pull-request-comments-all-empty.json");
    final List<BitbucketServerComment> actual = this.sut.pullRequestComments();
    assertThat(actual).isEmpty();
    assertThat(this.invoked)
        .isEqualTo(
            "bitbucketServerBaseUrl/rest/api/1.0/projects/bitbucketServerProject/repos/bitbucketServerRepo/pull-requests/1/activities?limit=9999");
  }

  @Test
  public void testPullRequestCommentsGeneral() {
    this.mockJson("pull-request-comments-generall.json");
    final List<BitbucketServerComment> actual = this.sut.pullRequestComments();
    assertThat(actual).hasSize(7);
    assertThat(this.invoked)
        .isEqualTo(
            "bitbucketServerBaseUrl/rest/api/1.0/projects/bitbucketServerProject/repos/bitbucketServerRepo/pull-requests/1/activities?limit=9999");
  }

  @Test
  public void testPullRequestCommentsOne() {
    this.mockJson("pull-request-comments-all-one.json");
    final List<BitbucketServerComment> actual = this.sut.pullRequestComments();
    assertThat(actual).hasSize(1);
    assertThat(actual.get(0).getId()).isEqualTo(1);
    assertThat(actual.get(0).getText()).isEqualTo("as");
    assertThat(actual.get(0).getVersion()).isEqualTo(0);
    assertThat(this.invoked)
        .isEqualTo(
            "bitbucketServerBaseUrl/rest/api/1.0/projects/bitbucketServerProject/repos/bitbucketServerRepo/pull-requests/1/activities?limit=9999");
  }

  @Test
  public void testPullRequestCommentsTwo() {
    this.mockJson("pull-request-comments-all-two.json");
    final List<BitbucketServerComment> actual = this.sut.pullRequestComments();
    assertThat(actual).hasSize(2);
    assertThat(actual.get(0).getId()).isEqualTo(23);
    assertThat(actual.get(0).getText().trim()).isEqualTo("this is another comment");
    assertThat(actual.get(0).getVersion()).isEqualTo(0);
    assertThat(actual.get(1).getId()).isEqualTo(22);
    assertThat(actual.get(1).getText()).isEqualTo("this is a comment");
    assertThat(actual.get(1).getVersion()).isEqualTo(0);
    assertThat(this.invoked)
        .isEqualTo(
            "bitbucketServerBaseUrl/rest/api/1.0/projects/bitbucketServerProject/repos/bitbucketServerRepo/pull-requests/1/activities?limit=9999");
  }

  @Test
  public void testPullRequestCommentsOnFileWithSpaces() {
    this.mockJson("pull-request-comments.json");
    final List<BitbucketServerComment> actual =
        this.sut.pullRequestComments("any folder with spaces/file.txt");
    assertThat(actual).isNotEmpty();
    assertThat(actual.get(0).getId()).isEqualTo(2);
    assertThat(actual.get(0).getText()).isEqualTo("in diff comment");
    assertThat(actual.get(0).getVersion()).isEqualTo(0);
    assertThat(this.invoked)
        .isEqualTo(
            "bitbucketServerBaseUrl/rest/api/1.0/projects/bitbucketServerProject/repos/bitbucketServerRepo/pull-requests/1/comments?path=any+folder+with+spaces%2Ffile.txt&limit=999999&anchorState=ALL");
  }

  @Test
  public void testPullRequestDiffDiffDeleted() {
    this.mockJson("pull-request-changes-3-deleted.json");
    final BitbucketServerDiffResponse response = this.sut.pullRequestDiff(this.path);
    assertThat(response) //
        .isNotNull();
    assertThat(response.getDiffs()) //
        .hasSize(12);
  }

  @Test
  public void testPullRequestDiffDiffTypes() {
    this.mockJson("pull-request-changes-2.json");
    final BitbucketServerDiffResponse response = this.sut.pullRequestDiff(this.path);
    assertThat(response) //
        .isNotNull();
    assertThat(response.getDiffs()) //
        .hasSize(12);
    final BitbucketServerDiff diff = response.getDiffs().get(0);
    assertThat(this.filterSegments(diff, CONTEXT)) //
        .hasSize(3);
    assertThat(this.filterSegments(diff, ADDED)) //
        .hasSize(1);
    assertThat(this.filterSegments(diff, ADDED).get(0).getLines()) //
        .hasSize(1);
    assertThat(this.filterSegments(diff, REMOVED)) //
        .hasSize(1);
    assertThat(this.filterSegments(diff, REMOVED).get(0).getLines()) //
        .hasSize(1);
  }

  @Test
  public void testPullRequestDiffPerFileTestCpp() {
    this.mockJson("pull-request-changes-2.json");
    final BitbucketServerDiffResponse response = this.sut.pullRequestDiff(this.path);
    assertThat(response) //
        .isNotNull();
    final List<BitbucketServerDiff> diffs = this.filterByFile(response, "cpp/test.cpp");
    assertThat(diffs) //
        .hasSize(1);

    assertThat(this.filterSegments(diffs.get(0), ADDED)) //
        .hasSize(1);
    assertThat(this.filterSegments(diffs.get(0), REMOVED)) //
        .hasSize(0);
    assertThat(this.filterSegments(diffs.get(0), CONTEXT)) //
        .hasSize(2);
  }

  @Test
  public void testPullRequestDiffPerFileTravisYml() {
    this.mockJson("pull-request-changes-2.json");
    final BitbucketServerDiffResponse response = this.sut.pullRequestDiff(this.path);
    assertThat(response) //
        .isNotNull();
    final List<BitbucketServerDiff> diffs = this.filterByFile(response, ".travis.yml");
    assertThat(diffs) //
        .hasSize(1);

    assertThat(this.filterSegments(diffs.get(0), ADDED)) //
        .hasSize(1);
    assertThat(this.filterSegments(diffs.get(0), REMOVED)) //
        .hasSize(1);
    assertThat(this.filterSegments(diffs.get(0), CONTEXT)) //
        .hasSize(3);
  }

  @Test
  public void testSaveJson() {
    assertThat(this.sut.safeJson("...ring: '\\s'. \nStr\"i\"ng ...")) //
        .isEqualTo("...ring: '\\\\s'. \\nString ...");
    assertThat(this.sut.safeJson("\thej\n\thej2")) //
        .isEqualTo("    hej\\n    hej2");
  }
}
