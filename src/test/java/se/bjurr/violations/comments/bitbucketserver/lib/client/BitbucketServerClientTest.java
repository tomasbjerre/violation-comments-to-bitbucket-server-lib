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
import org.junit.Before;
import org.junit.Test;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerComment;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerDiff;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerDiffResponse;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.DIFFTYPE;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.DiffHunk;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.Segment;

public class BitbucketServerClientTest {
  private String mockedJson = null;
  private final BitbucketServerClient sut =
      new BitbucketServerClient(
          "bitbucketServerBaseUrl",
          "bitbucketServerProject",
          "bitbucketServerRepo",
          1,
          "bitbucketServerUser",
          "bitbucketServerPassword",
          null,
          null,
          0,
          null,
          null);
  private String invoked;

  @Before
  public void before() {
    BitbucketServerClient.setBitbucketServerInvoker(
        new BitbucketServerInvoker() {
          @Override
          public String invokeUrl(
              final String url,
              final Method method,
              final String postContent,
              final String bitbucketServerUser,
              final String bitbucketServerPassword,
              final ProxyConfig proxyConfig) {
            invoked = url;
            return mockedJson;
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
      mockedJson = Resources.toString(Resources.getResource(resourceName), Charsets.UTF_8);
    } catch (final IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testPullRequestChanges() {
    mockJson("pull-request-changes.json");
    final List<String> actual = sut.pullRequestChanges();
    assertThat(actual).isNotEmpty();
    assertThat(actual.get(0)).isEqualTo("basic_branching/file.txt");
  }

  @Test
  public void testPullRequestCommentsOnFile() {
    mockJson("pull-request-comments.json");
    final List<BitbucketServerComment> actual = sut.pullRequestComments("any/file.txt");
    assertThat(actual).isNotEmpty();
    assertThat(actual.get(0).getId()).isEqualTo(2);
    assertThat(actual.get(0).getText()).isEqualTo("in diff comment");
    assertThat(actual.get(0).getVersion()).isEqualTo(0);
    assertThat(invoked)
        .isEqualTo(
            "bitbucketServerBaseUrl/rest/api/1.0/projects/bitbucketServerProject/repos/bitbucketServerRepo/pull-requests/1/comments?path=any%2Ffile.txt&limit=999999");
  }

  @Test
  public void testPullRequestCommentsOnFileWithSpaces() {
    mockJson("pull-request-comments.json");
    final List<BitbucketServerComment> actual =
        sut.pullRequestComments("any folder with spaces/file.txt");
    assertThat(actual).isNotEmpty();
    assertThat(actual.get(0).getId()).isEqualTo(2);
    assertThat(actual.get(0).getText()).isEqualTo("in diff comment");
    assertThat(actual.get(0).getVersion()).isEqualTo(0);
    assertThat(invoked)
        .isEqualTo(
            "bitbucketServerBaseUrl/rest/api/1.0/projects/bitbucketServerProject/repos/bitbucketServerRepo/pull-requests/1/comments?path=any+folder+with+spaces%2Ffile.txt&limit=999999");
  }

  @Test
  public void testPullRequestDiffDiffDeleted() {
    mockJson("pull-request-changes-3-deleted.json");
    final BitbucketServerDiffResponse response = sut.pullRequestDiff();
    assertThat(response) //
        .isNotNull();
    assertThat(response.getDiffs()) //
        .hasSize(12);
  }

  @Test
  public void testPullRequestDiffDiffTypes() {
    mockJson("pull-request-changes-2.json");
    final BitbucketServerDiffResponse response = sut.pullRequestDiff();
    assertThat(response) //
        .isNotNull();
    assertThat(response.getDiffs()) //
        .hasSize(12);
    final BitbucketServerDiff diff = response.getDiffs().get(0);
    assertThat(filterSegments(diff, CONTEXT)) //
        .hasSize(3);
    assertThat(filterSegments(diff, ADDED)) //
        .hasSize(1);
    assertThat(filterSegments(diff, ADDED).get(0).getLines()) //
        .hasSize(1);
    assertThat(filterSegments(diff, REMOVED)) //
        .hasSize(1);
    assertThat(filterSegments(diff, REMOVED).get(0).getLines()) //
        .hasSize(1);
  }

  @Test
  public void testPullRequestDiffPerFileTestCpp() {
    mockJson("pull-request-changes-2.json");
    final BitbucketServerDiffResponse response = sut.pullRequestDiff();
    assertThat(response) //
        .isNotNull();
    final List<BitbucketServerDiff> diffs = filterByFile(response, "cpp/test.cpp");
    assertThat(diffs) //
        .hasSize(1);

    assertThat(filterSegments(diffs.get(0), ADDED)) //
        .hasSize(1);
    assertThat(filterSegments(diffs.get(0), REMOVED)) //
        .hasSize(0);
    assertThat(filterSegments(diffs.get(0), CONTEXT)) //
        .hasSize(2);
  }

  @Test
  public void testPullRequestDiffPerFileTravisYml() {
    mockJson("pull-request-changes-2.json");
    final BitbucketServerDiffResponse response = sut.pullRequestDiff();
    assertThat(response) //
        .isNotNull();
    final List<BitbucketServerDiff> diffs = filterByFile(response, ".travis.yml");
    assertThat(diffs) //
        .hasSize(1);

    assertThat(filterSegments(diffs.get(0), ADDED)) //
        .hasSize(1);
    assertThat(filterSegments(diffs.get(0), REMOVED)) //
        .hasSize(1);
    assertThat(filterSegments(diffs.get(0), CONTEXT)) //
        .hasSize(3);
  }

  @Test
  public void testSaveJson() {
    assertThat(sut.safeJson("...ring: '\\s'. \nStr\"i\"ng ...")) //
        .isEqualTo("...ring: '\\\\s'. \\nString ...");
  }
}
