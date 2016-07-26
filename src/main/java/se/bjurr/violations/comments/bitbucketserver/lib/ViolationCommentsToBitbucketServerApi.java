package se.bjurr.violations.comments.bitbucketserver.lib;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;
import static se.bjurr.violations.comments.lib.CommentsCreator.createComments;

import java.util.List;

import se.bjurr.violations.comments.lib.model.CommentsProvider;
import se.bjurr.violations.lib.model.Violation;

public class ViolationCommentsToBitbucketServerApi {
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

 public String getBitbucketServerUrl() {
  return this.bitbucketServerUrl;
 }

 public boolean getCreateCommentWithAllSingleFileComments() {
  return this.createCommentWithAllSingleFileComments;
 }

 public boolean getCreateSingleFileComments() {
  return this.createSingleFileComments;
 }

 public String getPassword() {
  return this.password;
 }

 public String getProjectKey() {
  return this.projectKey;
 }

 public String getPropPassword() {
  return this.propPassword;
 }

 public String getPropUsername() {
  return this.propUsername;
 }

 public int getPullRequestId() {
  return this.pullRequestId;
 }

 public String getRepoSlug() {
  return this.repoSlug;
 }

 public String getUsername() {
  return this.username;
 }

 public void toPullRequest() throws Exception {
  populateFromEnvironmentVariables();
  checkState();
  CommentsProvider commentsProvider = new BitbucketServerCommentsProvider(this);
  createComments(commentsProvider, this.violations);
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
  this.propPassword = envPassword;
 }

 public void withPropUsername(String envUsername) {
  this.propUsername = envUsername;
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

 private void checkState() {
  if (this.username == null || this.password == null) {
   throw new IllegalStateException(
     "User and Password must be set! They can be set with the API or by setting properties.\n" + //
       "Username/password:\n" + //
       "-D" + DEFAULT_PROP_VIOLATIONS_USERNAME + "=theuser -D" + DEFAULT_PROP_VIOLATIONS_PASSWORD + "=thepassword");
  }
  checkNotNull(this.bitbucketServerUrl, "BitbucketServerURL");
  checkNotNull(this.pullRequestId, "PullRequestId");
  checkNotNull(this.repoSlug, "repoSlug");
  checkNotNull(this.projectKey, "projectKey");
 }

 private void populateFromEnvironmentVariables() {
  if (System.getProperty(this.propUsername) != null) {
   this.username = firstNonNull(this.username, System.getProperty(this.propUsername));
  }
  if (System.getProperty(this.propPassword) != null) {
   this.password = firstNonNull(this.password, System.getProperty(this.propPassword));
  }
 }
}
