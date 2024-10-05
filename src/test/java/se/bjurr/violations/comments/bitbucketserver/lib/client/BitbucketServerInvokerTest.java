package se.bjurr.violations.comments.bitbucketserver.lib.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.Test;

public class BitbucketServerInvokerTest {

  @Test
  public void testThatUrlIsEscaped() throws Exception {
    final String givenUrl =
        "http://bitbucket.x.int:7990/rest/api/1.0/projects/x/repos/ios-prototype/pull-requests/7920/diff/MB/MB Plugins/x.swift";

    final BitbucketServerInvoker sut = new BitbucketServerInvoker();
    final URI actual = sut.convertToURIEscapingIllegalCharacters(givenUrl);

    assertThat(actual.toString())
        .isEqualTo(
            "http://bitbucket.x.int:7990/rest/api/1.0/projects/x/repos/ios-prototype/pull-requests/7920/diff/MB/MB%20Plugins/x.swift");
  }
}
