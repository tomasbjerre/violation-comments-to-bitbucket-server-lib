package se.bjurr.violations.comments.bitbucketserver.lib;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.ofNullable;
import static se.bjurr.violations.comments.lib.CommentsCreator.createComments;
import static se.bjurr.violations.lib.FilteringViolationsLogger.filterLevel;
import static se.bjurr.violations.lib.util.Utils.firstNonNull;
import static se.bjurr.violations.lib.util.Utils.isNullOrEmpty;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.bjurr.violations.comments.lib.CommentsProvider;
import se.bjurr.violations.lib.ViolationsLogger;
import se.bjurr.violations.lib.model.Violation;

public class ViolationCommentsToBitbucketServerApi {
  public static final String DEFAULT_PROP_VIOLATIONS_PASSWORD = "VIOLATIONS_PASSWORD";
  public static final String DEFAULT_PROP_VIOLATIONS_USERNAME = "VIOLATIONS_USERNAME";
  public static final String DEFAULT_PROP_PERSONAL_ACCESS_TOKEN = "VIOLATIONS_PAT";
  public static final String DEFAULT_PROP_KEYSTORE_PATH = "VIOLATIONS_KEYSTORE_PATH";
  public static final String DEFAULT_PROP_KEYSTORE_PASS = "VIOLATIONS_KEYSTORE_PASS";

  public static ViolationCommentsToBitbucketServerApi violationCommentsToBitbucketServerApi() {
    return new ViolationCommentsToBitbucketServerApi();
  }

  private String bitbucketServerUrl = null;
  private boolean createCommentWithAllSingleFileComments = false;
  private boolean createSingleFileComments = true;
  private boolean createSingleFileCommentsTasks = false;
  private String password;
  private String projectKey;
  private String propPassword = DEFAULT_PROP_VIOLATIONS_PASSWORD;
  private String propUsername = DEFAULT_PROP_VIOLATIONS_USERNAME;
  private String propPersonalAccessToken = DEFAULT_PROP_PERSONAL_ACCESS_TOKEN;
  private String propKeyStorePath = DEFAULT_PROP_KEYSTORE_PATH;
  private String propKeyStorePass = DEFAULT_PROP_KEYSTORE_PASS;
  private String keyStorePath;
  private String keyStorePass;
  private int pullRequestId;
  private String repoSlug;
  private String username;
  private Set<Violation> violations;
  private boolean commentOnlyChangedContent = false;
  private int commentOnlyChangedContentContext;
  private boolean shouldKeepOldComments;
  private String personalAccessToken;
  private String commentTemplate;
  private String proxyHostNameOrIp;
  private Integer proxyHostPort = 0;
  private String proxyUser;
  private String proxyPassword;
  private ViolationsLogger violationsLogger =
      filterLevel(
          new ViolationsLogger() {
            @Override
            public void log(final Level level, final String string) {
              Logger.getLogger(ViolationsLogger.class.getSimpleName()).log(level, string);
            }

            @Override
            public void log(final Level level, final String string, final Throwable t) {
              Logger.getLogger(ViolationsLogger.class.getSimpleName()).log(level, string, t);
            }
          });
  private Integer maxNumberOfViolations;
  private boolean shouldCommentOnlyChangedFiles = true;
  private List<String> ignorePaths;

  private ViolationCommentsToBitbucketServerApi() {}

