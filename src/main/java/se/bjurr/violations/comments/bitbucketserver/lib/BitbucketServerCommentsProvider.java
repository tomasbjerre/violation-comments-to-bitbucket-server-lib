package se.bjurr.violations.comments.bitbucketserver.lib;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.SEVERE;
import static se.bjurr.violations.comments.bitbucketserver.lib.client.model.DIFFTYPE.ADDED;
import static se.bjurr.violations.lib.util.Utils.isNullOrEmpty;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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
import se.bjurr.violations.comments.lib.model.ChangedFile;
import se.bjurr.violations.comments.lib.model.Comment;
import se.bjurr.violations.lib.ViolationsLogger;
import se.bjurr.violations.lib.reports.ReportsFinder;

public class BitbucketServerCommentsProvider implements CommentsProvider {
  private static final Integer BITBUCKET_MAX_COMMENT_SIZE = 32_767;

  private final BitbucketServerClient client;

  private final ExpiringCache<String, BitbucketServerDiffResponse> diffResponse;

  private final ViolationCommentsToBitbucketServerApi violationCommentsToBitbucketApi;
  private final ViolationsLogger violationsLogger;

  BitbucketServerCommentsProvider() {
    this.client = null;
    this.diffResponse =
        new ExpiringCache<>(100, 2, MINUTES, path -> this.client.pullRequestDiff(path));
    this.violationCommentsToBitbucketApi = null;
    this.violationsLogger = null;
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
    final String keyStorePath = violationCommentsToBitbucketApi.getKeyStorePath();
    final String keyStorePass = violationCommentsToBitbucketApi.getKeyStorePass();
    this.client =
        new BitbucketServerClient(
            violationsLogger,
            bitbucketServerBaseUrl,
            bitbucketServerProject,
            bitbucketServerRepo,
            bitbucketServerPullRequestId,
            bitbucketServerUser,
            bitbucketServerPassword,
            bitbucketPersonalAccessToken,
            keyStorePath,
            keyStorePass,
            proxyHostNameOrIp,
            proxyHostPort,
            proxyUser,
            proxyPassword);
    this.diffResponse =
        new ExpiringCache<>(100, 2, MINUTES, path -> this.client.pullRequestDiff(path));
    this.violationCommentsToBitbucketApi = violationCommentsToBitbucketApi;
  }

  @Override
  public void createComment(final String comment) {
    this.client.pullRequestComment(comment);
  }

  @Override
  public void createSingleFileComment(
      final ChangedFile file, final Integer line, final String comment) {
    final BitbucketServerComment bitbucketComment =
        this.client.pullRequestComment(file.getFilename(), line, comment);

    if (this.violationCommentsToBitbucketApi.getCreateSingleFileCommentsTasks()) {
      this.client.commentCreateTask(bitbucketComment, file.getFilename(), line);
    }
  }

  @Override
  public List<Comment> getComments() {
    final Map<Integer, Comment> comments = new HashMap<>();
    if (this.shouldCreateSingleFileComment()) {
      /**
       * This is time consuming to do and is only needed if we are creating comments on each file.
       */
      for (final String changedFile : this.client.pullRequestChanges()) {
        final List<String> ignorePaths = this.violationCommentsToBitbucketApi.getIgnorePaths();
        if (ReportsFinder.isIgnored(changedFile, ignorePaths)) {
          this.violationsLogger.log(
              INFO,
              "Ignoring changed file "
                  + changedFile
                  + " because it matched ignored paths: "
                  + ignorePaths);
          continue;
        }
        final List<BitbucketServerComment> bitbucketServerCommentsOnFile =
            this.client.pullRequestComments(changedFile);
        for (final BitbucketServerComment fileComment : bitbucketServerCommentsOnFile) {
          final List<String> specifics = List.of(fileComment.getVersion() + "", changedFile);
          final Comment comment =
              new Comment(fileComment.getId() + "", fileComment.getText(), null, specifics);
          comments.put(fileComment.getId(), comment);
        }
      }
    }

    for (final BitbucketServerComment comment : this.client.pullRequestComments()) {
      final List<String> specifics = List.of(comment.getVersion() + "", "");
      if (!comments.containsKey(comment.getId())) {
        comments.put(
            comment.getId(), new Comment(comment.getId() + "", comment.getText(), null, specifics));
      }
    }

    return new ArrayList<>(comments.values());
  }

