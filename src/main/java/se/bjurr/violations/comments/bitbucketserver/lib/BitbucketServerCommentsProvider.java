package se.bjurr.violations.comments.bitbucketserver.lib;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.cache.CacheBuilder.newBuilder;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.logging.Level.SEVERE;
import static se.bjurr.violations.comments.bitbucketserver.lib.client.model.DIFFTYPE.ADDED;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import se.bjurr.violations.comments.bitbucketserver.lib.client.BitbucketServerClient;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerComment;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerDiff;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerDiffResponse;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerTask;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.DiffDestination;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.DiffHunk;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.Line;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.Segment;
import se.bjurr.violations.comments.lib.CommentsProvider;
import se.bjurr.violations.comments.lib.ViolationsLogger;
import se.bjurr.violations.comments.lib.model.ChangedFile;
import se.bjurr.violations.comments.lib.model.Comment;

public class BitbucketServerCommentsProvider implements CommentsProvider {
  private static final Integer BITBUCKET_MAX_COMMENT_SIZE = 32767;

  private final BitbucketServerClient client;

  private final LoadingCache<String, BitbucketServerDiffResponse> diffResponse =
      newBuilder()
          .maximumSize(100)
          .expireAfterWrite(2, MINUTES)
          .build(
              new CacheLoader<String, BitbucketServerDiffResponse>() {
                @Override
                public BitbucketServerDiffResponse load(final String path) {
                  return client.pullRequestDiff(path);
                }
              });

  private final ViolationCommentsToBitbucketServerApi violationCommentsToBitbucketApi;
  private final ViolationsLogger violationsLogger;

  @VisibleForTesting
  BitbucketServerCommentsProvider() {
    client = null;
    violationCommentsToBitbucketApi = null;
    violationsLogger = null;
  }

  public BitbucketServerCommentsProvider(
      final ViolationCommentsToBitbucketServerApi violationCommentsToBitbucketApi,
      final ViolationsLogger violationsLogger) {
    this.violationsLogger = violationsLogger;
    final String bitbucketServerBaseUrl = violationCommentsToBitbucketApi.getBitbucketServerUrl();
    final String bitbucketServerProject = violationCommentsToBitbucketApi.getProjectKey();
    final String bitbucketServerRepo = violationCommentsToBitbucketApi.getRepoSlug();
    final Integer bitbucketServerPullRequestId = violationCommentsToBitbucketApi.getPullRequestId();
    final String bitbucketServerUser = violationCommentsToBitbucketApi.getUsername();
    final String bitbucketServerPassword = violationCommentsToBitbucketApi.getPassword();
    final String bitbucketPersonalAccessToken =
        violationCommentsToBitbucketApi.getPersonalAccessToken();
    final String proxyHostNameOrIp = violationCommentsToBitbucketApi.getProxyHostNameOrIp();
    final Integer proxyHostPort = violationCommentsToBitbucketApi.getProxyHostPort();
    final String proxyUser = violationCommentsToBitbucketApi.getProxyUser();
    final String proxyPassword = violationCommentsToBitbucketApi.getProxyPassword();
    client =
        new BitbucketServerClient(
            violationsLogger,
            bitbucketServerBaseUrl,
            bitbucketServerProject,
            bitbucketServerRepo,
            bitbucketServerPullRequestId,
            bitbucketServerUser,
            bitbucketServerPassword,
            bitbucketPersonalAccessToken,
            proxyHostNameOrIp,
            proxyHostPort,
            proxyUser,
            proxyPassword);
    this.violationCommentsToBitbucketApi = violationCommentsToBitbucketApi;
  }

  @Override
  public void createCommentWithAllSingleFileComments(final String comment) {
    client.pullRequestComment(comment);
  }

  @Override
  public void createSingleFileComment(
      final ChangedFile file, final Integer line, final String comment) {
    final BitbucketServerComment bitbucketComment =
        client.pullRequestComment(file.getFilename(), line, comment);

    if (violationCommentsToBitbucketApi.getCreateSingleFileCommentsTasks()) {
      client.commentCreateTask(bitbucketComment, file.getFilename(), line);
    }
  }

  @Override
  public List<Comment> getComments() {
    final List<Comment> comments = newArrayList();
    if (shouldCreateSingleFileComment()) {
      /**
       * This is time consuming to do and is only needed if we are creating comments on each file.
       */
      for (final String changedFile : client.pullRequestChanges()) {
        final List<BitbucketServerComment> bitbucketServerCommentsOnFile =
            client.pullRequestComments(changedFile);
        for (final BitbucketServerComment fileComment : bitbucketServerCommentsOnFile) {
          final List<String> specifics = newArrayList(fileComment.getVersion() + "", changedFile);
          comments.add(
              new Comment(fileComment.getId() + "", fileComment.getText(), null, specifics));
        }
      }
    }

    for (final BitbucketServerComment comment : client.pullRequestComments()) {
      final List<String> specifics = newArrayList(comment.getVersion() + "", "");
      comments.add(new Comment(comment.getId() + "", comment.getText(), null, specifics));
    }

    return comments;
  }

