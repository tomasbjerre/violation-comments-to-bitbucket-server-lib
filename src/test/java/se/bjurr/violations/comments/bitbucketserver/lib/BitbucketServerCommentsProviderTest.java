package se.bjurr.violations.comments.bitbucketserver.lib;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerComment;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerDiff;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerTask;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.DIFFTYPE;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.DiffDestination;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.DiffHunk;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.Line;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.Segment;
import se.bjurr.violations.comments.lib.model.ChangedFile;

public class BitbucketServerCommentsProviderTest {

  private BitbucketServerCommentsProvider sut;

  @BeforeEach
  public void before() {
    sut = new BitbucketServerCommentsProvider();
  }

  @Test
  public void testThatDeletedFilesCanBeHandled() {
    ChangedFile changedFile = new ChangedFile("filename", null);
    List<BitbucketServerDiff> diffs = new ArrayList<>();
    List<DiffHunk> hunks = new ArrayList<>();
    List<Segment> segments = new ArrayList<>();
    List<Line> lines = new ArrayList<>();
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
    List<BitbucketServerDiff> diffs = new ArrayList<>();
    DiffDestination destination = new DiffDestination("filename");
    List<DiffHunk> hunks = new ArrayList<>();
    List<Segment> segments = new ArrayList<>();
    List<Line> lines = new ArrayList<>();
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

  @Test
  public void testAnyTaskInThreadIsFalseWhenNoCommentHasATask() {
    BitbucketServerComment parent = new BitbucketServerComment(0, "parent", 1);
    BitbucketServerComment reply = new BitbucketServerComment(0, "reply", 2);
    parent.setComments(List.of(reply));

    Deque<BitbucketServerComment> thread = BitbucketServerCommentsProvider.flattenThread(parent);

    assertThat(BitbucketServerCommentsProvider.anyTaskInThread(thread)) //
        .isFalse();
  }

  @Test
  public void testAnyTaskInThreadIsTrueWhenTheParentHasATask() {
    BitbucketServerComment parent = new BitbucketServerComment(0, "parent", 1);
    parent.setTasks(List.of(new BitbucketServerTask(10, "follow up")));

    Deque<BitbucketServerComment> thread = BitbucketServerCommentsProvider.flattenThread(parent);

    assertThat(BitbucketServerCommentsProvider.anyTaskInThread(thread)) //
        .isTrue();
  }

  @Test
  public void testAnyTaskInThreadIsTrueWhenAUserConvertedAReplyIntoATask() {
    // The tool never created a task - a user did, via the Bitbucket Server UI, on a reply. The
    // comment we fetch live from the API reflects that regardless of any tool-side config.
    BitbucketServerComment parent = new BitbucketServerComment(0, "parent", 1);
    BitbucketServerComment reply = new BitbucketServerComment(0, "reply", 2);
    reply.setTasks(List.of(new BitbucketServerTask(11, "please double check this")));
    parent.setComments(List.of(reply));

    Deque<BitbucketServerComment> thread = BitbucketServerCommentsProvider.flattenThread(parent);

    assertThat(thread) //
        .hasSize(2);
    assertThat(BitbucketServerCommentsProvider.anyTaskInThread(thread)) //
        .isTrue();
  }
}