  @Override
  public List<ChangedFile> getFiles() {
    final List<ChangedFile> changedFiles = new ArrayList<>();

    final List<String> bitbucketServerChangedFiles = this.client.pullRequestChanges();

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
            this.client.pullRequestComment((long) commentId);
        this.removeComment(bitbucketServerComment);
      } catch (final Exception e) {
        this.violationsLogger.log(
            SEVERE, "Was unable to remove comment " + commentId + " " + commentVersion, e);
      }
    }
  }

  @Override
  public boolean shouldComment(final ChangedFile changedFile, final Integer changedLine) {
    if (!this.violationCommentsToBitbucketApi.getCommentOnlyChangedContent()) {
      return true;
    }
    final int context = this.violationCommentsToBitbucketApi.getCommentOnlyChangedContentContext();
    try {
      final List<BitbucketServerDiff> diffs =
          this.diffResponse.get(changedFile.getFilename()).getDiffs();
      return this.shouldComment(changedFile, changedLine, context, diffs);
    } catch (final Exception e) {
      this.violationsLogger.log(
          SEVERE, "Was unable to get diff from " + changedFile.getFilename(), e);
      return false;
    }
  }

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
    return this.violationCommentsToBitbucketApi.getCreateCommentWithAllSingleFileComments();
  }

  @Override
  public boolean shouldCreateSingleFileComment() {
    return this.violationCommentsToBitbucketApi.getCreateSingleFileComments();
  }

  @Override
  public Optional<String> findCommentTemplate() {
    return this.violationCommentsToBitbucketApi.findCommentTemplate();
  }

  @Override
  public boolean shouldKeepOldComments() {
    return this.violationCommentsToBitbucketApi.getShouldKeepOldComments();
  }

  @Override
  public boolean shouldCommentOnlyChangedFiles() {
    return this.violationCommentsToBitbucketApi.getShouldCommentOnlyChangedFiles();
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

      this.removeTasks(stackComment);
      this.client.pullRequestRemoveComment(stackComment.getId(), stackComment.getVersion());
    }
  }

  private void removeTasks(final BitbucketServerComment comment) {
    final List<BitbucketServerTask> bitbucketServerTasks = comment.getTasks();

    for (final BitbucketServerTask bitbucketServerTask : bitbucketServerTasks) {
      this.client.removeTask(bitbucketServerTask);
    }
  }

  @Override
  public Integer getMaxCommentSize() {
    return BITBUCKET_MAX_COMMENT_SIZE;
  }

  @Override
  public Integer getMaxNumberOfViolations() {
    return this.violationCommentsToBitbucketApi.getMaxNumberOfViolations();
  }

  /** A size-bounded, time-expiring memoizing cache. */
  private static final class ExpiringCache<K, V> {
    private final long ttlMillis;
    private final Function<K, V> loader;
    private final Map<K, Entry<V>> entries;

    ExpiringCache(
        final int maximumSize,
        final long ttl,
        final TimeUnit ttlUnit,
        final Function<K, V> loader) {
      this.ttlMillis = ttlUnit.toMillis(ttl);
      this.loader = loader;
      this.entries =
          new LinkedHashMap<K, Entry<V>>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(final Map.Entry<K, Entry<V>> eldest) {
              return this.size() > maximumSize;
            }
          };
    }

    synchronized V get(final K key) {
      final Entry<V> cached = this.entries.get(key);
      final long now = System.currentTimeMillis();
      if (cached != null && now - cached.createdAt < this.ttlMillis) {
        return cached.value;
      }
      final V value = this.loader.apply(key);
      this.entries.put(key, new Entry<>(value, now));
      return value;
    }

    private static final class Entry<V> {
      private final V value;
      private final long createdAt;

      Entry(final V value, final long createdAt) {
        this.value = value;
        this.createdAt = createdAt;
      }
    }
  }
}
