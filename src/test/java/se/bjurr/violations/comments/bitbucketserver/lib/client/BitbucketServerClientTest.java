package se.bjurr.violations.comments.bitbucketserver.lib.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class BitbucketServerClientTest {
 private String mockedJson = null;
 private final BitbucketServerClient sut = new BitbucketServerClient("bitbucketServerBaseUrl",
   "bitbucketServerProject", "bitbucketServerRepo", 1, "bitbucketServerUser", "bitbucketServerPassword");

 @Before
 public void before() {
  BitbucketServerClient.setBitbucketServerInvoker(new BitbucketServerInvoker() {
   @Override
   public String invokeUrl(String url, Method method, String postContent, String bitbucketServerUser,
     String bitbucketServerPassword) throws IOException {
    return BitbucketServerClientTest.this.mockedJson;
   }
  });
 }

 @Test
 public void testPullRequestChanges() throws IOException {
  mockJson("pull-request-changes.json");
  List<String> actual = this.sut.pullRequestChanges();
  assertThat(actual).isNotEmpty();
  assertThat(actual.get(0)).isEqualTo("basic_branching/file.txt");
 }

 @Test
 public void testPullRequestComments() throws IOException {
  mockJson("pull-request-comments.json");
  List<BitbucketServerComment> actual = this.sut.pullRequestComments();
  assertThat(actual).isNotEmpty();
  assertThat(actual.get(0).getId()).isEqualTo(2);
  assertThat(actual.get(0).getText()).isEqualTo("in diff comment");
  assertThat(actual.get(0).getVersion()).isEqualTo(0);
 }

 @Test
 public void testPullRequestCommentsOnFile() throws IOException {
  mockJson("pull-request-comments.json");
  List<BitbucketServerComment> actual = this.sut.pullRequestComments("any/file.txt");
  assertThat(actual).isNotEmpty();
  assertThat(actual.get(0).getId()).isEqualTo(2);
  assertThat(actual.get(0).getText()).isEqualTo("in diff comment");
  assertThat(actual.get(0).getVersion()).isEqualTo(0);
 }

 private void mockJson(String resourceName) throws IOException {
  this.mockedJson = Resources.toString(Resources.getResource(resourceName), Charsets.UTF_8);
 }

}