  @Override
  public List<ChangedFile> getFiles() {
    final List<ChangedFile> changedFiles = newArrayList();

    final List<String> bitbucketServerChangedFiles = client.pullRequestChanges();

    for (final String changedFile : bitbucketServerChangedFiles) {
      changedFiles.add(new ChangedFile(changedFile, new ArrayList<String>()));
    }

    return changedFiles;
  }

  @Override
  public void removeComments(final List<Comment> comments) {
    for (final Comment comment : comments) {
      Integer commentId = null;
      Integer commentVersion = null;
      try {
        commentId = Integer.valueOf(comment.getIdentifier());
        commentVersion = Integer.valueOf(comment.getSpecifics().get(0));

        final BitbucketServerComment bitbucketServerComment =
            client.pullRequestComment((long) commentId);
        removeComment(bitbucketServerComment);
      } catch (final Exception e) {
        violationsLogger.log(
            SEVERE, "Was unable to remove comment " + commentId + " " + commentVersion, e);
      }
    }
  }

  @Override
  public boolean shouldComment(final ChangedFile changedFile, final Integer changedLine) {
    if (!violationCommentsToBitbucketApi.getCommentOnlyChangedContent()) {
      return true;
    }
    final int context = violationCommentsToBitbucketApi.getCommentOnlyChangedContentContext();
    try {
      final List<BitbucketServerDiff> diffs =
          diffResponse.get(changedFile.getFilename()).getDiffs();
      return shouldComment(changedFile, changedLine, context, diffs);
    } catch (final Exception e) {
      violationsLogger.log(SEVERE, "Was unable to get diff from " + changedFile.getFilename(), e);
      return false;
    }
  }

  @VisibleForTesting
  boolean shouldComment(
      final ChangedFile changedFile,
      final Integer changedLine,
      final int context,
      final List<BitbucketServerDiff> diffs) {
    for (final BitbucketServerDiff diff : diffs) {
      final DiffDestination destination = diff.getDestination();
      if (destination != null) {
        final String destinationToString = destination.getToString();
        if (!isNullOrEmpty(destinationToString)) {
          if (destinationToString.equals(changedFile.getFilename())) {
            if (diff.getHunks() != null) {
              for (final DiffHunk hunk : diff.getHunks()) {
                for (final Segment segment : hunk.getSegments()) {
                  if (segment.getType() == ADDED) {
                    for (final Line line : segment.getLines()) {
                      if (line.getDestination() >= changedLine - context
                          && line.getDestination() <= changedLine + context) {
                        return true;
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    return false;
  }

  @Override
  public boolean shouldCreateCommentWithAllSingleFileComments() {
    return violationCommentsToBitbucketApi.getCreateCommentWithAllSingleFileComments();
  }

  @Override
  public boolean shouldCreateSingleFileComment() {
    return violationCommentsToBitbucketApi.getCreateSingleFileComments();
  }

  @Override
  public Optional<String> findCommentTemplate() {
    return violationCommentsToBitbucketApi.findCommentTemplate();
  }

  @Override
  public boolean shouldKeepOldComments() {
    return violationCommentsToBitbucketApi.getShouldKeepOldComments();
  }

  private void removeComment(final BitbucketServerComment comment) {
    final Deque<BitbucketServerComment> commentStack = new ArrayDeque<>();
    commentStack.add(comment);

    Collection<BitbucketServerComment> subComments = comment.getComments();
    while (subComments != null && !subComments.isEmpty()) {
      commentStack.addAll(subComments);

      final Collection<BitbucketServerComment> currentSubComments = subComments;
      subComments = new ArrayList<>();
      for (final BitbucketServerComment subComment : currentSubComments) {
        subComments.addAll(subComment.getComments());
      }
    }

    final Iterator<BitbucketServerComment> commentStackIt = commentStack.descendingIterator();
    while (commentStackIt.hasNext()) {
      final BitbucketServerComment stackComment = commentStackIt.next();

      removeTasks(stackComment);
      client.pullRequestRemoveComment(stackComment.getId(), stackComment.getVersion());
    }
  }

  private void removeTasks(final BitbucketServerComment comment) {
    final List<BitbucketServerTask> bitbucketServerTasks = comment.getTasks();

    for (final BitbucketServerTask bitbucketServerTask : bitbucketServerTasks) {
      client.removeTask(bitbucketServerTask);
    }
  }

  @Override
  public Integer getMaxNumberOfComments() {
    return BITBUCKET_MAX_COMMENT_SIZE;
  }

  @Override
  public Integer getMaxCommentSize() {
    return violationCommentsToBitbucketApi.getMaxCommentSize();
  }
}
