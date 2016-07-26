package se.bjurr.violations.comments.bitbucketserver.lib;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import se.bjurr.violations.comments.bitbucketserver.lib.client.BitbucketServerClient;
import se.bjurr.violations.comments.bitbucketserver.lib.client.BitbucketServerComment;
import se.bjurr.violations.comments.lib.model.ChangedFile;
import se.bjurr.violations.comments.lib.model.Comment;
import se.bjurr.violations.comments.lib.model.CommentsProvider;

public class BitbucketServerCommentsProvider implements CommentsProvider {

 private final BitbucketServerClient client;

 private ViolationCommentsToBitbucketServerApi violationCommentsToGitHubApi;

 public BitbucketServerCommentsProvider(ViolationCommentsToBitbucketServerApi violationCommentsToBitbucketApi) {
  String bitbucketServerBaseUrl = violationCommentsToBitbucketApi.getBitbucketServerUrl();
  String bitbucketServerProject = violationCommentsToBitbucketApi.getProjectKey();
  String bitbucketServerRepo = violationCommentsToBitbucketApi.getRepoSlug();
  Integer bitbucketServerPullRequestId = violationCommentsToBitbucketApi.getPullRequestId();
  String bitbucketServerUser = violationCommentsToBitbucketApi.getUsername();
  String bitbucketServerPassword = violationCommentsToBitbucketApi.getPassword();
  this.client = new BitbucketServerClient(bitbucketServerBaseUrl, bitbucketServerProject, bitbucketServerRepo,
    bitbucketServerPullRequestId, bitbucketServerUser, bitbucketServerPassword);
 }

 @Override
 public void createCommentWithAllSingleFileComments(String comment) {
  if (!this.violationCommentsToGitHubApi.getCreateCommentWithAllSingleFileComments()) {
   return;
  }

  try {
   this.client.pullRequestComment(comment);
  } catch (IOException e) {
   throw propagate(e);
  }
 }

 @Override
 public void createSingleFileComment(ChangedFile file, Integer line, String comment) {
  if (!this.violationCommentsToGitHubApi.getCreateSingleFileComments()) {
   return;
  }

  try {
   this.client.pullRequestComment(file.getFilename(), line, comment);
  } catch (IOException e) {
   propagate(e);
  }
 }

 @Override
 public List<Comment> getComments() {
  List<Comment> comments = newArrayList();
  try {

   List<BitbucketServerComment> bitbucketServerCommentsOnPR = this.client.pullRequestComments();
   for (BitbucketServerComment pullRequestComment : bitbucketServerCommentsOnPR) {
    List<String> specifics = newArrayList(pullRequestComment.getVersion() + "");
    comments.add(new Comment(pullRequestComment.getId() + "", pullRequestComment.getText(), null, specifics));
   }

   for (String changedFile : this.client.pullRequestChanges()) {
    List<BitbucketServerComment> bitbucketServerCommentsOnFile = this.client.pullRequestComments(changedFile);
    for (BitbucketServerComment fileComment : bitbucketServerCommentsOnFile) {
     List<String> specifics = newArrayList(fileComment.getVersion() + "", changedFile);
     comments.add(new Comment(fileComment.getId() + "", fileComment.getText(), null, specifics));
    }
   }
  } catch (IOException e) {
   throw propagate(e);
  }

  return comments;
 }

 @Override
 public List<ChangedFile> getFiles() {
  try {
   List<ChangedFile> changedFiles = newArrayList();

   List<String> bitbucketServerChangedFiles = this.client.pullRequestChanges();

   for (String changedFile : bitbucketServerChangedFiles) {
    changedFiles.add(new ChangedFile(changedFile, new ArrayList<String>()));
   }

   return changedFiles;
  } catch (IOException e) {
   throw propagate(e);
  }
 }

 @Override
 public void removeComments(List<Comment> comments) {
  for (Comment comment : comments) {
   try {
    Integer commentId = Integer.valueOf(comment.getIdentifier());
    Integer commentVersion = Integer.valueOf(comment.getSpecifics().get(0));
    this.client.pullRequestRemoveComment(commentId, commentVersion);
   } catch (Exception e) {
    throw propagate(e);
   }
  }
 }
}
