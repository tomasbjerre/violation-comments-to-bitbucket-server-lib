package se.bjurr.violations.comments.bitbucketserver.lib;

import static com.google.common.collect.Lists.newArrayList;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.bjurr.violations.comments.bitbucketserver.lib.client.BitbucketServerClient;
import se.bjurr.violations.comments.bitbucketserver.lib.client.model.BitbucketServerComment;
import se.bjurr.violations.comments.lib.model.ChangedFile;
import se.bjurr.violations.comments.lib.model.Comment;
import se.bjurr.violations.comments.lib.model.CommentsProvider;

public class BitbucketServerCommentsProvider implements CommentsProvider {

 private static final Logger LOG = LoggerFactory.getLogger(BitbucketServerCommentsProvider.class);

 private final BitbucketServerClient client;

 private final ViolationCommentsToBitbucketServerApi violationCommentsToBitbucketApi;

 public BitbucketServerCommentsProvider(ViolationCommentsToBitbucketServerApi violationCommentsToBitbucketApi) {
  String bitbucketServerBaseUrl = violationCommentsToBitbucketApi.getBitbucketServerUrl();
  String bitbucketServerProject = violationCommentsToBitbucketApi.getProjectKey();
  String bitbucketServerRepo = violationCommentsToBitbucketApi.getRepoSlug();
  Integer bitbucketServerPullRequestId = violationCommentsToBitbucketApi.getPullRequestId();
  String bitbucketServerUser = violationCommentsToBitbucketApi.getUsername();
  String bitbucketServerPassword = violationCommentsToBitbucketApi.getPassword();
  this.client = new BitbucketServerClient(bitbucketServerBaseUrl, bitbucketServerProject, bitbucketServerRepo,
    bitbucketServerPullRequestId, bitbucketServerUser, bitbucketServerPassword);
  this.violationCommentsToBitbucketApi = violationCommentsToBitbucketApi;
 }

 @Override
 public void createCommentWithAllSingleFileComments(String comment) {
  if (!this.violationCommentsToBitbucketApi.getCreateCommentWithAllSingleFileComments()) {
   return;
  }

  this.client.pullRequestComment(comment);
 }

 @Override
 public void createSingleFileComment(ChangedFile file, Integer line, String comment) {
  if (!this.violationCommentsToBitbucketApi.getCreateSingleFileComments()) {
   return;
  }

  this.client.pullRequestComment(file.getFilename(), line, comment);
 }

 @Override
 public List<Comment> getComments() {
  List<Comment> comments = newArrayList();
  for (String changedFile : this.client.pullRequestChanges()) {
   List<BitbucketServerComment> bitbucketServerCommentsOnFile = this.client.pullRequestComments(changedFile);
   for (BitbucketServerComment fileComment : bitbucketServerCommentsOnFile) {
    List<String> specifics = newArrayList(fileComment.getVersion() + "", changedFile);
    comments.add(new Comment(fileComment.getId() + "", fileComment.getText(), null, specifics));
   }
  }

  return comments;
 }

 @Override
 public List<ChangedFile> getFiles() {
  List<ChangedFile> changedFiles = newArrayList();

  List<String> bitbucketServerChangedFiles = this.client.pullRequestChanges();

  for (String changedFile : bitbucketServerChangedFiles) {
   changedFiles.add(new ChangedFile(changedFile, new ArrayList<String>()));
  }

  return changedFiles;
 }

 @Override
 public void removeComments(List<Comment> comments) {
  for (Comment comment : comments) {
   Integer commentId = null;
   Integer commentVersion = null;
   try {
    commentId = Integer.valueOf(comment.getIdentifier());
    commentVersion = Integer.valueOf(comment.getSpecifics().get(0));
    this.client.pullRequestRemoveComment(commentId, commentVersion);
   } catch (Exception e) {
    LOG.warn("Was unable to remove comment " + commentId + " " + commentVersion, e);
   }
  }
 }
}