  private void checkState() {
    final boolean noUsername = isNullOrEmpty(this.username) || isNullOrEmpty(this.password);
    final boolean noPat = isNullOrEmpty(this.personalAccessToken);
    final boolean noCert = isNullOrEmpty(this.keyStorePath);
    if (noUsername && noPat && noCert) {
      throw new IllegalStateException(
          "User and Password or personal access token, or keystore path and keystore pass, must be set! They can be set with the API or by setting properties.\n"
              + //
              "Username/password:\n"
              + //
              "-D"
              + DEFAULT_PROP_VIOLATIONS_USERNAME
              + "=theuser -D"
              + DEFAULT_PROP_VIOLATIONS_PASSWORD
              + "=thepassword"
              + //
              "\n\nPersonal access token:\n"
              + //
              "-D"
              + DEFAULT_PROP_PERSONAL_ACCESS_TOKEN
              + "=asdasd"
              + "\n\nKeystore path and pass"
              + //
              "-D"
              + DEFAULT_PROP_KEYSTORE_PATH
              + "=keystorepath -D"
              + DEFAULT_PROP_KEYSTORE_PASS
              + "=keystorepass");
    }
    checkNotNull(this.bitbucketServerUrl, "BitbucketServerURL");
    checkNotNull(this.pullRequestId, "PullRequestId");
    checkNotNull(this.repoSlug, "repoSlug");
    checkNotNull(this.projectKey, "projectKey");
  }

  public ViolationCommentsToBitbucketServerApi withViolationsLogger(
      final ViolationsLogger violationsLogger) {
    this.violationsLogger = violationsLogger;
    return this;
  }

  public String getBitbucketServerUrl() {
    return this.bitbucketServerUrl;
  }

  public boolean getCommentOnlyChangedContent() {
    return this.commentOnlyChangedContent;
  }

  public int getCommentOnlyChangedContentContext() {
    return this.commentOnlyChangedContentContext;
  }

  public boolean getCreateCommentWithAllSingleFileComments() {
    return this.createCommentWithAllSingleFileComments;
  }

  public boolean getCreateSingleFileComments() {
    return this.createSingleFileComments;
  }

