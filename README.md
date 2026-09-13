# Violation Comments to Bitbucket Server Lib

[![Maven Central](https://img.shields.io/maven-central/v/se.bjurr.violations/violation-comments-to-bitbucket-server-lib.svg?label=Maven%20Central)](https://search.maven.org/artifact/se.bjurr.violations/violation-comments-to-bitbucket-server-lib)

This is a library that adds violation comments from static code analysis to Bitbucket Server.

> **Note:** Starting with version `3.x.y`, this plugin requires Java 17 or later.

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

To build the code, have a look at `.github/workflows/gradle-ci.yaml`.

To do a release you need to do `./gradlew updateVersion && ./gradlew release`. More information [here](https://github.com/vanniktech/gradle-maven-publish-plugin/blob/main/docs/central.md#secrets).
