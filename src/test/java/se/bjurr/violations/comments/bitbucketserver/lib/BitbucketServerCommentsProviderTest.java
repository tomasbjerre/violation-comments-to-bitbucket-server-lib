package se.bjurr.violations.comments.bitbucketserver.lib;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerDiff;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.DIFFTYPE;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.DiffDestination;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.DiffHunk;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.Line;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.Segment;
import se.bjurr.violations.comments.lib.model.ChangedFile;

public class BitbucketServerCommentsProviderTest {

  private BitbucketServerCommentsProvider sut;

  @Before
  public void before() {
    sut = new BitbucketServerCommentsProvider();
  }

  @Test
  public void testThatDeletedFilesCanBeHandled() {
    ChangedFile changedFile = new ChangedFile("filename", null);
    List<BitbucketServerDiff> diffs = newArrayList();
    List<DiffHunk> hunks = newArrayList();
    List<Segment> segments = newArrayList();
    List<Line> lines = newArrayList();
    lines.add(new Line(10));
    segments.add(new Segment(DIFFTYPE.ADDED, lines));
    hunks.add(new DiffHunk(segments));
    BitbucketServerDiff diff = new BitbucketServerDiff(null, hunks);
    diffs.add(diff);

    Integer changedLine = 10;
    int context = 0;
    boolean actual = sut.shouldComment(changedFile, changedLine, context, diffs);
    assertThat(actual).isFalse();
  }

  @Test
  public void testThatOnlyAddedCanBeCommented() {
    ChangedFile changedFile = new ChangedFile("filename", null);
    List<BitbucketServerDiff> diffs = newArrayList();
    DiffDestination destination = new DiffDestination("filename");
    List<DiffHunk> hunks = newArrayList();
    List<Segment> segments = newArrayList();
    List<Line> lines = newArrayList();
    lines.add(new Line(10));
    segments.add(new Segment(DIFFTYPE.ADDED, lines));
    hunks.add(new DiffHunk(segments));
    BitbucketServerDiff diff = new BitbucketServerDiff(destination, hunks);
    diffs.add(diff);

    Integer changedLine = 10;
    int context = 0;
    boolean actual = sut.shouldComment(changedFile, changedLine, context, diffs);
    assertThat(actual).isTrue();

    changedLine = 11;
    context = 0;
    actual = sut.shouldComment(changedFile, changedLine, context, diffs);
    assertThat(actual).isFalse();

    changedLine = 11;
    context = 1;
    actual = sut.shouldComment(changedFile, changedLine, context, diffs);
    assertThat(actual).isTrue();

    changedLine = 9;
    context = 1;
    actual = sut.shouldComment(changedFile, changedLine, context, diffs);
    assertThat(actual).isTrue();
  }
}