  public boolean getCreateSingleFileCommentsTasks() {
    return this.createSingleFileCommentsTasks;
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

  public boolean getShouldKeepOldComments() {
    return this.shouldKeepOldComments;
  }

  public boolean getShouldCommentOnlyChangedFiles() {
    return this.shouldCommentOnlyChangedFiles;
  }

  public String getPersonalAccessToken() {
    return this.personalAccessToken;
  }

  public String getProxyHostNameOrIp() {
    return this.proxyHostNameOrIp;
  }

  public Integer getProxyHostPort() {
    return this.proxyHostPort;
  }

  public String getProxyUser() {
    return this.proxyUser;
  }

  public String getProxyPassword() {
    return this.proxyPassword;
  }

  public String getPropKeyStorePath() {
    return this.propKeyStorePath;
  }

  public String getPropKeyStorePass() {
    return this.propKeyStorePass;
  }

  public String getKeyStorePass() {
    return this.keyStorePass;
  }

  public String getKeyStorePath() {
    return this.keyStorePath;
  }

  private void populateFromEnvironmentVariables() {
    if (System.getProperty(this.propUsername) != null) {
      this.username = firstNonNull(this.username, System.getProperty(this.propUsername));
    }
    if (System.getProperty(this.propPassword) != null) {
      this.password = firstNonNull(this.password, System.getProperty(this.propPassword));
    }
    if (System.getProperty(this.propPassword) != null) {
      this.personalAccessToken =
          firstNonNull(this.personalAccessToken, System.getProperty(this.propPersonalAccessToken));
    }
    if (System.getProperty(this.propKeyStorePath) != null) {
      this.keyStorePath =
          firstNonNull(this.keyStorePath, System.getProperty(this.propKeyStorePath));
    }
    if (System.getProperty(this.propKeyStorePass) != null) {
      this.keyStorePass =
          firstNonNull(this.keyStorePass, System.getProperty(this.propKeyStorePass));
    }
  }

  public void toPullRequest() throws Exception {
    this.populateFromEnvironmentVariables();
    this.checkState();
    final CommentsProvider commentsProvider =
        new BitbucketServerCommentsProvider(this, this.violationsLogger);
    createComments(this.violationsLogger, this.violations, commentsProvider);
  }

  public ViolationCommentsToBitbucketServerApi withBitbucketServerUrl(
      final String bitbucketServerUrl) {
    this.bitbucketServerUrl = bitbucketServerUrl;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withCommentOnlyChangedContent(
      final boolean commentOnlyChangedContent) {
    this.commentOnlyChangedContent = commentOnlyChangedContent;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withCommentOnlyChangedContentContext(
      final int commentOnlyChangedContentContext) {
    this.commentOnlyChangedContentContext = commentOnlyChangedContentContext;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withCreateCommentWithAllSingleFileComments(
      final boolean createCommentWithAllSingleFileComments) {
    this.createCommentWithAllSingleFileComments = createCommentWithAllSingleFileComments;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withCreateSingleFileComments(
      final boolean createSingleFileComments) {
    this.createSingleFileComments = createSingleFileComments;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withCreateSingleFileCommentsTasks(
      final boolean createSingleFileCommentsTasks) {
    this.createSingleFileCommentsTasks = createSingleFileCommentsTasks;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withPassword(final String password) {
    this.password = password;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withPersonalAccessToken(
      final String personalAccessToken) {
    this.personalAccessToken = personalAccessToken;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withProjectKey(final String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  public void withPropPassword(final String envPassword) {
    this.propPassword = envPassword;
  }

  public void withPropUsername(final String envUsername) {
    this.propUsername = envUsername;
  }

  public ViolationCommentsToBitbucketServerApi withPropPersonalAccessToken(
      final String propPersonalAccessToken) {
    this.propPersonalAccessToken = propPersonalAccessToken;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withPullRequestId(final int pullRequestId) {
    this.pullRequestId = pullRequestId;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withRepoSlug(final String repoSlug) {
    this.repoSlug = repoSlug;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withUsername(final String username) {
    this.username = username;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withViolations(final Set<Violation> violations) {
    this.violations = violations;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withShouldKeepOldComments(
      final boolean shouldKeepOldComments) {
    this.shouldKeepOldComments = shouldKeepOldComments;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withShouldCommentOnlyChangedFiles(
      final boolean shouldCommentOnlyChangedFiles) {
    this.shouldCommentOnlyChangedFiles = shouldCommentOnlyChangedFiles;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withProxyHostNameOrIp(
      final String proxyHostNameOrIp) {
    this.proxyHostNameOrIp = proxyHostNameOrIp;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withProxyHostPort(final Integer proxyHostPort) {
    this.proxyHostPort = proxyHostPort;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withProxyUser(final String proxyUser) {
    this.proxyUser = proxyUser;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withProxyPassword(final String proxyPassword) {
    this.proxyPassword = proxyPassword;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withCommentTemplate(final String commentTemplate) {
    this.commentTemplate = commentTemplate;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withKeyStorePath(final String keyStorePath) {
    this.keyStorePath = keyStorePath;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withKeyStorePass(final String keyStorePass) {
    this.keyStorePass = keyStorePass;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withPropKeyStorePath(final String propKeyStorePath) {
    this.propKeyStorePath = propKeyStorePath;
    return this;
  }

  public ViolationCommentsToBitbucketServerApi withPropKeyStorePass(final String propKeyStorePass) {
    this.propKeyStorePass = propKeyStorePass;
    return this;
  }

  public Optional<String> findCommentTemplate() {
    return ofNullable(this.commentTemplate);
  }

  public ViolationCommentsToBitbucketServerApi withMaxNumberOfViolations(
      final Integer maxNumberOfViolations) {
    this.maxNumberOfViolations = maxNumberOfViolations;
    return this;
  }

  public Integer getMaxNumberOfViolations() {
    return this.maxNumberOfViolations;
  }

  public List<String> getIgnorePaths() {
    return this.ignorePaths;
  }

  public ViolationCommentsToBitbucketServerApi withIgnorePaths(final List<String> ignorePaths) {
    this.ignorePaths = ignorePaths;
    return this;
  }
}
