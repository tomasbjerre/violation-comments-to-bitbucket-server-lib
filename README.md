# Violation Comments to Bitbucket Server Lib [![Build Status](https://travis-ci.org/tomasbjerre/violation-comments-to-bitbucket-server-lib.svg?branch=master)](https://travis-ci.org/tomasbjerre/violation-comments-to-bitbucket-server-lib) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/se.bjurr.violations/violation-comments-to-bitbucket-server-lib/badge.svg)](https://maven-badges.herokuapp.com/maven-central/se.bjurr.violations/violation-comments-to-bitbucket-server-lib) [ ![Bintray](https://api.bintray.com/packages/tomasbjerre/tomasbjerre/se.bjurr.violations%3Aviolation-comments-to-bitbucket-server-lib/images/download.svg) ](https://bintray.com/tomasbjerre/tomasbjerre/se.bjurr.violations%3Aviolation-comments-to-bitbucket-server-lib/_latestVersion)

This is a library that adds violation comments from static code analysis to Bitbucket Server.

It uses [Violation Comments Lib](https://github.com/tomasbjerre/violation-comments-lib) and supports the same formats as [Violations Lib](https://github.com/tomasbjerre/violations-lib).
 
Very easy to use with a nice builder pattern
```
  violationCommentsToBitbucketServerApi() //
    .withViolations(".*/findbugs/.*\\.xml$", FINDBUGS, rootFolder) //
    .withViolations(".*/checkstyle/.*\\.xml$", CHECKSTYLE, rootFolder) //
    .withUsername("username")
    .withPassword("password")
    .withProjectKey("projectKey")
    .withRepoSlug("repoSlug")
    .withPullRequestId("pullRequestId")
    .toPullRequest();
```

## Usage
This software can be used:
 * With a [Jenkins plugin](https://github.com/jenkinsci/violation-comments-to-stash-plugin).
 * From [Command Line](https://github.com/tomasbjerre/violation-comments-to-bitbucket-server-command-line)

### Properties

It can be configured with some Java properties:

 * `VIOLATIONS_KEYSTORE_PATH` - A path to a keystore.
 * `VIOLATIONS_KEYSTORE_PASS` - Password for the keystore. 
 * `VIOLATIONS_PAT` - Personal access token used to authenticate.
 * `VIOLATIONS_USERNAME` - Username to authenticate with.
 * `VIOLATIONS_PASSWORD` - Password to authenticate with.

## Developer instructions

To build the code, have a look at `.travis.yml`.

To do a release you need to do `./gradlew release` and release the artifact from [staging](https://oss.sonatype.org/#stagingRepositories). More information [here](http://central.sonatype.org/pages/releasing-the-deployment.html).
