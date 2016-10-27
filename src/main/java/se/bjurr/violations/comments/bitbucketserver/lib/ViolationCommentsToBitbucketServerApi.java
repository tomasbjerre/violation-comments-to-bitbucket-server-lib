package se.bjurr.violations.comments.bitbucketserver.lib;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;
import static se.bjurr.violations.comments.lib.CommentsCreator.createComments;

import java.util.List;

import se.bjurr.violations.comments.lib.model.CommentsProvider;
import se.bjurr.violations.lib.model.Violation;

public class ViolationCommentsToBitbucketServerApi {
 private static final Integer BITBUCKET_MAX_COMMENT_SIZE = 32767;
 public static final String DEFAULT_PROP_VIOLATIONS_PASSWORD = "VIOLATIONS_PASSWORD";
 public static final String DEFAULT_PROP_VIOLATIONS_USERNAME = "VIOLATIONS_USERNAME";

 public static ViolationCommentsToBitbucketServerApi violationCommentsToBitbucketServerApi() {
  return new ViolationCommentsToBitbucketServerApi();
 }

 private String bitbucketServerUrl = null;
 private boolean createCommentWithAllSingleFileComments = false;
 private boolean createSingleFileComments = true;
 private String password;
 private String projectKey;
 private String propPassword = DEFAULT_PROP_VIOLATIONS_PASSWORD;
 private String propUsername = DEFAULT_PROP_VIOLATIONS_USERNAME;
 private int pullRequestId;
 private String repoSlug;
 private String username;
 private List<Violation> violations;

 private ViolationCommentsToBitbucketServerApi() {

 }

 private void checkState() {
  if (username == null || password == null) {
   throw new IllegalStateException(
     "User and Password must be set! They can be set with the API or by setting properties.\n" + //
       "Username/password:\n" + //
       "-D" + DEFAULT_PROP_VIOLATIONS_USERNAME + "=theuser -D" + DEFAULT_PROP_VIOLATIONS_PASSWORD + "=thepassword");
  }
  checkNotNull(bitbucketServerUrl, "BitbucketServerURL");
  checkNotNull(pullRequestId, "PullRequestId");
  checkNotNull(repoSlug, "repoSlug");
  checkNotNull(projectKey, "projectKey");
 }

 public String getBitbucketServerUrl() {
  return bitbucketServerUrl;
 }

 public boolean getCreateCommentWithAllSingleFileComments() {
  return createCommentWithAllSingleFileComments;
 }

 public boolean getCreateSingleFileComments() {
  return createSingleFileComments;
 }

 public String getPassword() {
  return password;
 }

 public String getProjectKey() {
  return projectKey;
 }

 public String getPropPassword() {
  return propPassword;
 }

 public String getPropUsername() {
  return propUsername;
 }

 public int getPullRequestId() {
  return pullRequestId;
 }

 public String getRepoSlug() {
  return repoSlug;
 }

 public String getUsername() {
  return username;
 }

 private void populateFromEnvironmentVariables() {
  if (System.getProperty(propUsername) != null) {
   username = firstNonNull(username, System.getProperty(propUsername));
  }
  if (System.getProperty(propPassword) != null) {
   password = firstNonNull(password, System.getProperty(propPassword));
  }
 }

 public void toPullRequest() throws Exception {
  populateFromEnvironmentVariables();
  checkState();
  CommentsProvider commentsProvider = new BitbucketServerCommentsProvider(this);
  createComments(commentsProvider, violations, BITBUCKET_MAX_COMMENT_SIZE);
 }

 public ViolationCommentsToBitbucketServerApi withBitbucketServerUrl(String bitbucketServerUrl) {
  this.bitbucketServerUrl = bitbucketServerUrl;
  return this;
 }

 public ViolationCommentsToBitbucketServerApi withCreateCommentWithAllSingleFileComments(
   boolean createCommentWithAllSingleFileComments) {
  this.createCommentWithAllSingleFileComments = createCommentWithAllSingleFileComments;
  return this;
 }

 public ViolationCommentsToBitbucketServerApi withCreateSingleFileComments(boolean createSingleFileComments) {
  this.createSingleFileComments = createSingleFileComments;
  return this;
 }

 public ViolationCommentsToBitbucketServerApi withPassword(String password) {
  this.password = password;
  return this;
 }

 public ViolationCommentsToBitbucketServerApi withProjectKey(String projectKey) {
  this.projectKey = projectKey;
  return this;
 }

 public void withPropPassword(String envPassword) {
  propPassword = envPassword;
 }

 public void withPropUsername(String envUsername) {
  propUsername = envUsername;
 }

 public ViolationCommentsToBitbucketServerApi withPullRequestId(int pullRequestId) {
  this.pullRequestId = pullRequestId;
  return this;
 }

 public ViolationCommentsToBitbucketServerApi withRepoSlug(String repoSlug) {
  this.repoSlug = repoSlug;
  return this;
 }

 public ViolationCommentsToBitbucketServerApi withUsername(String username) {
  this.username = username;
  return this;
 }

 public ViolationCommentsToBitbucketServerApi withViolations(List<Violation> violations) {
  this.violations = violations;
  return this;
 }
}
